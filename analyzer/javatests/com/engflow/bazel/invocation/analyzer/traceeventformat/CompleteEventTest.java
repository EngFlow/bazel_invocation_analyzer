/*
 * Copyright 2024 EngFlow Inc.
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.google.gson.JsonObject;
import org.junit.Test;

public class CompleteEventTest {
  @Test
  public void fromJsonThrowsOnMissingMembers() {
    var jsonObject = new JsonObject();
    var e = assertThrows(IllegalArgumentException.class, () -> CompleteEvent.fromJson(jsonObject));
    for (var member : CompleteEvent.REQUIRED_JSON_MEMBERS) {
      assertThat(e.getMessage()).contains(member);
    }
  }

  @Test
  public void fromJsonMinimal() {
    var jsonObject = new JsonObject();
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_TIMESTAMP, 123_456_789);
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_DURATION, 456_789);
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_THREAD_ID, 42);
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_PROCESS_ID, 765);

    var event = CompleteEvent.fromJson(jsonObject);
    assertThat(event.name).isNull();
    assertThat(event.category).isNull();
    assertThat(event.start.getMicros()).isEqualTo(123_456_789);
    assertThat(TimeUtil.getMicros(event.duration)).isEqualTo(456_789);
    assertThat(event.threadId).isEqualTo(42);
    assertThat(event.processId).isEqualTo(765);
    assertThat(event.args).isEmpty();
  }

  @Test
  public void fromJsonAll() {
    var jsonObject = new JsonObject();
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_NAME, "fooName");
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_CATEGORY, "fooCat");
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_TIMESTAMP, 123_456_789);
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_DURATION, 456_789);
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_THREAD_ID, 42);
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_PROCESS_ID, 765);

    var args = new JsonObject();
    args.addProperty("argFooKey", "argFooVal");
    args.addProperty("argBarKey", "argBarVal");
    jsonObject.add(TraceEventFormatConstants.EVENT_ARGUMENTS, args);

    var event = CompleteEvent.fromJson(jsonObject);
    assertThat(event.name).isEqualTo("fooName");
    assertThat(event.category).isEqualTo("fooCat");
    assertThat(event.start.getMicros()).isEqualTo(123_456_789);
    assertThat(TimeUtil.getMicros(event.duration)).isEqualTo(456_789);
    assertThat(event.threadId).isEqualTo(42);
    assertThat(event.processId).isEqualTo(765);
    assertThat(event.args).hasSize(2);
    assertThat(event.args).containsEntry("argFooKey", "argFooVal");
    assertThat(event.args).containsEntry("argBarKey", "argBarVal");
  }
}
