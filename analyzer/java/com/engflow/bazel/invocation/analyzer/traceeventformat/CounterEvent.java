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
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class CounterEvent {
  private final String name;
  private final Timestamp timestamp;
  private final Map<String, Double> values = new HashMap<>();

  public CounterEvent(JsonObject event) {
    this.name = event.get(TraceEventFormatConstants.EVENT_NAME).getAsString();
    this.timestamp =
        Timestamp.ofMicros(event.get(TraceEventFormatConstants.EVENT_TIMESTAMP).getAsLong());

    // We assume that each key is present at most once.
    event.get(TraceEventFormatConstants.EVENT_ARGUMENTS).getAsJsonObject().entrySet().stream()
        .forEach(e -> values.put(e.getKey(), e.getValue().getAsDouble()));
  }

  public String getName() {
    return name;
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public Double getValue(String type) {
    Preconditions.checkNotNull(type);
    return values.get(type);
  }

  public double getValueOrZero(String type) {
    Preconditions.checkNotNull(type);
    return values.containsKey(type) ? getValue(type) : 0;
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
        && Objects.equal(values, that.values)
        && Objects.equal(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, timestamp, values);
  }
}
