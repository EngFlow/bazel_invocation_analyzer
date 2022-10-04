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

package com.engflow.bazel.invocation.analyzer.time;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A timestamp, as presented in a Bazel profile.
 *
 * <p>Bazel profiles include timestamps, expressed in micros since the start of the init phase,
 * {@link BazelProfilePhase#INIT}. Profiles may include negative timestamps. These indicate events
 * that occurred before the init phase, i.e. in the launch phase, {@link BazelProfilePhase#LAUNCH}.
 * <br>
 * For this reason, a Bazel profile timestamp is only meaningful relative to other timestamps.
 */
public class Timestamp implements Comparable<Timestamp> {
  // When matching critical path events to events in the actual threads, timestamps and durations
  // do not match up entirely. Accept a difference of up to 1ms.
  @VisibleForTesting static final Duration ACCEPTABLE_DIVERGENCE = Duration.ofMillis(1);

  private final long timestampInMicros;

  private Timestamp(long timestampInMicros) {
    this.timestampInMicros = timestampInMicros;
  }

  /**
   * Construct a {@link Timestamp} for the provided microseconds.
   *
   * @param micros the number of microseconds to get a timestamp for
   * @return the timestamp that represents the specified microseconds
   */
  public static Timestamp ofMicros(long micros) {
    return new Timestamp(micros);
  }

  /**
   * Construct a {@link Timestamp} for the provided seconds.<br>
   * This should only be used for testing purposes.
   *
   * @param seconds the number of seconds to get a timestamp for
   * @return the timestamp that represents the specified seconds
   */
  public static Timestamp ofSeconds(long seconds) {
    return new Timestamp(TimeUnit.SECONDS.toMicros(seconds));
  }

  /**
   * @return the number of microseconds this timestamp represents
   */
  public long getMicros() {
    return timestampInMicros;
  }

  /**
   * Returns a copy of this timestamp with the specified duration added.
   *
   * @param duration the duration to add
   * @return a timestamp based on this timestamp with the specified duration added
   */
  public Timestamp plus(Duration duration) {
    long durationInMicros = TimeUnit.NANOSECONDS.toMicros(duration.toNanos());
    return new Timestamp(this.timestampInMicros + durationInMicros);
  }

  /**
   * Returns whether this timestamp is almost equal to the specified timestamp.<br>
   * This check may be necessary when matching critical path events to events in the actual threads,
   * as timestamps and durations do not match up entirely.
   *
   * @param o the timestamp to compare with
   * @return whether this timestamp and the specified one are less than 1ms apart
   */
  public boolean almostEquals(Timestamp o) {
    if (o == null) {
      return false;
    }
    return TimeUtil.getDurationBetween(this, o).compareTo(ACCEPTABLE_DIVERGENCE) < 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Timestamp that = (Timestamp) o;
    return timestampInMicros == that.timestampInMicros;
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestampInMicros);
  }

  @Override
  public int compareTo(Timestamp o) {
    // NullPointerException on null o desired.
    return Long.compare(this.timestampInMicros, o.timestampInMicros);
  }
}
