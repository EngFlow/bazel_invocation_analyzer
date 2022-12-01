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
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.Map.Entry;
import javax.annotation.Nullable;

public class CompleteEvent {
  @Nullable public final String name;
  @Nullable public final String category;
  public final Timestamp start;
  public final Duration duration;
  public final Timestamp end;
  public final int threadId;
  public final int processId;
  public final ImmutableMap<String, String> args;

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
                .collect(toImmutableMap(Entry::getKey, e1 -> e1.getValue().getAsString()))
            : ImmutableMap.of());
  }

  public CompleteEvent(
      @Nullable String name,
      @Nullable String category,
      Timestamp start,
      Duration duration,
      int threadId,
      int processId,
      ImmutableMap<String, String> args) {
    this.name = name;
    this.category = category;
    this.start = start;
    this.duration = duration;
    this.end = start.plus(duration);
    this.threadId = threadId;
    this.processId = processId;
    this.args = args;
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
    return "CompleteEvent{" +
        "name='" + name + '\'' +
        ", category='" + category + '\'' +
        ", start=" + start +
        ", duration=" + duration +
        ", end=" + end +
        ", threadId=" + threadId +
        ", processId=" + processId +
        ", args=" + args +
        '}';
  }
}
