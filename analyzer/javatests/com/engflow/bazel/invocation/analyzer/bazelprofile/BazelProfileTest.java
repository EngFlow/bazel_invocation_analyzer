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
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.concat;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.count;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.sequence;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.engflow.bazel.invocation.analyzer.UnitTestBase;
import com.engflow.bazel.invocation.analyzer.WriteBazelProfile;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelVersion;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.engflow.bazel.invocation.analyzer.traceeventformat.TraceEventFormatConstants;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;

public class BazelProfileTest extends UnitTestBase {

  private static final WriteBazelProfile.ThreadEvent[] ACTION_COUNTS =
      sequence(
          IntStream.rangeClosed(0, 100).boxed(),
          ts ->
              count(
                  BazelProfileConstants.COUNTER_ACTION_COUNT,
                  ts,
                  BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                  "1"));
  private static final WriteBazelProfile.TraceEvent COUNTER_SERIES_THREAD_NULL =
      thread(null, 1, null, ACTION_COUNTS);
  private static final WriteBazelProfile.Property
      BAZEL_VERSION_PROPERTY_COUNTER_SERIES_IN_NULL_THREAD =
          WriteBazelProfile.Property.put(
              BazelProfileConstants.OTHER_DATA_BAZEL_VERSION,
              String.format(
                  "release %d.0.0",
                  BazelProfile.MIN_BAZEL_MAJOR_VERSION_FOR_COUNTER_SERIES_IN_NULL_THREAD));

