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

package com.engflow.bazel.invocation.analyzer.suggestionproviders;

import com.engflow.bazel.invocation.analyzer.Caveat;
import com.engflow.bazel.invocation.analyzer.PotentialImprovement;
import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelPhaseDescription;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelPhaseDescriptions;
import com.engflow.bazel.invocation.analyzer.dataproviders.CriticalPathDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedJobsFlagValue;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionUsed;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A {@link SuggestionProvider} that provides suggestions on how to speed up the invocation if the
 * critical path does not dominate the execution phase.
 */
public class CriticalPathNotDominantSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME =
      CriticalPathNotDominantSuggestionProvider.class.getName();
  private static final String SUGGESTION_ID_INCREASE_NUMBER_OF_CORES = "IncreaseNumberOfCores";
  private static final String SUGGESTION_ID_INCREASE_VALUE_OF_JOBS_FLAG = "IncreaseValueOfJobsFlag";

  private static final double MAX_CRITICAL_PATH_FACTOR = 0.75;
  private static final Duration MIN_DURATION_FOR_EVALUATION = Duration.ofSeconds(5);

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      BazelPhaseDescriptions phases = dataManager.getDatum(BazelPhaseDescriptions.class);
      Optional<BazelPhaseDescription> optionalExecutionPhaseDescription =
          phases.get(BazelProfilePhase.EXECUTE);
      if (optionalExecutionPhaseDescription.isEmpty()) {
        // No execution phase found, so critical path analysis not applicable.
        Caveat caveat =
            SuggestionProviderUtil.createCaveat(
                "The Bazel profile does not include an execution phase for this invocation, which"
                    + " is necessary to analyze whether the critical path is dominant or not.",
                false);
        return SuggestionProviderUtil.createSuggestionOutput(
            ANALYZER_CLASSNAME, null, List.of(caveat));
      }

      Duration executionDuration = optionalExecutionPhaseDescription.get().getDuration();
      if (executionDuration.compareTo(MIN_DURATION_FOR_EVALUATION) < 0) {
        Caveat caveat =
            SuggestionProviderUtil.createCaveat(
                "The execution phase of this invocation is too short to analyze the critical path"
                    + " relative to the entire invocation.",
                false);
        // Execution phase too short for this analysis to make sense.
        return SuggestionProviderUtil.createSuggestionOutput(
            ANALYZER_CLASSNAME, null, List.of(caveat));
      }

      Optional<Duration> optionalCriticalPathDuration =
          dataManager.getDatum(CriticalPathDuration.class).getCriticalPathDuration();
      if (optionalCriticalPathDuration.isEmpty()) {
        // We cannot make any suggestions if we have no data about the critical path.
        return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
            ANALYZER_CLASSNAME, CriticalPathDuration.class);
      }
      Duration criticalPathDuration = optionalCriticalPathDuration.get();
      if (executionDuration.compareTo(criticalPathDuration) < 0) {
        // Execution phase shorter than critical path.
        Caveat caveat =
            SuggestionProviderUtil.createCaveat(
                "The provided Bazel profile seems to have invalid data. The critical path takes"
                    + " longer than the execution phase, but it should be included within it.",
                false);
        return SuggestionProviderUtil.createSuggestionOutput(
            ANALYZER_CLASSNAME, null, List.of(caveat));
      }

      double criticalPathFactor =
          criticalPathDuration.toMillis() / (double) executionDuration.toMillis();
      if (criticalPathFactor > MAX_CRITICAL_PATH_FACTOR) {
        // Critical path is dominant, do not provide any suggestions.
        return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, null, null);
      }

      List<Suggestion> suggestions = new ArrayList<>();

      Optional<Duration> optionalTotalDuration =
          dataManager.getDatum(TotalDuration.class).getTotalDuration();
      if (optionalTotalDuration.isEmpty()) {
        return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
            ANALYZER_CLASSNAME, TotalDuration.class);
      }
      Duration totalDuration = optionalTotalDuration.get();
      Duration minimumDuration = totalDuration.minus(executionDuration).plus(criticalPathDuration);
      double durationReductionPercent =
          100 * (1 - minimumDuration.toMillis() / (double) totalDuration.toMillis());

      Optional<Integer> optionalEstimatedCoresUsed =
          dataManager.getDatum(EstimatedCoresUsed.class).getEstimatedCores();
      if (optionalEstimatedCoresUsed.isEmpty()) {
        return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
            ANALYZER_CLASSNAME, EstimatedCoresUsed.class);
      }
      int estimatedCoresUsed = optionalEstimatedCoresUsed.get();
      long optimalCores =
          (long)
              Math.ceil(
                  estimatedCoresUsed
                      * totalDuration.toMillis()
                      / (double) minimumDuration.toMillis());

      PotentialImprovement potentialImprovement =
          SuggestionProviderUtil.createPotentialImprovement(
              String.format(
                  "The invocation's duration might go down to %s, compared to the current %s. This"
                      + " assumes the execution phase duration can be reduced to the critical path"
                      + " duration.",
                  DurationUtil.formatDuration(minimumDuration),
                  DurationUtil.formatDuration(totalDuration)),
              durationReductionPercent);
      String rationaleCriticalPathNotDominant =
          String.format(
              Locale.US,
              "This invocation's critical path has a duration of %s, whereas the total execution"
                  + " phase has a duration of %s. In an ideally parallelized invocation, the"
                  + " critical path dominates the execution phase, but here it takes up only"
                  + " %.2f%%. This indicates that actions are not as parallelized as much as they"
                  + " could be.",
              DurationUtil.formatDuration(criticalPathDuration),
              DurationUtil.formatDuration(executionDuration),
              criticalPathFactor * 100);

      boolean remoteExecutionUsed =
          dataManager.getDatum(RemoteExecutionUsed.class).isRemoteExecutionUsed();
      if (remoteExecutionUsed) {
        String title = "Increase the value of --jobs";
        String recommendation =
            String.format(
                "Increasing the value of the Bazel flag --jobs to parallelize more"
                    + " actions.\n"
                    + "An optimal speedup is expected by increasing the value to %d or"
                    + " more.\n"
                    + "Also see"
                    + " https://docs.bazel.build/versions/main/command-line-reference.html"
                    + "#flag--jobs",
                optimalCores);
        String rationaleEstimatedCoresUsed =
            String.format(
                "The value of --jobs determines how many concurrent jobs Bazel should run"
                    + " in the execution phase. It looks like up to %d such jobs were"
                    + " run in parallel for this invocation.",
                estimatedCoresUsed);
        // We could potentially also factor in queuing within the critical path, but this
        // has its downsides. The critical path might be a different one without queuing, the
        // ratios change, and it is unclear what effect the suggestions have on queuing.
        // In particular, QueuingSuggestionProvider gives suggestions regarding observed queuing.
        // Therefore, we disregard queuing in the critical path here.
        List<Caveat> caveats = new ArrayList<>();
        if (optimalCores > 2500) {
          caveats.add(
              SuggestionProviderUtil.createCaveat(
                  "Setting --jobs to a value above 2500 may cause memory issues.", false));
        }
        if (optimalCores > 5000) {
          caveats.add(
              SuggestionProviderUtil.createCaveat(
                  "The value of --jobs must be between 1 and 5000.", false));
        }
        suggestions.add(
            SuggestionProviderUtil.createSuggestion(
                SuggestionCategory.BAZEL_FLAGS,
                createSuggestionId(SUGGESTION_ID_INCREASE_VALUE_OF_JOBS_FLAG),
                title,
                recommendation,
                potentialImprovement,
                List.of(rationaleCriticalPathNotDominant, rationaleEstimatedCoresUsed),
                caveats));
      } else /* !remoteExecutionUsed */ {
        EstimatedJobsFlagValue estimatedJobs = dataManager.getDatum(EstimatedJobsFlagValue.class);
        String title = "Increase the number of cores";
        String recommendation =
            String.format(
                "Add more cores to parallelize actions more. You can achieve this %s using"
                    + " a machine with more CPUs or by utilizing remote execution.\n"
                    + "An optimal speedup is expected by increasing the number of cores to"
                    + " %d or more.",
                estimatedJobs.isLikelySet()
                    ? "by adjusting the value of the Bazel flag --jobs, by"
                    : "by",
                optimalCores);
        String rationaleEstimatedCoresUsed =
            String.format(
                "It looks like %d cores were used for this invocation.", estimatedCoresUsed);
        Caveat caveat =
            SuggestionProviderUtil.createCaveat(
                "The number of cores used for this invocation is an approximation. It"
                    + " includes both physical and virtual cores.",
                false);
        suggestions.add(
            SuggestionProviderUtil.createSuggestion(
                SuggestionCategory.OTHER,
                createSuggestionId(SUGGESTION_ID_INCREASE_NUMBER_OF_CORES),
                title,
                recommendation,
                potentialImprovement,
                List.of(rationaleCriticalPathNotDominant, rationaleEstimatedCoresUsed),
                List.of(caveat)));
      }

      return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, suggestions, null);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }
}
