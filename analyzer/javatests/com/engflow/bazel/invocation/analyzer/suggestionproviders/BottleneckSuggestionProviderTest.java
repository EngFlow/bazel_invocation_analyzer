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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.dataproviders.ActionStats;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
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
  public void doesNotCreateSuggestionsIfThereAreNoBottlenecks()
      throws MissingInputException, InvalidProfileException {
    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class)).thenReturn(new ActionStats(List.of()));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, .0);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).isEmpty();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }

  @Test
  public void doesNotCreateSuggestionsIfBottlenecksAreShorterThanMinDuration()
      throws MissingInputException, InvalidProfileException {
    final var minDuration = Duration.ofSeconds(2);
    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class))
        .thenReturn(
            new ActionStats(
                List.of(
                    new ActionStats.Bottleneck(Timestamp.ofSeconds(0)) {
                      {
                        setEndTs(Timestamp.ofSeconds(minDuration.getSeconds() - 1));
                        addActionCountSample(1);
                      }
                    })));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, minDuration, .0);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).isEmpty();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getSuggestionList()).isEmpty();
  }

  @Test
  public void createSuggestionIfThereAreBottlenecks()
      throws MissingInputException, InvalidProfileException {
    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class))
        .thenReturn(
            new ActionStats(
                List.of(
                    new ActionStats.Bottleneck(Timestamp.ofSeconds(0)) {
                      {
                        setEndTs(Timestamp.ofSeconds(10));
                        addActionCountSample(1);
                      }
                    })));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, .0);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).hasSize(1);
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }

  @Test
  public void doesNotAddSuggestionIfBelowMinImprovementRatio()
      throws MissingInputException, InvalidProfileException {
    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class))
        .thenReturn(
            new ActionStats(
                List.of(
                    new ActionStats.Bottleneck(Timestamp.ofSeconds(0)) {
                      {
                        setEndTs(Timestamp.ofSeconds(10));
                        addActionCountSample(1);
                      }
                    })));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, 1.);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).isEmpty();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }

  @Test
  public void createsOnlyLimitedSuggestions()
      throws MissingInputException, InvalidProfileException {
    when(dataManager.getDatum(TotalDuration.class))
        .thenReturn(new TotalDuration(Duration.ofSeconds(100)));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));
    when(dataManager.getDatum(ActionStats.class))
        .thenReturn(
            new ActionStats(
                List.of(
                    new ActionStats.Bottleneck(Timestamp.ofSeconds(0)) {
                      {
                        setEndTs(Timestamp.ofSeconds(10));
                        addActionCountSample(1);
                      }
                    },
                    new ActionStats.Bottleneck(Timestamp.ofSeconds(10)) {
                      {
                        setEndTs(Timestamp.ofSeconds(20));
                        addActionCountSample(1);
                      }
                    })));

    final var bottleneckSuggestionProvider =
        new BottleneckSuggestionProvider(1, 1, Duration.ZERO, .0);
    final var suggestions = bottleneckSuggestionProvider.getSuggestions(dataManager);
    assertThat(suggestions.getSuggestionList()).hasSize(1);
    assertThat(suggestions.getCaveatList()).hasSize(1);
    assertThat(suggestions.getCaveat(0).getSuggestVerboseMode()).isTrue();
    assertThat(suggestions.hasFailure()).isFalse();
    assertThat(suggestions.getMissingInputList()).isEmpty();
  }
}
