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
import java.time.Duration;
import java.util.List;

/**
 * A {@link DataProvider} that supplies the duration spent queuing for remote execution. For this,
 * the sum over all queuing across all actions is computed. No distinction is made on whether the
 * queuing was happening in parallel.
 */
public class TotalQueuingDurationDataProvider extends DataProvider {

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            TotalQueuingDuration.class, DatumSupplier.memoized(this::getTotalQueuingDuration)));
  }

  @VisibleForTesting
  TotalQueuingDuration getTotalQueuingDuration()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
    Duration duration =
        bazelProfile
            .getThreads()
            .flatMap((thread) -> thread.getCompleteEvents().stream())
            .filter(
                (event) ->
                    BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME.equals(event.category))
            .map((event) -> event.duration)
            .reduce(Duration.ZERO, Duration::plus);
    return new TotalQueuingDuration(duration);
  }
}
