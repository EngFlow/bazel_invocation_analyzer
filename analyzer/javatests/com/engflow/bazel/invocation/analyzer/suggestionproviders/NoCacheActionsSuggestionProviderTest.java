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

import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_LOCAL_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.EventThreadBuilder;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.dataproviders.FlagValueExperimentalProfileIncludeTargetLabel;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteCachingUsed;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class NoCacheActionsSuggestionProviderTest extends SuggestionProviderUnitTestBase {
  // These variables are returned from calls to DataManager.getDatum for the associated types. They
  // are set up with reasonable defaults before each test is run, but can be overridden within the
  // tests when custom values are desired for the testing being conducted (without the need to
  // re-initialize the mocking).
  private RemoteCachingUsed remoteCachingUsed;
  private LocalActions localActions;
  private FlagValueExperimentalProfileIncludeTargetLabel actionsIncludeTargetName;

  @Before
  public void setup() throws Exception {
    // Create reasonable defaults and set up to return the class-variables when the associated types
    // are requested.
    actionsIncludeTargetName = new FlagValueExperimentalProfileIncludeTargetLabel(true);
    when(dataManager.getDatum(RemoteCachingUsed.class)).thenAnswer(i -> remoteCachingUsed);
    when(dataManager.getDatum(LocalActions.class)).thenAnswer(i -> localActions);
    when(dataManager.getDatum(FlagValueExperimentalProfileIncludeTargetLabel.class))
        .thenAnswer(i -> actionsIncludeTargetName);

    suggestionProvider = NoCacheActionsSuggestionProvider.createDefault();
  }

  @Test
  public void shouldNotReturnSuggestionIfRemoteCachingIsNotUsed() {
    remoteCachingUsed = new RemoteCachingUsed(false);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotReturnSuggestionForRemoteCachingInvocationWithoutLocalActions() {
    remoteCachingUsed = new RemoteCachingUsed(true);
    localActions = LocalActions.create(List.of());

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotReturnSuggestionForRemoteCachingInvocationWithRemoteCacheCheck() {
    remoteCachingUsed = new RemoteCachingUsed(true);
    var thread = new EventThreadBuilder(1, 1);
    var actionWithCacheCheck =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("some remote action", "b", 20, 10),
            List.of(thread.related(20, 2, CAT_REMOTE_ACTION_CACHE_CHECK)));
    localActions = LocalActions.create(List.of(actionWithCacheCheck));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldReturnSuggestionForRemoteCachingInvocationWithoutRemoteCacheCheck() {
    remoteCachingUsed = new RemoteCachingUsed(true);
    var thread = new EventThreadBuilder(1, 1);
    var actionWithRemoteCacheCheck =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("action with RC check", "a", 10, 10),
            List.of(thread.related(10, 2, CAT_REMOTE_ACTION_CACHE_CHECK)));
    var actionWithoutRemoteCacheCheckLocal =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("no RC check, local exec", "b", 20, 10),
            List.of(thread.related(20, 2, CAT_LOCAL_ACTION_EXECUTION)));
    var actionWithoutRemoteCacheCheckRemote =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("no RC check, remote exec", "c", 30, 10),
            List.of(thread.related(30, 2, CAT_REMOTE_ACTION_EXECUTION)));

    localActions =
        LocalActions.create(
            List.of(
                actionWithRemoteCacheCheck,
                actionWithoutRemoteCacheCheckLocal,
                actionWithoutRemoteCacheCheckRemote));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(NoCacheActionsSuggestionProvider.class.getName());
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    var suggestion = suggestionOutput.getSuggestion(0);
    assertThat(suggestion.getRecommendation())
        .contains(actionWithoutRemoteCacheCheckLocal.getAction().name);
    assertThat(suggestion.getRecommendation())
        .contains(actionWithoutRemoteCacheCheckRemote.getAction().name);
    assertThat(suggestion.getRecommendation())
        .doesNotContain(actionWithRemoteCacheCheck.getAction().name);
    assertThat(suggestion.getCaveatList()).isEmpty();
  }

  @Test
  public void shouldReturnSuggestionForRemoteCachingInvocationWithTooManyMatches() {
    suggestionProvider = new NoCacheActionsSuggestionProvider(1);
    remoteCachingUsed = new RemoteCachingUsed(true);
    var thread = new EventThreadBuilder(1, 1);
    var actionWithRemoteCacheCheck =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("action with RC check", "a", 10, 10),
            List.of(thread.related(10, 2, CAT_REMOTE_ACTION_CACHE_CHECK)));
    var actionWithoutRemoteCacheCheckLocal =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("no RC check, local exec", "b", 20, 10),
            List.of(thread.related(20, 2, CAT_LOCAL_ACTION_EXECUTION)));
    var actionWithoutRemoteCacheCheckRemote =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("no RC check, remote exec", "c", 30, 100),
            List.of(thread.related(30, 2, CAT_REMOTE_ACTION_EXECUTION)));

    localActions =
        LocalActions.create(
            List.of(
                actionWithRemoteCacheCheck,
                actionWithoutRemoteCacheCheckLocal,
                actionWithoutRemoteCacheCheckRemote));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(NoCacheActionsSuggestionProvider.class.getName());
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    var suggestion = suggestionOutput.getSuggestion(0);
    // This is the longest matching action. It is included.
    assertThat(suggestion.getRecommendation())
        .contains(actionWithoutRemoteCacheCheckRemote.getAction().name);
    // This is another matching action, but it exceeds the max count, so it's not included.
    assertThat(suggestion.getRecommendation())
        .doesNotContain(actionWithoutRemoteCacheCheckLocal.getAction().name);
    assertThat(suggestion.getRecommendation())
        .doesNotContain(actionWithRemoteCacheCheck.getAction().name);
    assertThat(suggestion.getCaveatCount()).isEqualTo(1);
    assertThat(suggestion.getCaveat(0).getSuggestVerboseMode()).isTrue();
  }

  @Test
  public void shouldReturnSuggestionForRemoteCachingInvocationWithTargetNameCaveat() {
    actionsIncludeTargetName = new FlagValueExperimentalProfileIncludeTargetLabel(false);
    remoteCachingUsed = new RemoteCachingUsed(true);
    var thread = new EventThreadBuilder(1, 1);
    var matchingAction =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("no RC check", "b", 20, 10), List.of());

    localActions = LocalActions.create(List.of(matchingAction));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(NoCacheActionsSuggestionProvider.class.getName());
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    var suggestion = suggestionOutput.getSuggestion(0);
    assertThat(suggestion.getRecommendation()).contains(matchingAction.getAction().name);
    assertThat(suggestion.getCaveatCount()).isEqualTo(1);
    assertThat(suggestion.getCaveat(0).getSuggestVerboseMode()).isFalse();
    assertThat(suggestion.getCaveat(0).getMessage())
        .contains(FlagValueExperimentalProfileIncludeTargetLabel.FLAG_NAME);
  }
}
