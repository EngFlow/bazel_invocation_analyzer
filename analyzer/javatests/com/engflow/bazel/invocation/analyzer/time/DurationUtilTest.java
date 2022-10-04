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
import org.junit.Test;

public class DurationUtilTest {
  @Test
  public void formatHours() throws Exception {
    assertThat(DurationUtil.formatDuration(Duration.ofHours(1))).isEqualTo("1h 0m 0s");
    assertThat(DurationUtil.formatDuration(Duration.ofHours(1).plus(Duration.ofSeconds(2))))
        .isEqualTo("1h 0m 2s");
    assertThat(DurationUtil.formatDuration(Duration.ofHours(36))).isEqualTo("36h 0m 0s");
  }

  @Test
  public void formatMinutes() throws Exception {
    assertThat(DurationUtil.formatDuration(Duration.ofMinutes(1))).isEqualTo("1m 0s");
    assertThat(DurationUtil.formatDuration(Duration.ofMinutes(1).plus(Duration.ofSeconds(2))))
        .isEqualTo("1m 2s");
    assertThat(DurationUtil.formatDuration(Duration.ofMinutes(59))).isEqualTo("59m 0s");
  }

  @Test
  public void formatSeconds() throws Exception {
    assertThat(DurationUtil.formatDuration(Duration.ofSeconds(10).plus(Duration.ofMillis(10))))
        .isEqualTo("10s");
    assertThat(DurationUtil.formatDuration(Duration.ofSeconds(59))).isEqualTo("59s");
  }

  @Test
  public void formatMilliseconds() throws Exception {
    assertThat(DurationUtil.formatDuration(Duration.ofSeconds(1))).isEqualTo("1000ms");
    assertThat(DurationUtil.formatDuration(Duration.ofSeconds(9).plus(Duration.ofMillis(42))))
        .isEqualTo("9042ms");
  }
}