  @Test
  public void shouldRejectWhenInvalidJson() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            BazelProfile.createFromInputStream(
                new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  public void shouldRejectWhenOtherDataIsMissing() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> BazelProfile.createFromInputStream(WriteBazelProfile.toInputStream(trace())));
    assertThat(exception.getMessage()).contains(TraceEventFormatConstants.SECTION_OTHER_DATA);
  }

  @Test
  public void shouldRejectWhenTraceEventsIsMissing() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> BazelProfile.createFromInputStream(WriteBazelProfile.toInputStream(trace())));
    assertThat(exception.getMessage()).contains(TraceEventFormatConstants.SECTION_TRACE_EVENTS);
  }

  @Test
  public void shouldRejectWhenOtherDataIsNotAJsonObject() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            BazelProfile.createFromInputStream(
                new ByteArrayInputStream(
                    "{\"otherData\":\"invalid\",\"traceEvents\":[]}"
                        .getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  public void shouldRejectWhenTraceEventsIsNotAJsonObject() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            BazelProfile.createFromInputStream(
                new ByteArrayInputStream(
                    "{\"otherData\":{},\"traceEvents\":\"invalid\"}"
                        .getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  public void shouldRejectWhenMainThreadIsMissing() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                BazelProfile.createFromInputStream(
                    WriteBazelProfile.toInputStream(metaData(), trace())));
    assertThat(exception.getMessage()).contains(BazelProfileConstants.THREAD_MAIN);
  }

  @Test
  public void shouldRejectWhenCounterSeriesIsMissing() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                BazelProfile.createFromInputStream(
                    WriteBazelProfile.toInputStream(metaData(), trace(mainThread()))));
    assertThat(exception.getMessage()).contains("counter series");
  }

  @Test
  public void shouldParseMinimalProfileBeforeActionCountNullThread() {
    BazelProfile bazelProfile =
        createMinimalProfile(
            BazelVersion.parse(
                String.format(
                    "release %d.0.0",
                    BazelProfile.MIN_BAZEL_MAJOR_VERSION_FOR_COUNTER_SERIES_IN_NULL_THREAD - 1)));
    assertThat(bazelProfile.getThreads().count()).isGreaterThan(0);
  }

  @Test
  public void shouldParseMinimalProfileWithActionCountNullThread() {
    BazelProfile bazelProfile =
        createMinimalProfile(
            BazelVersion.parse(
                String.format(
                    "release %d.0.0",
                    BazelProfile.MIN_BAZEL_MAJOR_VERSION_FOR_COUNTER_SERIES_IN_NULL_THREAD)));
    assertThat(bazelProfile.getThreads().count()).isGreaterThan(0);
  }

  @Test
  public void shouldParseGzippedJsonBazelProfile() {
    String profilePath = RUNFILES.rlocation(ROOT + "tiny.json.gz");
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
                mainThread(ACTION_COUNTS),
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

    assertThat(profile.getCriticalPath().get()).isEqualTo(want);
  }

  @Test
  public void getActionCountsWithCounterSeriesInNullThread()
      throws DuplicateProviderException,
          InvalidProfileException,
          MissingInputException,
          NullDatumException {
    var profile =
        useProfile(
            metaData(BAZEL_VERSION_PROPERTY_COUNTER_SERIES_IN_NULL_THREAD),
            trace(
                mainThread(
                    sequence(
                        Stream.of(0, 80, 160, 240),
                        ts ->
                            complete(
                                "An action",
                                BazelProfileConstants.CAT_ACTION_PROCESSING,
                                Timestamp.ofMicros(ts),
                                TimeUtil.getDurationForMicros(80)))),
                thread(
                    null,
                    1,
                    null,
                    concat(
                        sequence(
                            IntStream.rangeClosed(0, 50).boxed(),
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "4")),
                        sequence(
                            IntStream.rangeClosed(51, 100).boxed(),
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "1")),
                        sequence(
                            IntStream.rangeClosed(101, 149).boxed(),
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "4"))))));

    assertThat(profile.getActionCounts()).isNotNull();
    assertThat(profile.getActionCounts()).hasSize(150);
  }

  @Test
  public void getActionCountsWithCounterSeriesInMainThread()
      throws DuplicateProviderException,
          InvalidProfileException,
          MissingInputException,
          NullDatumException {
    var profile =
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
                            IntStream.rangeClosed(0, 10).boxed(),
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "4")),
                        sequence(
                            IntStream.rangeClosed(11, 20).boxed(),
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "1")),
                        sequence(
                            IntStream.rangeClosed(21, 30).boxed(),
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "4"))))));

    assertThat(profile.getActionCounts()).isNotNull();
    assertThat(profile.getActionCounts()).hasSize(31);
  }

  @Test
  public void getActionCountsOld()
      throws DuplicateProviderException,
          InvalidProfileException,
          MissingInputException,
          NullDatumException {
    var profile =
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
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_OLD,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "4")),
                        sequence(
                            IntStream.rangeClosed(101, 200).boxed(),
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_OLD,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "1")),
                        sequence(
                            IntStream.rangeClosed(201, 300).boxed(),
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_OLD,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "4"))))));

    assertThat(profile.getActionCounts()).isNotNull();
    assertThat(profile.getActionCounts()).hasSize(301);
  }

  @Test
  public void getActionCountsMixedUsesNew()
      throws DuplicateProviderException,
          InvalidProfileException,
          MissingInputException,
          NullDatumException {
    var profile =
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
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "4")),
                        sequence(
                            IntStream.rangeClosed(101, 200).boxed(),
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "1")),
                        sequence(
                            IntStream.rangeClosed(201, 300).boxed(),
                            ts ->
                                count(
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_OLD,
                                    ts,
                                    BazelProfileConstants.COUNTER_ACTION_COUNT_TYPE_ACTION,
                                    "4"))))));

    assertThat(profile.getActionCounts()).isNotNull();
    assertThat(profile.getActionCounts()).hasSize(201);
  }

  @Test
  public void isMainThreadShouldReturnFalseOnUnnamedProfileThread() {
    ProfileThread thread = new ProfileThread(new ThreadId(0, 0));
    assertThat(BazelProfile.isMainThread(thread)).isFalse();
  }

  @Test
  public void isMainThreadShouldReturnFalseOnOtherProfileThread() {
    ProfileThread thread =
        new ProfileThread(
            new ThreadId(0, 0), "skyframe-evaluator-1", null, null, null, null, null, null);
    assertThat(BazelProfile.isMainThread(thread)).isFalse();
  }

  @Test
  public void isMainThreadShouldReturnTrueOnNewName() {
    ProfileThread thread =
        new ProfileThread(new ThreadId(0, 0), "Main Thread", null, null, null, null, null, null);
    assertThat(BazelProfile.isMainThread(thread)).isTrue();
  }

  @Test
  public void isMainThreadShouldReturnTrueOnOldName() {
    ProfileThread thread =
        new ProfileThread(new ThreadId(0, 0), "grpc-command-3", null, null, null, null, null, null);
    assertThat(BazelProfile.isMainThread(thread)).isTrue();
  }

  @Test
  public void isGarbageCollectorThreadShouldReturnFalseOnUnnamedProfileThread() {
    ProfileThread thread = new ProfileThread(new ThreadId(0, 0));
    assertThat(BazelProfile.isGarbageCollectorThread(thread)).isFalse();
  }

  @Test
  public void isGarbageCollectorShouldReturnFalseOnOtherProfileThread() {
    ProfileThread thread =
        new ProfileThread(
            new ThreadId(0, 0), "skyframe-evaluator-1", null, null, null, null, null, null);
    assertThat(BazelProfile.isGarbageCollectorThread(thread)).isFalse();
  }

  @Test
  public void isGarbageCollectorShouldReturnTrueOnNewName() {
    ProfileThread thread =
        new ProfileThread(
            new ThreadId(0, 0), "Garbage Collector", null, null, null, null, null, null);
    assertThat(BazelProfile.isGarbageCollectorThread(thread)).isTrue();
  }

  @Test
  public void isGarbageCollectorShouldReturnTrueOnOldName() {
    ProfileThread thread =
        new ProfileThread(new ThreadId(0, 0), "Service Thread", null, null, null, null, null, null);
    assertThat(BazelProfile.isGarbageCollectorThread(thread)).isTrue();
  }

  @Test
  public void getBazelVersionShouldReturnEmptyOnMissingBazelVersion() {
    var profile = createMinimalProfile(null);
    assertThat(profile.getBazelVersion().isEmpty()).isTrue();
  }

  @Test
  public void getBazelVersionShouldReturnEmptyOnInvalidBazelVersion() {
    var profile = createMinimalProfile(BazelVersion.parse("invalid"));
    assertThat(profile.getBazelVersion().isEmpty()).isTrue();
  }

  @Test
  public void getBazelVersionShouldReturnVersionBeforeActionCountNullThread() {
    String validBazelVersion =
        String.format(
            "release %d.1.2",
            BazelProfile.MIN_BAZEL_MAJOR_VERSION_FOR_COUNTER_SERIES_IN_NULL_THREAD - 1);
    BazelVersion bazelVersion = BazelVersion.parse(validBazelVersion);
    var profile = createMinimalProfile(bazelVersion);
    assertThat(profile.getBazelVersion()).isEqualTo(bazelVersion);
  }

  @Test
  public void getBazelVersionShouldReturnVersionWithActionCountNullThread() {
    String validBazelVersion =
        String.format(
            "release %d.0.0",
            BazelProfile.MIN_BAZEL_MAJOR_VERSION_FOR_COUNTER_SERIES_IN_NULL_THREAD);
    BazelVersion bazelVersion = BazelVersion.parse(validBazelVersion);
    var profile = createMinimalProfile(bazelVersion);
    assertThat(profile.getBazelVersion()).isEqualTo(bazelVersion);
  }

  private BazelProfile createMinimalProfile(BazelVersion bazelVersion) {
    ArrayList<WriteBazelProfile.TraceEvent> traceEvents = new ArrayList<>();
    if (bazelVersion != null
        && bazelVersion.getMajor().isPresent()
        && bazelVersion.getMajor().get()
            >= BazelProfile.MIN_BAZEL_MAJOR_VERSION_FOR_COUNTER_SERIES_IN_NULL_THREAD) {
      traceEvents.add(mainThread());
      traceEvents.add(COUNTER_SERIES_THREAD_NULL);
    } else {
      traceEvents.add(mainThread(ACTION_COUNTS));
    }
    var bazelVersionProperty =
        bazelVersion != null
            ? WriteBazelProfile.Property.put(
                BazelProfileConstants.OTHER_DATA_BAZEL_VERSION, bazelVersion.toString())
            : null;
    var metaData = bazelVersionProperty != null ? metaData(bazelVersionProperty) : metaData();
    return BazelProfile.createFromInputStream(
        WriteBazelProfile.toInputStream(
            metaData, trace(traceEvents.toArray(new WriteBazelProfile.TraceEvent[0]))));
  }
}
