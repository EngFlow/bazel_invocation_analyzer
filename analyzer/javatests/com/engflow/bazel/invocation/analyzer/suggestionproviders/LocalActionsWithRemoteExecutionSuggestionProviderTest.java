/*
 * Copyright 2023 EngFlow Inc.
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

import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_GENERAL_INFORMATION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_LOCAL_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_SUBPROCESS_RUN;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.EventThreadBuilder;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionUsed;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class LocalActionsWithRemoteExecutionSuggestionProviderTest
    extends SuggestionProviderUnitTestBase {
  // These variables are returned from calls to DataManager.getDatum for the associated types. They
  // are set up with reasonable defaults before each test is run, but can be overridden within the
  // tests when custom values are desired for the testing being conducted (without the need to
  // re-initialize the mocking).
  private RemoteExecutionUsed remoteExecutionUsed;
  private LocalActions localActions;

  @Before
  public void setup() throws Exception {
    // Create reasonable defaults and set up to return the class-variables when the associated types
    // are requested.
    when(dataManager.getDatum(RemoteExecutionUsed.class)).thenAnswer(i -> remoteExecutionUsed);
    when(dataManager.getDatum(LocalActions.class)).thenAnswer(i -> localActions);

    suggestionProvider = LocalActionsWithRemoteExecutionSuggestionProvider.createDefault();
  }

  @Test
  public void shouldNotReturnSuggestionForLocalInvocation() {
    remoteExecutionUsed = new RemoteExecutionUsed(false);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotReturnSuggestionForRemoteExecutionInvocationWithoutLocalActions() {
    remoteExecutionUsed = new RemoteExecutionUsed(true);
    localActions = LocalActions.create(List.of());

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotReturnSuggestionForRemoteExecutionInvocationWithoutLocallyExecutedActions() {
    remoteExecutionUsed = new RemoteExecutionUsed(true);
    var thread = new EventThreadBuilder(1, 1);
    var remoteAction =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("some remote action", "b", 20, 10),
            List.of(thread.related(20, 2, CAT_REMOTE_ACTION_EXECUTION, "bar")));
    localActions = LocalActions.create(List.of(remoteAction));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldReturnSuggestionForRemoteExecutionInvocationWithLocalActions() {
    remoteExecutionUsed = new RemoteExecutionUsed(true);
    var thread = new EventThreadBuilder(1, 1);
    var localAction1 =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("a local action", "a", 10, 10),
            List.of(thread.related(10, 2, CAT_GENERAL_INFORMATION, COMPLETE_SUBPROCESS_RUN)));
    var remoteAction1 =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("some remote action", "b", 20, 10),
            List.of(thread.related(20, 2, CAT_REMOTE_ACTION_EXECUTION, "bar")));
    var localAction2 =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("another local action", "c", 30, 10),
            List.of(thread.related(30, 2, CAT_LOCAL_ACTION_EXECUTION, "baz")));

    localActions = LocalActions.create(List.of(localAction1, remoteAction1, localAction2));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(LocalActionsWithRemoteExecutionSuggestionProvider.class.getName());
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    var suggestion = suggestionOutput.getSuggestion(0);
    assertThat(suggestion.getRecommendation()).contains(localAction1.getAction().name);
    assertThat(suggestion.getRecommendation()).contains(localAction2.getAction().name);
    assertThat(suggestion.getRecommendation()).doesNotContain(remoteAction1.getAction().name);
    assertThat(suggestion.getCaveatList()).isEmpty();
  }

  @Test
  public void shouldReturnSuggestionForRemoteExecutionInvocationWithTooManyLocalActions() {
    suggestionProvider = new LocalActionsWithRemoteExecutionSuggestionProvider(1);
    remoteExecutionUsed = new RemoteExecutionUsed(true);
    var thread = new EventThreadBuilder(1, 1);
    var localAction1 =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("a local action", "a", 10, 10),
            List.of(thread.related(10, 2, CAT_GENERAL_INFORMATION, COMPLETE_SUBPROCESS_RUN)));
    var remoteAction1 =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("some remote action", "b", 20, 10),
            List.of(thread.related(20, 2, CAT_REMOTE_ACTION_EXECUTION, "bar")));
    var localAction2 =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("another local action", "c", 30, 100),
            List.of(thread.related(30, 2, CAT_LOCAL_ACTION_EXECUTION, "baz")));

    localActions = LocalActions.create(List.of(localAction1, remoteAction1, localAction2));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(LocalActionsWithRemoteExecutionSuggestionProvider.class.getName());
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    var suggestion = suggestionOutput.getSuggestion(0);
    // This is the longest local action. It is included.
    assertThat(suggestion.getRecommendation()).contains(localAction2.getAction().name);
    // This is another local action, but it exceeds the max count, so it's not included.
    assertThat(suggestion.getRecommendation()).doesNotContain(localAction1.getAction().name);
    assertThat(suggestion.getRecommendation()).doesNotContain(remoteAction1.getAction().name);
    assertThat(suggestion.getCaveatCount()).isEqualTo(1);
    assertThat(suggestion.getCaveat(0).getSuggestVerboseMode()).isTrue();
  }
}
