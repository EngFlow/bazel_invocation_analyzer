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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelPhaseDescription;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelPhaseDescriptions;
import com.engflow.bazel.invocation.analyzer.dataproviders.CriticalPathDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedJobsFlagValue;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionUsed;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;

public class CriticalPathNotDominantSuggestionProviderTest extends SuggestionProviderUnitTestBase {
  // These variables are returned from calls to DataManager.getDatum for the associated types. They
  // are set up with reasonable defaults before each test is run, but can be overridden within the
  // tests when custom values are desired for the testing being conducted (without the need to
  // re-initialize the mocking).
  private BazelPhaseDescriptions.Builder phases;
  @Nullable private CriticalPathDuration criticalPathDuration;
  private TotalDuration totalDuration;
  private RemoteExecutionUsed remoteExecutionUsed;
  private EstimatedCoresUsed estimatedCoresUsed;
  private EstimatedJobsFlagValue estimatedJobsFlagValue;

  @Before
  public void setup() throws Exception {
    dataManager = mock(DataManager.class);

    // Create reasonable defaults and set up to return the class-variables when the associated types
    // are requested.
    phases = BazelPhaseDescriptions.newBuilder();
    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenAnswer(i -> phases.build());
    criticalPathDuration = new CriticalPathDuration(Duration.ofSeconds(10));
    when(dataManager.getDatum(CriticalPathDuration.class)).thenAnswer(i -> criticalPathDuration);
    totalDuration = new TotalDuration(Duration.ofSeconds(100));
    when(dataManager.getDatum(TotalDuration.class)).thenAnswer(i -> totalDuration);
    remoteExecutionUsed = new RemoteExecutionUsed(false);
    when(dataManager.getDatum(RemoteExecutionUsed.class)).thenAnswer(i -> remoteExecutionUsed);
    estimatedCoresUsed = new EstimatedCoresUsed(4, 0);
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenAnswer(i -> estimatedCoresUsed);
    estimatedJobsFlagValue = new EstimatedJobsFlagValue(4, false);
    when(dataManager.getDatum(EstimatedJobsFlagValue.class))
        .thenAnswer(i -> estimatedJobsFlagValue);

    suggestionProvider = new CriticalPathNotDominantSuggestionProvider();
  }

  @Test
  public void shouldNotReturnSuggestionIfCriticalPathDurationIsMissing() throws Exception {
    phases.add(
        BazelProfilePhase.EXECUTE,
        new BazelPhaseDescription(Timestamp.ofMicros(0), Timestamp.ofSeconds(100)));
    criticalPathDuration = null;

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(CriticalPathNotDominantSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotReturnSuggestionForMissingExecutionPhase() throws Exception {
    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(CriticalPathNotDominantSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotReturnSuggestionForTooShortExecutionPhase() throws Exception {
    phases.add(
        BazelProfilePhase.EXECUTE,
        new BazelPhaseDescription(Timestamp.ofMicros(0), Timestamp.ofSeconds(1)));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(CriticalPathNotDominantSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotReturnSuggestionForInvocationWithDominantCriticalPath() throws Exception {
    int executionPhaseSeconds = 10;
    Duration criticalPath = Duration.ofSeconds(9);
    phases.add(
        BazelProfilePhase.EXECUTE,
        new BazelPhaseDescription(
            Timestamp.ofMicros(0), Timestamp.ofSeconds(executionPhaseSeconds)));
    criticalPathDuration = new CriticalPathDuration(criticalPath);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldReturnSuggestionForLocalInvocationWithNonDominantCriticalPath() {
    int executionPhaseSeconds = 20;
    Duration criticalPath = Duration.ofSeconds(9);
    Duration total = Duration.ofSeconds(22);
    phases.add(
        BazelProfilePhase.EXECUTE,
        new BazelPhaseDescription(
            Timestamp.ofMicros(0), Timestamp.ofSeconds(executionPhaseSeconds)));
    criticalPathDuration = new CriticalPathDuration(criticalPath);
    totalDuration = new TotalDuration(total);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    assertThat(suggestionOutput.getSuggestionList().get(0).getRecommendation())
        .contains("more cores");
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldReturnSuggestionForRemoteInvocationWithNonDominantCriticalPath() {
    int executionPhaseSeconds = 20;
    Duration criticalPath = Duration.ofSeconds(9);
    Duration total = Duration.ofSeconds(22);
    phases.add(
        BazelProfilePhase.EXECUTE,
        new BazelPhaseDescription(
            Timestamp.ofMicros(0), Timestamp.ofSeconds(executionPhaseSeconds)));
    criticalPathDuration = new CriticalPathDuration(criticalPath);
    totalDuration = new TotalDuration(total);
    remoteExecutionUsed = new RemoteExecutionUsed(true);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    assertThat(suggestionOutput.getSuggestionList().get(0).getRecommendation()).contains("--jobs");
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }
}
