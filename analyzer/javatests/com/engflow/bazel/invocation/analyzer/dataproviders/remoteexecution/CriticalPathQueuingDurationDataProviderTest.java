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

package com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.complete;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.concat;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.sequence;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.WriteBazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.dataproviders.DataProviderUnitTestBase;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CriticalPathQueuingDurationDataProviderTest extends DataProviderUnitTestBase {
  private CriticalPathQueuingDurationDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new CriticalPathQueuingDurationDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnQueuingDurationWhenTimestampsMatch() throws Exception {
    List<Integer> microseconds = List.of(1_000, 20_000, 300);
    String evaluatorThreadActionNameFormat = "some random action %d";
    String criticalPathThreadActionNameFormat = "action 'some random action %d'";
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_CRITICAL_PATH,
                sequence(
                    microseconds.stream(),
                    (m) ->
                        complete(
                            String.format(criticalPathThreadActionNameFormat, m),
                            BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                            Timestamp.ofMicros(m),
                            TimeUtil.getDurationForMicros(m)))),
            thread(
                1,
                1,
                "some thread",
                concat(
                    sequence(
                        microseconds.stream(),
                        m ->
                            complete(
                                String.format(evaluatorThreadActionNameFormat, m),
                                BazelProfileConstants.CAT_ACTION_PROCESSING,
                                Timestamp.ofMicros(m),
                                TimeUtil.getDurationForMicros(m))),
                    sequence(
                        microseconds.stream(),
                        m ->
                            complete(
                                String.format(evaluatorThreadActionNameFormat, m),
                                BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME,
                                Timestamp.ofMicros(m),
                                TimeUtil.getDurationForMicros(m / 10)))))));

    Duration totalQueueing =
        microseconds.stream()
            .map(m -> TimeUtil.getDurationForMicros(m / 10))
            .reduce(Duration.ZERO, Duration::plus);
    assertThat(provider.getCriticalPathQueuingDuration().getCriticalPathQueuingDuration().get())
        .isEqualTo(totalQueueing);
  }

  @Test
  public void shouldReturnQueuingDurationWhenTimestampsAlmostMatch() throws Exception {
    List<Integer> microseconds = List.of(1_030, 20_010, 380);
    long mod = TimeUtil.getMicros(Timestamp.ACCEPTABLE_DIVERGENCE);
    String evaluatorThreadActionNameFormat = "some random action %d";
    String criticalPathThreadActionNameFormat = "action 'some random action %d'";
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_CRITICAL_PATH,
                sequence(
                    microseconds.stream(),
                    (m) ->
                        complete(
                            String.format(criticalPathThreadActionNameFormat, m),
                            BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                            // Vary the timestamp of the critical path event compared to the
                            // action processing events.
                            Timestamp.ofMicros(m % mod > 50 ? m + m % mod : m - m % mod),
                            TimeUtil.getDurationForMicros(m)))),
            thread(
                1,
                1,
                "some thread",
                concat(
                    sequence(
                        microseconds.stream(),
                        m ->
                            complete(
                                String.format(evaluatorThreadActionNameFormat, m),
                                BazelProfileConstants.CAT_ACTION_PROCESSING,
                                Timestamp.ofMicros(m),
                                TimeUtil.getDurationForMicros(m))),
                    sequence(
                        microseconds.stream(),
                        m ->
                            complete(
                                String.format(evaluatorThreadActionNameFormat, m),
                                BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME,
                                Timestamp.ofMicros(m),
                                TimeUtil.getDurationForMicros(m / 10)))))));

    Duration totalQueueing =
        microseconds.stream()
            .map(m -> TimeUtil.getDurationForMicros(m / 10))
            .reduce(Duration.ZERO, Duration::plus);
    assertThat(provider.getCriticalPathQueuingDuration().getCriticalPathQueuingDuration().get())
        .isEqualTo(totalQueueing);
  }

  @Test
  public void shouldReturnClosestQueuingDurationWhenMultipleEventsMatch() throws Exception {
    String evaluatorThreadActionName = "a generic action name that is seen more often";
    String criticalPathThreadActionName = String.format("action '%s'", evaluatorThreadActionName);
    Duration expectedQueuingDuration = TimeUtil.getDurationForMicros(7_000);
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_CRITICAL_PATH,
                complete(
                    criticalPathThreadActionName,
                    BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                    Timestamp.ofMicros(200),
                    TimeUtil.getDurationForMicros(10_000))),
            thread(
                1,
                1,
                "other thread",
                concat(
                    new WriteBazelProfile.ThreadEvent[] {
                      complete(
                          evaluatorThreadActionName,
                          BazelProfileConstants.CAT_ACTION_PROCESSING,
                          Timestamp.ofMicros(200),
                          TimeUtil.getDurationForMicros(12_000))
                    },
                    new WriteBazelProfile.ThreadEvent[] {
                      complete(
                          "queue",
                          BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME,
                          Timestamp.ofMicros(200),
                          TimeUtil.getDurationForMicros(8_000))
                    })),
            thread(
                2,
                1,
                "preferred thread",
                concat(
                    new WriteBazelProfile.ThreadEvent[] {
                      complete(
                          evaluatorThreadActionName,
                          BazelProfileConstants.CAT_ACTION_PROCESSING,
                          Timestamp.ofMicros(200),
                          TimeUtil.getDurationForMicros(9_000))
                    },
                    new WriteBazelProfile.ThreadEvent[] {
                      complete(
                          "queue",
                          BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME,
                          Timestamp.ofMicros(200),
                          expectedQueuingDuration)
                    }))));

    assertThat(provider.getCriticalPathQueuingDuration().getCriticalPathQueuingDuration().get())
        .isEqualTo(expectedQueuingDuration);
  }

  @Test
  public void shouldNotIncludeQueuingDurationWhenTimestampsDifferTooMuch() throws Exception {
    List<Integer> microseconds = List.of(1_030, 20_010, 380);
    long divergence = TimeUtil.getMicros(Timestamp.ACCEPTABLE_DIVERGENCE) + 1;
    String evaluatorThreadActionNameFormat = "some random action %d";
    String criticalPathThreadActionNameFormat = "action 'some random action %d'";
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_CRITICAL_PATH,
                sequence(
                    microseconds.stream(),
                    (m) ->
                        complete(
                            String.format(criticalPathThreadActionNameFormat, m),
                            BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                            // Vary the timestamp of the critical path event compared to the
                            // action processing events.
                            Timestamp.ofMicros(m - divergence),
                            TimeUtil.getDurationForMicros(m)))),
            thread(
                1,
                1,
                "some thread",
                concat(
                    sequence(
                        microseconds.stream(),
                        m ->
                            complete(
                                String.format(evaluatorThreadActionNameFormat, m),
                                BazelProfileConstants.CAT_ACTION_PROCESSING,
                                Timestamp.ofMicros(m),
                                TimeUtil.getDurationForMicros(m))),
                    sequence(
                        microseconds.stream(),
                        m ->
                            complete(
                                String.format(evaluatorThreadActionNameFormat, m),
                                BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME,
                                Timestamp.ofMicros(m),
                                TimeUtil.getDurationForMicros(m / 10)))))));

    assertThat(provider.getCriticalPathQueuingDuration().getCriticalPathQueuingDuration().get())
        .isEqualTo(Duration.ZERO);
  }

  @Test
  public void shouldNotIncludeQueuingDurationWhenQueuingEndTimeExceedsCriticalPathEntry()
      throws Exception {
    List<Integer> microseconds = List.of(1_030, 20_010, 380);
    long divergence = TimeUtil.getMicros(Timestamp.ACCEPTABLE_DIVERGENCE) + 1;
    String evaluatorThreadActionNameFormat = "some random action %d";
    String criticalPathThreadActionNameFormat = "action 'some random action %d'";
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_CRITICAL_PATH,
                sequence(
                    microseconds.stream(),
                    (m) ->
                        complete(
                            String.format(criticalPathThreadActionNameFormat, m),
                            BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                            Timestamp.ofMicros(m),
                            TimeUtil.getDurationForMicros(m)))),
            thread(
                1,
                1,
                "some thread",
                concat(
                    sequence(
                        microseconds.stream(),
                        m ->
                            complete(
                                String.format(evaluatorThreadActionNameFormat, m),
                                BazelProfileConstants.CAT_ACTION_PROCESSING,
                                Timestamp.ofMicros(m),
                                TimeUtil.getDurationForMicros(m))),
                    sequence(
                        microseconds.stream(),
                        m ->
                            complete(
                                String.format(evaluatorThreadActionNameFormat, m),
                                BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME,
                                Timestamp.ofMicros(m),
                                TimeUtil.getDurationForMicros(m + divergence)))))));

    assertThat(provider.getCriticalPathQueuingDuration().getCriticalPathQueuingDuration().get())
        .isEqualTo(Duration.ZERO);
  }

  @Test
  public void shouldNotIncludeQueuingDurationWhenNamesDiffer() throws Exception {
    List<Integer> microseconds = List.of(1_000, 20_000, 300);
    String evaluatorThreadActionNameFormat = "some random action %d";
    String criticalPathThreadActionNameFormat = "action 'some other action %d'";
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_CRITICAL_PATH,
                sequence(
                    microseconds.stream(),
                    (m) ->
                        complete(
                            String.format(criticalPathThreadActionNameFormat, m),
                            BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                            Timestamp.ofMicros(m),
                            TimeUtil.getDurationForMicros(m)))),
            thread(
                1,
                1,
                "some thread",
                concat(
                    sequence(
                        microseconds.stream(),
                        m ->
                            complete(
                                String.format(evaluatorThreadActionNameFormat, m),
                                BazelProfileConstants.CAT_ACTION_PROCESSING,
                                Timestamp.ofMicros(m),
                                TimeUtil.getDurationForMicros(m))),
                    sequence(
                        microseconds.stream(),
                        m ->
                            complete(
                                String.format(evaluatorThreadActionNameFormat, m),
                                BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME,
                                Timestamp.ofMicros(m),
                                TimeUtil.getDurationForMicros(m / 10)))))));

    assertThat(provider.getCriticalPathQueuingDuration().getCriticalPathQueuingDuration().get())
        .isEqualTo(Duration.ZERO);
  }

  @Test
  public void shouldReturnZeroQueuingDurationWhenCriticalPathIsEmpty() throws Exception {
    useProfile(
        metaData(), trace(mainThread(), thread(0, 0, BazelProfileConstants.THREAD_CRITICAL_PATH)));

    assertThat(provider.getCriticalPathQueuingDuration().getCriticalPathQueuingDuration().get())
        .isEqualTo(Duration.ZERO);
  }

  @Test
  public void shouldBeEmptyWhenCriticalPathIsMissing() throws Exception {
    useProfile(metaData(), trace(mainThread()));

    assertThat(provider.getCriticalPathQueuingDuration().getCriticalPathQueuingDuration().isEmpty())
        .isTrue();
  }
}
