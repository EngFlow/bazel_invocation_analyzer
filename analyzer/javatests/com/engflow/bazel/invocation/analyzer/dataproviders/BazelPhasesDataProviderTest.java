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
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class BazelPhasesDataProviderTest extends DataProviderUnitTestBase {
  private final Timestamp LAUNCH_START = Timestamp.ofMicros(-10_000);
  private final Timestamp INIT_START = Timestamp.ofMicros(0);
  private final Timestamp EVAL_START = Timestamp.ofMicros(20_000);
  private final Timestamp ANALYZE_START = Timestamp.ofMicros(50_000);
  private final Timestamp LICENSE_START = Timestamp.ofMicros(80_000);
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
  public void getBazelPhaseDescriptionsShouldWorkWhenAllPhasesArePresentWithoutSkymeld()
      throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, LAUNCH_START);
    startTimes.put(BazelProfilePhase.INIT, INIT_START);
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, EVAL_START);
    startTimes.put(BazelProfilePhase.ANALYZE, ANALYZE_START);
    startTimes.put(BazelProfilePhase.LICENSE, LICENSE_START);
    startTimes.put(BazelProfilePhase.PREPARE, PREP_START);
    startTimes.put(BazelProfilePhase.EXECUTE, EXEC_START);
    startTimes.put(BazelProfilePhase.FINISH, FINISH_START);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, FINISH_TIME))));

    BazelPhaseDescriptions descriptions = provider.getBazelPhaseDescriptions();
    assertThat(descriptions.get(BazelProfilePhase.LAUNCH).get())
        .isEqualTo(
            new BazelPhaseDescription(
                LAUNCH_START, TimeUtil.getDurationBetween(LAUNCH_START, INIT_START)));
    assertThat(descriptions.get(BazelProfilePhase.INIT).get())
        .isEqualTo(
            new BazelPhaseDescription(
                INIT_START, TimeUtil.getDurationBetween(INIT_START, EVAL_START)));
    assertThat(descriptions.get(BazelProfilePhase.TARGET_PATTERN_EVAL).get())
        .isEqualTo(
            new BazelPhaseDescription(
                EVAL_START, TimeUtil.getDurationBetween(EVAL_START, ANALYZE_START)));
    assertThat(descriptions.get(BazelProfilePhase.ANALYZE).get())
        .isEqualTo(
            new BazelPhaseDescription(
                ANALYZE_START, TimeUtil.getDurationBetween(ANALYZE_START, LICENSE_START)));
    assertThat(descriptions.get(BazelProfilePhase.LICENSE).get())
        .isEqualTo(
            new BazelPhaseDescription(
                LICENSE_START, TimeUtil.getDurationBetween(LICENSE_START, PREP_START)));
    assertThat(descriptions.get(BazelProfilePhase.PREPARE).get())
        .isEqualTo(
            new BazelPhaseDescription(
                PREP_START, TimeUtil.getDurationBetween(PREP_START, EXEC_START)));
    assertThat(descriptions.get(BazelProfilePhase.EXECUTE).get())
        .isEqualTo(
            new BazelPhaseDescription(
                EXEC_START, TimeUtil.getDurationBetween(EXEC_START, FINISH_START)));
    assertThat(descriptions.get(BazelProfilePhase.FINISH).get())
        .isEqualTo(
            new BazelPhaseDescription(
                FINISH_START, TimeUtil.getDurationBetween(FINISH_START, FINISH_TIME)));
  }

  @Test
  public void getBazelPhaseDescriptionsShouldWorkWhenAllPhasesArePresentWithSkymeld()
      throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, LAUNCH_START);
    startTimes.put(BazelProfilePhase.INIT, INIT_START);
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, EVAL_START);
    startTimes.put(BazelProfilePhase.ANALYZE_AND_EXECUTE, ANALYZE_START);
    startTimes.put(BazelProfilePhase.FINISH, FINISH_START);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, FINISH_TIME))));

    BazelPhaseDescriptions descriptions = provider.getBazelPhaseDescriptions();
    assertThat(descriptions.get(BazelProfilePhase.LAUNCH).get())
        .isEqualTo(
            new BazelPhaseDescription(
                LAUNCH_START, TimeUtil.getDurationBetween(LAUNCH_START, INIT_START)));
    assertThat(descriptions.get(BazelProfilePhase.INIT).get())
        .isEqualTo(
            new BazelPhaseDescription(
                INIT_START, TimeUtil.getDurationBetween(INIT_START, EVAL_START)));
    assertThat(descriptions.get(BazelProfilePhase.TARGET_PATTERN_EVAL).get())
        .isEqualTo(
            new BazelPhaseDescription(
                EVAL_START, TimeUtil.getDurationBetween(EVAL_START, ANALYZE_START)));
    assertThat(descriptions.get(BazelProfilePhase.ANALYZE_AND_EXECUTE).get())
        .isEqualTo(
            new BazelPhaseDescription(
                ANALYZE_START, TimeUtil.getDurationBetween(ANALYZE_START, FINISH_START)));
    assertThat(descriptions.get(BazelProfilePhase.FINISH).get())
        .isEqualTo(
            new BazelPhaseDescription(
                FINISH_START, TimeUtil.getDurationBetween(FINISH_START, FINISH_TIME)));
  }

  @Test
  public void getBazelPhaseDescriptionsShouldWorkWhenSomePhasesAreMissing() throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, LAUNCH_START);
    startTimes.put(BazelProfilePhase.INIT, INIT_START);
    // No BazelProfilePhase.TARGET_PATTERN_EVAL
    // No BazelProfilePhase.ANALYZE
    // No BazelProfilePhase.LICENSE
    startTimes.put(BazelProfilePhase.PREPARE, PREP_START);
    // No BazelProfilePhase.EXECUTE
    startTimes.put(BazelProfilePhase.FINISH, FINISH_START);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, FINISH_TIME))));

    BazelPhaseDescriptions descriptions = provider.getBazelPhaseDescriptions();
    assertThat(descriptions.get(BazelProfilePhase.LAUNCH).get())
        .isEqualTo(
            new BazelPhaseDescription(
                LAUNCH_START, TimeUtil.getDurationBetween(LAUNCH_START, INIT_START)));
    assertThat(descriptions.get(BazelProfilePhase.INIT).get())
        .isEqualTo(
            new BazelPhaseDescription(
                INIT_START, TimeUtil.getDurationBetween(INIT_START, PREP_START)));
    assertThat(descriptions.get(BazelProfilePhase.PREPARE).get())
        .isEqualTo(
            new BazelPhaseDescription(
                PREP_START, TimeUtil.getDurationBetween(PREP_START, FINISH_START)));
    assertThat(descriptions.get(BazelProfilePhase.FINISH).get())
        .isEqualTo(
            new BazelPhaseDescription(
                FINISH_START, TimeUtil.getDurationBetween(FINISH_START, FINISH_TIME)));
  }

  @Test
  public void getBazelPhaseDescriptionsShouldWorkWhenAllButLaunchAndFinishPhaseAreMissing()
      throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, LAUNCH_START);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, FINISH_TIME))));

    BazelPhaseDescriptions descriptions = provider.getBazelPhaseDescriptions();
    assertThat(descriptions.get(BazelProfilePhase.FINISH).get())
        .isEqualTo(
            new BazelPhaseDescription(
                LAUNCH_START, TimeUtil.getDurationBetween(LAUNCH_START, FINISH_TIME)));
  }

  @Test
  public void getBazelPhaseDescriptionsShouldThrowWhenTwoMarkersHaveTheSameTimestamp()
      throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, LAUNCH_START);
    startTimes.put(BazelProfilePhase.INIT, INIT_START);
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, EVAL_START);
    startTimes.put(BazelProfilePhase.ANALYZE, ANALYZE_START);
    startTimes.put(BazelProfilePhase.PREPARE, PREP_START);
    // Duplicate timestamp.
    startTimes.put(BazelProfilePhase.EXECUTE, ANALYZE_START);
    startTimes.put(BazelProfilePhase.FINISH, FINISH_START);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, FINISH_TIME))));

    InvalidProfileException invalidProfileException =
        assertThrows(InvalidProfileException.class, () -> provider.getBazelPhaseDescriptions());
    assertThat(invalidProfileException).hasMessageThat().contains(BazelProfilePhase.ANALYZE.name);
    assertThat(invalidProfileException).hasMessageThat().contains(BazelProfilePhase.EXECUTE.name);
    assertThat(invalidProfileException)
        .hasMessageThat()
        .contains(String.valueOf(ANALYZE_START.getMicros()));
  }

  @Test
  public void getBazelPhaseDescriptionsShouldThrowWhenLaunchPhaseIsMissing() throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    // No BazelProfilePhase.LAUNCH
    startTimes.put(BazelProfilePhase.INIT, INIT_START);
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, EVAL_START);
    startTimes.put(BazelProfilePhase.ANALYZE, ANALYZE_START);
    startTimes.put(BazelProfilePhase.PREPARE, PREP_START);
    startTimes.put(BazelProfilePhase.EXECUTE, EXEC_START);
    startTimes.put(BazelProfilePhase.FINISH, FINISH_START);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, FINISH_TIME))));

    InvalidProfileException invalidProfileException =
        assertThrows(InvalidProfileException.class, () -> provider.getBazelPhaseDescriptions());
    assertThat(invalidProfileException).hasMessageThat().contains(BazelProfilePhase.LAUNCH.name);
  }

  @Test
  public void getBazelPhaseDescriptionsShouldThrowWhenFinishPhaseIsMissing() throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, LAUNCH_START);
    startTimes.put(BazelProfilePhase.INIT, INIT_START);
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, EVAL_START);
    startTimes.put(BazelProfilePhase.ANALYZE, ANALYZE_START);
    startTimes.put(BazelProfilePhase.PREPARE, PREP_START);
    startTimes.put(BazelProfilePhase.EXECUTE, EXEC_START);
    startTimes.put(BazelProfilePhase.FINISH, FINISH_START);

    useProfile(
        metaData(),
        trace(
            thread(20, 0, BazelProfileConstants.THREAD_MAIN, createPhaseEvents(startTimes, null))));

    InvalidProfileException invalidProfileException =
        assertThrows(InvalidProfileException.class, () -> provider.getBazelPhaseDescriptions());
    assertThat(invalidProfileException)
        .hasMessageThat()
        .contains(BazelProfileConstants.INSTANT_FINISHING);
  }

  @Test
  public void getTotalDurationShouldWorkWhenAllPhasesArePresent() throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, LAUNCH_START);
    startTimes.put(BazelProfilePhase.INIT, INIT_START);
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, EVAL_START);
    startTimes.put(BazelProfilePhase.ANALYZE, ANALYZE_START);
    startTimes.put(BazelProfilePhase.LICENSE, LICENSE_START);
    startTimes.put(BazelProfilePhase.PREPARE, PREP_START);
    startTimes.put(BazelProfilePhase.EXECUTE, EXEC_START);
    startTimes.put(BazelProfilePhase.FINISH, FINISH_START);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, FINISH_TIME))));

    TotalDuration duration = provider.getTotalDuration();
    assertThat(duration.getTotalDuration().isPresent()).isTrue();
    assertThat(duration.getTotalDuration().get())
        .isEqualTo(TimeUtil.getDurationBetween(LAUNCH_START, FINISH_TIME));
  }

  @Test
  public void getTotalDurationShouldWorkWhenAllButLaunchAndFinishPhaseAreMissing()
      throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, LAUNCH_START);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, FINISH_TIME))));

    TotalDuration duration = provider.getTotalDuration();
    assertThat(duration.getTotalDuration().isPresent()).isTrue();
    assertThat(duration.getTotalDuration().get())
        .isEqualTo(TimeUtil.getDurationBetween(LAUNCH_START, FINISH_TIME));
  }

  @Test
  public void getTotalDurationShouldThrowWhenLaunchPhasesIsMissing() throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    // No BazelProfilePhase.LAUNCH
    startTimes.put(BazelProfilePhase.INIT, INIT_START);
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, EVAL_START);
    startTimes.put(BazelProfilePhase.ANALYZE, ANALYZE_START);
    startTimes.put(BazelProfilePhase.LICENSE, LICENSE_START);
    startTimes.put(BazelProfilePhase.PREPARE, PREP_START);
    startTimes.put(BazelProfilePhase.EXECUTE, EXEC_START);
    startTimes.put(BazelProfilePhase.FINISH, FINISH_START);

    useProfile(
        metaData(),
        trace(
            thread(
                20,
                0,
                BazelProfileConstants.THREAD_MAIN,
                createPhaseEvents(startTimes, FINISH_TIME))));

    InvalidProfileException invalidProfileException =
        assertThrows(InvalidProfileException.class, () -> provider.getTotalDuration());
    assertThat(invalidProfileException).hasMessageThat().contains(BazelProfilePhase.LAUNCH.name);
  }

  @Test
  public void getTotalDurationShouldThrowWhenFinishingPhaseIsMissing() throws Exception {
    Map<BazelProfilePhase, Timestamp> startTimes = new HashMap<>();
    startTimes.put(BazelProfilePhase.LAUNCH, LAUNCH_START);
    startTimes.put(BazelProfilePhase.INIT, INIT_START);
    startTimes.put(BazelProfilePhase.TARGET_PATTERN_EVAL, EVAL_START);
    startTimes.put(BazelProfilePhase.ANALYZE, ANALYZE_START);
    startTimes.put(BazelProfilePhase.PREPARE, PREP_START);
    startTimes.put(BazelProfilePhase.EXECUTE, EXEC_START);
    startTimes.put(BazelProfilePhase.FINISH, FINISH_START);
    useProfile(
        metaData(),
        trace(
            thread(20, 0, BazelProfileConstants.THREAD_MAIN, createPhaseEvents(startTimes, null))));

    InvalidProfileException invalidProfileException =
        assertThrows(InvalidProfileException.class, () -> provider.getTotalDuration());
    assertThat(invalidProfileException)
        .hasMessageThat()
        .contains(BazelProfileConstants.INSTANT_FINISHING);
  }
}
