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
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.sequence;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class GarbageCollectionStatsDataProviderTest extends DataProviderUnitTestBase {
  private GarbageCollectionStatsDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new GarbageCollectionStatsDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnMajorGarbageCollection() throws Exception {
    Duration singleGcDuration = TimeUtil.getDurationForMicros(10_000);
    useProfile(
        metaData(),
        trace(
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_GARBAGE_COLLECTOR,
                concat(
                    sequence(
                        Stream.of(50_000, 100_000, 150_000, 200_000),
                        ts ->
                            complete(
                                BazelProfileConstants.COMPLETE_MAJOR_GARBAGE_COLLECTION,
                                BazelProfileConstants.CAT_GARBAGE_COLLECTION,
                                Timestamp.ofMicros(ts),
                                singleGcDuration))))));

    GarbageCollectionStats gcStats = provider.getGarbageCollectionStats();
    assertThat(gcStats.hasMajorGarbageCollection()).isTrue();
    assertThat(gcStats.getMajorGarbageCollectionDuration())
        .isEqualTo(singleGcDuration.multipliedBy(4));
  }

  @Test
  public void shouldReturnNoMajorGarbageCollection() throws Exception {
    Duration singleGcDuration = TimeUtil.getDurationForMicros(10_000);
    useProfile(
        metaData(),
        trace(
            thread(
                0,
                0,
                BazelProfileConstants.THREAD_GARBAGE_COLLECTOR,
                concat(
                    sequence(
                        Stream.of(50_000, 100_000, 150_000, 200_000),
                        ts ->
                            complete(
                                BazelProfileConstants.COMPLETE_MINOR_GARBAGE_COLLECTION,
                                BazelProfileConstants.CAT_GARBAGE_COLLECTION,
                                Timestamp.ofMicros(ts),
                                singleGcDuration))))));

    GarbageCollectionStats gcStats = provider.getGarbageCollectionStats();
    assertThat(gcStats.hasMajorGarbageCollection()).isFalse();
    assertThat(gcStats.getMajorGarbageCollectionDuration()).isEqualTo(Duration.ZERO);
  }
}
