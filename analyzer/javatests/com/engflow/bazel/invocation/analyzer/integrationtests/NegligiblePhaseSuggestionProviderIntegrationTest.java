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
import java.util.HashMap;
import java.util.Map;
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
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, Timestamp.ofSeconds(0));
    startTimes.put(BazelProfilePhase.INIT, Timestamp.ofSeconds(1));
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, Timestamp.ofSeconds(2));
    startTimes.put(BazelProfilePhase.ANALYZE, Timestamp.ofSeconds(12));
    // Too long
    startTimes.put(BazelProfilePhase.PREPARE, Timestamp.ofSeconds(22));
    startTimes.put(BazelProfilePhase.EXECUTE, Timestamp.ofSeconds(32));
    startTimes.put(BazelProfilePhase.FINISH, Timestamp.ofSeconds(99));
    Timestamp finishTime = Timestamp.ofSeconds(100);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, finishTime))));

    SuggestionOutput output = provider.getSuggestions(dataManager);

    assertThat(output.hasFailure()).isFalse();
    assertThat(output.getMissingInputList()).isEmpty();
    assertThat(output.getSuggestionList().size()).isEqualTo(1);
    assertThat(output.getSuggestionList().get(0).getRecommendation())
        .contains(BazelProfilePhase.PREPARE.name);
  }

  @Test
  public void allLongPhaseSuggestion() throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    for (var phase : BazelProfilePhase.values()) {
      startTimes.put(phase, Timestamp.ofSeconds(10 * (phase.ordinal() + 1)));
    }
    Timestamp finishTime = Timestamp.ofSeconds(10 * (BazelProfilePhase.values().length + 1));

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, finishTime))));

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
  public void shouldNotProduceOutputOnOkayProfileWithoutSkymeld() throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, Timestamp.ofMicros(0));
    startTimes.put(BazelProfilePhase.INIT, Timestamp.ofMicros(1_000));
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, Timestamp.ofMicros(2_000));
    startTimes.put(BazelProfilePhase.ANALYZE, Timestamp.ofMicros(12_000));
    startTimes.put(BazelProfilePhase.PREPARE, Timestamp.ofMicros(22_000));
    startTimes.put(BazelProfilePhase.EXECUTE, Timestamp.ofMicros(23_000));
    startTimes.put(BazelProfilePhase.FINISH, Timestamp.ofMicros(99_000));
    Timestamp finishTime = Timestamp.ofMicros(100_000);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, finishTime))));

    SuggestionOutput output = provider.getSuggestions(dataManager);

    assertThat(output.hasFailure()).isFalse();
    assertThat(output.getMissingInputList()).isEmpty();
    assertThat(output.getSuggestionList().size()).isEqualTo(0);
  }

  @Test
  public void shouldNotProduceOutputOnOkayProfileWithSkymeld() throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, Timestamp.ofMicros(0));
    startTimes.put(BazelProfilePhase.INIT, Timestamp.ofMicros(1_000));
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, Timestamp.ofMicros(2_000));
    startTimes.put(BazelProfilePhase.ANALYZE_AND_EXECUTE, Timestamp.ofMicros(12_000));
    startTimes.put(BazelProfilePhase.FINISH, Timestamp.ofMicros(99_000));
    Timestamp finishTime = Timestamp.ofMicros(100_000);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, finishTime))));

    SuggestionOutput output = provider.getSuggestions(dataManager);

    assertThat(output.hasFailure()).isFalse();
    assertThat(output.getMissingInputList()).isEmpty();
    assertThat(output.getSuggestionList().size()).isEqualTo(0);
  }
}
