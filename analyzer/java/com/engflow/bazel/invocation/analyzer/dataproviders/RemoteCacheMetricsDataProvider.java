package com.engflow.bazel.invocation.analyzer.dataproviders;

import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_EXECUTION_UPLOAD_TIME;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;

import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions.LocalAction;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
        getDataManager().getDatum(LocalActions.class).parallelStream()
            .map(this::coalesce)
            .collect(Collectors.toList());

    return new RemoteCacheMetrics(
        metrics.parallelStream().map(d -> d.check).reduce(Duration::plus).orElse(Duration.ZERO),
        metrics.parallelStream().map(d -> d.download).reduce(Duration::plus).orElse(Duration.ZERO),
        metrics.parallelStream().map(d -> d.upload).reduce(Duration::plus).orElse(Duration.ZERO),
        (metrics.parallelStream()
                    .map(d -> d.check.isZero() || d.download.isZero() ? 0 : 1f)
                    .reduce(0f, Float::sum)
                / metrics.size())
            * 100);
  }

  RemoteCacheData coalesce(LocalAction action) {
    return new RemoteCacheData(
        action.action.name,
        action.action.args.get("mnemonic"),
        sumDuration(action.relatedEvents, CAT_REMOTE_ACTION_CACHE_CHECK),
        sumDuration(action.relatedEvents, CAT_REMOTE_OUTPUT_DOWNLOAD),
        sumDuration(action.relatedEvents, CAT_REMOTE_EXECUTION_UPLOAD_TIME));
  }

  private static Duration sumDuration(Collection<CompleteEvent> events, String category) {
    return events.stream()
        .filter(e -> category.equals(e.category))
        .map(e -> e.duration)
        .findFirst()
        .orElse(Duration.ZERO);
  }

  static class RemoteCacheData {
    public final String name;
    public final String mnemonic;
    public final Duration check;
    public final Duration download;
    private final Duration upload;

    RemoteCacheData(
        String name, String mnemonic, Duration check, Duration download, Duration upload) {
      this.name = name;
      this.mnemonic = mnemonic;
      this.check = check;
      this.download = download;
      this.upload = upload;
    }

    @Override
    public String toString() {
      return "RemoteCacheData{"
          + "name='"
          + name
          + '\''
          + ", mnemonic='"
          + mnemonic
          + '\''
          + ", check="
          + check
          + ", download="
          + download
          + ", upload="
          + upload
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
      RemoteCacheData that = (RemoteCacheData) o;
      return Objects.equal(name, that.name)
          && Objects.equal(mnemonic, that.mnemonic)
          && Objects.equal(check, that.check)
          && Objects.equal(download, that.download)
          && Objects.equal(upload, that.upload);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(name, mnemonic, check, download, upload);
    }
  }
}
