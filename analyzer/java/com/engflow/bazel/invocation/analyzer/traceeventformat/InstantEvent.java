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
import com.google.gson.JsonObject;

public class InstantEvent {
  private final String category;
  private final String name;
  private final Timestamp timestamp;

  public InstantEvent(JsonObject event) {
    this.category = event.get(TraceEventFormatConstants.EVENT_CATEGORY).getAsString();
    this.name = event.get(TraceEventFormatConstants.EVENT_NAME).getAsString();
    this.timestamp =
        Timestamp.ofMicros(event.get(TraceEventFormatConstants.EVENT_TIMESTAMP).getAsLong());
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
