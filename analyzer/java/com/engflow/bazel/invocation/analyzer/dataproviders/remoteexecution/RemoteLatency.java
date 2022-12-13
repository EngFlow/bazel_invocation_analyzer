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

/** Estimated upper bound of latency for remote operations */
public class RemoteLatency implements Datum {
  private final Optional<Duration> remoteLatency;
  @Nullable private final String emptyReason;

  public RemoteLatency(String emptyReason) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(emptyReason));
    this.remoteLatency = Optional.empty();
    this.emptyReason = emptyReason;
  }

  public RemoteLatency(Duration remoteLatency) {
    Preconditions.checkNotNull(remoteLatency);
    this.remoteLatency = Optional.of(remoteLatency);
    this.emptyReason = null;
  }

  public Optional<Duration> getRemoteLatency() {
    return remoteLatency;
  }

  @Override
  public boolean isEmpty() {
    return remoteLatency.isEmpty();
  }

  @Override
  public String getEmptyReason() {
    return emptyReason;
  }

  @Override
  public String getDescription() {
    return "Upper bound estimate of latency to remote execution/caching system";
  }

  @Override
  public String getSummary() {
    return isEmpty() ? null : DurationUtil.formatDuration(remoteLatency.get());
  }
}
