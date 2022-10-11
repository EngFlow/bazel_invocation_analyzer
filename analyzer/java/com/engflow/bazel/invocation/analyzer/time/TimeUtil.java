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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class TimeUtil {
  /**
   * Given two {@link Timestamp}s, return the {@link Duration} that passed between them. This method
   * is commutative, i.e. the timestamps need not be passed in a specific order.
   *
   * @param t1 the first timestamp
   * @param t2 the second timestamp
   * @return the duration that passed between the two specified timestamps
   */
  public static Duration getDurationBetween(Timestamp t1, Timestamp t2) {
    return getDurationForMicros(Math.abs(t1.getMicros() - t2.getMicros()));
  }

  /**
   * Given a value in microseconds, return a {@link Duration} of that length.
   *
   * @param durationInMicros the number of microseconds in the duration to return
   * @return the duration that represents the specified microseconds
   */
  public static Duration getDurationForMicros(long durationInMicros) {
    // This should be equivalent to Duration.of(durationInMicros, ChronoUnit.MICROS).
    // The implementation below clarifies that combining getDurationForMicros and getMicros
    // leads to an identity function.
    return Duration.ofNanos(TimeUnit.MICROSECONDS.toNanos(durationInMicros));
  }

  /**
   * Given a {@link Duration}, return its value in microseconds.
   *
   * @param duration the Duration to convert
   * @return the number of microseconds that represent the specified duration
   */
  public static long getMicros(Duration duration) {
    return TimeUnit.NANOSECONDS.toMicros(duration.toNanos());
  }
}
