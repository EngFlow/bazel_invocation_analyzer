package com.engflow.bazel.invocation.analyzer.dataproviders;

import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_EXECUTION_UPLOAD_TIME;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD_OUTPUTS;

import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions.LocalAction;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link DataProvider} that supplies data on remote caching. Note that `disk_cache` is also a
 * remote cache, even though it interacts with the local disk.
 */
public class RemoteCacheMetricsDataProvider extends DataProvider {

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            RemoteCacheMetrics.class, DatumSupplier.memoized(this::derive)));
  }

  @VisibleForTesting
  RemoteCacheMetrics derive()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    var metrics =
        getDataManager().getDatum(LocalActions.class).stream()
            .filter(action -> action.hasRemoteCacheCheck())
            .parallel()
            .map(this::coalesce)
            .collect(Collectors.toList());
    var summary = metrics.stream().reduce(RemoteCacheData.EMPTY, RemoteCacheData::plus);
    return new RemoteCacheMetrics(
        metrics.size(), summary.cacheMisses, summary.check, summary.download, summary.upload);
  }

  RemoteCacheData coalesce(LocalAction action) {
    return action.getRelatedEvents().stream()
        .reduce(RemoteCacheData.EMPTY, RemoteCacheData::plus, RemoteCacheData::plus)
        .calculateCacheState();
  }

  private static class RemoteCacheData {
    private static final RemoteCacheData EMPTY =
        new RemoteCacheData(Duration.ZERO, Duration.ZERO, Duration.ZERO, 0) {
          @Override
          RemoteCacheData plus(RemoteCacheData that) {
            return that;
          }
        };

    public final Duration check;
    public final Duration download;
    private final Duration upload;
    private final int cacheMisses;

    RemoteCacheData(Duration check, Duration download, Duration upload, int cacheMisses) {
      this.check = check;
      this.download = download;
      this.upload = upload;
      this.cacheMisses = cacheMisses;
    }

    RemoteCacheData plus(RemoteCacheData other) {
      return new RemoteCacheData(
          check.plus(other.check),
          download.plus(other.download),
          upload.plus(other.upload),
          cacheMisses + other.cacheMisses);
    }

    RemoteCacheData calculateCacheState() {
      // The action was checked against remote, and nothing was downloaded.
      // This means it was not cached remotely.
      if (!check.isZero() && download.isZero()) {
        return new RemoteCacheData(check, download, upload, 1);
      }
      return this;
    }

    RemoteCacheData plus(CompleteEvent event) {
      // The event documents a remote cache check.
      if (CAT_REMOTE_ACTION_CACHE_CHECK.equals(event.category)) {
        return new RemoteCacheData(check.plus(event.duration), download, upload, cacheMisses);
      }
      // The event documents downloading outputs from a remote cache.
      if (CAT_REMOTE_OUTPUT_DOWNLOAD.equals(event.category)) {
        return new RemoteCacheData(check, download.plus(event.duration), upload, cacheMisses);
      }
      // The event documents uploading outputs to a remote cache.
      // See
      // https://github.com/bazelbuild/bazel/blob/7d10999fc0357596824f2b6022bbbd895f245a3c/src/main/java/com/google/devtools/build/lib/remote/RemoteExecutionService.java#L1417
      // and
      // https://github.com/bazelbuild/bazel/blob/7d10999fc0357596824f2b6022bbbd895f245a3c/src/main/java/com/google/devtools/build/lib/remote/RemoteSpawnRunner.java#L359
      if (CAT_REMOTE_EXECUTION_UPLOAD_TIME.equals(event.category)
          && (COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD_OUTPUTS.equals(event.name)
              || COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD.equals(event.name))) {
        return new RemoteCacheData(check, download, upload.plus(event.duration), cacheMisses);
      }
      return this;
    }
  }
}
