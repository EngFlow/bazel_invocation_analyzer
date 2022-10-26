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

import static com.engflow.bazel.invocation.analyzer.suggestionproviders.BottleneckSuggestionProvider.SUGGESTION_ID_AVOID_BOTTLENECKS_DUE_TO_QUEUING;
import static com.engflow.bazel.invocation.analyzer.suggestionproviders.BottleneckSuggestionProvider.SUGGESTION_ID_BREAK_DOWN_BOTTLENECK_ACTIONS;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.bazelprofile.ThreadId;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.dataproviders.ActionStats;
import com.engflow.bazel.invocation.analyzer.dataproviders.Bottleneck;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class BottleneckSuggestionProviderTest extends SuggestionProviderUnitTestBase {
  @Before
  public void setup() {
    suggestionProvider = BottleneckSuggestionProvider.createDefault();
  }

  @Test
  public void doesNotCreateSuggestionsIfActionStatsIsEmpty()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats("empty"));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, 0, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).isEmpty();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getCaveatList()).hasSize(1);
    assertThat(suggestions.getCaveat(0).getMessage())
        .contains(BottleneckSuggestionProvider.EMPTY_REASON_PREFIX);
    assertThat(suggestions.getCaveat(0).getMessage()).contains("empty");
  }

  @Test
  public void doesNotCreateSuggestionsIfEstimatedCoresUsedIsEmpty()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats(List.of()));
    when(dataManager.getDatum(EstimatedCoresUsed.class))
        .thenReturn(new EstimatedCoresUsed("empty"));
    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, 0, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).isEmpty();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getCaveatList()).hasSize(1);
    assertThat(suggestions.getCaveat(0).getMessage())
        .contains(BottleneckSuggestionProvider.EMPTY_REASON_PREFIX);
    assertThat(suggestions.getCaveat(0).getMessage()).contains("empty");
  }

  @Test
  public void doesNotCreateSuggestionsIfTotalDurationIsEmpty()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats(List.of()));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(TotalDuration.class)).thenReturn(new TotalDuration("empty"));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, 0, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).isEmpty();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getCaveatList()).hasSize(1);
    assertThat(suggestions.getCaveat(0).getMessage())
        .contains(BottleneckSuggestionProvider.EMPTY_REASON_PREFIX);
    assertThat(suggestions.getCaveat(0).getMessage()).contains("empty");
  }

  @Test
  public void doesNotCreateSuggestionsIfThereAreNoBottlenecks()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats(List.of()));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, 0, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).isEmpty();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }

  @Test
  public void doesNotCreateSuggestionsIfBottlenecksAreShorterThanMinDuration()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    final var minDuration = Duration.ofSeconds(2);
    Bottleneck bottleneck =
        Bottleneck.newBuilder(Timestamp.ofSeconds(0))
            .setEnd(Timestamp.ofSeconds(minDuration.getSeconds() - 1))
            .addActionCountSample(1)
            .build();

    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats(List.of(bottleneck)));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, minDuration, 0, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).isEmpty();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getSuggestionList()).isEmpty();
  }

  @Test
  public void createSuggestionIfThereAreBottlenecks()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    Bottleneck bottleneck =
        Bottleneck.newBuilder(Timestamp.ofSeconds(0))
            .setEnd(Timestamp.ofSeconds(10))
            .addActionCountSample(1)
            .build();

    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats(List.of(bottleneck)));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, 0, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).hasSize(1);
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }

  @Test
  public void createsQueuingSuggestionIfThereIsQueuing()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    Bottleneck bottleneck =
        Bottleneck.newBuilder(Timestamp.ofSeconds(0))
            .setEnd(Timestamp.ofSeconds(10))
            .addActionCountSample(1)
            .addQueuingDuration(new ThreadId(0, 0), Duration.ofSeconds(8))
            .build();

    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats(List.of(bottleneck)));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, 0, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).hasSize(1);
    assertThat(suggestions.getSuggestion(0).getId())
        .contains(SUGGESTION_ID_AVOID_BOTTLENECKS_DUE_TO_QUEUING);
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }

  @Test
  public void createsBreakDownActionsSuggestionOnLittleQueuing()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    Bottleneck bottleneck =
        Bottleneck.newBuilder(Timestamp.ofSeconds(0))
            .setEnd(Timestamp.ofSeconds(100))
            .addActionCountSample(1)
            .addQueuingDuration(new ThreadId(0, 0), Duration.ofSeconds(1))
            .build();

    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats(List.of(bottleneck)));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, 0, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).hasSize(1);
    assertThat(suggestions.getSuggestion(0).getId())
        .contains(SUGGESTION_ID_BREAK_DOWN_BOTTLENECK_ACTIONS);
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }

  @Test
  public void doesNotAddSuggestionIfBelowMinImprovementRatio()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    Bottleneck bottleneck =
        Bottleneck.newBuilder(Timestamp.ofSeconds(0))
            .setEnd(Timestamp.ofSeconds(10))
            .addActionCountSample(1)
            .build();

    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats(List.of(bottleneck)));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, 100, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).isEmpty();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }

  @Test
  public void doesNotAddSuggestionIfAboveMaxActionCountRatio()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    int actionCountSample = 1;
    int coresUsed = 4;
    Bottleneck bottleneck =
        Bottleneck.newBuilder(Timestamp.ofSeconds(0))
            .setEnd(Timestamp.ofSeconds(10))
            .addActionCountSample(actionCountSample)
            .build();

    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class))
        .thenReturn(new EstimatedCoresUsed(coresUsed, 0));
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats(List.of(bottleneck)));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(
            1, 1, Duration.ZERO, 100, actionCountSample / (double) (coresUsed + 1));
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).isEmpty();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }

  @Test
  public void createsOnlyLimitedSuggestions()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    Bottleneck minorBottleneck =
        Bottleneck.newBuilder(Timestamp.ofSeconds(0))
            .setEnd(Timestamp.ofSeconds(10))
            .addActionCountSample(1)
            .build();
    Bottleneck majorBottleneck =
        Bottleneck.newBuilder(Timestamp.ofSeconds(10))
            .setEnd(Timestamp.ofSeconds(500))
            .addActionCountSample(1)
            .build();
    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class))
        .thenReturn(new ActionStats(List.of(minorBottleneck, majorBottleneck)));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, 0, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionCount()).isEqualTo(1);
    assertThat(suggestions.getSuggestion(0).getRationaleCount()).isEqualTo(1);
    assertThat(suggestions.getSuggestion(0).getRationale(0))
        .contains(
            String.format(" %s ", DurationUtil.formatDuration(majorBottleneck.getDuration())));
    assertThat(suggestions.getCaveatList()).hasSize(1);
    assertThat(suggestions.getCaveat(0).getSuggestVerboseMode()).isTrue();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }
}
