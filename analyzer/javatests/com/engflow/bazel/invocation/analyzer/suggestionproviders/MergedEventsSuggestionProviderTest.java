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
import com.engflow.bazel.invocation.analyzer.dataproviders.MergedEventsPresent;
import org.junit.Before;
import org.junit.Test;

public class MergedEventsSuggestionProviderTest extends SuggestionProviderUnitTestBase {
  @Before
  public void setup() {
    suggestionProvider = new MergedEventsSuggestionProvider();
  }

  @Test
  public void shouldNotReturnSuggestionWhenNoMergedEventsArePresent() throws Exception {
    when(dataManager.getDatum(MergedEventsPresent.class))
        .thenReturn(new MergedEventsPresent(false));
    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    verify(dataManager).getDatum(MergedEventsPresent.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(MergedEventsSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldReturnSuggestionWhenMergedEventsArePresent() throws Exception {
    when(dataManager.getDatum(MergedEventsPresent.class)).thenReturn(new MergedEventsPresent(true));
    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    verify(dataManager).getDatum(MergedEventsPresent.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(MergedEventsSuggestionProvider.class.getName());
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }
}
