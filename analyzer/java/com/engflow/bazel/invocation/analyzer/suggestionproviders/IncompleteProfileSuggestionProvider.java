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

import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.CriticalPathDuration;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresAvailable;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link SuggestionProvider} that provides suggestions on how to reduce remote execution queuing.
 */
public class IncompleteProfileSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME =
      IncompleteProfileSuggestionProvider.class.getName();

  private static final String SUGGESTION_ID_ANALYZE_DIFFERENT_PROFILE = "AnalyzeDifferentProfile";

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      CriticalPathDuration criticalPathDuration = dataManager.getDatum(CriticalPathDuration.class);
      TotalDuration totalDuration = dataManager.getDatum(TotalDuration.class);
      EstimatedCoresAvailable estimatedCoresAvailable =
          dataManager.getDatum(EstimatedCoresAvailable.class);
      EstimatedCoresUsed estimatedCoresUsed = dataManager.getDatum(EstimatedCoresUsed.class);
      List<String> rationaleList = new ArrayList<>();
      if (criticalPathDuration.isEmpty()) {
        rationaleList.add("The Bazel profile does not include a critical path.");
      }
      if (totalDuration.isEmpty()) {
        rationaleList.add(
            "The Bazel profile does not include all data to determine the invocation's duration.");
      }
      if (estimatedCoresAvailable.isEmpty()) {
        rationaleList.add(
            "The Bazel profile does not include the data required to estimate how many cores were"
                + " available on the machine that the invocation was run on.");
      }
      if (estimatedCoresUsed.isEmpty()) {
        rationaleList.add(
            "The Bazel profile does not include the data required to estimate how many cores Bazel"
                + " used during the invocation.");
      }
      if (rationaleList.isEmpty()) {
        return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, null, null);
      }
      Suggestion suggestion =
          SuggestionProviderUtil.createSuggestion(
              SuggestionCategory.OTHER,
              createSuggestionId(SUGGESTION_ID_ANALYZE_DIFFERENT_PROFILE),
              "Analyze a different Bazel profile",
              "Consider analyzing a different Bazel profile. Many evaluations depend on the profile"
                  + " reporting some action execution. This will usually be the case when invoking"
                  + " a build-like Bazel command, such as `bazel build` or `bazel test`.",
              null,
              rationaleList,
              null);
      return SuggestionProviderUtil.createSuggestionOutput(
          ANALYZER_CLASSNAME, List.of(suggestion), null);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }
}
