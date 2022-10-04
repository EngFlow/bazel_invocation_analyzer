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
import com.engflow.bazel.invocation.analyzer.dataproviders.MergedEventsPresent;
import java.util.ArrayList;
import java.util.List;

/** A {@link SuggestionProvider} that suggest creating Bazel profiles without merged events. */
public class MergedEventsSuggestionProvider implements SuggestionProvider {
  private static final String ANALYZER_CLASSNAME = MergedEventsSuggestionProvider.class.getName();
  private static final String SUGGESTION_ID_DISABLE_MERGED_EVENTS =
      ANALYZER_CLASSNAME + "-DisableMergedEvents";

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      boolean hasMergedEvents = dataManager.getDatum(MergedEventsPresent.class).hasMergedEvents();
      List<Suggestion> suggestions = new ArrayList<>();
      if (hasMergedEvents) {
        String title = "Disable merged events in the Bazel profile";
        String recommendation =
            "To disable Bazel's default behavior of potentially merging events in the Bazel"
                + " profile, include the flag --noslim_profile when writing a profile. Run"
                + " this tool on the new profile for a possibly improved analysis.\n"
                + "Also see"
                + " https://bazel.build/reference/command-line-reference#flag--slim_profile";
        String rationale =
            "The analyzed Bazel profile include merged events, which may hide data that"
                + " this tool could leverage to produce additional or more specific"
                + " suggestions.";
        Caveat caveat =
            SuggestionProviderUtil.createCaveat(
                "Disabling merged events may increase the size of the Bazel profile"
                    + " significantly.",
                false);
        suggestions.add(
            SuggestionProviderUtil.createSuggestion(
                SuggestionCategory.BAZEL_FLAGS,
                SUGGESTION_ID_DISABLE_MERGED_EVENTS,
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
