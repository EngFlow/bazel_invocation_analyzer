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

import com.engflow.bazel.invocation.analyzer.Caveat;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelEventsUtil;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.FlagValueExperimentalProfileIncludeTargetLabel;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteCachingUsed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link SuggestionProvider} that suggests making non-remote-cacheable actions cacheable when
 * remote caching is used.
 */
public class NoCacheActionsSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME = NoCacheActionsSuggestionProvider.class.getName();
  private static final String SUGGESTION_ID_NO_CACHE_ACTIONS = "NoCacheActions";

  /** The default minimum duration actions need to take to be included in the suggestions. */
  @VisibleForTesting static final Duration MIN_DURATION = Duration.ofSeconds(5);

  public static NoCacheActionsSuggestionProvider createDefault() {
    return new NoCacheActionsSuggestionProvider(MIN_DURATION, 5);
  }

  public static NoCacheActionsSuggestionProvider createVerbose() {
    return new NoCacheActionsSuggestionProvider(Duration.ZERO, Integer.MAX_VALUE);
  }

  private final Duration minDuration;

  private final int maxActions;

  @VisibleForTesting
  NoCacheActionsSuggestionProvider(Duration minDuration, int maxActions) {
    this.minDuration = minDuration;
    this.maxActions = maxActions;
  }

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      boolean remoteCachingUsed =
          dataManager.getDatum(RemoteCachingUsed.class).isRemoteCachingUsed();
      if (!remoteCachingUsed) {
        return noSuggestions();
      }
      var localActions = dataManager.getDatum(LocalActions.class);
      if (localActions.isEmpty()) {
        return noSuggestions();
      }
      var actionsWithoutRemoteCacheCheck =
          localActions.stream()
              .filter(action -> !action.hasRemoteCacheCheck())
              .sorted((a, b) -> b.getAction().duration.compareTo(a.getAction().duration))
              .collect(Collectors.toList());
      if (actionsWithoutRemoteCacheCheck.isEmpty()) {
        return noSuggestions();
      }
      var longEnoughActions =
          actionsWithoutRemoteCacheCheck.stream()
              .filter(action -> minDuration.compareTo(action.getAction().duration) <= 0)
              .collect(Collectors.toList());
      if (longEnoughActions.isEmpty()) {
        return SuggestionProviderUtil.createSuggestionOutput(
            ANALYZER_CLASSNAME,
            null,
            ImmutableList.of(
                SuggestionProviderUtil.createCaveat(
                    String.format(
                        "No actions that are not using remote caching were highlighted. None of the"
                            + " %d actions that do not check the remote cache took sufficiently"
                            + " long.",
                        actionsWithoutRemoteCacheCheck.size()),
                    true)));
      }
      List<Caveat> caveats = new ArrayList<>();
      var targetLabelIncluded =
          dataManager.getDatum(FlagValueExperimentalProfileIncludeTargetLabel.class);
      if (!targetLabelIncluded.isProfileIncludeTargetLabelEnabled()) {
        caveats.add(
            SuggestionProviderUtil.createCaveat(
                FlagValueExperimentalProfileIncludeTargetLabel.getNotSetButUsefulForStatement(
                    "investigating actions that are not using remote caching"),
                false));
      }
      if (longEnoughActions.size() > maxActions) {
        caveats.add(
            SuggestionProviderUtil.createCaveat(
                String.format(
                    "Only the %d longest actions that did not check the remote cache of the"
                        + " %d found were listed.",
                    maxActions, actionsWithoutRemoteCacheCheck.size()),
                true));
      } else if (longEnoughActions.size() < actionsWithoutRemoteCacheCheck.size()) {
        caveats.add(
            SuggestionProviderUtil.createCaveat(
                String.format(
                    "%d actions did not take long enough to be listed.",
                    actionsWithoutRemoteCacheCheck.size() - longEnoughActions.size()),
                true));
      }
      StringBuilder recommendation = new StringBuilder();
      recommendation.append(
          "Some actions did not check the remote cache. Likely the targets the actions were"
              + " executed for include the tag `no-cache` or `no-remote-cache`. Investigate"
              + " whether these tags can be removed:");
      longEnoughActions.stream()
          .limit(maxActions)
          .forEachOrdered(
              action ->
                  recommendation.append(
                      "\n" + BazelEventsUtil.summarizeCompleteEvent(action.getAction())));
      var suggestions =
          ImmutableList.of(
              SuggestionProviderUtil.createSuggestion(
                  SuggestionCategory.OTHER,
                  createSuggestionId(SUGGESTION_ID_NO_CACHE_ACTIONS),
                  "Investigate actions that are not using remote caching",
                  recommendation.toString(),
                  null,
                  ImmutableList.of(
                      "The profile suggests remote caching was used, but some actions did not"
                          + " check the remote cache. In most cases, enabling remote caching"
                          + " for actions improves build performance. An example case where"
                          + " using remote caching might not be beneficial is when the outputs"
                          + " are very large and the the cost of downloading them is higher"
                          + " than locally building the target."),
                  caveats));
      return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, suggestions, null);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }

  private static SuggestionOutput noSuggestions() {
    return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, null, null);
  }
}
