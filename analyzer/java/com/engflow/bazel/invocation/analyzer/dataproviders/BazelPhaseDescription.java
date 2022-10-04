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

package com.engflow.bazel.invocation.analyzer.dataproviders;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.Objects;

/** Metadata about a {@link BazelProfilePhase} */
public class BazelPhaseDescription {
  private final Timestamp start;
  private final Duration duration;

  public BazelPhaseDescription(Timestamp start, Timestamp end) {
    this(start, TimeUtil.getDurationBetween(start, end));
  }

  @VisibleForTesting
  BazelPhaseDescription(Timestamp start, Duration duration) {
    this.start = start;
    this.duration = duration;
  }

  public Duration getDuration() {
    return duration;
  }

  public Timestamp getEnd() {
    return start.plus(duration);
  }

  public Timestamp getStart() {
    return start;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BazelPhaseDescription that = (BazelPhaseDescription) o;
    return start.equals(that.start) && duration.equals(that.duration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, duration);
  }
}
