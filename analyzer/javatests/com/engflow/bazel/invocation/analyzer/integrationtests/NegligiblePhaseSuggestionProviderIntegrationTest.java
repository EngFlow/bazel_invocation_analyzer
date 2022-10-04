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

package com.engflow.bazel.invocation.analyzer.integrationtests;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelPhasesDataProvider;
import com.engflow.bazel.invocation.analyzer.suggestionproviders.NegligiblePhaseSuggestionProvider;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import org.junit.Before;
import org.junit.Test;

public class NegligiblePhaseSuggestionProviderIntegrationTest extends IntegerationTestBase {
  private NegligiblePhaseSuggestionProvider provider;

  @Before
  public void before() throws Exception {
    provider = new NegligiblePhaseSuggestionProvider();

    new BazelPhasesDataProvider().register(dataManager);
  }

  @Test
  public void singleLongPhaseSuggestion() throws Exception {
    final Timestamp LAUNCH_START = Timestamp.ofSeconds(0);
    final Timestamp INIT_START = Timestamp.ofSeconds(1);
    final Timestamp EVAL_START = Timestamp.ofSeconds(2);
    final Timestamp DEP_START = Timestamp.ofSeconds(12);
    // Too long
    final Timestamp PREP_START = Timestamp.ofSeconds(22);
    final Timestamp EXEC_START = Timestamp.ofSeconds(32);
    final Timestamp FINISH_START = Timestamp.ofSeconds(99);
    final Timestamp FINISH_TIME = Timestamp.ofSeconds(100);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(
                    LAUNCH_START,
                    INIT_START,
                    EVAL_START,
                    DEP_START,
                    PREP_START,
                    EXEC_START,
                    FINISH_START,
                    FINISH_TIME))));

    SuggestionOutput output = provider.getSuggestions(dataManager);

    assertThat(output.hasFailure()).isFalse();
    assertThat(output.getMissingInputList()).isEmpty();
    assertThat(output.getSuggestionList().size()).isEqualTo(1);
    assertThat(output.getSuggestionList().get(0).getRecommendation())
        .contains(BazelProfilePhase.PREPARE.name);
  }

  @Test
  public void allLongPhaseSuggestion() throws Exception {
    final Timestamp LAUNCH_START = Timestamp.ofSeconds(10);
    final Timestamp INIT_START = Timestamp.ofSeconds(20);
    final Timestamp EVAL_START = Timestamp.ofSeconds(30);
    final Timestamp DEP_START = Timestamp.ofSeconds(40);
    final Timestamp PREP_START = Timestamp.ofSeconds(50);
    final Timestamp EXEC_START = Timestamp.ofSeconds(60);
    final Timestamp FINISH_START = Timestamp.ofSeconds(70);
    final Timestamp FINISH_TIME = Timestamp.ofSeconds(80);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(
                    LAUNCH_START,
                    INIT_START,
                    EVAL_START,
                    DEP_START,
                    PREP_START,
                    EXEC_START,
                    FINISH_START,
                    FINISH_TIME))));

    SuggestionOutput output = provider.getSuggestions(dataManager);

    assertThat(output.hasFailure()).isFalse();
    assertThat(output.getMissingInputList()).isEmpty();
    assertThat(output.getSuggestionList().size())
        .isEqualTo(
            BazelProfilePhase.values().length
                - NegligiblePhaseSuggestionProvider.NON_NEGLIGIBLE_PHASES.size());
    for (var phase : BazelProfilePhase.values()) {
      if (NegligiblePhaseSuggestionProvider.NON_NEGLIGIBLE_PHASES.contains(phase)) {
        continue;
      }
      assertThat(
              output.getSuggestionList().stream()
                  .anyMatch(x -> x.getRecommendation().contains(phase.name)))
          .isTrue();
    }
  }

  @Test
  public void shouldNotProduceOutputOnOkayProfile() throws Exception {
    final Timestamp LAUNCH_START = Timestamp.ofMicros(0);
    final Timestamp INIT_START = Timestamp.ofMicros(1_000);
    final Timestamp EVAL_START = Timestamp.ofMicros(2_000);
    final Timestamp DEP_START = Timestamp.ofMicros(12_000);
    final Timestamp PREP_START = Timestamp.ofMicros(22_000);
    final Timestamp EXEC_START = Timestamp.ofMicros(23_000);
    final Timestamp FINISH_START = Timestamp.ofMicros(99_000);
    final Timestamp FINISH_TIME = Timestamp.ofMicros(100_000);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(
                    LAUNCH_START,
                    INIT_START,
                    EVAL_START,
                    DEP_START,
                    PREP_START,
                    EXEC_START,
                    FINISH_START,
                    FINISH_TIME))));

    SuggestionOutput output = provider.getSuggestions(dataManager);

    assertThat(output.hasFailure()).isFalse();
    assertThat(output.getMissingInputList()).isEmpty();
    assertThat(output.getSuggestionList().size()).isEqualTo(0);
  }
}
