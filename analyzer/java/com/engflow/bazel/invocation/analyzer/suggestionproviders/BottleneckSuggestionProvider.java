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

import static com.engflow.bazel.invocation.analyzer.time.DurationUtil.formatDuration;

import com.engflow.bazel.invocation.analyzer.Caveat;
import com.engflow.bazel.invocation.analyzer.PotentialImprovement;
import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.dataproviders.ActionStats;
import com.engflow.bazel.invocation.analyzer.dataproviders.Bottleneck;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.engflow.bazel.invocation.analyzer.traceeventformat.PartialCompleteEvent;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BottleneckSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME = BottleneckSuggestionProvider.class.getName();

  @VisibleForTesting
  static final String EMPTY_REASON_PREFIX = "No bottleneck optimizations could be suggested. ";

  @VisibleForTesting
  static final String SUGGESTION_ID_BREAK_DOWN_BOTTLENECK_ACTIONS = "BreakDownBottleneckActions";

  @VisibleForTesting
  static final String SUGGESTION_ID_AVOID_BOTTLENECKS_DUE_TO_QUEUING =
      "AvoidBottlenecksDueToQueuing";

  private static final double SIGNIFICANT_QUEUING_PERCENTAGE = 20;

  private final Duration minDuration;
  private final int maxSuggestions;
  private final int maxActionsPerBottleneck;
  private final double minImprovementPercentage;
  private final double maxActionCountRatio;

  /**
   * @param maxSuggestions the maximum number of bottlenecks suggested by this provider
   * @param maxActionsPerBottleneck the maximum number of actions listed per bottleneck suggestion
   * @param minDuration the minimum duration of a bottleneck for it to be suggested
   * @param minImprovementPercentage the minimum improvement you'd get out of fixing a bottleneck
   *     for it to be suggested, where the improvement is the 100 -
   *     (theoretical_wall_duration_without_bottleneck / wall_duration_with_bottleneck)
   * @param maxActionCountRatio the maximum ratio for (bottleneck action count / cores used in
   *     invocation) for a bottleneck to be suggested
   */
  @VisibleForTesting
  public BottleneckSuggestionProvider(
      int maxSuggestions,
      int maxActionsPerBottleneck,
      Duration minDuration,
      double minImprovementPercentage,
      double maxActionCountRatio) {
    this.maxSuggestions = maxSuggestions;
    this.maxActionsPerBottleneck = maxActionsPerBottleneck;
    this.minDuration = minDuration;
    this.minImprovementPercentage = minImprovementPercentage;
    this.maxActionCountRatio = maxActionCountRatio;
  }

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      ActionStats actionStats = dataManager.getDatum(ActionStats.class);
      if (actionStats.isEmpty()) {
        return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
            ANALYZER_CLASSNAME, EMPTY_REASON_PREFIX + actionStats.getEmptyReason());
      }
      List<Bottleneck> bottlenecks = actionStats.getBottlenecks().get();

      EstimatedCoresUsed estimatedCoresUsedDatum = dataManager.getDatum(EstimatedCoresUsed.class);
      if (estimatedCoresUsedDatum.isEmpty()) {
        return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
            ANALYZER_CLASSNAME, EMPTY_REASON_PREFIX + estimatedCoresUsedDatum.getEmptyReason());
      }
      final var coresUsed = estimatedCoresUsedDatum.getEstimatedCores().get();

      TotalDuration totalDurationDatum = dataManager.getDatum(TotalDuration.class);
      if (totalDurationDatum.isEmpty()) {
        return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
            ANALYZER_CLASSNAME, EMPTY_REASON_PREFIX + totalDurationDatum.getEmptyReason());
      }
      final var totalDuration = totalDurationDatum.getTotalDuration().get();

      final List<Caveat> caveats = new ArrayList<>();
      final var suggestions =
          bottlenecks.stream()
              // Only consider bottlenecks with sufficiently fewer actions than cores used.
              .filter(
                  bottleneck -> bottleneck.getAvgActionCount() / coresUsed < maxActionCountRatio)
              // Only consider bottlenecks that are sufficiently long.
              .filter(bottleneck -> bottleneck.getDuration().compareTo(minDuration) >= 0)
              .map(
                  bottleneck ->
                      BottleneckStats.fromBottleneckAndCoresAndWallDuration(
                          bottleneck, coresUsed, totalDuration))
              .filter(
                  bottleneckStats ->
                      bottleneckStats.improvementPercentage >= minImprovementPercentage)
              .sorted(
                  Comparator.<BottleneckStats>comparingDouble(
                          bottleneckStats -> bottleneckStats.improvementPercentage)
                      .reversed())
              .limit(maxSuggestions)
              .map(bottleneckStats -> generateSuggestion(bottleneckStats, totalDuration))
              .collect(Collectors.toList());

      if (suggestions.size() < bottlenecks.size()) {
        String caveat;
        switch (suggestions.size()) {
          case 0:
            caveat =
                String.format(
                    "None of the %d bottlenecks found were returned.", bottlenecks.size());
            break;
          case 1:
            caveat =
                String.format(
                    "Only one of the %d bottlenecks found was returned.", bottlenecks.size());
            break;
          default:
            caveat =
                String.format(
                    "Only the %d most significant bottlenecks of the %d found were returned.",
                    suggestions.size(), bottlenecks.size());
        }
        if (suggestions.size() < maxSuggestions) {
          caveat += " The withheld bottlenecks do not look sufficiently significant.";
        }
        caveats.add(SuggestionProviderUtil.createCaveat(caveat, true));
      }

      return SuggestionProviderUtil.createSuggestionOutput(
          ANALYZER_CLASSNAME, suggestions, caveats);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }

  private Suggestion generateSuggestion(BottleneckStats bottleneck, Duration totalDuration) {
    StringBuilder recommendation =
        new StringBuilder(
            String.format(
                "These actions are involved in a bottleneck preventing parallelization:\n" + "%s",
                this.suggestTargetsOrActions(bottleneck)));
    final List<String> rationale = new ArrayList<>();
    rationale.add(
        String.format(
            Locale.US,
            "The profile includes a bottleneck lasting %s with an average action count of"
                + " %.2f.",
            formatDuration(bottleneck.bottleneck.getDuration()),
            bottleneck.bottleneck.getAvgActionCount()));
    final List<Caveat> caveats = new ArrayList<>();
    final var actionCount = bottleneck.bottleneck.getPartialEvents().size();
    if (actionCount > maxActionsPerBottleneck) {
      caveats.add(
          SuggestionProviderUtil.createCaveat(
              String.format(
                  "Only the %d longest actions out of %d were returned.",
                  maxActionsPerBottleneck, actionCount),
              true));
    }

    Duration maxQueuingDuration = bottleneck.bottleneck.getMaxQueuingDuration();
    double percentage =
        DurationUtil.getPercentageOf(maxQueuingDuration, bottleneck.bottleneck.getDuration());
    String bottleneckQueuingData =
        String.format(
            Locale.US,
            "This bottleneck includes queuing for up to %s, which is %.2f%% of the bottleneck's"
                + " duration.",
            DurationUtil.formatDuration(maxQueuingDuration),
            percentage);
    if (percentage > SIGNIFICANT_QUEUING_PERCENTAGE) {
      recommendation.append(
          "\n"
              + "The bottleneck includes significant queuing.\n"
              + "Investigate whether your remote execution cluster is overloaded. If so, consider"
              + " increasing the number of workers to avoid queuing and review the cluster's"
              + " autoscaling settings.");
      rationale.add(bottleneckQueuingData);
      return SuggestionProviderUtil.createSuggestion(
          SuggestionCategory.OTHER,
          createSuggestionId(SUGGESTION_ID_AVOID_BOTTLENECKS_DUE_TO_QUEUING),
          "Avoid bottlenecks due to queuing",
          recommendation.toString(),
          potentialImprovement(bottleneck, totalDuration),
          rationale,
          caveats);
    } else {
      if (percentage > 0) {
        caveats.add(SuggestionProviderUtil.createCaveat(bottleneckQueuingData, false));
      }
      recommendation.append("\nTry breaking them down into smaller actions.");
      return SuggestionProviderUtil.createSuggestion(
          SuggestionCategory.BUILD_FILE,
          createSuggestionId(SUGGESTION_ID_BREAK_DOWN_BOTTLENECK_ACTIONS),
          "Break down bottleneck actions",
          recommendation.toString(),
          potentialImprovement(bottleneck, totalDuration),
          rationale,
          caveats);
    }
  }

  private String suggestTargetsOrActions(BottleneckStats bottleneck) {
    // TODO(antonio) suggest targets instead of actions if possible
    return bottleneck.bottleneck.getPartialEvents().stream()
        .sorted(
            Comparator.<PartialCompleteEvent, Duration>comparing(event -> event.croppedDuration)
                .reversed())
        .limit(maxActionsPerBottleneck)
        .map(
            event -> {
              if (event.isCropped()) {
                return String.format(
                    "\t- %s (partially: %s of %s)",
                    event.completeEvent.name,
                    formatDuration(event.croppedDuration),
                    formatDuration(event.completeEvent.duration));
              } else {
                return String.format(
                    "\t- %s (%s)",
                    event.completeEvent.name, formatDuration(event.completeEvent.duration));
              }
            })
        .collect(Collectors.joining("\n"));
  }

  private PotentialImprovement potentialImprovement(
      BottleneckStats bottleneck, Duration totalDuration) {
    String message =
        String.format(
            "The initial build time was %s and could be reduced to %s.",
            formatDuration(totalDuration), formatDuration(bottleneck.optimalWallDuration));
    return SuggestionProviderUtil.createPotentialImprovement(
        message, bottleneck.improvementPercentage);
  }

  public static BottleneckSuggestionProvider createDefault() {
    return new BottleneckSuggestionProvider(5, 5, Duration.ofSeconds(5), 5, .9);
  }

  public static BottleneckSuggestionProvider createVerbose() {
    return new BottleneckSuggestionProvider(
        Integer.MAX_VALUE, Integer.MAX_VALUE, Duration.ZERO, 0, .0);
  }

  private static class BottleneckStats {
    private final Bottleneck bottleneck;
    private final Duration optimalWallDuration;
    private final double improvementPercentage;

    private BottleneckStats(
        Bottleneck bottleneck, Duration optimalWallDuration, double improvementPercentage) {
      this.bottleneck = bottleneck;
      this.optimalWallDuration = optimalWallDuration;
      this.improvementPercentage = improvementPercentage;
    }

    private static BottleneckStats fromBottleneckAndCoresAndWallDuration(
        Bottleneck bottleneck, int coresCount, Duration totalDuration) {
      final var optimalBottleneckDuration =
          bottleneck
              .getDuration()
              .multipliedBy((int) Math.ceil(bottleneck.getAvgActionCount()))
              .dividedBy(coresCount);
      final var optimalWallDuration =
          totalDuration.minus(bottleneck.getDuration()).plus(optimalBottleneckDuration);
      final var improvement =
          100 - DurationUtil.getPercentageOf(optimalWallDuration, totalDuration);
      return new BottleneckStats(bottleneck, optimalWallDuration, improvement);
    }
  }
}
