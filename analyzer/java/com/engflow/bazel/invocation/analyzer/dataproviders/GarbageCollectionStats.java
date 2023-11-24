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
import javax.annotation.Nullable;

/**
 * Data on the garbage collection performed during the invocation. Major garbage collection suspends
 * all other threads, whereas minor garbage collection does not, or is sufficiently short to be
 * negligible. In that, major garbage collection occurrences have a much more significant impact on
 * an invocation's performance.
 */
public class GarbageCollectionStats implements Datum {
  private final Optional<Duration> majorGarbageCollectionDuration;
  @Nullable private final String emptyReason;

  public GarbageCollectionStats(Duration majorGarbageCollectionDuration) {
    this.majorGarbageCollectionDuration = Optional.of(majorGarbageCollectionDuration);
    this.emptyReason = null;
  }

  public GarbageCollectionStats(String emptyReason) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(emptyReason));
    this.majorGarbageCollectionDuration = Optional.empty();
    this.emptyReason = emptyReason;
  }

  public boolean hasMajorGarbageCollection() {
    return !isEmpty() && !majorGarbageCollectionDuration.get().isZero();
  }

  public Optional<Duration> getMajorGarbageCollectionDuration() {
    return majorGarbageCollectionDuration;
  }

  @Override
  public boolean isEmpty() {
    return majorGarbageCollectionDuration.isEmpty();
  }

  @Override
  public String getEmptyReason() {
    return emptyReason;
  }

  @Override
  public String getDescription() {
    return "The total duration of major garbage collection as extracted from the Bazel profile.";
  }

  @Override
  public String getSummary() {
    if (isEmpty()) {
      return null;
    }
    return hasMajorGarbageCollection()
        ? String.format(
            "Major GC duration of %s",
            DurationUtil.formatDuration(majorGarbageCollectionDuration.get()))
        : "No major GC occurred";
  }
}
