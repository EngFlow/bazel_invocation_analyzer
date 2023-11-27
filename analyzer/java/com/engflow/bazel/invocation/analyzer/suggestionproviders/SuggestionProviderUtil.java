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
import com.engflow.bazel.invocation.analyzer.PotentialImprovement;
import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class SuggestionProviderUtil {
  private static final Logger logger = Logger.getLogger(SuggestionProviderUtil.class.getName());

  /**
   * Convenience method for retrieving all available {@link SuggestionProvider}s. When adding a new
   * SuggestionProvider, also add it to the list returned by this method.
   *
   * @param verbose If available, return the verbose version of the {@link SuggestionProvider}s.
   * @return The list of all available {@link SuggestionProvider}s.
   */
  public static List<SuggestionProvider> getAllSuggestionProviders(boolean verbose) {
    return List.of(
        verbose
            ? BottleneckSuggestionProvider.createVerbose()
            : BottleneckSuggestionProvider.createDefault(),
        verbose
            ? LocalActionsWithRemoteExecutionSuggestionProvider.createVerbose()
            : LocalActionsWithRemoteExecutionSuggestionProvider.createDefault(),
        new BuildWithoutTheBytesSuggestionProvider(),
        new CriticalPathNotDominantSuggestionProvider(),
        new GarbageCollectionSuggestionProvider(),
        new JobsSuggestionProvider(),
        new UseSkymeldSuggestionProvider(),
        new NegligiblePhaseSuggestionProvider(),
        new QueuingSuggestionProvider(),
        new IncompleteProfileSuggestionProvider(),
        // Consciously put this suggestion last, as it's not about the invocation itself,
        // but guidance on how to potentially get a better analysis using this tool.
        new MergedEventsSuggestionProvider());
  }

  /**
   * Returns a string describing the potential invocation duration improvement, or null for values
   * outside the interval (0, 100].
   *
   * @param invocationDurationReductionPercent The percentage by which the duration may be reduced.
   * @return A message describing the potential invocation duration improvement.
   */
  public static String invocationDurationReductionMsg(double invocationDurationReductionPercent) {
    if (invocationDurationReductionPercent < 0 || invocationDurationReductionPercent > 100) {
      logger.log(
          Level.WARNING,
          "Called invocationDurationReductionMsg with value %.2f, was not in range 0-100.",
          invocationDurationReductionPercent);
      return null;
    }
    if (invocationDurationReductionPercent == 0) {
      return null;
    }
    return String.format(
        Locale.US,
        "The duration of the invocation can potentially be reduced by %.2f%%.",
        invocationDurationReductionPercent);
  }

  /**
   * Creates as {@link Caveat} with the passed in values.
   *
   * @param message The message.
   * @param suggestVerboseMode Whether the caveat suggest running the tool in verbose mode.
   * @return A Caveat containing the passed in data.
   */
  public static Caveat createCaveat(String message, boolean suggestVerboseMode) {
    return Caveat.newBuilder()
        .setMessage(message)
        .setSuggestVerboseMode(suggestVerboseMode)
        .build();
  }

  /**
   * Creates as {@link PotentialImprovement} with the passed in values.
   *
   * @param message The message, if any.
   * @param invocationDurationReductionPercent By what percentage the duration can be reduced.
   * @return An PotentialImprovement containing the passed in data.
   */
  public static PotentialImprovement createPotentialImprovement(
      @Nullable String message, @Nullable Double invocationDurationReductionPercent) {
    PotentialImprovement.Builder builder = PotentialImprovement.newBuilder();
    if (message != null) {
      builder.setMessage(message);
    }
    if (invocationDurationReductionPercent != null) {
      builder.setDurationReductionPercentage(invocationDurationReductionPercent.doubleValue());
    }
    return builder.build();
  }

  /**
   * Creates a {@link Suggestion} with the passed in values.
   *
   * @param category The category of the suggestion.
   * @param uniqueSuggestionTypeId The id that uniquely identifies the type of suggestion. Must not
   *     contain any whitespace characters.
   * @param title The title of the suggestion.
   * @param recommendation The recommendation, if any.
   * @param potentialImprovement The potential improvement, if any.
   * @param rationale The rationale behind the recommendation, if any.
   * @param caveats Caveats, if any.
   * @return A {@link Suggestion} surfacing the data provided.
   */
  public static Suggestion createSuggestion(
      SuggestionCategory category,
      SuggestionId uniqueSuggestionTypeId,
      String title,
      @Nullable String recommendation,
      @Nullable PotentialImprovement potentialImprovement,
      @Nullable List<String> rationale,
      @Nullable List<Caveat> caveats) {
    Preconditions.checkNotNull(uniqueSuggestionTypeId);
    String suggestionIdWithoutSpaces = uniqueSuggestionTypeId.id.replaceAll("\\s+", "");
    Preconditions.checkArgument(uniqueSuggestionTypeId.id.equals(suggestionIdWithoutSpaces));
    Preconditions.checkNotNull(title);
    Suggestion.Builder builder =
        Suggestion.newBuilder()
            .setCategory(category)
            .setId(uniqueSuggestionTypeId.id)
            .setTitle(title);
    if (recommendation != null) {
      builder.setRecommendation(recommendation);
    }
    if (potentialImprovement != null) {
      builder.setPotentialImprovement(potentialImprovement);
    }
    if (rationale != null) {
      builder.addAllRationale(rationale);
    }
    if (caveats != null) {
      builder.addAllCaveat(caveats);
    }
    return builder.build();
  }

  /**
   * Creates a {@link SuggestionOutput} with the passed in values.
   *
   * @param analyzerClassname The name of the analyzer that produces this output.
   * @param suggestions The list of suggestions, if any.
   * @param caveats Caveats, if any.
   * @return A {@link SuggestionOutput} surfacing suggestions and caveats, if any.
   */
  public static SuggestionOutput createSuggestionOutput(
      String analyzerClassname,
      @Nullable List<Suggestion> suggestions,
      @Nullable List<Caveat> caveats) {
    Preconditions.checkNotNull(analyzerClassname);
    SuggestionOutput.Builder builder =
        SuggestionOutput.newBuilder().setAnalyzerClassname(analyzerClassname);
    if (suggestions != null) {
      builder.addAllSuggestion(suggestions);
    }
    if (caveats != null) {
      builder.addAllCaveat(caveats);
    }
    return builder.build();
  }

  /**
   * Creates a basic {@link SuggestionOutput} when an essential input is empty. Prefer using {@link
   * #createSuggestionOutputForEmptyInput(String, String)} and provide a meaningful message
   * explaining why the empty input prevents further analysis.
   *
   * @param analyzerClassname The name of the analyzer that produces this output.
   * @param emptyClazz The class of the {@link Datum} that was empty.
   * @return A {@link SuggestionOutput} with a caveat surfacing which input is empty.
   */
  public static SuggestionOutput createSuggestionOutputForEmptyInput(
      String analyzerClassname, Class<? extends Datum> emptyClazz) {
    Preconditions.checkNotNull(analyzerClassname);
    Preconditions.checkNotNull(emptyClazz);
    return SuggestionOutput.newBuilder()
        .setAnalyzerClassname(analyzerClassname)
        .addCaveat(
            createCaveat(
                String.format(
                    "An essential input for determining suggestions was empty: %s",
                    emptyClazz.getSimpleName()),
                false))
        .build();
  }

  public static SuggestionOutput createSuggestionOutputForEmptyInput(
      String analyzerClassname, String reason) {
    Preconditions.checkNotNull(analyzerClassname);
    return SuggestionOutput.newBuilder()
        .setAnalyzerClassname(analyzerClassname)
        .addCaveat(createCaveat(reason, false))
        .build();
  }

  /**
   * Creates a {@link SuggestionOutput} when an essential input is missing.
   *
   * @param analyzerClassname The name of the analyzer that produces this output.
   * @param missingInputException The exception thrown due to an essential input missing.
   * @return A {@link SuggestionOutput} surfacing which input is missing.
   */
  public static SuggestionOutput createSuggestionOutputForMissingInput(
      String analyzerClassname, MissingInputException missingInputException) {
    return createSuggestionOutputForMissingInputs(
        analyzerClassname, List.of(missingInputException));
  }

  /**
   * Creates a {@link SuggestionOutput} when essential inputs are missing.
   *
   * @param analyzerClassname The name of the analyzer that produces this output.
   * @param missingInputExceptions The exceptions thrown due to essential inputs missing.
   * @return A {@link SuggestionOutput} surfacing which input is missing.
   */
  public static SuggestionOutput createSuggestionOutputForMissingInputs(
      String analyzerClassname, List<MissingInputException> missingInputExceptions) {
    Preconditions.checkNotNull(analyzerClassname);
    Preconditions.checkNotNull(missingInputExceptions);
    return SuggestionOutput.newBuilder()
        .setAnalyzerClassname(analyzerClassname)
        .addAllMissingInput(
            missingInputExceptions.stream()
                .map(e -> e.getMissingInputClass().getName())
                .collect(Collectors.toList()))
        .build();
  }

  /**
   * Creates a {@link SuggestionOutput} when analysis failed for an unexpected reason.
   *
   * @param analyzerClassname The name of the analyzer that produces this output.
   * @param t The exception which caused the analysis to fail.
   * @return A {@link SuggestionOutput} surfacing which error caused the analysis to fail.
   */
  public static SuggestionOutput createSuggestionOutputForFailure(
      String analyzerClassname, Throwable t) {
    Preconditions.checkNotNull(analyzerClassname);
    Preconditions.checkNotNull(t);
    SuggestionOutput.Failure.Builder failureBuilder =
        SuggestionOutput.Failure.newBuilder().setStackTrace(Throwables.getStackTraceAsString(t));
    if (t.getMessage() != null) {
      failureBuilder.setMessage(t.getMessage());
    }
    return SuggestionOutput.newBuilder()
        .setAnalyzerClassname(analyzerClassname)
        .setFailure(failureBuilder)
        .build();
  }

  static class SuggestionId {
    private final String id;

    SuggestionId(String id) {
      Preconditions.checkNotNull(id);
      this.id = id;
    }
  }
}
