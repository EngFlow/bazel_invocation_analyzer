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
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelVersion;
import com.engflow.bazel.invocation.analyzer.dataproviders.SkymeldUsed;
import java.util.ArrayList;
import java.util.List;

/** A {@link SuggestionProvider} that suggests using Skymeld where applicable. */
public class UseSkymeldSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME = UseSkymeldSuggestionProvider.class.getName();

  private static final int BAZEL_MAJOR_VERSION_WITH_SKYMELD_SUPPORT = 6;
  private static final String SUGGESTION_ID_USE_SKYMELD = "UseSkymeld";

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      boolean isSkymeldUsed = dataManager.getDatum(SkymeldUsed.class).isSkymeldUsed();
      List<Suggestion> suggestions = new ArrayList<>();
      if (!isSkymeldUsed) {
        var bazelVersion = dataManager.getDatum(BazelVersion.class);

        String title = "Enable Skymeld";
        String recommendation =
            "Consider setting the Bazel flag"
                + " `--experimental_merged_skyframe_analysis_execution=true`.";
        String rationale =
            "Enabling Skymeld lets Bazel merge the analysis and execution phases of Skyframe. This"
                + " can improve the build performance especially for multi-target builds. Once the"
                + " analysis of one target is completed, execution can start without having to wait"
                + " for the analysis of the other targets to complete.\n"
                + "Also see https://github.com/bazelbuild/bazel/issues/14057";
        ArrayList<Caveat> caveats = new ArrayList<>();
        // Only include the caveat if we don't know whether Bazel 6+ was used.
        if (bazelVersion.isEmpty()
            || bazelVersion.getMajor().isEmpty()
            || bazelVersion.getMajor().get() < BAZEL_MAJOR_VERSION_WITH_SKYMELD_SUPPORT) {
          caveats.add(
              SuggestionProviderUtil.createCaveat(
                  "Skymeld support was added with release 6.0.0 and enabled by default with 7.0.0."
                      + " The version you are using may not support it.",
                  false));
        }
        suggestions.add(
            SuggestionProviderUtil.createSuggestion(
                SuggestionCategory.BAZEL_FLAGS,
                createSuggestionId(SUGGESTION_ID_USE_SKYMELD),
                title,
                recommendation,
                null,
                List.of(rationale),
                caveats));
      }
      return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, suggestions, null);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }
}
