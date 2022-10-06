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
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.sequence;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.dataproviders.DataProviderUnitTestBase;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class TotalQueuingDurationDataProviderTest extends DataProviderUnitTestBase {
  private TotalQueuingDurationDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new TotalQueuingDurationDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnNonZeroQueuingDuration() throws Exception {
    Duration[] durations = {Duration.ofMillis(123), Duration.ofMillis(432), Duration.ofMillis(8)};
    useProfile(
        metaData(),
        trace(
            thread(
                0,
                0,
                "foo",
                sequence(
                    Stream.of(durations),
                    (duration) ->
                        complete(
                            "bar",
                            BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME,
                            Timestamp.ofMicros(0),
                            duration)))));

    Duration totalDuration = Stream.of(durations).reduce(Duration.ZERO, Duration::plus);
    assertThat(provider.getTotalQueuingDuration().getTotalQueuingDuration())
        .isEqualTo(totalDuration);
  }

  @Test
  public void shouldReturnZeroQueuingDuration() throws Exception {
    useProfile(metaData(), trace());

    TotalQueuingDuration queuing = provider.getTotalQueuingDuration();
    verify(dataManager).registerProvider(provider);
    verify(dataManager).getDatum(BazelProfile.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(provider.getTotalQueuingDuration().getTotalQueuingDuration())
        .isEqualTo(Duration.ZERO);
  }
}
