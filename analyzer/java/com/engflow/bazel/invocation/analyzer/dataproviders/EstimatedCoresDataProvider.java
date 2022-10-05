/*
 * Copyright 2022 EngFlow Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.engflow.bazel.invocation.analyzer.dataproviders;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.google.common.annotations.VisibleForTesting;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link DataProvider} that estimates different values around the number of cores available and
 * used when the Bazel profile was created:
 * <li>how many cores seem to have been available
 * <li>how many cores seem to have been used
 * <li>what value the flag `--jobs` may have been set to, which influences the number of concurrent
 *     jobs Bazel can run in the execution phase. <br>
 *     No differentiation is made between physical and virtual cores.
 */
public class EstimatedCoresDataProvider extends DataProvider {
  private static final Pattern SKYFRAME_EVALUATOR_REGEX =
      Pattern.compile("skyframe-evaluator[^\\d]*(\\d*)");

  private Set<Integer> executionPhaseSkyframeEvaluators;
  private Integer executionPhaseSkyframeEvaluatorsMaxValue;
  private Set<Integer> evaluateAndDependenciesPhaseSkyframeEvaluators;
  private Integer evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue;

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            EstimatedCoresUsed.class, DatumSupplier.memoized(this::getEstimatedCoresUsed)),
        DatumSupplierSpecification.of(
            EstimatedCoresAvailable.class,
            DatumSupplier.memoized(this::getEstimatedCoresAvailable)),
        DatumSupplierSpecification.of(
            EstimatedJobsFlagValue.class, DatumSupplier.memoized(this::getEstimatedFlagValueJobs)));
  }

  @VisibleForTesting
  EstimatedJobsFlagValue getEstimatedFlagValueJobs()
      throws MissingInputException, InvalidProfileException {
    determineEstimatedCoresAvailable();
    determineEstimatedCoresUsed();
    return new EstimatedJobsFlagValue(
        executionPhaseSkyframeEvaluatorsMaxValue + 1,
        !executionPhaseSkyframeEvaluatorsMaxValue.equals(
            evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue));
  }

  @VisibleForTesting
  EstimatedCoresAvailable getEstimatedCoresAvailable()
      throws MissingInputException, InvalidProfileException {
    determineEstimatedCoresAvailable();
    return new EstimatedCoresAvailable(
        evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue + 1,
        getGaps(
            evaluateAndDependenciesPhaseSkyframeEvaluators,
            evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue));
  }

  @VisibleForTesting
  EstimatedCoresUsed getEstimatedCoresUsed() throws MissingInputException, InvalidProfileException {
    determineEstimatedCoresUsed();
    return new EstimatedCoresUsed(
        executionPhaseSkyframeEvaluators.size(),
        getGaps(executionPhaseSkyframeEvaluators, executionPhaseSkyframeEvaluatorsMaxValue));
  }

  /**
   * skyframe-evaluators are numbered starting with 0. If some are not used, they may be dropped
   * from the profile, which leads to gaps in the extracted list of numbered skyframe-evaluators.
   * This method returns how many such skyframe-evaluators are not present in the profile.<br>
   * It assumes the maxValue passed in is the largest value present in the set of values.
   */
  @VisibleForTesting
  int getGaps(Set<Integer> values, Integer maxValue) {
    return (maxValue + 1) - values.size();
  }

  private synchronized void determineEstimatedCoresAvailable()
      throws MissingInputException, InvalidProfileException {
    if (evaluateAndDependenciesPhaseSkyframeEvaluators == null) {
      BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
      BazelPhaseDescriptions bazelPhaseDescriptions =
          getDataManager().getDatum(BazelPhaseDescriptions.class);
      // Ideally, evaluate only events within the target pattern evaluation and dependency analysis
      // phases. If the phase markers are not present, extend the range only as much as necessary.
      Timestamp start =
          bazelPhaseDescriptions.getOrClosestBefore(BazelProfilePhase.EVALUATE).getStart();
      Timestamp end =
          bazelPhaseDescriptions.getOrClosestAfter(BazelProfilePhase.DEPENDENCIES).getEnd();
      evaluateAndDependenciesPhaseSkyframeEvaluators =
          getSkyframeEvaluators(bazelProfile, start, end);
      evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue =
          evaluateAndDependenciesPhaseSkyframeEvaluators.stream().max(Integer::compareTo).get();
    }
  }

  private synchronized void determineEstimatedCoresUsed()
      throws MissingInputException, InvalidProfileException {
    if (executionPhaseSkyframeEvaluators == null) {
      BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
      BazelPhaseDescriptions bazelPhaseDescriptions =
          getDataManager().getDatum(BazelPhaseDescriptions.class);
      // Ideally, evaluate only threads with events in the execution phase. If the phase marker is
      // not present, extend the range only as much as necessary.
      Timestamp start =
          bazelPhaseDescriptions.getOrClosestBefore(BazelProfilePhase.EXECUTE).getStart();
      Timestamp end = bazelPhaseDescriptions.getOrClosestAfter(BazelProfilePhase.EXECUTE).getEnd();
      executionPhaseSkyframeEvaluators = getSkyframeEvaluators(bazelProfile, start, end);
      executionPhaseSkyframeEvaluatorsMaxValue =
          executionPhaseSkyframeEvaluators.stream().max(Integer::compareTo).get();
    }
  }

  private static Set<Integer> getSkyframeEvaluators(
      BazelProfile bazelProfile, Timestamp start, Timestamp end) throws InvalidProfileException {
    Set<Integer> result = new HashSet<>();
    bazelProfile
        .getThreads()
        // Consider only threads from the relevant time range.
        .filter(
            thread ->
                thread.getCompleteEvents().stream()
                    .anyMatch(
                        completeEvent ->
                            completeEvent.start.compareTo(start) >= 0
                                && completeEvent.end.compareTo(end) <= 0))
        // Each core should have at least one of "skyframe-evaluator [x]",
        // "skyframe-evaluator-[x]", and "skyframe-evaluator-cpu-heavy-[x]".
        .filter(thread -> SKYFRAME_EVALUATOR_REGEX.matcher(thread.getName()).matches())
        .map(
            thread -> {
              Matcher matcher = SKYFRAME_EVALUATOR_REGEX.matcher(thread.getName());
              matcher.find();
              return matcher.group(1);
            })
        .mapToInt(x -> Integer.valueOf(x))
        .forEach(x -> result.add(x));
    if (result.isEmpty()) {
      throw new InvalidProfileException(
          String.format(
              "Could not find any skyframe evaluators in the time range %d μs to %d μs.",
              start.getMicros(), end.getMicros()));
    }
    return result;
  }
}
