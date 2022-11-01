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

import com.engflow.bazel.invocation.analyzer.Caveat;
import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionUsed;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link SuggestionProvider} that suggests enabling "Build without the Bytes" where applicable.
 */
public class BuildWithoutTheBytesSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME =
      BuildWithoutTheBytesSuggestionProvider.class.getName();
  private static final String SUGGESTION_ID_BUILD_WITHOUT_THE_BYTES = "BuildWithoutTheBytes";

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      boolean remoteExecutionUsed =
          dataManager.getDatum(RemoteExecutionUsed.class).isRemoteExecutionUsed();
      List<Suggestion> suggestions = new ArrayList<>();
      if (remoteExecutionUsed) {
        String title = "Build without the Bytes";
        String recommendation =
            "Consider setting the Bazel flag --remote_download_minimal to minimize the data"
                + " downloaded while using remote execution.\nAlso see"
                + " https://bazel.build/reference/command-line-reference"
                + "#flag--remote_download_minimal";
        String rationale = "This profile includes events that indicate remote execution was used.";
        // TODO: Can we deduce from the profile whether this flag was already set?
        Caveat caveat =
            SuggestionProviderUtil.createCaveat(
                "The profile does not expose whether this flag is already set.", false);
        suggestions.add(
            SuggestionProviderUtil.createSuggestion(
                SuggestionCategory.BAZEL_FLAGS,
                createSuggestionId(SUGGESTION_ID_BUILD_WITHOUT_THE_BYTES),
                title,
                recommendation,
                null,
                List.of(rationale),
                List.of(caveat)));
      }
      return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, suggestions, null);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }
}
