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

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelEventsUtil;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.List;

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
            .flatMap(action -> action.getRelatedEvents().stream())
            .parallel()
            .reduce(RemoteCacheData.EMPTY, RemoteCacheData::plus, RemoteCacheData::plus);
    return new RemoteCacheMetrics(metrics.check, metrics.download, metrics.upload);
  }

  private static class RemoteCacheData {
    private static final RemoteCacheData EMPTY =
        new RemoteCacheData(Duration.ZERO, Duration.ZERO, Duration.ZERO) {
          @Override
          RemoteCacheData plus(RemoteCacheData that) {
            return that;
          }
        };

    public final Duration check;
    public final Duration download;
    private final Duration upload;

    RemoteCacheData(Duration check, Duration download, Duration upload) {
      this.check = check;
      this.download = download;
      this.upload = upload;
    }

    RemoteCacheData plus(RemoteCacheData other) {
      return new RemoteCacheData(
          check.plus(other.check), download.plus(other.download), upload.plus(other.upload));
    }

    RemoteCacheData plus(CompleteEvent event) {
      if (BazelEventsUtil.indicatesRemoteCacheCheck(event)) {
        return new RemoteCacheData(check.plus(event.duration), download, upload);
      }
      if (BazelEventsUtil.indicatesRemoteDownloadOutputs(event)) {
        return new RemoteCacheData(check, download.plus(event.duration), upload);
      }
      if (BazelEventsUtil.indicatesRemoteUploadOutputs(event)) {
        return new RemoteCacheData(check, download, upload.plus(event.duration));
      }
      return this;
    }
  }
}
