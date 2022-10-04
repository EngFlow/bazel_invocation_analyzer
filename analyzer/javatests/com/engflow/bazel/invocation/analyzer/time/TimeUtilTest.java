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

public class TimeUtilTest {
  @Test
  public void getDurationForMicrosAndGetMicrosShouldMatch() {
    long durationInMicros = 123_456;
    assertThat(TimeUtil.getMicros(TimeUtil.getDurationForMicros(durationInMicros)))
        .isEqualTo(durationInMicros);
  }

  @Test
  public void getDurationForMicrosShouldNotLoseAccuracy() {
    Duration duration = TimeUtil.getDurationForMicros(1);
    assertThat(TimeUtil.getMicros(duration))
        .isEqualTo(TimeUnit.NANOSECONDS.toMicros(duration.toNanos()));
  }

  @Test
  public void getDurationBetweenShouldNotLoseAccuracy() {
    long first = 123_456;
    long second = 789_012;
    Timestamp firstTimestamp = Timestamp.ofMicros(first);
    Timestamp secondTimestamp = Timestamp.ofMicros(second);
    assertThat(TimeUtil.getDurationBetween(firstTimestamp, secondTimestamp))
        .isEqualTo(TimeUtil.getDurationForMicros(second - first));
  }

  @Test
  public void getDurationBetweenShouldBeCommutative() {
    long first = 123_456;
    long second = 789_012;
    Timestamp firstTimestamp = Timestamp.ofMicros(first);
    Timestamp secondTimestamp = Timestamp.ofMicros(second);
    assertThat(TimeUtil.getDurationBetween(firstTimestamp, secondTimestamp))
        .isEqualTo(TimeUtil.getDurationBetween(secondTimestamp, firstTimestamp));
  }
}
