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

package com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution;

import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nullable;

/** The total time spent queued on the critical path */
public class CriticalPathQueuingDuration implements Datum {
  private final Optional<Duration> criticalPathQueuingDuration;
  @Nullable private final String emptyReason;

  public CriticalPathQueuingDuration(String emptyReason) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(emptyReason));
    this.criticalPathQueuingDuration = Optional.empty();
    this.emptyReason = emptyReason;
  }

  public CriticalPathQueuingDuration(Duration criticalPathQueuingDuration) {
    Preconditions.checkNotNull(criticalPathQueuingDuration);
    this.criticalPathQueuingDuration = Optional.of(criticalPathQueuingDuration);
    this.emptyReason = null;
  }

  public Optional<Duration> getCriticalPathQueuingDuration() {
    return criticalPathQueuingDuration;
  }

  @Override
  public boolean isEmpty() {
    return criticalPathQueuingDuration.isEmpty();
  }

  @Override
  public String getEmptyReason() {
    return emptyReason;
  }

  @Override
  public String getDescription() {
    return "The duration of queuing within the Bazel profile's critical path.";
  }

  @Override
  public String getSummary() {
    return isEmpty() ? null : DurationUtil.formatDuration(criticalPathQueuingDuration.get());
  }
}
