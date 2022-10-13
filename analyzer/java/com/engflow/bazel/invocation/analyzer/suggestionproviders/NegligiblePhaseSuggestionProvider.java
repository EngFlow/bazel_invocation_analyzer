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
import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelPhaseDescription;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelPhaseDescriptions;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A {@link SuggestionProvider} that highlights Bazel phases if they take longer than expected
 * relative to the total duration of the invocation. Usually only three phases should have a
 * non-negligible duration:<br>
 * evaluating the target patterns, loading and analyzing the dependencies, and executing the
 * actions.
 */
public class NegligiblePhaseSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME =
      NegligiblePhaseSuggestionProvider.class.getName();
  private static final String SUGGESTION_ID_UNUSUAL_PHASE_DURATION_FORMAT =
      "UnusualPhaseDuration-%s";

  // These phases are expected to be more than a negligible percentage of the overall time
  @VisibleForTesting
  public static final List<BazelProfilePhase> NON_NEGLIGIBLE_PHASES =
      List.of(
          BazelProfilePhase.EVALUATE, BazelProfilePhase.DEPENDENCIES, BazelProfilePhase.EXECUTE);
  // Don't apply this check to profiles shorter than this duration
  private static final Duration MIN_DURATION = Duration.ofSeconds(30);
  // Non-negligible phases should be less than this percentage of total duration (1.0 = 1%)
  private static final double PCT_THRESHOLD = 2.0;

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      Optional<Duration> optionalTotalDuration =
          dataManager.getDatum(TotalDuration.class).getTotalDuration();
      if (optionalTotalDuration.isEmpty()) {
        return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
            ANALYZER_CLASSNAME, TotalDuration.class);
      }
      Duration totalDuration = optionalTotalDuration.get();
      if (totalDuration.compareTo(MIN_DURATION) < 0) {
        // Too short for this check to be valid
        Caveat caveat =
            SuggestionProviderUtil.createCaveat(
                "This invocation is too short to analyze abnormally long phases.", false);
        return SuggestionProviderUtil.createSuggestionOutput(
            ANALYZER_CLASSNAME, null, List.of(caveat));
      }
      BazelPhaseDescriptions bazelPhaseDescriptions =
          dataManager.getDatum(BazelPhaseDescriptions.class);
      List<Suggestion> suggestions = new ArrayList<>();
      for (BazelProfilePhase phase : BazelProfilePhase.values()) {
        if (NON_NEGLIGIBLE_PHASES.contains(phase)) {
          continue;
        }
        Optional<BazelPhaseDescription> optionalPhaseDescription =
            bazelPhaseDescriptions.get(phase);
        if (optionalPhaseDescription.isEmpty()) {
          continue;
        }
        Duration phaseDuration = optionalPhaseDescription.get().getDuration();
        double percentOfTotal = phaseDuration.toMillis() / (double) totalDuration.toMillis() * 100;
        if (percentOfTotal > PCT_THRESHOLD) {
          String title = "Unusual Phase Duration";
          String recommendation =
              String.format(
                  "Investigate the \"%s\" build phase, as it is unexpectedly long "
                      + "compared to the overall build duration.",
                  phase.name);
          String rationale =
              String.format(
                  "The Bazel phase \"%s\" took %s of the build's total duration of %s. So"
                      + " this phase took %.2f%%, which is longer than the %.2f%%"
                      + " threshold. It should be negligible compared to the overall run.",
                  phase.name,
                  DurationUtil.formatDuration(phaseDuration),
                  DurationUtil.formatDuration(totalDuration),
                  percentOfTotal,
                  PCT_THRESHOLD);
          suggestions.add(
              SuggestionProviderUtil.createSuggestion(
                  SuggestionCategory.OTHER,
                  createSuggestionId(
                      String.format(SUGGESTION_ID_UNUSUAL_PHASE_DURATION_FORMAT, phase)),
                  title,
                  recommendation,
                  null,
                  List.of(rationale),
                  null));
        }
      }
      return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, suggestions, null);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }
}
