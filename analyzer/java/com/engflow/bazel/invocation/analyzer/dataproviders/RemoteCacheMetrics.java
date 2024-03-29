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
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.time.Duration;

/**
 * Metrics on the time spent interacting with a remote cache, namely time spent on cache checks,
 * downloading outputs, and uploading outputs.
 *
 * <p>Note that events in Bazel profiles do not differentiate between a remote cache configured via
 * {@code --disk_cache} or {@code --remote_cache}.
 */
public class RemoteCacheMetrics implements Datum {

  private final Duration cacheCheckDuration;
  private final Duration downloadOutputsDuration;
  private final Duration uploadOutputsDuration;

  RemoteCacheMetrics(
      Duration totalCacheCheckDuration,
      Duration downloadOutputsDuration,
      Duration uploadOutputsDuration) {
    this.cacheCheckDuration = Preconditions.checkNotNull(totalCacheCheckDuration);
    this.downloadOutputsDuration = Preconditions.checkNotNull(downloadOutputsDuration);
    this.uploadOutputsDuration = Preconditions.checkNotNull(uploadOutputsDuration);
  }

  public Duration getCacheCheckDuration() {
    return cacheCheckDuration;
  }

  public Duration getDownloadOutputsDuration() {
    return downloadOutputsDuration;
  }

  public Duration getUploadOutputsDuration() {
    return uploadOutputsDuration;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public String getEmptyReason() {
    return isEmpty() ? "Could not find any remote cache checks." : null;
  }

  @Override
  public String getDescription() {
    return "Metrics on the time spent interacting with a remote cache configured with"
        + " `--remote_cache` or `--disk_cache`.";
  }

  @Override
  public String toString() {
    return getSummary();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RemoteCacheMetrics that = (RemoteCacheMetrics) o;
    return Objects.equal(cacheCheckDuration, that.cacheCheckDuration)
        && Objects.equal(downloadOutputsDuration, that.downloadOutputsDuration)
        && Objects.equal(uploadOutputsDuration, that.uploadOutputsDuration);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(cacheCheckDuration, downloadOutputsDuration, uploadOutputsDuration);
  }

  @Override
  public String getSummary() {
    return String.format(
        "Remote cache checks:     %s\n"
            + "Downloading outputs:     %s\n"
            + "Uploading outputs:       %s",
        DurationUtil.formatDuration(cacheCheckDuration),
        DurationUtil.formatDuration(downloadOutputsDuration),
        DurationUtil.formatDuration(uploadOutputsDuration));
  }
}
