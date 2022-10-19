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

package com.engflow.bazel.invocation.analyzer.consoleoutput;

import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.Caveat;
import com.engflow.bazel.invocation.analyzer.PotentialImprovement;
import com.engflow.bazel.invocation.analyzer.Suggestion;
import com.engflow.bazel.invocation.analyzer.SuggestionCategory;
import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.TestDatum.CharDatum;
import com.engflow.bazel.invocation.analyzer.core.TestDatum.DoubleDatum;
import com.engflow.bazel.invocation.analyzer.core.TestDatum.IntegerDatum;
import com.engflow.bazel.invocation.analyzer.core.TestDatum.StringDatum;
import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class ConsoleOutputTest {
  private static final Caveat CAVEAT =
      Caveat.newBuilder().setMessage("Note, though, this.").build();
  private static final Caveat CAVEAT_SUGGESTING_VERBOSE_MODE =
      Caveat.newBuilder().setMessage("And note that.").setSuggestVerboseMode(true).build();
  private static final String EXCEPTION_MESSAGE = "Oops! Something went wrong.";
  private static final String IMPROVEMENT = "It'll get so much better.";
  private static final String RATIONALE_1 = "This is the reason why";
  private static final String RATIONALE_2 = "Here's another reason";
  private static final String RECOMMENDATION = "Change this!";
  private static final String TITLE = "This is a title";

  @Test
  public void formatSuggestionsShouldIncludeSuggestionData() {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    List<Suggestion> suggestions =
        List.of(
            Suggestion.newBuilder()
                .setCategory(SuggestionCategory.OTHER)
                .setTitle(TITLE)
                .setRecommendation(RECOMMENDATION)
                .addRationale(RATIONALE_1)
                .addRationale(RATIONALE_2)
                .setPotentialImprovement(PotentialImprovement.newBuilder().setMessage(IMPROVEMENT))
                .addCaveat(CAVEAT)
                .addCaveat(CAVEAT_SUGGESTING_VERBOSE_MODE)
                .build());
    String formattedOutput = consoleOutput.formatSuggestions(suggestions);
    assertThat(formattedOutput).contains(TITLE);
    assertThat(formattedOutput).contains(RECOMMENDATION);
    assertThat(formattedOutput).contains(RATIONALE_1);
    assertThat(formattedOutput).contains(RATIONALE_2);
    assertThat(formattedOutput).contains(IMPROVEMENT);
    assertThat(formattedOutput).contains(CAVEAT.getMessage());
    assertThat(formattedOutput).contains(CAVEAT_SUGGESTING_VERBOSE_MODE.getMessage());
  }

  @Test
  public void formatSuggestionsShouldOrderSuggestions() {
    String none = "no performance improvement";
    double lowest = .1;
    double middle = .2;
    double highest = .3;
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    List<Suggestion> suggestions =
        List.of(
            Suggestion.newBuilder()
                .setCategory(SuggestionCategory.OTHER)
                .setTitle(String.valueOf(middle))
                .setRecommendation(String.valueOf(middle))
                .setPotentialImprovement(
                    PotentialImprovement.newBuilder().setDurationReductionPercentage(middle))
                .build(),
            Suggestion.newBuilder()
                .setCategory(SuggestionCategory.OTHER)
                .setTitle(String.valueOf(lowest))
                .setRecommendation(String.valueOf(lowest))
                .setPotentialImprovement(
                    PotentialImprovement.newBuilder().setDurationReductionPercentage(lowest))
                .build(),
            Suggestion.newBuilder()
                .setCategory(SuggestionCategory.OTHER)
                .setTitle(none)
                .setRecommendation(none)
                .build(),
            Suggestion.newBuilder()
                .setCategory(SuggestionCategory.OTHER)
                .setTitle(String.valueOf(highest))
                .setRecommendation(String.valueOf(highest))
                .setPotentialImprovement(
                    PotentialImprovement.newBuilder().setDurationReductionPercentage(highest))
                .build());
    String formattedOutput = consoleOutput.formatSuggestions(suggestions);
    int indexNone = formattedOutput.indexOf(none);
    int indexLowest = formattedOutput.indexOf(String.valueOf(lowest));
    int indexMiddle = formattedOutput.indexOf(String.valueOf(middle));
    int indexHighest = formattedOutput.indexOf(String.valueOf(highest));
    assertThat(indexHighest).isAtLeast(0);
    assertThat(indexMiddle).isAtLeast(0);
    assertThat(indexLowest).isAtLeast(0);
    assertThat(indexNone).isAtLeast(0);
    assertThat(indexHighest).isLessThan(indexMiddle);
    assertThat(indexMiddle).isLessThan(indexLowest);
    assertThat(indexLowest).isLessThan(indexNone);
  }

  @Test
  public void formatFailuresShouldIncludeExceptionMessage() {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    Exception e = new Exception(EXCEPTION_MESSAGE);
    var failures =
        List.of(
            SuggestionOutput.Failure.newBuilder()
                .setMessage(e.getMessage())
                .setStackTrace(Throwables.getStackTraceAsString(e))
                .build());
    String formattedOutput = consoleOutput.formatFailures(failures);
    assertThat(formattedOutput).contains(EXCEPTION_MESSAGE);
  }

  @Test
  public void formatSuggestionOutputCaveatsShouldIncludeCaveatMessage() {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    String formattedOutput = consoleOutput.formatSuggestionOutputCaveats(List.of(CAVEAT));
    assertThat(formattedOutput).contains(CAVEAT.getMessage());
  }

  @Test
  public void shouldVisualizeDurationReductionPercentageZero() {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    assertThat(consoleOutput.visualizeDurationReductionPercentage(0))
        .isEqualTo("[..................................................]");
  }

  @Test
  public void shouldVisualizeDurationReductionPercentageRoundDown() {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    assertThat(consoleOutput.visualizeDurationReductionPercentage(3.49))
        .isEqualTo("[.................................................X]");
  }

  @Test
  public void shouldVisualizeDurationReductionPercentageRoundUp() {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    assertThat(consoleOutput.visualizeDurationReductionPercentage(3.51))
        .isEqualTo("[................................................XX]");
  }

  @Test
  public void shouldVisualizeDurationReductionPercentageHundred() {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    assertThat(consoleOutput.visualizeDurationReductionPercentage(100))
        .isEqualTo("[XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX]");
  }

  @Test
  public void shouldVisualizeDurationReductionPercentageOutOfRangeBelow() {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    assertThat(consoleOutput.visualizeDurationReductionPercentage(-0.1)).isNull();
  }

  @Test
  public void shouldVisualizeDurationReductionPercentageOutOfRangeAbove() {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    assertThat(consoleOutput.visualizeDurationReductionPercentage(100.1)).isNull();
  }

  @Test
  public void shouldFormat() {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    assertThat(
            consoleOutput.format(
                TITLE, ConsoleOutputStyle.TEXT_UNDERLINE, ConsoleOutputStyle.TEXT_GREEN))
        .isEqualTo("\u001B[4;32m" + TITLE + "\u001B[0m");
  }

  @Test
  public void shouldFormatHeading() throws Exception {
    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);
    assertThat(consoleOutput.formatAsHeading(TITLE)).isEqualTo("\u001B[1m" + TITLE + "\u001B[0m");
  }

  @Test
  public void shouldNotFormatIfFormattingIsDisabled() {
    ConsoleOutput consoleOutput = new ConsoleOutput(true, false);
    assertThat(consoleOutput.format(TITLE, ConsoleOutputStyle.TEXT_GREEN)).isEqualTo(TITLE);
    assertThat(consoleOutput.formatAsHeading(TITLE)).isEqualTo(TITLE);
  }

  @Test
  public void formatAnalysisDataShouldIncludeData() {
    var myDouble = new DoubleDatum(1.23);
    var myInt = new IntegerDatum(5);
    var myChar = new CharDatum('L');
    var myEmptyString = new StringDatum(null);

    var data =
        new HashMap<Class<? extends DataProvider>, Map<Class<? extends Datum>, Datum>>() {
          {
            put(
                TestDataProvider.class,
                new HashMap<>() {
                  {
                    put(IntegerDatum.class, myInt);
                    put(DoubleDatum.class, myDouble);
                    put(StringDatum.class, myEmptyString);
                  }
                });
            put(
                TestDataProvider2.class,
                new HashMap<>() {
                  {
                    put(CharDatum.class, myChar);
                  }
                });
          }
        };

    ConsoleOutput consoleOutput = new ConsoleOutput(false, false);

    var result = consoleOutput.formatAnalysisData(data);
    assertThat(result).contains(IntegerDatum.class.getSimpleName());
    assertThat(result).contains(myInt.getDescription());
    assertThat(result).contains(myInt.getSummary());
    assertThat(result).contains(DoubleDatum.class.getSimpleName());
    assertThat(result).contains(myDouble.getDescription());
    assertThat(result).contains(myDouble.getSummary());
    assertThat(result).contains(CharDatum.class.getSimpleName());
    assertThat(result).contains(myChar.getDescription());
    assertThat(result).contains(myChar.getSummary());
    assertThat(result).contains(myChar.getSummary());
    assertThat(myEmptyString.getSummary()).isNull();
    assertThat(result).doesNotContain(myEmptyString.getDescription());
    assertThat(result).doesNotContain(TestDataProvider.class.getSimpleName());
    assertThat(result).doesNotContain(TestDataProvider2.class.getSimpleName());
  }

  @Test
  public void formatAnalysisDataWithVerboseShouldIncludeVerboseData() {
    var myDouble = new DoubleDatum(1.23);
    var myInt = new IntegerDatum(5);
    var myChar = new CharDatum('L');
    var myEmptyString = new StringDatum(null);

    var data =
        new HashMap<Class<? extends DataProvider>, Map<Class<? extends Datum>, Datum>>() {
          {
            put(
                TestDataProvider.class,
                new HashMap<>() {
                  {
                    put(IntegerDatum.class, myInt);
                    put(DoubleDatum.class, myDouble);
                    put(StringDatum.class, myEmptyString);
                  }
                });
            put(
                TestDataProvider2.class,
                new HashMap<>() {
                  {
                    put(CharDatum.class, myChar);
                  }
                });
          }
        };

    ConsoleOutput consoleOutput = new ConsoleOutput(false, true);

    var result = consoleOutput.formatAnalysisData(data);
    assertThat(result).contains(IntegerDatum.class.getSimpleName());
    assertThat(result).contains(myInt.getDescription());
    assertThat(result).contains(myInt.getSummary());
    assertThat(result).contains(DoubleDatum.class.getSimpleName());
    assertThat(result).contains(myDouble.getDescription());
    assertThat(result).contains(myDouble.getSummary());
    assertThat(result).contains(StringDatum.class.getSimpleName());
    assertThat(result).contains(myEmptyString.getDescription());
    assertThat(myEmptyString.getEmptyReason()).isNotEmpty();
    assertThat(result).contains(myEmptyString.getEmptyReason());
    assertThat(result).contains(CharDatum.class.getSimpleName());
    assertThat(result).contains(myChar.getDescription());
    assertThat(result).contains(myChar.getSummary());
    assertThat(result).contains(TestDataProvider.class.getSimpleName());
    assertThat(result).contains(TestDataProvider2.class.getSimpleName());
  }

  // These are just dummy DataProviders used as place-holders for formatting testing
  private static class TestDataProvider extends DataProvider {
    @Override
    public List<DatumSupplierSpecification<?>> getSuppliers() {
      return new ArrayList<>();
    }
  }

  private static class TestDataProvider2 extends TestDataProvider {}
}
