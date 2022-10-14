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

import com.engflow.bazel.invocation.analyzer.PotentialImprovement;
import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.CriticalPathDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.CriticalPathQueuingDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.QueuingObserved;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.TotalQueuingDuration;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A {@link SuggestionProvider} that provides suggestions on how to reduce remote execution queuing.
 */
public class QueuingSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME = QueuingSuggestionProvider.class.getName();

  @VisibleForTesting
  static final String EMPTY_REASON_PREFIX =
      "No optimizations for reducing queuing could be suggested. ";

  private static final String SUGGESTION_ID_INCREASE_RE_CLUSTER_SIZE =
      "IncreaseRemoteExecutionClusterSize";

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      boolean hasQueuing = dataManager.getDatum(QueuingObserved.class).isQueuingObserved();
      List<Suggestion> suggestions = new ArrayList<>();
      if (hasQueuing) {
        Duration totalQueuingDuration =
            dataManager.getDatum(TotalQueuingDuration.class).getTotalQueuingDuration();
        Optional<Duration> optionalCriticalPathQueuingDuration =
            dataManager
                .getDatum(CriticalPathQueuingDuration.class)
                .getCriticalPathQueuingDuration();
        Optional<Duration> optionalCriticalPathDuration =
            dataManager.getDatum(CriticalPathDuration.class).getCriticalPathDuration();

        String title = "Increase the remote execution cluster size";
        String recommendation =
            "Investigate whether your remote execution cluster is overloaded. If so,"
                + " consider increasing the number of workers to avoid queuing and review"
                + " the cluster's autoscaling settings.";
        PotentialImprovement potentialImprovement = null;
        if (optionalCriticalPathQueuingDuration.isPresent()
            && !optionalCriticalPathQueuingDuration.get().isZero()
            && optionalCriticalPathDuration.isPresent()
            && !optionalCriticalPathDuration.get().isZero()) {
          Duration criticalPathDuration = optionalCriticalPathDuration.get();
          Duration queuingDuration = optionalCriticalPathQueuingDuration.get();
          TotalDuration totalDurationDatum = dataManager.getDatum(TotalDuration.class);
          if (totalDurationDatum.isEmpty()) {
            return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
                ANALYZER_CLASSNAME, EMPTY_REASON_PREFIX + totalDurationDatum.getEmptyReason());
          }
          Duration totalDuration = totalDurationDatum.getTotalDuration().get();
          double invocationDurationReductionPercentage =
              100 * queuingDuration.toMillis() / (double) totalDuration.toMillis();
          potentialImprovement =
              SuggestionProviderUtil.createPotentialImprovement(
                  String.format(
                      Locale.US,
                      "The critical path includes queuing for %s. Without queuing it could be"
                          + " reduced by %.2f%%, from %s to %s.",
                      DurationUtil.formatDuration(queuingDuration),
                      100
                          * (1
                              - criticalPathDuration.minus(queuingDuration).toMillis()
                                  / (double) criticalPathDuration.toMillis()),
                      DurationUtil.formatDuration(criticalPathDuration),
                      DurationUtil.formatDuration(criticalPathDuration.minus(queuingDuration))),
                  invocationDurationReductionPercentage);
        }
        String rationale =
            String.format(
                "This invocation includes (potentially parallel) queuing for %s. This can"
                    + " indicate that the remote execution cluster was under too much"
                    + " load.",
                DurationUtil.formatDuration(totalQueuingDuration));
        suggestions.add(
            SuggestionProviderUtil.createSuggestion(
                SuggestionCategory.OTHER,
                createSuggestionId(SUGGESTION_ID_INCREASE_RE_CLUSTER_SIZE),
                title,
                recommendation,
                potentialImprovement,
                List.of(rationale),
                null));
      }
      return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, suggestions, null);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }
}
