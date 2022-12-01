package com.engflow.bazel.invocation.analyzer.dataproviders;

import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.google.common.base.Objects;
import java.time.Duration;

public class RemoteCacheMetrics implements Datum {

  private final Duration totalCacheCheck;
  private final Duration totalDownloadOutputs;
  private final Duration totalUploadOutputs;

  private final float percentCached;

  RemoteCacheMetrics(
      Duration totalCacheCheck,
      Duration totalDownloadOutputs,
      Duration totalUploadOutputs,
      float percentCached) {
    this.totalCacheCheck = totalCacheCheck;
    this.totalDownloadOutputs = totalDownloadOutputs;
    this.totalUploadOutputs = totalUploadOutputs;
    this.percentCached = percentCached;
  }

  @Override
  public boolean isEmpty() {
    return totalCacheCheck.isZero() && totalDownloadOutputs.isZero();
  }

  @Override
  public String getEmptyReason() {
    return "No Remote Cache checks or downloads";
  }

  @Override
  public String getDescription() {
    return "Remote cache usage.";
  }

  @Override
  public String toString() {
    return "RemoteCacheMetrics{" +
        "totalCacheCheck=" + totalCacheCheck +
        ", totalDownloadOutputs=" + totalDownloadOutputs +
        ", totalUploadOutputs=" + totalUploadOutputs +
        ", percentCached=" + percentCached +
        '}';
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
    return Float.compare(that.percentCached, percentCached) == 0
        && Objects.equal(totalCacheCheck, that.totalCacheCheck)
        && Objects.equal(totalDownloadOutputs, that.totalDownloadOutputs)
        && Objects.equal(totalUploadOutputs, that.totalUploadOutputs);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        totalCacheCheck,
        totalDownloadOutputs,
        totalUploadOutputs,
        percentCached);
  }

  @Override
  public String getSummary() {
    return String.format(
        "Total Remote Cache Check Duration: %s\n"
            + "Total Remote Download Outputs: %s\n"
            + "Total Remote Upload Outputs: %s\n"
            + "Percent cached: %s",
        totalCacheCheck, totalDownloadOutputs, totalUploadOutputs, percentCached);
  }
}
