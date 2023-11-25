package com.engflow.bazel.invocation.analyzer.dataproviders;

import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.time.Duration;

/**
 * Metrics on remote caching, namely how much time was spent on cache checks, downloading outputs,
 * uploading outputs, and what percentage of actions were cached remotely.
 */
public class RemoteCacheMetrics implements Datum {

  private final int cacheChecks;
  private final int cacheMisses;
  private final Duration cacheCheckDuration;
  private final Duration downloadOutputsDuration;
  private final Duration uploadOutputsDuration;

  private final float percentCachedRemotely;

  RemoteCacheMetrics() {
    this(0, 0, Duration.ZERO, Duration.ZERO, Duration.ZERO);
  }

  RemoteCacheMetrics(
      int cacheChecks,
      int cacheMisses,
      Duration totalCacheCheckDuration,
      Duration downloadOutputsDuration,
      Duration totalUploadOutputs) {
    this.cacheChecks = cacheChecks;
    this.cacheMisses = cacheMisses;
    this.percentCachedRemotely = 100f * (cacheChecks - cacheMisses) / cacheChecks;
    this.cacheCheckDuration = Preconditions.checkNotNull(totalCacheCheckDuration);
    this.downloadOutputsDuration = Preconditions.checkNotNull(downloadOutputsDuration);
    this.uploadOutputsDuration = Preconditions.checkNotNull(totalUploadOutputs);
  }

  @Override
  public boolean isEmpty() {
    return cacheCheckDuration.isZero()
        && downloadOutputsDuration.isZero()
        && uploadOutputsDuration.isZero();
  }

  @Override
  public String getEmptyReason() {
    return isEmpty() ? "No remote cache operations available." : null;
  }

  @Override
  public String getDescription() {
    return "Collection of remote cache related metrics.";
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
    return that.cacheChecks == cacheChecks
        && that.cacheMisses == cacheMisses
        && Float.compare(that.percentCachedRemotely, percentCachedRemotely) == 0
        && Objects.equal(cacheCheckDuration, that.cacheCheckDuration)
        && Objects.equal(downloadOutputsDuration, that.downloadOutputsDuration)
        && Objects.equal(uploadOutputsDuration, that.uploadOutputsDuration);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        cacheChecks,
        cacheMisses,
        percentCachedRemotely,
        cacheCheckDuration,
        downloadOutputsDuration,
        uploadOutputsDuration);
  }

  @Override
  public String getSummary() {
    String formattedPercentage = String.format("%,.2f%%", percentCachedRemotely);
    var width = Math.max(formattedPercentage.length(), String.valueOf(cacheChecks).length());
    return String.format(
        "Number of cache checks:             %s\n"
            + "Number of cache misses:             %s\n"
            + "Cache hit percentage:               %s\n"
            + "Time spend checking for cache hits: %s\n"
            + "Time spend downloading outputs:     %s\n"
            + "Time spend uploading outputs:       %s",
        Strings.padStart(String.valueOf(cacheChecks), width, ' '),
        Strings.padStart(String.valueOf(cacheMisses), width, ' '),
        Strings.padStart(formattedPercentage, width, ' '),
        DurationUtil.formatDuration(cacheCheckDuration),
        DurationUtil.formatDuration(downloadOutputsDuration),
        DurationUtil.formatDuration(uploadOutputsDuration));
  }
}
