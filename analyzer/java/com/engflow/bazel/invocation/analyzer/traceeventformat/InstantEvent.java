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

import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;

/**
 * Instant events describe an event that has no duration.
 *
 * @see <a
 *     https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/preview#heading=h.lenwiilchoxp">specification</a>
 */
public class InstantEvent {
  @VisibleForTesting
  static final List<String> REQUIRED_JSON_MEMBERS =
      List.of(
          TraceEventFormatConstants.EVENT_CATEGORY,
          TraceEventFormatConstants.EVENT_NAME,
          TraceEventFormatConstants.EVENT_TIMESTAMP);

  private final String category;
  private final String name;
  private final Timestamp timestamp;

  public static InstantEvent fromJson(JsonObject event) {
    List<String> missingMembers = Lists.newArrayList();
    for (String requiredMember : REQUIRED_JSON_MEMBERS) {
      if (!event.has(requiredMember)) {
        missingMembers.add(requiredMember);
      }
    }
    if (!missingMembers.isEmpty()) {
      throw new IllegalArgumentException(
          "Missing members: " + Arrays.toString(missingMembers.toArray()));
    }

    return new InstantEvent(
        event.get(TraceEventFormatConstants.EVENT_CATEGORY).getAsString(),
        event.get(TraceEventFormatConstants.EVENT_NAME).getAsString(),
        Timestamp.ofMicros(event.get(TraceEventFormatConstants.EVENT_TIMESTAMP).getAsLong()));
  }

  /**
   * Parses a {@link InstantEvent} from a JsonObject.
   *
   * @deprecated Use {@link #fromJson(JsonObject)} instead.
   */
  @Deprecated
  public InstantEvent(JsonObject event) {
    this(
        event.get(TraceEventFormatConstants.EVENT_CATEGORY).getAsString(),
        event.get(TraceEventFormatConstants.EVENT_NAME).getAsString(),
        Timestamp.ofMicros(event.get(TraceEventFormatConstants.EVENT_TIMESTAMP).getAsLong()));
  }

  private InstantEvent(String category, String name, Timestamp timestamp) {
    this.category = Preconditions.checkNotNull(category).intern();
    this.name = Preconditions.checkNotNull(name).intern();
    this.timestamp = Preconditions.checkNotNull(timestamp);
  }

  public String getCategory() {
    return category;
  }

  public String getName() {
    return name;
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InstantEvent that = (InstantEvent) o;
    return Objects.equal(timestamp, that.timestamp)
        && Objects.equal(category, that.category)
        && Objects.equal(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(category, name, timestamp);
  }
}
