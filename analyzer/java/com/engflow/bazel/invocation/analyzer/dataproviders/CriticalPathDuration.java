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
import java.time.Duration;

/** The duration of the critical path */
public class CriticalPathDuration implements Datum {
  private final Duration criticalPathDuration;

  public CriticalPathDuration(Duration criticalPathDuration) {
    this.criticalPathDuration = criticalPathDuration;
  }

  public Duration getCriticalPathDuration() {
    return criticalPathDuration;
  }

  @Override
  public String getDescription() {
    return "The duration of the Bazel profile's critical path.";
  }

  @Override
  public String getSummary() {
    return DurationUtil.formatDuration(criticalPathDuration);
  }
}
