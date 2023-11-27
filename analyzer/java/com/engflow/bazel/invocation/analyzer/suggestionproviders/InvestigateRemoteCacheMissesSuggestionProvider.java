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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** A {@link SuggestionProvider} that suggests investigating cache misses. */
public class InvestigateRemoteCacheMissesSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME =
      InvestigateRemoteCacheMissesSuggestionProvider.class.getName();
  private static final String EMPTY_REASON_PREFIX = "No remote cache misses could be highlighted. ";

  private static final String INVESTIGATE_REMOTE_CACHE_MISSES = "InvestigateRemoteCacheMisses";

  public static InvestigateRemoteCacheMissesSuggestionProvider createDefault() {
    return new InvestigateRemoteCacheMissesSuggestionProvider(5);
  }

  public static InvestigateRemoteCacheMissesSuggestionProvider createVerbose() {
    return new InvestigateRemoteCacheMissesSuggestionProvider(Integer.MAX_VALUE);
  }

  /** The maximum number of cache miss actions to list in a suggestion. */
  private final int maxActions;

  @VisibleForTesting
  InvestigateRemoteCacheMissesSuggestionProvider(int maxActions) {
    this.maxActions = maxActions;
  }

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      RemoteCachingUsed remoteCachingUsed = dataManager.getDatum(RemoteCachingUsed.class);
      if (!remoteCachingUsed.isRemoteCachingUsed()) {
        return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
            ANALYZER_CLASSNAME, EMPTY_REASON_PREFIX + "Remote caching wasn't used.");
      }
      LocalActions localActions = dataManager.getDatum(LocalActions.class);
      var cacheMisses =
          localActions.stream()
              .filter(action -> !action.isRemoteCacheHit())
              .sorted((a, b) -> b.getAction().duration.compareTo(a.getAction().duration))
              .collect(Collectors.toList());
      if (cacheMisses.isEmpty()) {
        return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
            ANALYZER_CLASSNAME, EMPTY_REASON_PREFIX + "No cache misses were found.");
      }

      String title = "Investigate remote cache misses";
      StringBuilder recommendation = new StringBuilder();
      recommendation.append("The following actions with cache misses took the longest to execute:");
      cacheMisses.stream()
          .limit(maxActions)
          .forEachOrdered(
              action ->
                  recommendation.append(
                      "\n" + BazelEventsUtil.summarizeCompleteEvent(action.getAction())));
      recommendation.append(
          "\nCheck https://bazel.build/remote/cache-remote#troubleshooting-cache-hits to learn more"
              + " about how to debug remote cache misses. Increasing the cache hit rate can"
              + " significantly speed up builds.");
      var caveats = new ArrayList<Caveat>();
      var targetLabelIncluded =
          dataManager.getDatum(FlagValueExperimentalProfileIncludeTargetLabel.class);
      if (!targetLabelIncluded.isProfileIncludeTargetLabelEnabled()) {
        caveats.add(
            SuggestionProviderUtil.createCaveat(
                FlagValueExperimentalProfileIncludeTargetLabel.getNotSetButUsefulForStatement(
                    "investigating remote cache misses"),
                false));
      }
      if (cacheMisses.size() > maxActions) {
        caveats.add(
            SuggestionProviderUtil.createCaveat(
                String.format(
                    "Only the first %d of %d cache misses are listed.",
                    maxActions, cacheMisses.size()),
                true));
      }
      var suggestion =
          SuggestionProviderUtil.createSuggestion(
              SuggestionCategory.OTHER,
              createSuggestionId(INVESTIGATE_REMOTE_CACHE_MISSES),
              title,
              recommendation.toString(),
              null,
              null,
              caveats);
      return SuggestionProviderUtil.createSuggestionOutput(
          ANALYZER_CLASSNAME, List.of(suggestion), null);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }
}
