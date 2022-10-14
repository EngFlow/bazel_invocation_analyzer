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

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.complete;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.concat;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.count;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.sequence;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class ActionStatsDataProviderTest extends DataProviderUnitTestBase {
  private ActionStatsDataProvider provider;

  @Before
  public void setupTest() throws Exception {
    provider = new ActionStatsDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnEmptyOnEmptyProfile()
      throws DuplicateProviderException, MissingInputException, InvalidProfileException {
    useEstimatedCoresUsed(4);
    useProfile(metaData(), trace(thread(0, 0, BazelProfileConstants.THREAD_MAIN)));

    assertThat(provider.getActionStats().getBottlenecks().isEmpty()).isTrue();
  }

  @Test
  public void shouldReturnEmptyOnEmptyEstimatedCoresUsed()
      throws DuplicateProviderException, MissingInputException, InvalidProfileException {
    useEstimatedCoresUsed(null);
    useProfile(
        metaData(),
        trace(
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_MAIN,
                sequence(
                    IntStream.rangeClosed(0, 100).boxed(),
                    ts -> count(BazelProfileConstants.COUNTER_ACTION_COUNT, ts, "action", "4")))));

    assertThat(provider.getActionStats().getBottlenecks().isEmpty()).isTrue();
  }

  @Test
  public void shouldCaptureBottleneckRunningSingleAction()
      throws DuplicateProviderException, MissingInputException, InvalidProfileException {
    useEstimatedCoresUsed(4);
    useProfile(
        metaData(),
        trace(
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_MAIN,
                concat(
                    sequence(
                        Stream.of(0, 80, 160, 240),
                        ts ->
                            complete(
                                "An action",
                                BazelProfileConstants.CAT_ACTION_PROCESSING,
                                Timestamp.ofMicros(ts),
                                TimeUtil.getDurationForMicros(80))),
                    sequence(
                        IntStream.rangeClosed(0, 100).boxed(),
                        ts -> count(BazelProfileConstants.COUNTER_ACTION_COUNT, ts, "action", "4")),
                    sequence(
                        IntStream.rangeClosed(100, 200).boxed(),
                        ts -> count(BazelProfileConstants.COUNTER_ACTION_COUNT, ts, "action", "1")),
                    sequence(
                        IntStream.rangeClosed(200, 300).boxed(),
                        ts ->
                            count(
                                BazelProfileConstants.COUNTER_ACTION_COUNT, ts, "action", "4"))))));

    final var actionStats = provider.getActionStats();

    assertThat(actionStats.getBottlenecks().get()).hasSize(1);
    final var bottleneck = actionStats.getBottlenecks().get().get(0);
    assertThat(bottleneck.getStart().getMicros()).isEqualTo(100);
    assertThat(bottleneck.getEnd().getMicros()).isEqualTo(200);
    assertThat(bottleneck.getAvgActionCount()).isWithin(.0001).of(1);
    assertThat(bottleneck.getPartialEvents()).hasSize(2);
    final var firstAction = bottleneck.getPartialEvents().get(0);
    assertThat(firstAction.completeEvent.start.getMicros()).isEqualTo(80);
    assertThat(firstAction.croppedStart).isEqualTo(bottleneck.getStart());
    assertThat(TimeUtil.getMicros(firstAction.completeEvent.duration)).isEqualTo(80);
    assertThat(TimeUtil.getMicros(firstAction.croppedDuration)).isEqualTo(60);
    final var secondAction = bottleneck.getPartialEvents().get(1);
    assertThat(secondAction.completeEvent.start.getMicros()).isEqualTo(160);
    assertThat(secondAction.croppedEnd).isEqualTo(bottleneck.getEnd());
    assertThat(TimeUtil.getMicros(secondAction.completeEvent.duration)).isEqualTo(80);
    assertThat(TimeUtil.getMicros(secondAction.croppedDuration)).isEqualTo(40);
  }

  @Test
  public void shouldNotCaptureBottleneckWhenRunningMaxActionCount()
      throws DuplicateProviderException, MissingInputException, InvalidProfileException {
    useEstimatedCoresUsed(4);
    useProfile(
        metaData(),
        trace(
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_MAIN,
                concat(
                    sequence(
                        Stream.of(0, 80, 160, 240),
                        ts ->
                            complete(
                                "An action",
                                BazelProfileConstants.CAT_ACTION_PROCESSING,
                                Timestamp.ofMicros(ts),
                                TimeUtil.getDurationForMicros(80))),
                    sequence(
                        IntStream.rangeClosed(0, 300).boxed(),
                        ts ->
                            count(
                                BazelProfileConstants.COUNTER_ACTION_COUNT, ts, "action", "4"))))));

    final var actionStats = provider.getActionStats();

    assertThat(actionStats.getBottlenecks().get()).isEmpty();
  }

  private void useEstimatedCoresUsed(Integer count)
      throws MissingInputException, InvalidProfileException {
    when(dataManager.getDatum(EstimatedCoresUsed.class))
        .thenReturn(
            count == null ? new EstimatedCoresUsed("empty") : new EstimatedCoresUsed(count, 0));
  }
}
