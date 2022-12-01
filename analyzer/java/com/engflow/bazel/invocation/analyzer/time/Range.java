package com.engflow.bazel.invocation.analyzer.time;

public class Range {

  public static Range between(Timestamp start, Timestamp end) {
    return new Range(start, end);
  }

  private final Timestamp start;
  private final Timestamp end;

  private Range(Timestamp start, Timestamp end) {
    this.start = start;
    this.end = end;
  }

  public boolean contains(Timestamp... values) {
    for (Timestamp value : values) {
      if (start.plus(Timestamp.ACCEPTABLE_DIVERGENCE.negated()).compareTo(value) > 0 || end.plus(Timestamp.ACCEPTABLE_DIVERGENCE).compareTo(value) < 0) {
        return false;
      }
    }
    return true;
  }
}
