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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import org.junit.Before;
import org.junit.Test;

public abstract class SuggestionProviderUnitTestBase {
  /** DataManager to coordinate the transfer of data between Providers */
  protected DataManager dataManager;

  /** Suggestion provider to test. Must be set by the subclass. */
  protected SuggestionProvider suggestionProvider;

  @Before
  public void setupTestBase() {
    dataManager = mock(DataManager.class);
  }

  // This verifies that MissingInputException gets included in the missingInputs field
  // of the SuggestionOutput. It assumes that a SuggestionProvider uses at least one
  // piece of data from the DataManager for analysis. Note that if only some inputs are
  // missing, a suggestion provider may be able to make some suggestions and/or guesses
  // about suggestions based on the data that is available, and return both Suggestions
  // along with the missingInputs.
  @Test
  public void shouldIncludeMissingInputsWhenAllInputsAreMissing() throws Exception {
    assertNotNull(
        "suggestionProvider must be set by the subclass. Set super.suggestionProvider to an"
            + " instance of the SuggestionProvider in a @Before method.",
        suggestionProvider);

    // Save the class type requested for later comparison. Must be wrapped in a final object to use
    // in a lambda.
    final var savedParameter =
        new Object() {
          Class classRequested;
        };

    // Throw MissingInputException when any Datum is requested
    when(dataManager.getDatum(any()))
        .thenAnswer(
            x -> {
              savedParameter.classRequested = x.getArgument(0);
              throw new MissingInputException(savedParameter.classRequested);
            });

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getMissingInputList()).isNotEmpty();
    assertThat(suggestionOutput.getMissingInputList().size()).isGreaterThan(0);
    assertThat(suggestionOutput.getMissingInputList())
        .contains(savedParameter.classRequested.getName());
  }
}
