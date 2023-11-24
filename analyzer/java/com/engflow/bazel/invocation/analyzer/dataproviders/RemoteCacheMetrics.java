package com.engflow.bazel.invocation.analyzer.dataproviders;

import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.time.Duration;

/**
 * Metrics on remote caching, namely how much time was spent on cache checks, downloading outputs,
 * uploading outputs, and what percentage of actions were cached remotely.
 */
public class RemoteCacheMetrics implements Datum {

  private final Duration totalCacheCheck;
  private final Duration totalDownloadOutputs;
  private final Duration totalUploadOutputs;

  private final float percentCachedRemotely;

  RemoteCacheMetrics() {
    this(Duration.ZERO, Duration.ZERO, Duration.ZERO, 0f);
  }

  RemoteCacheMetrics(
      Duration totalCacheCheck,
      Duration totalDownloadOutputs,
      Duration totalUploadOutputs,
      float percentCachedRemotely) {
    this.totalCacheCheck = Preconditions.checkNotNull(totalCacheCheck);
    this.totalDownloadOutputs = Preconditions.checkNotNull(totalDownloadOutputs);
    this.totalUploadOutputs = Preconditions.checkNotNull(totalUploadOutputs);
    this.percentCachedRemotely = percentCachedRemotely;
  }

  @Override
  public boolean isEmpty() {
    return totalCacheCheck.isZero() && totalDownloadOutputs.isZero() && totalUploadOutputs.isZero();
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
    return "RemoteCacheMetrics{"
        + "totalCacheCheck="
        + totalCacheCheck
        + ", totalDownloadOutputs="
        + totalDownloadOutputs
        + ", totalUploadOutputs="
        + totalUploadOutputs
        + ", percentCached="
        + percentCachedRemotely
        + '}';
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
    return Float.compare(that.percentCachedRemotely, percentCachedRemotely) == 0
        && Objects.equal(totalCacheCheck, that.totalCacheCheck)
        && Objects.equal(totalDownloadOutputs, that.totalDownloadOutputs)
        && Objects.equal(totalUploadOutputs, that.totalUploadOutputs);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        totalCacheCheck, totalDownloadOutputs, totalUploadOutputs, percentCachedRemotely);
  }

  @Override
  public String getSummary() {
    return String.format(
        "Total Remote Cache Check Duration: %s\n"
            + "Total Remote Download Outputs:     %s\n"
            + "Total Remote Upload Outputs:       %s\n"
            + "Percent cached remotely:           %,.2f%%",
        DurationUtil.formatDuration(totalCacheCheck),
        DurationUtil.formatDuration(totalDownloadOutputs),
        DurationUtil.formatDuration(totalUploadOutputs),
        percentCachedRemotely);
  }
}
