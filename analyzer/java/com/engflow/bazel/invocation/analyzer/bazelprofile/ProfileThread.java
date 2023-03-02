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

package com.engflow.bazel.invocation.analyzer.bazelprofile;

import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CounterEvent;
import com.engflow.bazel.invocation.analyzer.traceeventformat.InstantEvent;
import com.engflow.bazel.invocation.analyzer.traceeventformat.TraceEventFormatConstants;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class ProfileThread {
  private final ThreadId threadId;

  @Nullable private String name;
  @Nullable private Integer sortIndex;

  private final List<JsonObject> extraMetadata;
  private final List<JsonObject> extraEvents;
  private final List<CompleteEvent> completeEvents;
  private final Map<String, List<CounterEvent>> counts;
  private final Map<String, List<InstantEvent>> instants;

  public ProfileThread(ThreadId threadId) {
    this(
        threadId,
        null,
        null,
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        new HashMap<>(),
        new HashMap<>());
  }

  @Override
  public String toString() {
    return String.format(
        "ProfileThread{"
            + "threadId=%s, "
            + "name='%s', "
            + "sortIndex=%s, "
            + "extraMetadata=%s, "
            + "extraEvents=%s,"
            + "completeEvents=%s, "
            + "counts=%s, "
            + "instants=%s"
            + "}",
        threadId, name, sortIndex, extraMetadata, extraEvents, completeEvents, counts, instants);
  }

  public ProfileThread(
      ThreadId threadId,
      @Nullable String name,
      @Nullable Integer sortIndex,
      List<JsonObject> extraMetadata,
      List<JsonObject> extraEvents,
      List<CompleteEvent> completeEvents,
      Map<String, List<CounterEvent>> counts,
      Map<String, List<InstantEvent>> instants) {
    this.threadId = threadId;
    this.name = name;
    this.sortIndex = sortIndex;
    this.extraMetadata = extraMetadata;
    this.extraEvents = extraEvents;
    this.completeEvents = completeEvents;
    this.counts = counts;
    this.instants = instants;
  }

  public ThreadId getThreadId() {
    return threadId;
  }

  @Nullable
  public String getName() {
    return name;
  }

  @Nullable
  public Integer getSortIndex() {
    return sortIndex;
  }

  /**
   * Parses a {@link JsonObject} as a tracing event and adds it to this thread. Returns {@code true}
   * if parsing and adding the event was successful and {@code false} otherwise.
   */
  public boolean addEvent(JsonObject event) {
    try {
      switch (event.get(TraceEventFormatConstants.EVENT_PHASE).getAsString()) {
        case TraceEventFormatConstants.PHASE_COMPLETE: // Complete events
          {
            completeEvents.add(new CompleteEvent(event));
            break;
          }

        case "I": // Deprecated, fall-through
        case TraceEventFormatConstants.PHASE_INSTANT: // Instant events
          {
            InstantEvent instantEvent = new InstantEvent(event);

            List<InstantEvent> instantList =
                instants.compute(
                    instantEvent.getCategory(),
                    (key, c) -> {
                      if (c == null) {
                        c = new ArrayList<>();
                      }
                      return c;
                    });

            instantList.add(instantEvent);
            break;
          }

        case TraceEventFormatConstants.PHASE_COUNTER: // Counter events
          {
            CounterEvent counterEvent = new CounterEvent(event);

            List<CounterEvent> countList =
                counts.compute(
                    counterEvent.getName(),
                    (key, c) -> {
                      if (c == null) {
                        c = new ArrayList<>();
                      }
                      return c;
                    });

            countList.add(counterEvent);
            break;
          }

        case TraceEventFormatConstants.PHASE_METADATA: // Metadata events
          {
            String eventName = event.get(TraceEventFormatConstants.EVENT_NAME).getAsString();
            if (TraceEventFormatConstants.METADATA_THREAD_NAME.equals(eventName)) {
              this.name =
                  event
                      .get(TraceEventFormatConstants.EVENT_ARGUMENTS)
                      .getAsJsonObject()
                      .get("name")
                      .getAsString();
            } else if (TraceEventFormatConstants.METADATA_THREAD_SORT_INDEX.equals(eventName)) {
              this.sortIndex =
                  Integer.parseInt(
                      event
                          .get(TraceEventFormatConstants.EVENT_ARGUMENTS)
                          .getAsJsonObject()
                          .get("sort_index")
                          .getAsString());
            } else {
              extraMetadata.add(event);
            }
            break;
          }

        default:
          extraEvents.add(event);
      }

      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  public List<CompleteEvent> getCompleteEvents() {
    completeEvents.sort(Comparator.comparing((e) -> e.start));
    return ImmutableList.copyOf(completeEvents);
  }

  public ImmutableMap<String, ImmutableList<CounterEvent>> getCounts() {
    return ImmutableMap.copyOf(
        counts.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                      List<CounterEvent> entries = e.getValue();
                      entries.sort(Comparator.comparing(CounterEvent::getTimestamp));
                      return ImmutableList.copyOf(entries);
                    })));
  }

  public ImmutableMap<String, ImmutableList<InstantEvent>> getInstants() {
    return ImmutableMap.copyOf(
        instants.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                      List<InstantEvent> entries = e.getValue();
                      entries.sort(Comparator.comparing(InstantEvent::getTimestamp));
                      return ImmutableList.copyOf(entries);
                    })));
  }

  public ImmutableList<JsonObject> getExtraEvents() {
    extraEvents.sort(Comparator.comparingLong(e -> e.get("ts").getAsLong()));
    return ImmutableList.copyOf(extraEvents);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProfileThread that = (ProfileThread) o;
    return Objects.equal(threadId, that.threadId)
        && Objects.equal(name, that.name)
        && Objects.equal(sortIndex, that.sortIndex)
        && Objects.equal(extraMetadata, that.extraMetadata)
        && Objects.equal(extraEvents, that.extraEvents)
        && Objects.equal(completeEvents, that.completeEvents)
        && Objects.equal(counts, that.counts)
        && Objects.equal(instants, that.instants);
  }

  public static Iterator<CompleteEvent> ofCategoryTypes(
          Iterable<CompleteEvent> events, String... categories) {
    Predicate<String> predicate = Set.of(categories)::contains;
    return Iterators.filter(
            events.iterator(),
            e -> predicate.test(e.category));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        threadId, name, sortIndex, extraMetadata, extraEvents, completeEvents, counts, instants);
  }
}
