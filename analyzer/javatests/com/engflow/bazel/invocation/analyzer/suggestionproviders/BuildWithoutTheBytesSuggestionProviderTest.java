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
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionUsed;
import org.junit.Before;
import org.junit.Test;

public class BuildWithoutTheBytesSuggestionProviderTest extends SuggestionProviderUnitTestBase {
  // These variables are returned from calls to DataManager.getDatum for the associated types. They
  // are set up with reasonable defaults before each test is run, but can be overridden within the
  // tests when custom values are desired for the testing being conducted (without the need to
  // re-initialize the mocking).
  private RemoteExecutionUsed remoteExecutionUsed;

  @Before
  public void setup() throws Exception {
    // Create reasonable defaults and set up to return the class-variables when the associated types
    // are requested.
    remoteExecutionUsed = new RemoteExecutionUsed(true);
    when(dataManager.getDatum(RemoteExecutionUsed.class)).thenAnswer(i -> remoteExecutionUsed);

    suggestionProvider = new BuildWithoutTheBytesSuggestionProvider();
  }

  @Test
  public void shouldReturnSuggestionForRemoteExecutionInvocation() throws Exception {
    remoteExecutionUsed = new RemoteExecutionUsed(true);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    verify(dataManager).getDatum(RemoteExecutionUsed.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(BuildWithoutTheBytesSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotReturnSuggestionForLocalInvocation() throws Exception {
    remoteExecutionUsed = new RemoteExecutionUsed(false);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    verify(dataManager).getDatum(RemoteExecutionUsed.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }
}
