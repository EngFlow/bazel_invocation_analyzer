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

import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.dataproviders.CriticalPathDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresAvailable;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;

public class IncompleteProfileSuggestionProviderTest extends SuggestionProviderUnitTestBase {
  // These variables are returned from calls to DataManager.getDatum for the associated types. They
  // are set up with reasonable defaults before each test is run, but can be overridden within the
  // tests when custom values are desired for the testing being conducted (without the need to
  // re-initialize the mocking).
  private CriticalPathDuration criticalPathDuration;
  private TotalDuration totalDuration;
  private EstimatedCoresAvailable estimatedCoresAvailable;
  private EstimatedCoresUsed estimatedCoresUsed;

  @Before
  public void setup() throws Exception {
    // Create reasonable defaults and set up to return the class-variables when the associated types
    // are requested.
    criticalPathDuration = new CriticalPathDuration(Duration.ofSeconds(10));
    when(dataManager.getDatum(CriticalPathDuration.class)).thenAnswer(i -> criticalPathDuration);
    totalDuration = new TotalDuration(Duration.ofSeconds(60));
    when(dataManager.getDatum(TotalDuration.class)).thenAnswer(i -> totalDuration);
    estimatedCoresAvailable = new EstimatedCoresAvailable(16, 0);
    when(dataManager.getDatum(EstimatedCoresAvailable.class))
        .thenAnswer(i -> estimatedCoresAvailable);
    estimatedCoresUsed = new EstimatedCoresUsed(8, 0);
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenAnswer(i -> estimatedCoresUsed);

    suggestionProvider = new IncompleteProfileSuggestionProvider();
  }

  @Test
  public void shouldNotReturnSuggestionIfDataIsPresent() throws Exception {
    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(IncompleteProfileSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getCaveatList()).isEmpty();
    assertThat(suggestionOutput.getMissingInputList()).isEmpty();
  }

  @Test
  public void shouldReturnSuggestionForEmptyCriticalPathDuration() {
    criticalPathDuration = new CriticalPathDuration("empty");

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(IncompleteProfileSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isNotEmpty();
  }

  @Test
  public void shouldReturnSuggestionForEmptyEstimatedCoresAvailable() {
    estimatedCoresAvailable = new EstimatedCoresAvailable("empty");

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(IncompleteProfileSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isNotEmpty();
  }

  @Test
  public void shouldReturnSuggestionForEmptyEstimatedCoresUsed() {
    estimatedCoresUsed = new EstimatedCoresUsed("empty");

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(IncompleteProfileSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isNotEmpty();
  }
}
