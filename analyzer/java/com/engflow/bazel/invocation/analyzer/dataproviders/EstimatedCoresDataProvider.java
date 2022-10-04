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
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
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
  private static final Pattern EXECUTION_PHASE_SKYFRAME_EVALUATOR_REGEX =
      Pattern.compile("skyframe-evaluator\\s(\\d*)");
  private static final Pattern EVALUATE_AND_DEPENDENCIES_PHASE_SKYFRAME_EVALUATOR_REGEX =
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
      // Evaluate only events within the target pattern evaluation and dependency analysis phases.
      Timestamp start = bazelPhaseDescriptions.get(BazelProfilePhase.EVALUATE).getStart();
      Timestamp end = bazelPhaseDescriptions.get(BazelProfilePhase.DEPENDENCIES).getEnd();
      evaluateAndDependenciesPhaseSkyframeEvaluators = new HashSet<>();
      bazelProfile
          .getThreads()
          // Consider only threads from the relevant phases.
          .filter(
              thread ->
                  thread.getCompleteEvents().stream()
                      .anyMatch(
                          completeEvent ->
                              completeEvent.start.compareTo(start) >= 0
                                  && completeEvent.end.compareTo(end) <= 0))
          // Expect for each core to see at least one of "skyframe-evaluator [x]",
          // "skyframe-evaluator-[x]", and "skyframe-evaluator-cpu-heavy-[x]".
          .filter(
              thread ->
                  EVALUATE_AND_DEPENDENCIES_PHASE_SKYFRAME_EVALUATOR_REGEX
                      .matcher(thread.getName())
                      .matches())
          .map(
              thread -> {
                Matcher matcher =
                    EVALUATE_AND_DEPENDENCIES_PHASE_SKYFRAME_EVALUATOR_REGEX.matcher(
                        thread.getName());
                matcher.find();
                return matcher.group(1);
              })
          .mapToInt(x -> Integer.valueOf(x))
          .forEach(x -> evaluateAndDependenciesPhaseSkyframeEvaluators.add(x));
      if (evaluateAndDependenciesPhaseSkyframeEvaluators.isEmpty()) {
        throw new InvalidProfileException(
            String.format(
                "Could not find any skyframe evaluators in the phases \"%s\" and \"%s\"",
                BazelProfilePhase.EVALUATE.name, BazelProfilePhase.DEPENDENCIES.name));
      }
      evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue =
          evaluateAndDependenciesPhaseSkyframeEvaluators.stream().max(Integer::compareTo).get();
    }
  }

  private synchronized void determineEstimatedCoresUsed()
      throws MissingInputException, InvalidProfileException {
    if (executionPhaseSkyframeEvaluators == null) {
      executionPhaseSkyframeEvaluators = new HashSet<>();
      BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
      bazelProfile
          .getThreads()
          // Consider only the threads from the execution phase and discard the rest
          .filter(
              thread ->
                  thread.getCompleteEvents().stream()
                      .anyMatch(
                          completeEvent ->
                              completeEvent.category.equals(
                                  BazelProfileConstants.CAT_ACTION_PROCESSING)))
          // Each core should have exactly one "skyframe-evaluator [x]" in the Bazel profile.
          .filter(
              (thread) ->
                  EXECUTION_PHASE_SKYFRAME_EVALUATOR_REGEX.matcher(thread.getName()).matches())
          .map(
              thread -> {
                Matcher matcher =
                    EVALUATE_AND_DEPENDENCIES_PHASE_SKYFRAME_EVALUATOR_REGEX.matcher(
                        thread.getName());
                matcher.find();
                return matcher.group(1);
              })
          .mapToInt(x -> Integer.valueOf(x))
          .forEach(x -> executionPhaseSkyframeEvaluators.add(x));
      if (executionPhaseSkyframeEvaluators.isEmpty()) {
        throw new InvalidProfileException(
            String.format(
                "Could not find any skyframe evaluators that include a duration event of category"
                    + " \"%s\"",
                BazelProfileConstants.CAT_ACTION_PROCESSING));
      }
      executionPhaseSkyframeEvaluatorsMaxValue =
          executionPhaseSkyframeEvaluators.stream().max(Integer::compareTo).get();
    }
  }
}
