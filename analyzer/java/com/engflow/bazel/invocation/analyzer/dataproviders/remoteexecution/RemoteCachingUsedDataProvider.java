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

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;

/**
 * A {@link DataProvider} that extracts from the Bazel profile whether it is likely that remote
 * caching was enabled when running the invocation.
 */
public class RemoteCachingUsedDataProvider extends DataProvider {

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            RemoteCachingUsed.class, DatumSupplier.memoized(this::getRemoteCachingUsed)));
  }

  @VisibleForTesting
  RemoteCachingUsed getRemoteCachingUsed()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    RemoteExecutionUsed remoteExecutionUsed = getDataManager().getDatum(RemoteExecutionUsed.class);
    if (remoteExecutionUsed.isRemoteExecutionUsed()) {
      // While in principle it is possible to use remote execution without remote caching, this is
      // a highly unusual setup. Return `true` here to avoid false negatives.
      // For example, if RE is enabled and only targets with tag `no-remote-cache` or `no-cache`
      // were built, the code farther down may not correctly detect that RC was used.
      return new RemoteCachingUsed(true);
    }
    BazelProfile profile = getDataManager().getDatum(BazelProfile.class);
    return new RemoteCachingUsed(
        profile
            .getThreads()
            .flatMap((thread) -> thread.getCompleteEvents().stream())
            .anyMatch(
                (event) ->
                    BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK.equals(event.category)));
  }
}
