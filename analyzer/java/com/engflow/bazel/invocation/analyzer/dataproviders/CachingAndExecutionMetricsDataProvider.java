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

import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link DataProvider} that supplies data on caching and execution location. Note that
 * `disk_cache` is also a remote cache, even though it interacts with the local disk.
 */
public class CachingAndExecutionMetricsDataProvider extends DataProvider {

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            CachingAndExecutionMetrics.class, DatumSupplier.memoized(this::getMetrics)));
  }

  @VisibleForTesting
  CachingAndExecutionMetrics getMetrics()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    var localActions = getDataManager().getDatum(LocalActions.class);

    long actions = localActions.size();
    AtomicLong cacheHit = new AtomicLong();
    AtomicLong cacheCheck = new AtomicLong();
    AtomicLong cacheMissLocalExec = new AtomicLong();
    AtomicLong cacheMissRemoteExec = new AtomicLong();
    AtomicLong cacheMissExecNotReported = new AtomicLong();
    AtomicLong nocacheLocalExec = new AtomicLong();
    AtomicLong nocacheRemoteExec = new AtomicLong();
    AtomicLong nocacheExecNotReported = new AtomicLong();
    localActions.parallelStream()
        .forEach(
            action -> {
              var localExec = action.isExecutedLocally();
              var remoteExec = action.isExecutedRemotely();
              if (action.isRemoteCacheHit()) {
                cacheCheck.getAndIncrement();
                cacheHit.getAndIncrement();
              } else if (action.hasRemoteCacheCheck()) {
                cacheCheck.getAndIncrement();
                if (localExec) {
                  cacheMissLocalExec.getAndIncrement();
                } else if (remoteExec) {
                  cacheMissRemoteExec.getAndIncrement();
                } else {
                  cacheMissExecNotReported.getAndIncrement();
                }
              } else {
                if (localExec) {
                  nocacheLocalExec.getAndIncrement();
                } else if (remoteExec) {
                  nocacheRemoteExec.getAndIncrement();
                } else {
                  // TODO: Can we separate out Bazel "internal" actions?
                  nocacheExecNotReported.getAndIncrement();
                }
              }
            });
    return new CachingAndExecutionMetrics(
        actions,
        cacheHit.get(),
        cacheMissLocalExec.get(),
        cacheMissRemoteExec.get(),
        cacheMissExecNotReported.get(),
        nocacheLocalExec.get(),
        nocacheRemoteExec.get(),
        nocacheExecNotReported.get());
  }
}
