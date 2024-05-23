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
 * Counter events can track a value or multiple values as they change over time.
 *
 * @see <a
 *     href="https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/preview#heading=h.msg3086636uq">specification</a>
 */
public class CounterEvent {
  @VisibleForTesting
  static final List<String> REQUIRED_JSON_MEMBERS =
      List.of(
          TraceEventFormatConstants.EVENT_NAME,
          TraceEventFormatConstants.EVENT_TIMESTAMP,
          TraceEventFormatConstants.EVENT_ARGUMENTS);

  private final String name;
  private final Timestamp timestamp;
  private final double totalValue;

  public static CounterEvent fromJson(JsonObject event) {
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

    // For now we are treating all the different counts as a single metric by summing them
    // together. In the future we may want to respect each count individually.
    var totalValue =
        event.get(TraceEventFormatConstants.EVENT_ARGUMENTS).getAsJsonObject().entrySet().stream()
            .mapToDouble(e -> e.getValue().getAsDouble())
            .sum();

    return new CounterEvent(
        event.get(TraceEventFormatConstants.EVENT_NAME).getAsString(),
        Timestamp.ofMicros(event.get(TraceEventFormatConstants.EVENT_TIMESTAMP).getAsLong()),
        totalValue);
  }

  /**
   * Parses a {@link CounterEvent} from a JsonObject.
   *
   * @deprecated Use {@link #fromJson(JsonObject)} instead.
   */
  @Deprecated
  public CounterEvent(JsonObject event) {
    this(
        event.get(TraceEventFormatConstants.EVENT_NAME).getAsString(),
        Timestamp.ofMicros(event.get(TraceEventFormatConstants.EVENT_TIMESTAMP).getAsLong()),
        // For now we are treating all the different counts as a single metric by summing them
        // together. In the future we may want to respect each count individually.
        event.get(TraceEventFormatConstants.EVENT_ARGUMENTS).getAsJsonObject().entrySet().stream()
            .mapToDouble(e -> e.getValue().getAsDouble())
            .sum());
  }

  private CounterEvent(String name, Timestamp timestamp, double totalValue) {
    this.name = Preconditions.checkNotNull(name).intern();
    this.timestamp = Preconditions.checkNotNull(timestamp);
    this.totalValue = totalValue;
  }

  public String getName() {
    return name;
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public double getTotalValue() {
    return totalValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CounterEvent that = (CounterEvent) o;
    return Objects.equal(timestamp, that.timestamp)
        && Double.compare(that.totalValue, totalValue) == 0
        && Objects.equal(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, timestamp, totalValue);
  }
}
