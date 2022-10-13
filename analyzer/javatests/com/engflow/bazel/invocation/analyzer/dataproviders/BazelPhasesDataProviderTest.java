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

package com.engflow.bazel.invocation.analyzer.dataproviders;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import org.junit.Before;
import org.junit.Test;

public class BazelPhasesDataProviderTest extends DataProviderUnitTestBase {
  private final Timestamp LAUNCH_START = Timestamp.ofMicros(-10_000);
  private final Timestamp INIT_START = Timestamp.ofMicros(0);
  private final Timestamp EVAL_START = Timestamp.ofMicros(20_000);
  private final Timestamp DEP_START = Timestamp.ofMicros(50_000);
  private final Timestamp PREP_START = Timestamp.ofMicros(90_000);
  private final Timestamp EXEC_START = Timestamp.ofMicros(140_000);
  private final Timestamp FINISH_START = Timestamp.ofMicros(200_000);
  private final Timestamp FINISH_TIME = Timestamp.ofMicros(270_000);

  private BazelPhasesDataProvider provider;

  @Before
  public void setupTest() throws Exception {
    provider = new BazelPhasesDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void getBazelPhaseDescriptionsShouldWorkWhenAllPhasesArePresent() throws Exception {
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

    BazelPhaseDescriptions descriptions = provider.getBazelPhaseDescriptions();
    assertThat(descriptions.get(BazelProfilePhase.LAUNCH))
        .isEqualTo(
            new BazelPhaseDescription(
                LAUNCH_START, TimeUtil.getDurationBetween(LAUNCH_START, INIT_START)));
    assertThat(descriptions.get(BazelProfilePhase.INIT))
        .isEqualTo(
            new BazelPhaseDescription(
                INIT_START, TimeUtil.getDurationBetween(INIT_START, EVAL_START)));
    assertThat(descriptions.get(BazelProfilePhase.EVALUATE))
        .isEqualTo(
            new BazelPhaseDescription(
                EVAL_START, TimeUtil.getDurationBetween(EVAL_START, DEP_START)));
    assertThat(descriptions.get(BazelProfilePhase.DEPENDENCIES))
        .isEqualTo(
            new BazelPhaseDescription(
                DEP_START, TimeUtil.getDurationBetween(DEP_START, PREP_START)));
    assertThat(descriptions.get(BazelProfilePhase.PREPARE))
        .isEqualTo(
            new BazelPhaseDescription(
                PREP_START, TimeUtil.getDurationBetween(PREP_START, EXEC_START)));
    assertThat(descriptions.get(BazelProfilePhase.EXECUTE))
        .isEqualTo(
            new BazelPhaseDescription(
                EXEC_START, TimeUtil.getDurationBetween(EXEC_START, FINISH_START)));
    assertThat(descriptions.get(BazelProfilePhase.FINISH))
        .isEqualTo(
            new BazelPhaseDescription(
                FINISH_START, TimeUtil.getDurationBetween(FINISH_START, FINISH_TIME)));
  }

  @Test
  public void getBazelPhaseDescriptionsShouldWorkWhenSomePhasesAreMissing() throws Exception {
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
                    null,
                    null,
                    PREP_START,
                    null,
                    FINISH_START,
                    FINISH_TIME))));

    BazelPhaseDescriptions descriptions = provider.getBazelPhaseDescriptions();
    assertThat(descriptions.get(BazelProfilePhase.LAUNCH))
        .isEqualTo(
            new BazelPhaseDescription(
                LAUNCH_START, TimeUtil.getDurationBetween(LAUNCH_START, INIT_START)));
    assertThat(descriptions.get(BazelProfilePhase.INIT))
        .isEqualTo(
            new BazelPhaseDescription(
                INIT_START, TimeUtil.getDurationBetween(INIT_START, PREP_START)));
    assertThat(descriptions.get(BazelProfilePhase.PREPARE))
        .isEqualTo(
            new BazelPhaseDescription(
                PREP_START, TimeUtil.getDurationBetween(PREP_START, FINISH_START)));
    assertThat(descriptions.get(BazelProfilePhase.FINISH))
        .isEqualTo(
            new BazelPhaseDescription(
                FINISH_START, TimeUtil.getDurationBetween(FINISH_START, FINISH_TIME)));
  }

  @Test
  public void getBazelPhaseDescriptionsShouldWorkWhenAllButLaunchAndFinishPhaseAreMissing()
      throws Exception {
    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(LAUNCH_START, null, null, null, null, null, null, FINISH_TIME))));

    BazelPhaseDescriptions descriptions = provider.getBazelPhaseDescriptions();
    assertThat(descriptions.get(BazelProfilePhase.FINISH))
        .isEqualTo(
            new BazelPhaseDescription(
                LAUNCH_START, TimeUtil.getDurationBetween(LAUNCH_START, FINISH_TIME)));
  }

  @Test
  public void getBazelPhaseDescriptionsShouldThrowWhenTwoMarkersHaveTheSameTimestamp()
      throws Exception {
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
                    DEP_START,
                    FINISH_START,
                    FINISH_TIME))));

    InvalidProfileException invalidProfileException =
        assertThrows(InvalidProfileException.class, () -> provider.getBazelPhaseDescriptions());
    assertThat(invalidProfileException)
        .hasMessageThat()
        .contains(BazelProfilePhase.DEPENDENCIES.name);
    assertThat(invalidProfileException).hasMessageThat().contains(BazelProfilePhase.EXECUTE.name);
    assertThat(invalidProfileException)
        .hasMessageThat()
        .contains(String.valueOf(DEP_START.getMicros()));
  }

  @Test
  public void getBazelPhaseDescriptionsShouldThrowWhenLaunchPhaseIsMissing() throws Exception {
    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(
                    null,
                    INIT_START,
                    EVAL_START,
                    DEP_START,
                    PREP_START,
                    EXEC_START,
                    FINISH_START,
                    FINISH_TIME))));

    InvalidProfileException invalidProfileException =
        assertThrows(InvalidProfileException.class, () -> provider.getBazelPhaseDescriptions());
    assertThat(invalidProfileException).hasMessageThat().contains(BazelProfilePhase.LAUNCH.name);
  }

  @Test
  public void getBazelPhaseDescriptionsShouldThrowWhenFinishPhaseIsMissing() throws Exception {
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
                    null))));

    InvalidProfileException invalidProfileException =
        assertThrows(InvalidProfileException.class, () -> provider.getBazelPhaseDescriptions());
    assertThat(invalidProfileException)
        .hasMessageThat()
        .contains(BazelProfileConstants.INSTANT_FINISHING);
  }

  @Test
  public void getTotalDurationShouldWorkWhenAllPhasesArePresent() throws Exception {
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

    TotalDuration duration = provider.getTotalDuration();
    assertThat(duration.getTotalDuration().isPresent()).isTrue();
    assertThat(duration.getTotalDuration().get())
        .isEqualTo(TimeUtil.getDurationBetween(LAUNCH_START, FINISH_TIME));
  }

  @Test
  public void getTotalDurationShouldWorkWhenAllButLaunchAndFinishPhaseAreMissing()
      throws Exception {
    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(LAUNCH_START, null, null, null, null, null, null, FINISH_TIME))));

    TotalDuration duration = provider.getTotalDuration();
    assertThat(duration.getTotalDuration().isPresent()).isTrue();
    assertThat(duration.getTotalDuration().get())
        .isEqualTo(TimeUtil.getDurationBetween(LAUNCH_START, FINISH_TIME));
  }

  @Test
  public void getTotalDurationShouldThrowWhenLaunchPhasesIsMissing() throws Exception {
    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(
                    null,
                    INIT_START,
                    EVAL_START,
                    DEP_START,
                    PREP_START,
                    EXEC_START,
                    FINISH_START,
                    FINISH_TIME))));

    InvalidProfileException invalidProfileException =
        assertThrows(InvalidProfileException.class, () -> provider.getTotalDuration());
    assertThat(invalidProfileException).hasMessageThat().contains(BazelProfilePhase.LAUNCH.name);
  }

  @Test
  public void getTotalDurationShouldThrowWhenFinishingPhaseIsMissing() throws Exception {
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
                    null))));

    InvalidProfileException invalidProfileException =
        assertThrows(InvalidProfileException.class, () -> provider.getTotalDuration());
    assertThat(invalidProfileException)
        .hasMessageThat()
        .contains(BazelProfileConstants.INSTANT_FINISHING);
  }
}
