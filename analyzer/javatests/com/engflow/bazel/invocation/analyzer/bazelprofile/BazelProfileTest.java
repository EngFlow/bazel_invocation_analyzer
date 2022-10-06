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

package com.engflow.bazel.invocation.analyzer.bazelprofile;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.complete;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.sequence;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.UnitTestBase;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;

public class BazelProfileTest extends UnitTestBase {
  @Test
  public void shouldParseGzippedJsonBazelProfile() {
    String profilePath = RUNFILES.rlocation(ROOT + "tiny.json.gz");
    BazelProfile bazelProfile = BazelProfile.createFromPath(profilePath);
    assertThat(bazelProfile.getThreads().count()).isGreaterThan(0);
  }

  @Test
  public void shouldParseJsonBazelProfile() {
    String profilePath = RUNFILES.rlocation(ROOT + "bazel-profile-Long-Phase-Test.json");
    BazelProfile bazelProfile = BazelProfile.createFromPath(profilePath);
    assertThat(bazelProfile.getThreads().count()).isGreaterThan(0);
  }

  @Test
  public void shouldReturnDataProviderForBazelProfile() throws Exception {
    String profilePath = RUNFILES.rlocation(ROOT + "tiny.json.gz");

    BazelProfile profile = BazelProfile.createFromPath(profilePath);
    DataManager dataManager = new DataManager();
    profile.registerWithDataManager(dataManager);

    assertThat(dataManager.getDatum(BazelProfile.class)).isEqualTo(profile);
  }

  @Test
  public void parseCriticalPath() throws Exception {
    var name = "CPP";
    var want =
        new ProfileThread(
            new ThreadId(1, 1),
            BazelProfileConstants.THREAD_CRITICAL_PATH,
            0,
            ImmutableList.of(),
            ImmutableList.of(),
            Lists.newArrayList(
                new CompleteEvent(
                    name,
                    BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                    Timestamp.ofMicros(11),
                    TimeUtil.getDurationForMicros(10),
                    1,
                    1,
                    ImmutableMap.of()),
                new CompleteEvent(
                    name,
                    BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                    Timestamp.ofMicros(21),
                    TimeUtil.getDurationForMicros(10),
                    1,
                    1,
                    ImmutableMap.of()),
                new CompleteEvent(
                    name,
                    BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                    Timestamp.ofMicros(31),
                    TimeUtil.getDurationForMicros(10),
                    1,
                    1,
                    ImmutableMap.of()),
                new CompleteEvent(
                    name,
                    BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                    Timestamp.ofMicros(50),
                    TimeUtil.getDurationForMicros(10),
                    1,
                    1,
                    ImmutableMap.of())),
            ImmutableMap.of(),
            ImmutableMap.of());

    var profile =
        useProfile(
            metaData(),
            trace(
                thread(
                    1,
                    0,
                    BazelProfileConstants.THREAD_CRITICAL_PATH,
                    sequence(
                        want.getCompleteEvents().stream().map(e -> e.start),
                        timestamp ->
                            complete(
                                name,
                                BazelProfileConstants.CAT_CRITICAL_PATH_COMPONENT,
                                timestamp,
                                TimeUtil.getDurationForMicros(10))))));

    assertThat(profile.getCriticalPath()).isEqualTo(want);
  }
}
