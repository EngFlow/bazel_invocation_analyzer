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
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.sequence;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class CriticalPathDurationDataProviderTest extends DataProviderUnitTestBase {
  private CriticalPathDurationDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new CriticalPathDurationDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnCriticalPathDuration() throws Exception {
    Duration[] durations = {Duration.ofMillis(12), Duration.ofMillis(234), Duration.ofMillis(5)};
    useProfileWithDefaults(
        List.of(),
        List.of(
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_CRITICAL_PATH,
                sequence(
                    Stream.of(durations),
                    (duration) ->
                        complete(
                            "some action", "some category", Timestamp.ofMicros(0), duration)))));

    Duration totalDuration = Stream.of(durations).reduce(Duration.ZERO, Duration::plus);
    assertThat(provider.getCriticalPathDuration().getCriticalPathDuration().get())
        .isEqualTo(totalDuration);
  }

  @Test
  public void shouldBeEmptyWhenCriticalPathIsEmpty() throws Exception {
    useMinimalProfile();

    assertThat(provider.getCriticalPathDuration().getCriticalPathDuration().isEmpty()).isTrue();
  }

  @Test
  public void shouldBeEmptyWhenCriticalPathHasNoEvents() throws Exception {
    useProfileWithDefaults(
        List.of(), List.of(thread(0, 0, BazelProfileConstants.THREAD_CRITICAL_PATH)));
    assertThat(provider.getCriticalPathDuration().getCriticalPathDuration().isEmpty()).isTrue();
  }
}
