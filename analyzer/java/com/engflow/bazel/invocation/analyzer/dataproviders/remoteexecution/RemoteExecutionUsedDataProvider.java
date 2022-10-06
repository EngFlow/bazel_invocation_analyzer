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
import com.google.common.annotations.VisibleForTesting;
import java.util.List;

public class RemoteExecutionUsedDataProvider extends DataProvider {

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            RemoteExecutionUsed.class, DatumSupplier.memoized(this::getRemoteExecutionUsed)));
  }

  /**
   * Extracts from the Bazel profile whether it is likely that remote execution was enabled when
   * running the invocation.
   */
  @VisibleForTesting
  RemoteExecutionUsed getRemoteExecutionUsed()
      throws MissingInputException, InvalidProfileException {
    BazelProfile profile = getDataManager().getDatum(BazelProfile.class);
    return new RemoteExecutionUsed(
        profile
            .getThreads()
            .flatMap((thread) -> thread.getCompleteEvents().stream())
            .anyMatch(
                (event) ->
                    BazelProfileConstants.CAT_REMOTE_EXECUTION_SETUP.equals(event.category)
                        || BazelProfileConstants.CAT_REMOTE_EXECUTION_PROCESS_WALL_TIME.equals(
                            event.category)
                        || BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME.equals(
                            event.category)));
  }
}
