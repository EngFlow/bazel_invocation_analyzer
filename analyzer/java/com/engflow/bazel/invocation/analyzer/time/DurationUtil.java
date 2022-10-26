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

public class DurationUtil {
  /**
   * Returns the duration in a formatted form.
   *
   * <p>For duration >= 1 hour, return 1h 2m 3s.<br>
   * For 1 hour > duration >= 1 minute, return 1m 2s.<br>
   * For 1 minute > duration >= 10 seconds, return 1s.<br>
   * Else return the duration in milliseconds as 1ms.
   *
   * @param duration The duration to format
   * @return a human-readable formatting of the duration
   */
  public static String formatDuration(Duration duration) {
    if (duration.compareTo(Duration.ofHours(1)) >= 0) {
      return String.format(
          "%dh %dm %ds", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    } else if (duration.compareTo(Duration.ofMinutes(1)) >= 0) {
      return String.format("%dm %ds", duration.toMinutes(), duration.toSecondsPart());
    } else if (duration.compareTo(Duration.ofSeconds(10)) >= 0) {
      return String.format("%ds", duration.toSeconds());
    } else {
      return String.format("%dms", duration.toMillis());
    }
  }

  /**
   * Returns the percentage of one duration relative to another duration.
   *
   * @param partial The duration to return the percentage of related to base
   * @param base The duration to base the percentage on
   * @return the percentage of partial relative to base
   */
  public static double getPercentageOf(Duration partial, Duration base) {
    if (base.isZero()) {
      throw new IllegalArgumentException("Duration base must not be zero.");
    }
    return 100.0 * partial.toNanos() / base.toNanos();
  }
}
