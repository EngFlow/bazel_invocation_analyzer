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
import com.engflow.bazel.invocation.analyzer.dataproviders.CriticalPathDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.CriticalPathQueuingDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.QueuingObserved;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.TotalQueuingDuration;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;

public class QueuingSuggestionProviderTest extends SuggestionProviderUnitTestBase {
  // These variables are returned from calls to DataManager.getDatum for the associated types. They
  // are set up with reasonable defaults before each test is run, but can be overridden within the
  // tests when custom values are desired for the testing being conducted (without the need to
  // re-initialize the mocking).
  private TotalDuration totalDuration;
  private TotalQueuingDuration totalQueuingDuration;
  private CriticalPathQueuingDuration criticalPathQueuingDuration;
  private CriticalPathDuration criticalPathDuration;
  private QueuingObserved queuingObserved;

  @Before
  public void setup() throws Exception {
    // Create reasonable defaults and set up to return the class-variables when the associated types
    // are requested.
    totalDuration = new TotalDuration(Duration.ofSeconds(80));
    when(dataManager.getDatum(TotalDuration.class)).thenAnswer(i -> totalDuration);
    totalQueuingDuration = new TotalQueuingDuration(Duration.ofSeconds(50));
    when(dataManager.getDatum(TotalQueuingDuration.class)).thenAnswer(i -> totalQueuingDuration);
    criticalPathQueuingDuration = new CriticalPathQueuingDuration(Duration.ofSeconds(20));
    when(dataManager.getDatum(CriticalPathQueuingDuration.class))
        .thenAnswer(i -> criticalPathQueuingDuration);
    criticalPathDuration = new CriticalPathDuration(Duration.ofSeconds(60));
    when(dataManager.getDatum(CriticalPathDuration.class)).thenAnswer(i -> criticalPathDuration);
    queuingObserved = new QueuingObserved(true);
    when(dataManager.getDatum(QueuingObserved.class)).thenAnswer(i -> queuingObserved);

    suggestionProvider = new QueuingSuggestionProvider();
  }

  @Test
  public void shouldReturnSuggestionForInvocationWithQueuing() {
    Duration totalQueuing = Duration.ofSeconds(10);
    Duration criticalPathQueuing = Duration.ofSeconds(2);
    Duration criticalPath = Duration.ofSeconds(6);
    totalQueuingDuration = new TotalQueuingDuration(totalQueuing);
    criticalPathQueuingDuration = new CriticalPathQueuingDuration(criticalPathQueuing);
    criticalPathDuration = new CriticalPathDuration(criticalPath);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(QueuingSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldReturnSuggestionForInvocationWithoutQueuingInCriticalPath() {
    Duration totalQueuing = Duration.ofSeconds(10);
    Duration criticalPathQueuing = Duration.ZERO;
    Duration criticalPath = Duration.ofSeconds(6);
    totalQueuingDuration = new TotalQueuingDuration(totalQueuing);
    criticalPathQueuingDuration = new CriticalPathQueuingDuration(criticalPathQueuing);
    criticalPathDuration = new CriticalPathDuration(criticalPath);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(QueuingSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldReturnSuggestionForInvocationWithoutCriticalPath() {
    Duration totalQueuing = Duration.ofSeconds(10);
    totalQueuingDuration = new TotalQueuingDuration(totalQueuing);
    criticalPathQueuingDuration = null;
    criticalPathDuration = null;

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(QueuingSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotReturnSuggestionForInvocationWithoutQueuing() throws Exception {
    queuingObserved = new QueuingObserved(false);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    verify(dataManager).getDatum(QueuingObserved.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(QueuingSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }
}
