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

package com.engflow.bazel.invocation.analyzer;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.complete;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.property;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_ACTION_PROCESSING;

import com.engflow.bazel.invocation.analyzer.WriteBazelProfile.Property;
import com.engflow.bazel.invocation.analyzer.WriteBazelProfile.ThreadEvent;
import com.engflow.bazel.invocation.analyzer.WriteBazelProfile.TraceEvent;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class EventThreadBuilder {

  private final int id;
  private final int pid;
  private final int index;

  private final List<CompleteEvent> events = new ArrayList<>();

  private int nextActionStart = 0;
  private int nextRelatedStart = 0;

  public EventThreadBuilder(int id, int index) {
    this.id = id;
    this.pid = 1; // hard coded by bazel
    this.index = index;
  }

  public CompleteEvent actionProcessingAction(
      String name, @Nullable String target, @Nullable String mnemonic, int start, int duration) {
    Map<String, String> args = new HashMap<>();
    if (mnemonic != null) {
      args.put(BazelProfileConstants.ARGS_CAT_ACTION_PROCESSING_MNEMONIC, mnemonic);
    }
    if (target != null) {
      args.put(BazelProfileConstants.ARGS_CAT_ACTION_PROCESSING_TARGET, target);
    }
    CompleteEvent event =
        new CompleteEvent(
            name,
            CAT_ACTION_PROCESSING,
            Timestamp.ofSeconds(start),
            Duration.ofSeconds(duration),
            id,
            pid,
            ImmutableMap.copyOf(args));
    events.add(event);
    nextRelatedStart = start;
    nextActionStart = start + duration;
    return event;
  }

  public CompleteEvent actionProcessingAction(
      String name, String mnemonic, int start, int duration) {
    return actionProcessingAction(name, null, mnemonic, start, duration);
  }

  public CompleteEvent actionProcessingAction(String name, String mnemonic, int duration) {
    return actionProcessingAction(name, null, mnemonic, nextActionStart, duration);
  }

  public CompleteEvent related(int start, int duration, String category, String name) {
    var event =
        new CompleteEvent(
            name,
            category,
            Timestamp.ofSeconds(start),
            Duration.ofSeconds(duration),
            id,
            pid,
            ImmutableMap.of());
    events.add(event);
    nextRelatedStart = start + duration;
    return event;
  }

  public CompleteEvent related(int start, int duration, String category) {
    return related(start, duration, category, category);
  }

  public CompleteEvent related(int duration, String category) {
    return related(nextRelatedStart, duration, category);
  }

  public CompleteEvent related(int duration, String category, String name) {
    return related(nextRelatedStart, duration, category, name);
  }

  public TraceEvent asEvent() {
    return thread(
        id,
        index,
        "processor " + id,
        events.stream()
            .map(
                event ->
                    complete(
                        event.name,
                        event.category,
                        event.start,
                        event.duration,
                        event.args.entrySet().stream()
                            .map(e -> property(e.getKey(), e.getValue()))
                            .toArray(Property[]::new)))
            .toArray(ThreadEvent[]::new));
  }
}
