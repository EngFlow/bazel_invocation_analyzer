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
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BottleneckSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME = BottleneckSuggestionProvider.class.getName();
  private static final String SUGGESTION_ID_BREAK_DOWN_BOTTLENECK_ACTIONS =
      "BreakDownBottleneckActions";

  private final Duration minDuration;
  private final int maxSuggestions;
  private final int maxActionsPerBottleneck;
  private final double minImprovementRatio;

  /**
   * @param maxSuggestions the maximum number of bottlenecks suggested by this provider
   * @param maxActionsPerBottleneck the maximum number of actions listed per bottleneck suggestion
   * @param minDuration the minimum duration of a bottleneck for it to be suggested
   * @param minImprovementRatio the minimum improvement you'd get out of fixing a bottleneck for it
   *     to be suggested, where the improvement is the 1 -
   *     (theoretical_wall_duration_without_bottleneck / wall_duration_with_bottleneck)
   */
  @VisibleForTesting
  public BottleneckSuggestionProvider(
      int maxSuggestions,
      int maxActionsPerBottleneck,
      Duration minDuration,
      double minImprovementRatio) {
    this.maxSuggestions = maxSuggestions;
    this.maxActionsPerBottleneck = maxActionsPerBottleneck;
    this.minDuration = minDuration;
    this.minImprovementRatio = minImprovementRatio;
  }

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      final var actionStats = dataManager.getDatum(ActionStats.class);
      final var cores = dataManager.getDatum(EstimatedCoresUsed.class).getEstimatedCores();
      final var totalDuration = dataManager.getDatum(TotalDuration.class).getTotalDuration();
      final List<Caveat> caveats = new ArrayList<>();

      final var suggestions =
          actionStats.bottlenecks.stream()
              .filter(bottleneck -> bottleneck.getDuration().compareTo(minDuration) >= 0)
              .map(
                  bottleneck ->
                      BottleneckStats.fromBottleneckAndCoresAndWallDuration(
                          bottleneck, cores, totalDuration))
              .filter(bottleneckStats -> bottleneckStats.improvement >= minImprovementRatio)
              .sorted(
                  Comparator.<BottleneckStats>comparingDouble(
                          bottleneckStats -> bottleneckStats.improvement)
                      .reversed())
              .limit(maxSuggestions)
              .map(bottleneckStats -> generateSuggestion(bottleneckStats, totalDuration))
              .collect(Collectors.toList());

      if (suggestions.size() < actionStats.bottlenecks.size()) {
        String caveat;
        switch (suggestions.size()) {
          case 0:
            caveat =
                String.format(
                    "None of the %d bottlenecks found were returned.",
                    actionStats.bottlenecks.size());
            break;
          case 1:
            caveat =
                String.format(
                    "Only one of the %d bottlenecks found was returned.",
                    actionStats.bottlenecks.size());
            break;
          default:
            caveat =
                String.format(
                    "Only the %d most significant bottlenecks of the %d found were returned.",
                    suggestions.size(), actionStats.bottlenecks.size());
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
    String title = "Break down bottleneck actions";
    String recommendation =
        String.format(
            "These actions are involved in a bottleneck preventing parallelization. Try"
                + " breaking them down into smaller actions:\n"
                + "%s",
            this.suggestTargetsOrActions(bottleneck));
    String rationale =
        String.format(
            Locale.US,
            "The profile includes a bottleneck lasting %s with an average action count of"
                + " %.2f.",
            formatDuration(bottleneck.bottleneck.getDuration()),
            bottleneck.bottleneck.getAvgActionCount());
    final List<Caveat> caveats = new ArrayList<>();
    final var actionCount = bottleneck.bottleneck.getEvents().size();
    if (actionCount > maxActionsPerBottleneck) {
      caveats.add(
          SuggestionProviderUtil.createCaveat(
              String.format(
                  "Only the %d longest actions out of %d were returned.",
                  maxActionsPerBottleneck, actionCount),
              true));
    }
    return SuggestionProviderUtil.createSuggestion(
        SuggestionCategory.BUILD_FILE,
        createSuggestionId(SUGGESTION_ID_BREAK_DOWN_BOTTLENECK_ACTIONS),
        title,
        recommendation,
        potentialImprovement(bottleneck, totalDuration),
        List.of(rationale),
        caveats);
  }

  private String suggestTargetsOrActions(BottleneckStats bottleneck) {
    // TODO(antonio) suggest targets instead of actions if possible
    return bottleneck.bottleneck.getEvents().stream()
        .sorted(Comparator.<CompleteEvent, Duration>comparing(event -> event.duration).reversed())
        .limit(maxActionsPerBottleneck)
        .map(event -> String.format("\t- %s (%s)", event.name, formatDuration(event.duration)))
        .collect(Collectors.joining("\n"));
  }

  private PotentialImprovement potentialImprovement(
      BottleneckStats bottleneck, Duration totalDuration) {
    double percentage = 100 * bottleneck.improvement;
    String message =
        String.format(
            "The initial build time was %s and could be reduced to %s.",
            formatDuration(totalDuration), formatDuration(bottleneck.optimalWallDuration));
    return SuggestionProviderUtil.createPotentialImprovement(message, percentage);
  }

  public static BottleneckSuggestionProvider createDefault() {
    return new BottleneckSuggestionProvider(5, 5, Duration.ofSeconds(5), .05);
  }

  public static BottleneckSuggestionProvider createVerbose() {
    return new BottleneckSuggestionProvider(
        Integer.MAX_VALUE, Integer.MAX_VALUE, Duration.ZERO, .0);
  }

  private static class BottleneckStats {
    private final Bottleneck bottleneck;
    private final Duration optimalWallDuration;
    private final double improvement;

    private BottleneckStats(
        Bottleneck bottleneck, Duration optimalWallDuration, double improvement) {
      this.bottleneck = bottleneck;
      this.optimalWallDuration = optimalWallDuration;
      this.improvement = improvement;
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
          1 - optimalWallDuration.toMillis() / (double) totalDuration.toMillis();
      return new BottleneckStats(bottleneck, optimalWallDuration, improvement);
    }
  }
}
