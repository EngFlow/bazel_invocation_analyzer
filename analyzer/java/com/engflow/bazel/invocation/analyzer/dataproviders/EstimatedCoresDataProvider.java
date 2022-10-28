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
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.google.common.annotations.VisibleForTesting;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

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
  public static final String ESTIMATED_CORES_USED_EMPTY_REASON_EXEC_MISSING =
      "The Bazel profile does not include an execution phase, which is required for estimating the"
          + " number of cores used. Try analyzing a profile that processes actions, for example a"
          + " build or test.";
  public static final String ESTIMATED_CORES_USED_EMPTY_REASON_EXEC_EVENTS_MISSING =
      "The Bazel profile does not include any threads with events in the execution phase, which is"
          + " required for estimating the number of cores used. Try analyzing a profile that"
          + " processes actions, for example a build or test.";
  public static final String ESTIMATED_CORES_AVAILABLE_EMPTY_REASON_EVAL_DEP_MISSING =
      "The Bazel profile does not includes phases for evaluating target patterns or loading and"
          + " analyzing dependencies, which is required for estimating the number of cores used."
          + " Try analyzing a profile that processes actions, for example a build or test.";
  public static final String ESTIMATED_CORES_AVAILABLE_EMPTY_REASON_EVAL_DEP_EVENTS_MISSING =
      "The Bazel profile does not include any threads with events in the phases for evaluating"
          + " target patterns or loading and analyzing dependencies, which is required for"
          + " estimating the number of cores used. Try analyzing a profile that processes actions,"
          + " for example a build or test.";
  public static final String ESTIMATED_JOBS_FLAG_VALUE_EMPTY_REASON_EVAL_DEP_MISSING =
      "The Bazel profile does not includes phases for evaluating target patterns or loading and"
          + " analyzing dependencies, which is required for estimating the value of the Bazel flag"
          + " --jobs. Try analyzing a profile that processes actions, for example a build or test.";
  public static final String ESTIMATED_JOBS_FLAG_VALUE_EMPTY_REASON_EXEC_MISSING =
      "The Bazel profile does not include an execution phase, which is required for estimating the"
          + " value of the Bazel flag --jobs. Try analyzing a profile that processes actions, for"
          + " example a build or test.";
  public static final String ESTIMATED_JOBS_FLAG_VALUE_EMPTY_REASON_EVENTS_MISSING =
      "The Bazel profile does not include events in the phases required for estimating the value"
          + " of the Bazel flag --jobs. Try analyzing a profile that processes actions, for example"
          + " a build or test.";

  private static final Pattern SKYFRAME_EVALUATOR_REGEX =
      Pattern.compile("skyframe-evaluator[^\\d]*(\\d*)");

  private Set<Integer> executionPhaseSkyframeEvaluators;
  @Nullable private Integer executionPhaseSkyframeEvaluatorsMaxValue;
  private Set<Integer> evaluateAndDependenciesPhaseSkyframeEvaluators;
  @Nullable private Integer evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue;

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
      throws InvalidProfileException, MissingInputException, NullDatumException {
    determineEstimatedCoresAvailable();
    determineEstimatedCoresUsed();
    if (executionPhaseSkyframeEvaluators == null) {
      return new EstimatedJobsFlagValue(ESTIMATED_JOBS_FLAG_VALUE_EMPTY_REASON_EXEC_MISSING);
    }
    if (evaluateAndDependenciesPhaseSkyframeEvaluators == null) {
      return new EstimatedJobsFlagValue(ESTIMATED_JOBS_FLAG_VALUE_EMPTY_REASON_EVAL_DEP_MISSING);
    }
    if (executionPhaseSkyframeEvaluators == null
        || evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue == null) {
      return new EstimatedJobsFlagValue(ESTIMATED_JOBS_FLAG_VALUE_EMPTY_REASON_EVENTS_MISSING);
    }
    return new EstimatedJobsFlagValue(
        executionPhaseSkyframeEvaluatorsMaxValue + 1,
        !executionPhaseSkyframeEvaluatorsMaxValue.equals(
            evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue));
  }

  @VisibleForTesting
  EstimatedCoresAvailable getEstimatedCoresAvailable()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    determineEstimatedCoresAvailable();
    if (evaluateAndDependenciesPhaseSkyframeEvaluators == null) {
      // The Bazel profile does not include the required phase(s).
      return new EstimatedCoresAvailable(ESTIMATED_CORES_AVAILABLE_EMPTY_REASON_EVAL_DEP_MISSING);
    }
    if (evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue == null) {
      // The Bazel profile does not include events in the targeted phase(s).
      return new EstimatedCoresAvailable(
          ESTIMATED_CORES_AVAILABLE_EMPTY_REASON_EVAL_DEP_EVENTS_MISSING);
    }
    return new EstimatedCoresAvailable(
        evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue + 1,
        getGaps(
            evaluateAndDependenciesPhaseSkyframeEvaluators,
            evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue));
  }

  @VisibleForTesting
  EstimatedCoresUsed getEstimatedCoresUsed()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    determineEstimatedCoresUsed();
    if (executionPhaseSkyframeEvaluators == null) {
      // The Bazel profile does not include the required phase.
      return new EstimatedCoresUsed(ESTIMATED_CORES_USED_EMPTY_REASON_EXEC_MISSING);
    }
    if (executionPhaseSkyframeEvaluatorsMaxValue == null) {
      // The Bazel profile does not include events in the targeted phase.
      return new EstimatedCoresUsed(ESTIMATED_CORES_USED_EMPTY_REASON_EXEC_EVENTS_MISSING);
    }
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
      throws InvalidProfileException, MissingInputException, NullDatumException {
    if (evaluateAndDependenciesPhaseSkyframeEvaluators == null) {
      BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
      BazelPhaseDescriptions bazelPhaseDescriptions =
          getDataManager().getDatum(BazelPhaseDescriptions.class);
      // Evaluate only events within the target pattern evaluation and dependency analysis
      // phases. These phases should use as many cores as there are available, irrespective of
      // whether the Bazel flag `--jobs` is set or not.
      Optional<BazelPhaseDescription> start =
          bazelPhaseDescriptions.has(BazelProfilePhase.EVALUATE)
              ? bazelPhaseDescriptions.get(BazelProfilePhase.EVALUATE)
              : bazelPhaseDescriptions.get(BazelProfilePhase.DEPENDENCIES);
      Optional<BazelPhaseDescription> end =
          bazelPhaseDescriptions.has(BazelProfilePhase.DEPENDENCIES)
              ? bazelPhaseDescriptions.get(BazelProfilePhase.DEPENDENCIES)
              : bazelPhaseDescriptions.get(BazelProfilePhase.EVALUATE);
      if (start.isEmpty() || end.isEmpty()) {
        // The profile does not include that data necessary.
        return;
      }
      evaluateAndDependenciesPhaseSkyframeEvaluators =
          getSkyframeEvaluators(bazelProfile, start.get().getStart(), end.get().getEnd());
      evaluateAndDependenciesPhaseSkyframeEvaluatorsMaxValue =
          evaluateAndDependenciesPhaseSkyframeEvaluators.stream()
              .max(Integer::compareTo)
              .orElse(null);
    }
  }

  private synchronized void determineEstimatedCoresUsed()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    if (executionPhaseSkyframeEvaluators == null) {
      BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
      BazelPhaseDescriptions bazelPhaseDescriptions =
          getDataManager().getDatum(BazelPhaseDescriptions.class);
      // Evaluate only threads with events in the execution phase, as the Bazel flag `--jobs`
      // applies to that phase specifically.
      Optional<BazelPhaseDescription> execution =
          bazelPhaseDescriptions.get(BazelProfilePhase.EXECUTE);
      if (execution.isEmpty()) {
        return;
      }
      executionPhaseSkyframeEvaluators =
          getSkyframeEvaluators(bazelProfile, execution.get().getStart(), execution.get().getEnd());
      executionPhaseSkyframeEvaluatorsMaxValue =
          executionPhaseSkyframeEvaluators.stream().max(Integer::compareTo).orElse(null);
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
    return result;
  }
}
