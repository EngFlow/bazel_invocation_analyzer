package com.engflow.bazel.invocation.analyzer.dataproviders;

import com.engflow.bazel.invocation.analyzer.bazelprofile.ThreadId;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.engflow.bazel.invocation.analyzer.traceeventformat.PartialCompleteEvent;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A {@link Bottleneck} captures an interval of a Bazel invocation during which the action count is
 * constantly lower than the core count. The core count is theoretically the highest possible action
 * count, as well as the optimal one the build should always be running at.
 */
public class Bottleneck {
  private final Timestamp start;
  private final Timestamp end;
  private final double sampleTotal;
  private final int sampleCount;
  private final ImmutableList<CompleteEvent> eventsWithOverlap;
  private final ImmutableMap<ThreadId, Duration> queuingDurationByThreadId;

  public Bottleneck(
      Timestamp start,
      Timestamp end,
      double sampleTotal,
      int sampleCount,
      List<CompleteEvent> eventsWithOverlap,
      Map<ThreadId, Duration> queuingDurationByThreadId) {
    this.start = start;
    this.end = end;
    this.sampleTotal = sampleTotal;
    this.sampleCount = sampleCount;
    this.eventsWithOverlap = ImmutableList.copyOf(eventsWithOverlap);
    this.queuingDurationByThreadId = ImmutableMap.copyOf(queuingDurationByThreadId);
  }

  public static Builder newBuilder(Timestamp startTs) {
    return new Builder(startTs);
  }

  public static Builder newBuilder(Bottleneck bottleneck) {
    return new Builder(bottleneck);
  }

  /**
   * @return The timestamp at which this bottleneck starts.
   */
  public Timestamp getStart() {
    return start;
  }

  /**
   * @return The timestamp at which this bottleneck ends.
   */
  public Timestamp getEnd() {
    return end;
  }

  /**
   * @return The average action count of this bottleneck.
   */
  public double getAvgActionCount() {
    return sampleTotal / sampleCount;
  }

  /**
   * Returns a list of {@link CompleteEvent}s that are at least partially involved in the
   * bottleneck. That is, the interval of each event overlaps with the interval of the bottleneck.
   *
   * @return A list of events that are at least partially involved in the bottleneck.
   */
  public List<CompleteEvent> getOverlappingEvents() {
    return eventsWithOverlap;
  }

  /**
   * Returns a list of {@link PartialCompleteEvent}s that are involved in the bottleneck. That is,
   * the interval of each underlying event overlaps with the interval of the bottleneck, and the
   * start and end time have been cropped to reflect the interval that is completely contained in
   * the bottleneck's interval.
   *
   * @return A list of partial events that are involved in the bottleneck.
   */
  public List<PartialCompleteEvent> getPartialEvents() {
    return eventsWithOverlap.stream()
        .map(
            event ->
                new PartialCompleteEvent(
                    event,
                    start.compareTo(event.start) > 0 ? start : event.start,
                    end.compareTo(event.end) < 0 ? end : event.end))
        .collect(Collectors.toList());
  }

  /**
   * @return The duration of this bottleneck.
   */
  public Duration getDuration() {
    return TimeUtil.getDurationBetween(end, start);
  }

  /**
   * @return The maximum queuing duration found for the same {@link ThreadId}.
   */
  public Duration getMaxQueuingDuration() {
    return queuingDurationByThreadId.values().stream()
        .max(Duration::compareTo)
        .orElse(Duration.ZERO);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Bottleneck that = (Bottleneck) o;
    return Double.compare(that.sampleTotal, sampleTotal) == 0
        && sampleCount == that.sampleCount
        && Objects.equals(start, that.start)
        && Objects.equals(end, that.end)
        && eventsWithOverlap.equals(that.eventsWithOverlap)
        && queuingDurationByThreadId.equals(that.queuingDurationByThreadId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        start, end, sampleTotal, sampleCount, eventsWithOverlap, queuingDurationByThreadId);
  }

  public static class Builder {
    private Timestamp start;
    private Timestamp end;
    private double sampleTotal;
    private int sampleCount;
    private final List<CompleteEvent> events = new ArrayList<>();
    private final Map<ThreadId, Duration> queuingDurationByThreadId = new HashMap<>();

    private Builder(Timestamp start) {
      this.start = start;
      this.end = start;
    }

    private Builder(Bottleneck bottleneck) {
      this.start = bottleneck.start;
      this.end = bottleneck.end;
      this.sampleTotal = bottleneck.sampleTotal;
      this.sampleCount = bottleneck.sampleCount;
      this.events.addAll(bottleneck.eventsWithOverlap);
    }

    public Timestamp getStart() {
      return start;
    }

    public Builder setStart(Timestamp start) {
      Preconditions.checkNotNull(start);
      // Start time must not be modified after events were added.
      Preconditions.checkState(events.isEmpty());
      this.start = start;
      return this;
    }

    public Timestamp getEnd() {
      return end;
    }

    public Builder setEnd(Timestamp end) {
      Preconditions.checkNotNull(end);
      // End time must not be modified after events were added.
      Preconditions.checkState(events.isEmpty());
      this.end = end;
      return this;
    }

    public Builder addActionCountSample(double actionCount) {
      sampleCount++;
      sampleTotal += actionCount;
      return this;
    }

    public Builder addQueuingDuration(ThreadId threadId, Duration queuingDuration) {
      Duration previousDuration = queuingDurationByThreadId.getOrDefault(threadId, Duration.ZERO);
      queuingDurationByThreadId.put(threadId, previousDuration.plus(queuingDuration));
      return this;
    }

    public Builder addEvent(CompleteEvent event) {
      events.add(event);
      return this;
    }

    public Bottleneck build() {
      return new Bottleneck(
          start, end, sampleTotal, sampleCount, events, queuingDurationByThreadId);
    }
  }
}
