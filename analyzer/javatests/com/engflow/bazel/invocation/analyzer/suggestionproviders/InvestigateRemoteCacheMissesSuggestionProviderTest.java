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
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_SUBPROCESS_RUN;
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

public class InvestigateRemoteCacheMissesSuggestionProviderTest
    extends SuggestionProviderUnitTestBase {
  // These variables are returned from calls to DataManager.getDatum for the associated types. They
  // are set up with reasonable defaults before each test is run, but can be overridden within the
  // tests when custom values are desired for the testing being conducted (without the need to
  // re-initialize the mocking).
  private RemoteCachingUsed remoteCachingUsed;
  private LocalActions localActions;
  private FlagValueExperimentalProfileIncludeTargetLabel experimentalProfileIncludeTargetLabel;

  @Before
  public void setup() throws Exception {
    // Create reasonable defaults and set up to return the class-variables when the associated types
    // are requested.
    experimentalProfileIncludeTargetLabel =
        new FlagValueExperimentalProfileIncludeTargetLabel(true);
    when(dataManager.getDatum(RemoteCachingUsed.class)).thenAnswer(i -> remoteCachingUsed);
    when(dataManager.getDatum(LocalActions.class)).thenAnswer(i -> localActions);
    when(dataManager.getDatum(FlagValueExperimentalProfileIncludeTargetLabel.class))
        .thenAnswer(i -> experimentalProfileIncludeTargetLabel);

    suggestionProvider = InvestigateRemoteCacheMissesSuggestionProvider.createDefault();
  }

  @Test
  public void shouldNotReturnSuggestionIfNoRemoteCachingIsUsed() {
    remoteCachingUsed = new RemoteCachingUsed(false);

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldNotReturnSuggestionWithoutCacheMisses() {
    remoteCachingUsed = new RemoteCachingUsed(true);
    var thread = new EventThreadBuilder(1, 1);
    var actionWitRemoteCacheHit =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("action with cache hit", "a", 10, 10),
            List.of(
                thread.related(10, 1, CAT_REMOTE_ACTION_CACHE_CHECK),
                thread.related(12, 2, CAT_REMOTE_OUTPUT_DOWNLOAD)));
    var actionWithoutRemoteCaching =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("action without a cache check", "a", 10, 10),
            List.of(thread.related(12, 2, CAT_LOCAL_ACTION_EXECUTION)));
    localActions =
        LocalActions.create(List.of(actionWitRemoteCacheHit, actionWithoutRemoteCaching));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getSuggestionList()).isEmpty();
    assertThat(suggestionOutput.hasFailure()).isFalse();
  }

  @Test
  public void shouldReturnSuggestionIfThereAreCacheMisses() {
    remoteCachingUsed = new RemoteCachingUsed(true);
    var thread = new EventThreadBuilder(1, 1);
    var cacheMissWithLocalExec =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("cache miss action with local execution", "a", 10, 10),
            List.of(
                thread.related(10, 1, CAT_REMOTE_ACTION_CACHE_CHECK),
                thread.related(12, 2, CAT_GENERAL_INFORMATION, COMPLETE_SUBPROCESS_RUN)));
    var cacheMissWithRemoteExec =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("cache miss with remote execution", "a", 10, 10),
            List.of(
                thread.related(10, 1, CAT_REMOTE_ACTION_CACHE_CHECK),
                thread.related(12, 2, CAT_REMOTE_ACTION_EXECUTION),
                thread.related(14, 1, CAT_REMOTE_OUTPUT_DOWNLOAD)));
    var cacheHit =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("example cache hit action", "a", 10, 10),
            List.of(
                thread.related(10, 1, CAT_REMOTE_ACTION_CACHE_CHECK),
                thread.related(11, 5, CAT_REMOTE_OUTPUT_DOWNLOAD)));
    localActions =
        LocalActions.create(List.of(cacheMissWithLocalExec, cacheMissWithRemoteExec, cacheHit));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(InvestigateRemoteCacheMissesSuggestionProvider.class.getName());
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    var suggestion = suggestionOutput.getSuggestion(0);
    assertThat(suggestion.getRecommendation()).contains(cacheMissWithLocalExec.getAction().name);
    assertThat(suggestion.getRecommendation()).contains(cacheMissWithRemoteExec.getAction().name);
    assertThat(suggestion.getRecommendation()).doesNotContain(cacheHit.getAction().name);
    assertThat(suggestion.getCaveatList()).isEmpty();
  }

  @Test
  public void shouldReturnSuggestionWithCaveatIfThereAreManyCacheMisses() {
    remoteCachingUsed = new RemoteCachingUsed(true);
    suggestionProvider = new InvestigateRemoteCacheMissesSuggestionProvider(1);
    var thread = new EventThreadBuilder(1, 1);
    var cacheMissWithLocalExec =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("cache miss action with local execution", "a", 10, 10),
            List.of(
                thread.related(10, 1, CAT_REMOTE_ACTION_CACHE_CHECK),
                thread.related(12, 2, CAT_GENERAL_INFORMATION, COMPLETE_SUBPROCESS_RUN)));
    var cacheMissWithRemoteExec =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("cache miss with remote execution", "a", 10, 100),
            List.of(
                thread.related(10, 1, CAT_REMOTE_ACTION_CACHE_CHECK),
                thread.related(12, 90, CAT_REMOTE_ACTION_EXECUTION),
                thread.related(14, 1, CAT_REMOTE_OUTPUT_DOWNLOAD)));
    var cacheHit =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("example cache hit action", "a", 10, 10),
            List.of(
                thread.related(10, 1, CAT_REMOTE_ACTION_CACHE_CHECK),
                thread.related(11, 5, CAT_REMOTE_OUTPUT_DOWNLOAD)));
    localActions =
        LocalActions.create(List.of(cacheMissWithLocalExec, cacheMissWithRemoteExec, cacheHit));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(InvestigateRemoteCacheMissesSuggestionProvider.class.getName());
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    var suggestion = suggestionOutput.getSuggestion(0);
    // This is the longest cache miss. It is included.
    assertThat(suggestion.getRecommendation()).contains(cacheMissWithRemoteExec.getAction().name);
    // This is another cache miss, but it exceeds the max count, so it's not included.
    assertThat(suggestion.getRecommendation())
        .doesNotContain(cacheMissWithLocalExec.getAction().name);
    assertThat(suggestion.getRecommendation()).doesNotContain(cacheHit.getAction().name);
    assertThat(suggestion.getCaveatCount()).isEqualTo(1);
    assertThat(suggestion.getCaveat(0).getSuggestVerboseMode()).isTrue();
  }

  @Test
  public void shouldReturnSuggestionWithCaveatIfTheTargetNamesAreMissing() {
    remoteCachingUsed = new RemoteCachingUsed(true);
    experimentalProfileIncludeTargetLabel =
        new FlagValueExperimentalProfileIncludeTargetLabel(false);
    var thread = new EventThreadBuilder(1, 1);
    var cacheMissWithLocalExec =
        new LocalActions.LocalAction(
            thread.actionProcessingAction("cache miss action with local execution", "a", 10, 10),
            List.of(
                thread.related(10, 1, CAT_REMOTE_ACTION_CACHE_CHECK),
                thread.related(12, 2, CAT_GENERAL_INFORMATION, COMPLETE_SUBPROCESS_RUN)));
    localActions = LocalActions.create(List.of(cacheMissWithLocalExec));

    SuggestionOutput suggestionOutput = suggestionProvider.getSuggestions(dataManager);

    assertThat(suggestionOutput.getAnalyzerClassname())
        .isEqualTo(InvestigateRemoteCacheMissesSuggestionProvider.class.getName());
    assertThat(suggestionOutput.hasFailure()).isFalse();
    assertThat(suggestionOutput.getSuggestionList().size()).isEqualTo(1);
    var suggestion = suggestionOutput.getSuggestion(0);
    assertThat(suggestion.getRecommendation()).contains(cacheMissWithLocalExec.getAction().name);
    assertThat(suggestion.getCaveatCount()).isEqualTo(1);
    assertThat(suggestion.getCaveat(0).getMessage())
        .contains(FlagValueExperimentalProfileIncludeTargetLabel.FLAG_NAME);
    assertThat(suggestion.getCaveat(0).getSuggestVerboseMode()).isFalse();
  }
}
