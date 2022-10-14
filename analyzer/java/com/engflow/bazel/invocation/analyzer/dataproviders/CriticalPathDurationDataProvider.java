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

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
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
 * A {@link DataProvider} that supplies the total duration of the critical path. For this, the sum
 * of the durations of all actions that are part of the critical path is calculated.
 */
public class CriticalPathDurationDataProvider extends DataProvider {
  public static final String EMPTY_REASON =
      "The Bazel profile does not include a critical path, which is required for determining its"
          + " duration. Try analyzing a profile that processes actions, for example a build or"
          + " test.";

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            CriticalPathDuration.class, DatumSupplier.memoized(this::getCriticalPathDuration)));
  }

  @VisibleForTesting
  CriticalPathDuration getCriticalPathDuration()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
    if (bazelProfile.getCriticalPath().isEmpty()) {
      return new CriticalPathDuration(EMPTY_REASON);
    }
    Duration duration =
        bazelProfile.getCriticalPath().get().getCompleteEvents().stream()
            .map((event) -> event.duration)
            .reduce(Duration.ZERO, Duration::plus);
    return new CriticalPathDuration(duration);
  }
}
