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
import java.time.Duration;

/** Total time actions spent queued */
public class TotalQueuingDuration implements Datum {
  private final Duration totalQueuingDuration;

  public TotalQueuingDuration(Duration totalQueuingDuration) {
    Preconditions.checkNotNull(totalQueuingDuration);
    this.totalQueuingDuration = totalQueuingDuration;
  }

  public Duration getTotalQueuingDuration() {
    return totalQueuingDuration;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public String getEmptyReason() {
    return null;
  }

  @Override
  public String getDescription() {
    return "The total time that was spent on queuing as extracted from the Bazel profile.";
  }

  @Override
  public String getSummary() {
    return DurationUtil.formatDuration(totalQueuingDuration);
  }
}
