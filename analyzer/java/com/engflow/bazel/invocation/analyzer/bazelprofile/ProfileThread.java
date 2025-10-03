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
import com.google.common.base.Preconditions;
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

  @Nullable private final String name;
  @Nullable private final Integer sortIndex;

  private final ImmutableList<JsonObject> extraMetadata;
  private final ImmutableList<JsonObject> extraEvents;
  private final ImmutableList<CompleteEvent> completeEvents;
  private final ImmutableMap<String, ImmutableList<CounterEvent>> counts;
  private final ImmutableMap<String, ImmutableList<InstantEvent>> instants;

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

  private ProfileThread(
      ThreadId threadId,
      @Nullable String name,
      @Nullable Integer sortIndex,
      @Nullable ImmutableList<JsonObject> extraMetadata,
      @Nullable ImmutableList<JsonObject> extraEvents,
      @Nullable ImmutableList<CompleteEvent> completeEvents,
      @Nullable ImmutableMap<String, ImmutableList<CounterEvent>> counts,
      @Nullable ImmutableMap<String, ImmutableList<InstantEvent>> instants) {
    this.threadId = Preconditions.checkNotNull(threadId);
    this.name = name;
    this.sortIndex = sortIndex;
    this.extraMetadata = extraMetadata == null ? ImmutableList.of() : extraMetadata;
    this.extraEvents = extraEvents == null ? ImmutableList.of() : extraEvents;
    this.completeEvents = completeEvents == null ? ImmutableList.of() : completeEvents;
    this.counts = counts == null ? ImmutableMap.of() : counts;
    this.instants = instants == null ? ImmutableMap.of() : instants;
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

  public List<CompleteEvent> getCompleteEvents() {
    return completeEvents;
  }

  public ImmutableMap<String, ImmutableList<CounterEvent>> getCounts() {
    return counts;
  }

  public ImmutableMap<String, ImmutableList<InstantEvent>> getInstants() {
    return instants;
  }

  public ImmutableList<JsonObject> getExtraEvents() {
    return extraEvents;
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
    return Iterators.filter(events.iterator(), e -> predicate.test(e.category));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        threadId, name, sortIndex, extraMetadata, extraEvents, completeEvents, counts, instants);
  }

  public static class Builder {

    private ThreadId threadId;

    @Nullable private String name;
    @Nullable private Integer sortIndex;

    private List<JsonObject> extraMetadata = new ArrayList<>();
    private List<JsonObject> extraEvents = new ArrayList<>();
    private List<CompleteEvent> completeEvents = new ArrayList<>();
    private Map<String, List<CounterEvent>> counts = new HashMap<>();
    private Map<String, List<InstantEvent>> instants = new HashMap<>();

    /**
     * Parses a {@link JsonObject} as a tracing event and adds it to this thread. Returns {@code
     * true} if parsing and adding the event was successful and {@code false} otherwise.
     */
    public boolean addEvent(JsonObject event) {
      try {
        switch (event.get(TraceEventFormatConstants.EVENT_PHASE).getAsString()) {
          case TraceEventFormatConstants.PHASE_COMPLETE: // Complete events
            {
              completeEvents.add(CompleteEvent.fromJson(event));
              break;
            }

          case "I": // Deprecated, fall-through
          case TraceEventFormatConstants.PHASE_INSTANT: // Instant events
            {
              InstantEvent instantEvent = InstantEvent.fromJson(event);

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
              CounterEvent counterEvent = CounterEvent.fromJson(event);

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

    public ThreadId getThreadId() {
      return threadId;
    }

    public Builder setThreadId(ThreadId threadId) {
      this.threadId = threadId;
      return this;
    }

    @Nullable
    public String getName() {
      return name;
    }

    public Builder setName(@Nullable String name) {
      this.name = name;
      return this;
    }

    @Nullable
    public Integer getSortIndex() {
      return sortIndex;
    }

    public Builder setSortIndex(@Nullable Integer sortIndex) {
      this.sortIndex = sortIndex;
      return this;
    }

    public List<JsonObject> getExtraMetadata() {
      return extraMetadata;
    }

    public Builder setExtraMetadata(List<JsonObject> extraMetadata) {
      this.extraMetadata = extraMetadata;
      return this;
    }

    public List<JsonObject> getExtraEvents() {
      return extraEvents;
    }

    public Builder setExtraEvents(List<JsonObject> extraEvents) {
      this.extraEvents = extraEvents;
      return this;
    }

    public List<CompleteEvent> getCompleteEvents() {
      return completeEvents;
    }

    public Builder setCompleteEvents(List<CompleteEvent> completeEvents) {
      this.completeEvents = completeEvents;
      return this;
    }

    public Map<String, List<CounterEvent>> getCounts() {
      return counts;
    }

    public Builder setCounts(Map<String, List<CounterEvent>> counts) {
      this.counts = counts;
      return this;
    }

    public Map<String, List<InstantEvent>> getInstants() {
      return instants;
    }

    public Builder setInstants(Map<String, List<InstantEvent>> instants) {
      this.instants = instants;
      return this;
    }

    public ProfileThread build() {
      var extraEventsSorted =
          ImmutableList.sortedCopyOf(
              Comparator.comparingLong(e -> e.get("ts").getAsLong()), this.extraEvents);
      var completeEventSorted =
          ImmutableList.sortedCopyOf(Comparator.comparing((e) -> e.start), this.completeEvents);
      var countsSorted =
          ImmutableMap.copyOf(
              counts.entrySet().stream()
                  .collect(
                      Collectors.toMap(
                          Map.Entry::getKey,
                          e -> {
                            List<CounterEvent> entries = e.getValue();
                            entries.sort(Comparator.comparing(CounterEvent::getTimestamp));
                            return ImmutableList.copyOf(entries);
                          })));
      var instantsSorted =
          ImmutableMap.copyOf(
              instants.entrySet().stream()
                  .collect(
                      Collectors.toMap(
                          Map.Entry::getKey,
                          e -> {
                            List<InstantEvent> entries = e.getValue();
                            entries.sort(Comparator.comparing(InstantEvent::getTimestamp));
                            return ImmutableList.copyOf(entries);
                          })));
      return new ProfileThread(
          this.threadId,
          this.name,
          this.sortIndex,
          ImmutableList.copyOf(this.extraMetadata),
          extraEventsSorted,
          completeEventSorted,
          countsSorted,
          instantsSorted);
    }
  }
}
