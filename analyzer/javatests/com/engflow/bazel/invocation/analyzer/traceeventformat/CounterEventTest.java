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

import com.google.gson.JsonObject;
import org.junit.Test;

public class CounterEventTest {
  @Test
  public void fromJsonThrowsOnMissingMembers() {
    var jsonObject = new JsonObject();
    var e = assertThrows(IllegalArgumentException.class, () -> CounterEvent.fromJson(jsonObject));
    for (var member : CounterEvent.REQUIRED_JSON_MEMBERS) {
      assertThat(e.getMessage()).contains(member);
    }
  }

  @Test
  public void fromJson() {
    var jsonObject = new JsonObject();
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_NAME, "someEventName");
    jsonObject.addProperty(TraceEventFormatConstants.EVENT_TIMESTAMP, 1_234_567_890);

    var args = new JsonObject();
    args.addProperty("argFooKey", 1.23);
    args.addProperty("argBarKey", 3.45);
    jsonObject.add(TraceEventFormatConstants.EVENT_ARGUMENTS, args);

    var event = CounterEvent.fromJson(jsonObject);
    assertThat(event.getName()).isEqualTo("someEventName");
    assertThat(event.getTimestamp().getMicros()).isEqualTo(1_234_567_890);
    assertThat(event.getTotalValue()).isEqualTo(1.23 + 3.45);
  }
}
