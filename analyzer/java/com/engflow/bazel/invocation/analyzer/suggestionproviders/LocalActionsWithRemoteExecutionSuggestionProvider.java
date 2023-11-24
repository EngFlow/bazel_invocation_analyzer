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
import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionUsed;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link SuggestionProvider} that suggests migrating locally executed events to remote execution
 * if remote execution is already being used.
 */
public class LocalActionsWithRemoteExecutionSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME =
      LocalActionsWithRemoteExecutionSuggestionProvider.class.getName();
  private static final String SUGGESTION_ID_LOCAL_ACTIONS_WITH_REMOTE_EXECUTION =
      "LocalActionsWithRemoteExecution";

  public static LocalActionsWithRemoteExecutionSuggestionProvider createDefault() {
    return new LocalActionsWithRemoteExecutionSuggestionProvider(5);
  }

  public static LocalActionsWithRemoteExecutionSuggestionProvider createVerbose() {
    return new LocalActionsWithRemoteExecutionSuggestionProvider(Integer.MAX_VALUE);
  }

  private final int maxActions;

  @VisibleForTesting
  LocalActionsWithRemoteExecutionSuggestionProvider(int maxActions) {
    this.maxActions = maxActions;
  }

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      boolean remoteExecutionUsed =
          dataManager.getDatum(RemoteExecutionUsed.class).isRemoteExecutionUsed();
      List<Suggestion> suggestions = new ArrayList<>();
      List<Caveat> caveats = new ArrayList<>();
      if (remoteExecutionUsed) {
        var localActions = dataManager.getDatum(LocalActions.class);
        if (!localActions.isEmpty()) {
          var locallyExecuted =
              localActions.stream()
                  .filter(action -> action.isExecutedLocally())
                  .sorted((a, b) -> b.getAction().duration.compareTo(a.getAction().duration))
                  .collect(Collectors.toList());
          if (locallyExecuted.size() > maxActions) {
            caveats.add(
                SuggestionProviderUtil.createCaveat(
                    String.format(
                        "Only the %d longest, locally executed actions of the %d found were"
                            + " listed.",
                        maxActions, locallyExecuted.size()),
                    true));
          }
          StringBuilder recommendation = new StringBuilder();
          recommendation.append(
              "Although remote execution was used for this invocation, some actions were still"
                  + " executed locally. Investigate whether you can migrate these actions to remote"
                  + " execution to speed up future builds and improve hermeticity:\n");
          locallyExecuted.stream()
              .limit(maxActions)
              .forEachOrdered(
                  action -> {
                    recommendation.append("\t- ");
                    recommendation.append(action.getAction().name);
                    recommendation.append(" (");
                    recommendation.append(DurationUtil.formatDuration(action.getAction().duration));
                    recommendation.append(")\n");
                  });
          suggestions.add(
              SuggestionProviderUtil.createSuggestion(
                  SuggestionCategory.OTHER,
                  createSuggestionId(SUGGESTION_ID_LOCAL_ACTIONS_WITH_REMOTE_EXECUTION),
                  "Migrate locally executed actions to remote execution",
                  recommendation.toString(),
                  null,
                  null,
                  caveats));
        }
      }
      return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, suggestions, null);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }
}
