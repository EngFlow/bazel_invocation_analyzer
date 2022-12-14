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
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresAvailable;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedJobsFlagValue;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteCachingUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionUsed;
import org.junit.Before;
import org.junit.Test;

public class JobsSuggestionProviderTest extends SuggestionProviderUnitTestBase {
  @Before
  public void setup() {
    suggestionProvider = new JobsSuggestionProvider();
  }

  @Test
  public void doesNotCreateSuggestionsIfEstimatedJobsFlagValueIsEmpty()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    when(dataManager.getDatum(EstimatedJobsFlagValue.class))
        .thenReturn(new EstimatedJobsFlagValue("empty"));
    when(dataManager.getDatum(RemoteExecutionUsed.class))
        .thenReturn(new RemoteExecutionUsed(false));
    when(dataManager.getDatum(RemoteCachingUsed.class)).thenReturn(new RemoteCachingUsed(false));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getMissingInputList()).isEmpty();
    assertThat(suggestionOutput.getCaveatList()).hasSize(1);
    assertThat(suggestionOutput.getCaveat(0).getMessage())
        .contains(JobsSuggestionProvider.EMPTY_REASON_PREFIX);
    assertThat(suggestionOutput.getCaveat(0).getMessage()).contains("empty");
  }

  @Test
  public void doesNotCreateSuggestionsIfEstimatedCoresAvailableIsEmpty()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    when(dataManager.getDatum(EstimatedJobsFlagValue.class))
        .thenReturn(new EstimatedJobsFlagValue(4, true));
    when(dataManager.getDatum(RemoteExecutionUsed.class))
        .thenReturn(new RemoteExecutionUsed(false));
    when(dataManager.getDatum(RemoteCachingUsed.class)).thenReturn(new RemoteCachingUsed(false));
    when(dataManager.getDatum(EstimatedCoresAvailable.class))
        .thenReturn(new EstimatedCoresAvailable("empty"));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getMissingInputList()).isEmpty();
    assertThat(suggestionOutput.getCaveatList()).hasSize(1);
    assertThat(suggestionOutput.getCaveat(0).getMessage())
        .contains(JobsSuggestionProvider.EMPTY_REASON_PREFIX);
    assertThat(suggestionOutput.getCaveat(0).getMessage()).contains("empty");
  }

  @Test
  public void doesNotCreateSuggestionsIfEstimatedCoresUsedIsEmpty()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    when(dataManager.getDatum(EstimatedJobsFlagValue.class))
        .thenReturn(new EstimatedJobsFlagValue(4, true));
    when(dataManager.getDatum(RemoteExecutionUsed.class))
        .thenReturn(new RemoteExecutionUsed(false));
    when(dataManager.getDatum(RemoteCachingUsed.class)).thenReturn(new RemoteCachingUsed(false));
    when(dataManager.getDatum(EstimatedCoresAvailable.class))
        .thenReturn(new EstimatedCoresAvailable(2, 1));
    when(dataManager.getDatum(EstimatedCoresUsed.class))
        .thenReturn(new EstimatedCoresUsed("empty"));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getMissingInputList()).isEmpty();
    assertThat(suggestionOutput.getCaveatList()).hasSize(1);
    assertThat(suggestionOutput.getCaveat(0).getMessage())
        .contains(JobsSuggestionProvider.EMPTY_REASON_PREFIX);
    assertThat(suggestionOutput.getCaveat(0).getMessage()).contains("empty");
  }

  @Test
  public void doesNotCreateSuggestionsIfJobsFlagValueIsLikelyNotSet()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    when(dataManager.getDatum(EstimatedJobsFlagValue.class))
        .thenReturn(new EstimatedJobsFlagValue(4, false));
    when(dataManager.getDatum(RemoteExecutionUsed.class))
        .thenReturn(new RemoteExecutionUsed(false));
    when(dataManager.getDatum(RemoteCachingUsed.class)).thenReturn(new RemoteCachingUsed(false));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getMissingInputList()).isEmpty();
  }

  @Test
  public void createsSuggestionsIfJobsFlagValueIsLikelySetAndLocal()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    when(dataManager.getDatum(EstimatedJobsFlagValue.class))
        .thenReturn(new EstimatedJobsFlagValue(4, true));
    when(dataManager.getDatum(RemoteExecutionUsed.class))
        .thenReturn(new RemoteExecutionUsed(false));
    when(dataManager.getDatum(RemoteCachingUsed.class)).thenReturn(new RemoteCachingUsed(false));
    when(dataManager.getDatum(EstimatedCoresAvailable.class))
        .thenReturn(new EstimatedCoresAvailable(7, 1));
    when(dataManager.getDatum(EstimatedCoresUsed.class)).thenReturn(new EstimatedCoresUsed(4, 0));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);
    assertThat(suggestionOutput.getSuggestionList()).hasSize(1);
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getMissingInputList()).isEmpty();
  }
}
