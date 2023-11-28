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

import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteCachingUsed;
import java.util.ArrayList;
import java.util.List;

/** A {@link SuggestionProvider} that suggests using remote caching, if not already used. */
public class UseRemoteCachingSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME =
      UseRemoteCachingSuggestionProvider.class.getName();
  private static final String SUGGESTION_ID_USE_REMOTE_CACHING = "UseRemoteCaching";

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      boolean remoteCachingUsed =
          dataManager.getDatum(RemoteCachingUsed.class).isRemoteCachingUsed();
      List<Suggestion> suggestions = new ArrayList<>();
      if (!remoteCachingUsed) {
        String title = "Use remote caching";
        String recommendation =
            "Consider using remote caching to speed up builds: https://bazel.build/remote/caching\n"
                + "There are two kinds of remote caches you can set up. The first is configured"
                + " with the Bazel flag `--remote_cache` and leverages a separate service for"
                + " caching, which usually runs on a different machine. The second cache is"
                + " configured with the Bazel flag `--disk_cache` and uses your local disk. It is"
                + " possible and in many cases recommended to use both `--disk_cache` and"
                + " `--remote_cache` together.";
        String rationale =
            "A remote cache allows you to share build outputs between different Bazel clients. This"
                + " can make builds significantly faster.";
        suggestions.add(
            SuggestionProviderUtil.createSuggestion(
                SuggestionCategory.BAZEL_FLAGS,
                createSuggestionId(SUGGESTION_ID_USE_REMOTE_CACHING),
                title,
                recommendation,
                null,
                List.of(rationale),
                null));
      }
      return SuggestionProviderUtil.createSuggestionOutput(ANALYZER_CLASSNAME, suggestions, null);
    } catch (MissingInputException e) {
      return SuggestionProviderUtil.createSuggestionOutputForMissingInput(ANALYZER_CLASSNAME, e);
    } catch (Throwable t) {
      return SuggestionProviderUtil.createSuggestionOutputForFailure(ANALYZER_CLASSNAME, t);
    }
  }
}
