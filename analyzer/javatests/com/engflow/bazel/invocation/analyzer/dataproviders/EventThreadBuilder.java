package com.engflow.bazel.invocation.analyzer.dataproviders;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.complete;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mnemonic;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_ACTION_PROCESSING;

import com.engflow.bazel.invocation.analyzer.WriteBazelProfile.Property;
import com.engflow.bazel.invocation.analyzer.WriteBazelProfile.ThreadEvent;
import com.engflow.bazel.invocation.analyzer.WriteBazelProfile.TraceEvent;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

class EventThreadBuilder {

  private final int id;
  private final int pid;
  private final int index;

  private final List<CompleteEvent> events = new ArrayList<>();

  private int nextActionStart = 0;
  private int nextRelatedStart = 0;

  EventThreadBuilder(int id, int index) {
    this.id = id;
    this.pid = 1; // hard coded by bazel
    this.index = index;
  }

  public CompleteEvent action(String name, String mnemonic, int start, int duration) {
    CompleteEvent event =
        new CompleteEvent(
            name,
            CAT_ACTION_PROCESSING,
            Timestamp.ofSeconds(start),
            Duration.ofSeconds(duration),
            id,
            pid,
            ImmutableMap.of("mnemonic", mnemonic));
    events.add(event);
    nextRelatedStart = start;
    nextActionStart = start + duration;
    return event;
  }

  public CompleteEvent action(String name, String mnemonic, int duration) {
    return action(name, mnemonic, nextActionStart, duration);
  }

  public CompleteEvent related(int start, int duration, String category) {
    var event =
        new CompleteEvent(
            category,
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

  public CompleteEvent related(int duration, String category) {
    return related(nextRelatedStart, duration, category);
  }

  TraceEvent asEvent() {
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
                            .filter(e -> "mnemonic".equals(e.getKey()))
                            .map(e -> mnemonic(e.getValue()))
                            .toArray(Property[]::new)))
            .toArray(ThreadEvent[]::new));
  }
}
