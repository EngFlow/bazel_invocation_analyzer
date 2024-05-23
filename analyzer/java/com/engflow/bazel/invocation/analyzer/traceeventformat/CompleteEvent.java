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

package com.engflow.bazel.invocation.analyzer.traceeventformat;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Complete events describe an event with a duration.
 *
 * @see <a
 *     https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/preview#heading=h.lpfof2aylapb">specification</a>
 */
public class CompleteEvent {
  @VisibleForTesting
  static final List<String> REQUIRED_JSON_MEMBERS =
      List.of(
          TraceEventFormatConstants.EVENT_TIMESTAMP,
          TraceEventFormatConstants.EVENT_DURATION,
          TraceEventFormatConstants.EVENT_THREAD_ID,
          TraceEventFormatConstants.EVENT_PROCESS_ID);

  @Nullable public final String name;
  @Nullable public final String category;
  public final Timestamp start;
  public final Duration duration;
  public final Timestamp end;
  public final int threadId;
  public final int processId;
  public final Map<String, String> args;

  public static CompleteEvent fromJson(JsonObject object) {
    Preconditions.checkNotNull(object);
    List<String> missingMembers = Lists.newArrayList();
    for (String requiredMember : REQUIRED_JSON_MEMBERS) {
      if (!object.has(requiredMember)) {
        missingMembers.add(requiredMember);
      }
    }
    if (!missingMembers.isEmpty()) {
      throw new IllegalArgumentException(
          "Missing members: " + Arrays.toString(missingMembers.toArray()));
    }

    return new CompleteEvent(
        object.has(TraceEventFormatConstants.EVENT_NAME)
            ? object.get(TraceEventFormatConstants.EVENT_NAME).getAsString()
            : null,
        object.has(TraceEventFormatConstants.EVENT_CATEGORY)
            ? object.get(TraceEventFormatConstants.EVENT_CATEGORY).getAsString()
            : null,
        Timestamp.ofMicros(object.get(TraceEventFormatConstants.EVENT_TIMESTAMP).getAsLong()),
        TimeUtil.getDurationForMicros(
            object.get(TraceEventFormatConstants.EVENT_DURATION).getAsLong()),
        object.get(TraceEventFormatConstants.EVENT_THREAD_ID).getAsInt(),
        object.get(TraceEventFormatConstants.EVENT_PROCESS_ID).getAsInt(),
        object.has(TraceEventFormatConstants.EVENT_ARGUMENTS)
            ? object
                .get(TraceEventFormatConstants.EVENT_ARGUMENTS)
                .getAsJsonObject()
                .entrySet()
                .stream()
                .collect(toImmutableMap(e -> e.getKey(), e -> e.getValue().getAsString()))
            : ImmutableMap.of());
  }

  /**
   * Parses a {@link CompleteEvent} from a JsonObject.
   *
   * @deprecated Use {@link #fromJson(JsonObject)} instead.
   */
  @Deprecated
  public CompleteEvent(JsonObject object) {
    this(
        object.has(TraceEventFormatConstants.EVENT_NAME)
            ? object.get(TraceEventFormatConstants.EVENT_NAME).getAsString()
            : null,
        object.has(TraceEventFormatConstants.EVENT_CATEGORY)
            ? object.get(TraceEventFormatConstants.EVENT_CATEGORY).getAsString()
            : null,
        Timestamp.ofMicros(object.get(TraceEventFormatConstants.EVENT_TIMESTAMP).getAsLong()),
        TimeUtil.getDurationForMicros(
            object.get(TraceEventFormatConstants.EVENT_DURATION).getAsLong()),
        object.get(TraceEventFormatConstants.EVENT_THREAD_ID).getAsInt(),
        object.get(TraceEventFormatConstants.EVENT_PROCESS_ID).getAsInt(),
        object.has(TraceEventFormatConstants.EVENT_ARGUMENTS)
            ? object
                .get(TraceEventFormatConstants.EVENT_ARGUMENTS)
                .getAsJsonObject()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getAsString()))
            : ImmutableMap.of());
  }

  public CompleteEvent(
      @Nullable String name,
      @Nullable String category,
      Timestamp start,
      Duration duration,
      int threadId,
      int processId,
      Map<String, String> args) {
    this.name = name == null ? null : name.intern();
    this.category = category == null ? null : category.intern();
    this.start = start;
    this.duration = duration;
    this.end = start.plus(duration);
    this.threadId = threadId;
    this.processId = processId;
    this.args =
        args.entrySet().stream()
            .collect(
                Collectors.toUnmodifiableMap(e -> e.getKey().intern(), e -> e.getValue().intern()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CompleteEvent that = (CompleteEvent) o;
    return Objects.equal(name, that.name)
        && Objects.equal(category, that.category)
        && Objects.equal(start, that.start)
        && Objects.equal(duration, that.duration)
        && Objects.equal(end, that.end)
        && threadId == that.threadId
        && processId == that.processId
        && Objects.equal(args, that.args);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, category, start, duration, end, threadId, processId, args);
  }

  @Override
  public String toString() {
    return String.format(
        "CompleteEvent{"
            + "name='%s', category='%s', "
            + "start=%s, duration=%s, end=%s, "
            + "threadId=%d, processId=%d, args=%s"
            + "}",
        name, category, start, duration, end, threadId, processId, args);
  }
}
