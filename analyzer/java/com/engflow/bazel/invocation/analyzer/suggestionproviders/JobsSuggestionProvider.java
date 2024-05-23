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
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresAvailable;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedJobsFlagValue;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteCachingUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionUsed;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;

/** A {@link SuggestionProvider} that provides suggestions on setting the Bazel flag `--jobs`. */
public class JobsSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME = JobsSuggestionProvider.class.getName();

  @VisibleForTesting
  static final String EMPTY_REASON_PREFIX =
      "No optimizations regarding the value of the Bazel flag --jobs could be suggested. ";

  private static final String SUGGESTION_ID_UNSET_JOBS_FLAG = "UnsetJobsFlag";

  @VisibleForTesting
  public static final String RATIONALE_FOR_LOCAL_TOO_HIGH_JOBS_VALUE =
      "For local builds, specifying a value higher than the number of available cores increases"
          + " Bazel's overhead without adding value.";

  @VisibleForTesting
  public static final String RATIONALE_FOR_LOCAL_TOO_LOW_JOBS_VALUE =
      "Limiting the number of allowed concurrent jobs hinders parallelization and can slow down"
          + " invocations.";

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      List<Suggestion> suggestions = new ArrayList<>();

      EstimatedJobsFlagValue estimatedJobs = dataManager.getDatum(EstimatedJobsFlagValue.class);
      if (estimatedJobs.isEmpty()) {
        return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
            ANALYZER_CLASSNAME, EMPTY_REASON_PREFIX + estimatedJobs.getEmptyReason());
      }

      RemoteExecutionUsed remoteExecutionUsed = dataManager.getDatum(RemoteExecutionUsed.class);
      RemoteCachingUsed remoteCachingUsed = dataManager.getDatum(RemoteCachingUsed.class);
      if (!remoteExecutionUsed.isRemoteExecutionUsed()
          && !remoteCachingUsed.isRemoteCachingUsed()
          && estimatedJobs.isLikelySet()) {
        // The profile seems to be for a fully local build where --jobs was set.
        EstimatedCoresAvailable coresAvailable =
            dataManager.getDatum(EstimatedCoresAvailable.class);
        if (coresAvailable.isEmpty()) {
          return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
              ANALYZER_CLASSNAME, EMPTY_REASON_PREFIX + coresAvailable.getEmptyReason());
        }
        EstimatedCoresUsed coresUsed = dataManager.getDatum(EstimatedCoresUsed.class);
        if (coresUsed.isEmpty()) {
          return SuggestionProviderUtil.createSuggestionOutputForEmptyInput(
              ANALYZER_CLASSNAME, EMPTY_REASON_PREFIX + coresUsed.getEmptyReason());
        }
        String title = "Unset the value of the Bazel flag --jobs";
        String recommendation =
            "For local builds, setting the Bazel flag --jobs to a number is not"
                + " recommended. Instead, omit this flag or set the value to \"auto\".\n"
                + "Also see"
                + " https://bazel.build/reference/command-line-reference"
                + "#flag--jobs";
        String rationaleGeneralInfo =
            "The value of --jobs determines how many concurrent jobs Bazel should run in"
                + " the execution phase. When omitted or set to \"auto\" Bazel will"
                + " determine a reasonable default based on the machine's resources.";
        String rationaleInvocationSpecific =
            String.format(
                "It looks like this flag was set to %d%s. However, the profile"
                    + " suggests that %d cores are available on the machine that the"
                    + " invocation was run on.",
                estimatedJobs.getLowerBound().get(),
                coresUsed.hasGaps() ? " or more" : "",
                coresAvailable.getEstimatedCores().get());
        Caveat caveat =
            SuggestionProviderUtil.createCaveat(
                "The number of available cores and the value of --jobs are approximations."
                    + " The Bazel profile does not include definitive data on these.",
                false);

        suggestions.add(
            SuggestionProviderUtil.createSuggestion(
                SuggestionCategory.BAZEL_FLAGS,
                createSuggestionId(SUGGESTION_ID_UNSET_JOBS_FLAG),
                title,
                recommendation,
                null,
                List.of(
                    rationaleGeneralInfo,
                    rationaleInvocationSpecific,
                    coresAvailable.getEstimatedCores().get() < estimatedJobs.getLowerBound().get()
                        ? RATIONALE_FOR_LOCAL_TOO_HIGH_JOBS_VALUE
                        : RATIONALE_FOR_LOCAL_TOO_LOW_JOBS_VALUE),
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
