package com.engflow.bazel.invocation.analyzer.dataproviders;

import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
  private final ImmutableList<CompleteEvent> events;

  public Bottleneck(
      Timestamp start,
      Timestamp end,
      double sampleTotal,
      int sampleCount,
      List<CompleteEvent> events) {
    this.start = start;
    this.end = end;
    this.sampleTotal = sampleTotal;
    this.sampleCount = sampleCount;
    this.events = ImmutableList.copyOf(events);
  }

  public static Builder newBuilder(Timestamp startTs) {
    return new Builder(startTs);
  }

  public static Builder newBuilder(Bottleneck bottleneck) {
    return new Builder(bottleneck);
  }

  public Timestamp getStart() {
    return start;
  }

  public Timestamp getEnd() {
    return end;
  }

  public double getAvgActionCount() {
    return sampleTotal / sampleCount;
  }

  public List<CompleteEvent> getEvents() {
    return events;
  }

  public Duration getDuration() {
    return TimeUtil.getDurationBetween(end, start);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Bottleneck that = (Bottleneck) o;
    return Double.compare(that.sampleTotal, sampleTotal) == 0
        && sampleCount == that.sampleCount
        && Objects.equals(start, that.start)
        && Objects.equals(end, that.end)
        && events.equals(that.events);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end, sampleTotal, sampleCount, events);
  }

  public static class Builder {
    private Timestamp start;
    private Timestamp end;
    private double sampleTotal;
    private int sampleCount;
    private final List<CompleteEvent> events = new ArrayList<>();

    private Builder(Timestamp start) {
      this.start = start;
      this.end = start;
    }

    private Builder(Bottleneck bottleneck) {
      this.start = bottleneck.start;
      this.end = bottleneck.end;
      this.sampleTotal = bottleneck.sampleTotal;
      this.sampleCount = bottleneck.sampleCount;
      this.events.addAll(bottleneck.events);
    }

    public Timestamp getStart() {
      return start;
    }

    public Builder setStart(Timestamp start) {
      Preconditions.checkNotNull(start);
      this.start = start;
      return this;
    }

    public Timestamp getEnd() {
      return end;
    }

    public Builder setEnd(Timestamp end) {
      Preconditions.checkNotNull(end);
      this.end = end;
      return this;
    }

    public Builder addActionCountSample(double actionCount) {
      sampleCount++;
      sampleTotal += actionCount;
      return this;
    }

    public Builder addEvent(CompleteEvent event) {
      events.add(event);
      return this;
    }

    public Bottleneck build() {
      return new Bottleneck(this.start, this.end, sampleTotal, sampleCount, events);
    }
  }
}
