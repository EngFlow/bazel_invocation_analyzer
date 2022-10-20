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
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;

public class MergedEventsPresentDataProviderTest extends DataProviderUnitTestBase {
  private MergedEventsPresentDataProvider provider;

  @Before
  public void setupTest() throws Exception {
    provider = new MergedEventsPresentDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnMergedEventsPresent() throws Exception {
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                20,
                0,
                "skyframe-evaluator 0",
                complete(
                    "merged 3 events",
                    "Unknown event",
                    Timestamp.ofMicros(123),
                    Duration.ofMillis(1)))));

    MergedEventsPresent mergedEventsPresent = provider.getMergedEventsPresent();
    assertThat(mergedEventsPresent.hasMergedEvents()).isTrue();
  }

  @Test
  public void shouldReturnMergedEventsNotPresent() throws Exception {
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                20,
                0,
                "skyframe-evaluator 0",
                complete(
                    "other rule merged 3 events",
                    "Unknown event",
                    Timestamp.ofMicros(123),
                    Duration.ofMillis(1)))));

    MergedEventsPresent mergedEventsPresent = provider.getMergedEventsPresent();
    assertThat(mergedEventsPresent.hasMergedEvents()).isFalse();
  }
}
