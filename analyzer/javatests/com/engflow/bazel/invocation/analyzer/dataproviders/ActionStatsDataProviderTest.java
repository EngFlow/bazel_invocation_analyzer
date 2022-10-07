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
  public void shouldReturnNoBottlenecksOnEmptyProfile()
      throws DuplicateProviderException, MissingInputException, InvalidProfileException {
    useProfile(metaData(), trace(thread(0, 0, BazelProfileConstants.THREAD_MAIN)));
    assertThat(provider.getActionStats().bottlenecks).isEmpty();
  }

  @Test
  public void shouldCaptureBottleneckRunningSingleAction()
      throws DuplicateProviderException, MissingInputException, InvalidProfileException {
    useCoreCount(4);
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

    assertThat(actionStats.bottlenecks).hasSize(1);
    final var bottleneck = actionStats.bottlenecks.get(0);
    assertThat(bottleneck.getStart().getMicros()).isEqualTo(100);
    assertThat(bottleneck.getEnd().getMicros()).isEqualTo(200);
    assertThat(bottleneck.getAvgActionCount()).isWithin(.0001).of(1);
    assertThat(bottleneck.getEvents()).hasSize(2);
    final var firstAction = bottleneck.getEvents().get(0);
    assertThat(firstAction.start.getMicros()).isEqualTo(80);
    assertThat(TimeUtil.getMicros(firstAction.duration)).isEqualTo(80);
    final var secondAction = bottleneck.getEvents().get(1);
    assertThat(secondAction.start.getMicros()).isEqualTo(160);
    assertThat(TimeUtil.getMicros(secondAction.duration)).isEqualTo(80);
  }

  @Test
  public void shouldNotCaptureBottleneckWhenRunningMaxActionCount()
      throws DuplicateProviderException, MissingInputException, InvalidProfileException {
    useCoreCount(4);
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

    assertThat(actionStats.bottlenecks).isEmpty();
  }

  private void useCoreCount(int count) throws MissingInputException, InvalidProfileException {
    when(dataManager.getDatum(EstimatedCoresUsed.class))
        .thenReturn(new EstimatedCoresUsed(count, 0));
  }
}
