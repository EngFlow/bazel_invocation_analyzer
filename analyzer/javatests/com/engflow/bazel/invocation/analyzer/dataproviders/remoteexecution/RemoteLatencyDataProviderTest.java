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
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.dataproviders.DataProviderUnitTestBase;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;

public class RemoteLatencyDataProviderTest extends DataProviderUnitTestBase {
  private RemoteLatencyDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new RemoteLatencyDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnRemoteLatencyWhenCacheCheck() throws Exception {
    var expectedLatencyMillis = 123456;
    var checkRoundTripTimeMillis = expectedLatencyMillis * 2;
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                0,
                0,
                "foo",
                complete(
                    "bar",
                    BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK,
                    Timestamp.ofMicros(123),
                    Duration.ofMillis(checkRoundTripTimeMillis)))));

    var latency = provider.getRemoteLatency();
    assertThat(latency.isEmpty()).isFalse();
    assertThat(latency.getRemoteLatency().isEmpty()).isFalse();
    assertThat(latency.getRemoteLatency().get())
        .isEqualTo(Duration.ofMillis(expectedLatencyMillis));
  }

  @Test
  public void shouldReturnRemoteLatencyWhenRemoteExecution() throws Exception {
    var expectedLatencyMillis = 543210;
    var executionRoundTripTimeMillis = expectedLatencyMillis * 2;
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                0,
                0,
                "foo",
                complete(
                    "bar",
                    BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION,
                    Timestamp.ofMicros(123),
                    Duration.ofMillis(executionRoundTripTimeMillis)))));

    var latency = provider.getRemoteLatency();
    assertThat(latency.isEmpty()).isFalse();
    assertThat(latency.getRemoteLatency().isEmpty()).isFalse();
    assertThat(latency.getRemoteLatency().get())
        .isEqualTo(Duration.ofMillis(expectedLatencyMillis));
  }

  @Test
  public void shouldReturnMinimum() throws Exception {
    var latencyCheckMillis1 = 543210;
    var latencyCheckMillis2 = 123450;
    var latencyExecutionMillis1 = 369120;
    var latencyExecutionMillis2 = 246810;

    var checkRoundTripTimeMillis1 = latencyCheckMillis1 * 2;
    var checkRoundTripTimeMillis2 = latencyCheckMillis2 * 2;
    var executionRoundTripTimeMillis1 = latencyExecutionMillis1 * 2;
    var executionRoundTripTimeMillis2 = latencyExecutionMillis2 * 2;

    var expectedLatency =
        Math.min(
            latencyCheckMillis1,
            Math.min(
                latencyCheckMillis2, Math.min(latencyExecutionMillis1, latencyExecutionMillis2)));
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                0,
                0,
                "foo",
                complete(
                    "bar",
                    BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK,
                    Timestamp.ofMicros(123),
                    Duration.ofMillis(checkRoundTripTimeMillis1)),
                complete(
                    "bam",
                    BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION,
                    Timestamp.ofMicros(123),
                    Duration.ofMillis(executionRoundTripTimeMillis1))),
            thread(
                1,
                1,
                "alpha",
                complete(
                    "beta",
                    BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK,
                    Timestamp.ofMicros(123),
                    Duration.ofMillis(checkRoundTripTimeMillis2)),
                complete(
                    "gamma",
                    BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION,
                    Timestamp.ofMicros(123),
                    Duration.ofMillis(executionRoundTripTimeMillis2)))));

    var latency = provider.getRemoteLatency();
    assertThat(latency.isEmpty()).isFalse();
    assertThat(latency.getRemoteLatency().isEmpty()).isFalse();
    assertThat(latency.getRemoteLatency().get()).isEqualTo(Duration.ofMillis(expectedLatency));
  }

  @Test
  public void shouldBeEmptyWhenRemoteActionsMissing() throws Exception {
    useProfile(metaData(), trace(mainThread()));

    var remoteLatency = provider.getRemoteLatency();
    assertThat(remoteLatency.isEmpty()).isTrue();

    assertThat(remoteLatency.getRemoteLatency().isEmpty()).isTrue();

    assertThat(remoteLatency.getEmptyReason()).isEqualTo(RemoteLatencyDataProvider.EMPTY_REASON);
  }
}
