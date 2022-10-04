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

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class TimestampTest {
  @Test
  public void plusShouldAddDurationCorrectly() {
    long timestampMicros = 100_000;
    long durationMicros = 23_456;
    Timestamp timestamp = Timestamp.ofMicros(timestampMicros);
    Duration duration = Duration.ofNanos(TimeUnit.MICROSECONDS.toNanos(durationMicros));
    assertThat(timestamp.plus(duration))
        .isEqualTo(Timestamp.ofMicros(timestampMicros + durationMicros));
  }

  @Test
  public void plusShouldBeCommutative() {
    long a = 12;
    long b = 34;
    assertThat(Timestamp.ofSeconds(a).plus(Duration.ofSeconds(b)))
        .isEqualTo(Timestamp.ofSeconds(b).plus(Duration.ofSeconds(a)));
  }

  @Test
  public void ofMicrosAndOfSecondsShouldEqual() {
    int seconds = 12;
    Timestamp ofSeconds = Timestamp.ofSeconds(seconds);
    Timestamp ofMicros = Timestamp.ofMicros(TimeUnit.SECONDS.toMicros(seconds));
    assertThat(ofSeconds).isEqualTo(ofMicros);
  }

  @Test
  public void almostEqualsShouldReturnTrueOnSameTimestamps() {
    Timestamp timestamp = Timestamp.ofMicros(123_456);
    assertThat(timestamp.almostEquals(timestamp)).isTrue();
  }

  @Test
  public void almostEqualsShouldReturnTrueOnVeryCloseTimestamps() {
    Timestamp timestamp = Timestamp.ofMicros(123_456);
    Duration acceptableDifference = Timestamp.ACCEPTABLE_DIVERGENCE.minus(Duration.ofNanos(1));
    assertThat(timestamp.almostEquals(timestamp.plus(acceptableDifference))).isTrue();
  }

  @Test
  public void almostEqualsShouldReturnFalseOnDifferingTimestamps() {
    Timestamp timestamp = Timestamp.ofMicros(123_456);
    assertThat(timestamp.almostEquals(timestamp.plus(Timestamp.ACCEPTABLE_DIVERGENCE))).isFalse();
  }
}
