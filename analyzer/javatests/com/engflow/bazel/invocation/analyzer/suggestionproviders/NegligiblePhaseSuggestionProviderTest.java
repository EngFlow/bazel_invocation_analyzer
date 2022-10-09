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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelPhaseDescription;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelPhaseDescriptions;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;

public class NegligiblePhaseSuggestionProviderTest extends SuggestionProviderUnitTestBase {
  // These variables are returned from calls to DataManager.getDatum for the associated types. They
  // are set up with reasonable defaults before each test is run, but can be overridden within the
  // tests when custom values are desired for the testing being conducted (without the need to
  // re-initialize the mocking).
  private TotalDuration totalDuration;
  private BazelPhaseDescriptions.Builder bazelPhaseDescriptions;

  @Before
  public void setup() throws Exception {
    // Create reasonable defaults and set up to return the class-variables when the associated types
    // are requested.
    totalDuration = new TotalDuration(Duration.ofSeconds(60));
    when(dataManager.getDatum(TotalDuration.class)).thenAnswer(i -> totalDuration);
    bazelPhaseDescriptions = BazelPhaseDescriptions.newBuilder();
    when(dataManager.getDatum(BazelPhaseDescriptions.class))
        .thenAnswer(i -> bazelPhaseDescriptions.build());

    suggestionProvider = new NegligiblePhaseSuggestionProvider();
  }

  @Test
  public void shouldNotReturnSuggestionForMissingBazelPhaseDescriptions() {
    bazelPhaseDescriptions = null;
    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(NegligiblePhaseSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getMissingInputList())
        .contains(BazelPhaseDescriptions.class.getName());
  }

  @Test
  public void shouldNotReturnSuggestionForMissingTotalDuration() {
    totalDuration = null;
    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(NegligiblePhaseSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getMissingInputList()).contains(TotalDuration.class.getName());
  }

  @Test
  public void shouldNotReturnSuggestionForShortDuration() throws Exception {
    totalDuration = new TotalDuration(Duration.ofMillis(20));
    for (BazelProfilePhase phase : BazelProfilePhase.values()) {
      bazelPhaseDescriptions.add(
          phase, new BazelPhaseDescription(Timestamp.ofMicros(0), Timestamp.ofSeconds(5)));
    }

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    verify(dataManager).getDatum(TotalDuration.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(NegligiblePhaseSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getMissingInputList()).isEmpty();
  }

  @Test
  public void shouldNotReturnSuggestionForMissingPhases() {
    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(NegligiblePhaseSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getMissingInputList()).isEmpty();
  }

  @Test
  public void shouldNotProduceOutputForNegligiblePhasesThatAreNegligible() throws Exception {
    for (BazelProfilePhase phase : BazelProfilePhase.values()) {
      bazelPhaseDescriptions.add(
          phase, new BazelPhaseDescription(Timestamp.ofMicros(0), Timestamp.ofSeconds(1)));
    }

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    verify(dataManager).getDatum(TotalDuration.class);
    verify(dataManager).getDatum(BazelPhaseDescriptions.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(NegligiblePhaseSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotProduceOutputForNonNegligiblePhasesThatAreNonNegligible() throws Exception {
    for (BazelProfilePhase phase : BazelProfilePhase.values()) {
      if (NegligiblePhaseSuggestionProvider.NON_NEGLIGIBLE_PHASES.contains(phase)) {
        bazelPhaseDescriptions.add(
            phase, new BazelPhaseDescription(Timestamp.ofMicros(0), Timestamp.ofSeconds(25)));
      } else {
        bazelPhaseDescriptions.add(
            phase, new BazelPhaseDescription(Timestamp.ofMicros(0), Timestamp.ofSeconds(1)));
      }
    }

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    verify(dataManager).getDatum(TotalDuration.class);
    verify(dataManager).getDatum(BazelPhaseDescriptions.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(NegligiblePhaseSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldProduceOutputForNegligiblePhasesThatAreNonNegligible() throws Exception {
    for (BazelProfilePhase phase : BazelProfilePhase.values()) {
      if (phase == BazelProfilePhase.PREPARE || phase == BazelProfilePhase.FINISH) {
        bazelPhaseDescriptions.add(
            phase, new BazelPhaseDescription(Timestamp.ofMicros(0), Timestamp.ofSeconds(25)));
      } else {
        bazelPhaseDescriptions.add(
            phase, new BazelPhaseDescription(Timestamp.ofMicros(0), Timestamp.ofSeconds(1)));
      }
    }

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    verify(dataManager).getDatum(TotalDuration.class);
    verify(dataManager).getDatum(BazelPhaseDescriptions.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(NegligiblePhaseSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(2);
    assertThat(suggestionOutput.getSuggestionList().get(0).getRecommendation())
        .contains(BazelProfilePhase.PREPARE.name);
    assertThat(suggestionOutput.getSuggestionList().get(1).getRecommendation())
        .contains(BazelProfilePhase.FINISH.name);
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }
}
