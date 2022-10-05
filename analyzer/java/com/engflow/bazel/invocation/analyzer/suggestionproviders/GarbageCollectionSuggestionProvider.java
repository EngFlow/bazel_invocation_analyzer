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

import com.engflow.bazel.invocation.analyzer.PotentialImprovement;
import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.GarbageCollectionStats;
import com.engflow.bazel.invocation.analyzer.dataproviders.TotalDuration;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A {@link SuggestionProvider} that provides suggestions regarding garbage collection. */
public class GarbageCollectionSuggestionProvider extends SuggestionProviderBase {
  private static final String ANALYZER_CLASSNAME =
      GarbageCollectionSuggestionProvider.class.getName();
  private static final String SUGGESTION_ID_INCREASE_JAVA_HEAP_SIZE = "IncreaseJavaHeapSize";
  private static final String SUGGESTION_ID_REDUCE_RULES_MEMORY_USAGE = "ReduceRulesMemoryUsage";
  // Only return a suggestion if major garbage collection takes up a significant portion of the
  // whole invocation.
  @VisibleForTesting static final double MAJOR_GC_MIN_PERCENTAGE = 5.0;

  @Override
  public SuggestionOutput getSuggestions(DataManager dataManager) {
    try {
      List<Suggestion> suggestions = new ArrayList<>();
      GarbageCollectionStats gcStats = dataManager.getDatum(GarbageCollectionStats.class);

      if (gcStats.hasMajorGarbageCollection()) {
        Duration totalDuration = dataManager.getDatum(TotalDuration.class).getTotalDuration();
        double percentOfTotal =
            100.0
                * gcStats.getMajorGarbageCollectionDuration().toMillis()
                / totalDuration.toMillis();
        if (percentOfTotal >= MAJOR_GC_MIN_PERCENTAGE) {
          Duration optimalDuration =
              totalDuration.minus(gcStats.getMajorGarbageCollectionDuration());
          PotentialImprovement potentialImprovement =
              SuggestionProviderUtil.createPotentialImprovement(
                  String.format(
                      "Reducing the invocation's duration from %s to %s might be possible. This"
                          + " assumes the stop-the-world pauses caused by major garbage collection"
                          + " can be fully eliminated.",
                      DurationUtil.formatDuration(totalDuration),
                      DurationUtil.formatDuration(optimalDuration)),
                  percentOfTotal);
          String rationaleGarbageCollectionTime =
              String.format(
                  Locale.US,
                  "%s or %.2f%% of the invocation is spent on major garbage collection which"
                      + " suspends all other threads.",
                  DurationUtil.formatDuration(gcStats.getMajorGarbageCollectionDuration()),
                  percentOfTotal);

          String titleIncreaseHeapSize = "Increase the Java heap size available to Bazel";
          String recommendationIncreaseHeapSize =
              "Using the Bazel flag --host_jvm_args you can control the startup options to"
                  + " be passed to the Java virtual machine in which Bazel itself runs. You"
                  + " may want to increase the heap size. \n"
                  + "Also see"
                  + " https://bazel.build/reference/command-line-reference#flag--host_jvm_args";
          String rationaleIncreaseHeapSize =
              "Increasing the heap size may reduce the frequency and length of major"
                  + " garbage collection.";
          suggestions.add(
              SuggestionProviderUtil.createSuggestion(
                  SuggestionCategory.BAZEL_FLAGS,
                  createSuggestionId(SUGGESTION_ID_INCREASE_JAVA_HEAP_SIZE),
                  titleIncreaseHeapSize,
                  recommendationIncreaseHeapSize,
                  potentialImprovement,
                  List.of(rationaleGarbageCollectionTime, rationaleIncreaseHeapSize),
                  null));

          String titleReduceMemoryUsage =
              "Investigate whether the memory use of some rules can be improved";
          String recommendationReduceMemoryUsage =
              "Check the memory use of your Bazel rules and investigate whether reducing"
                  + " their memory requirements is possible, especially if you maintain"
                  + " custom rules.\n"
                  + "Also see https://bazel.build/rules/performance#memory-profiling for"
                  + " information on Bazel's built-in memory profiler.";
          String rationaleReduceMemoryUsage =
              "Improved rules may reduce the frequency and length of major garbage"
                  + " collection and ideally require only minor garbage collection"
                  + " instead.";
          suggestions.add(
              SuggestionProviderUtil.createSuggestion(
                  SuggestionCategory.RULES,
                  createSuggestionId(SUGGESTION_ID_REDUCE_RULES_MEMORY_USAGE),
                  titleReduceMemoryUsage,
                  recommendationReduceMemoryUsage,
                  potentialImprovement,
                  List.of(rationaleGarbageCollectionTime, rationaleReduceMemoryUsage),
                  null));
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
