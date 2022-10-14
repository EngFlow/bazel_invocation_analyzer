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

import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.time.Duration;
import java.util.Optional;

/** The total duration of the invocation */
public class TotalDuration implements Datum {
  private final Optional<Duration> totalDuration;
  private final String emptyReason;

  public TotalDuration(String emptyReason) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(emptyReason));
    this.totalDuration = Optional.empty();
    this.emptyReason = emptyReason;
  }

  public TotalDuration(Duration totalDuration) {
    Preconditions.checkNotNull(totalDuration);
    this.totalDuration = Optional.of(totalDuration);
    this.emptyReason = null;
  }

  public Optional<Duration> getTotalDuration() {
    return totalDuration;
  }

  @Override
  public boolean isEmpty() {
    return totalDuration.isEmpty();
  }

  @Override
  public String getEmptyReason() {
    return emptyReason;
  }

  @Override
  public String getDescription() {
    return "The duration of the invocation as extracted from the Bazel profile.";
  }

  @Override
  public String getSummary() {
    return isEmpty() ? null : DurationUtil.formatDuration(totalDuration.get());
  }
}
