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

import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.dataproviders.GarbageCollectionStats;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import java.time.Duration;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;

public class GarbageCollectionSuggestionProviderTest extends SuggestionProviderUnitTestBase {
  // These variables are returned from calls to DataManager.getDatum for the associated types. They
  // are set up with reasonable defaults before each test is run, but can be overridden within the
  // tests when custom values are desired for the testing being conducted (without the need to
  // re-initialize the mocking).
  private GarbageCollectionStats garbageCollectionStats;
  private TotalDuration totalDuration;

  @Before
  public void setup() throws Exception {
    // Create reasonable defaults and set up to return the class-variables when the associated types
    // are requested.
    garbageCollectionStats = new GarbageCollectionStats(Duration.ofSeconds(1));
    when(dataManager.getDatum(GarbageCollectionStats.class))
        .thenAnswer(i -> garbageCollectionStats);
    totalDuration = new TotalDuration(Duration.ofSeconds(60));
    when(dataManager.getDatum(TotalDuration.class)).thenAnswer(i -> totalDuration);

    suggestionProvider = new GarbageCollectionSuggestionProvider();
  }

  @Test
  public void shouldNotReturnSuggestionForEmptyTotalDuration() {
    totalDuration = new TotalDuration("empty");

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(GarbageCollectionSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getCaveatList()).hasSize(1);
    assertThat(suggestionOutput.getCaveat(0).getMessage())
        .contains(GarbageCollectionSuggestionProvider.EMPTY_REASON_PREFIX);
    assertThat(suggestionOutput.getCaveat(0).getMessage()).contains("empty");
  }

  @Test
  public void shouldNotReturnSuggestionForNoMajorGarbageCollection() throws Exception {
    garbageCollectionStats = new GarbageCollectionStats(Duration.ZERO);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(GarbageCollectionSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldReturnSuggestionsForMajorGarbageCollection() {
    long fraction =
        (long) Math.floor(100 / GarbageCollectionSuggestionProvider.MAJOR_GC_MIN_PERCENTAGE);
    int secondsOfGc = 5;
    garbageCollectionStats = new GarbageCollectionStats(Duration.ofSeconds(secondsOfGc));
    totalDuration = new TotalDuration(Duration.ofSeconds(fraction * secondsOfGc));
    double percent = 100.0 / fraction;

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(GarbageCollectionSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(2);

    Suggestion heapSize = suggestionOutput.getSuggestionList().get(0);
    assertThat(heapSize.getCategory()).isEqualTo(SuggestionCategory.BAZEL_FLAGS);
    assertThat(heapSize.getTitle()).contains("heap size");
    assertThat(String.join(" ", heapSize.getRationaleList()))
        .contains(
            String.format(
                Locale.US,
                "%s or %.2f%% of the invocation is spent on major garbage collection",
                DurationUtil.formatDuration(
                    garbageCollectionStats.getMajorGarbageCollectionDuration()),
                percent));

    Suggestion rules = suggestionOutput.getSuggestionList().get(1);
    assertThat(rules.getCategory()).isEqualTo(SuggestionCategory.RULES);
    assertThat(rules.getTitle()).contains("rules");
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }
}
