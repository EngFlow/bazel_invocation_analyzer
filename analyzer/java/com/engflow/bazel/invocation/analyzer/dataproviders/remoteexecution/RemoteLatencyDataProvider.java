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
import java.util.Optional;

/**
 * A {@link DataProvider} that supplies an upper bound for the latency between the Bazel client and
 * the remote execution/caching service. It does this estimation by taking half the minimum
 * round-trip-time across all "remote action cache check" and "remote action execution" actions.
 */
public class RemoteLatencyDataProvider extends DataProvider {

  @VisibleForTesting
  public static final String EMPTY_REASON =
      "The Bazel profile does not include any remote cache checks or executions, which are required"
          + " for determining the latency. Try analyzing a profile for an invocation run against a"
          + " remote execution service.";

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            RemoteLatency.class, DatumSupplier.memoized(this::getRemoteLatency)));
  }

  @VisibleForTesting
  RemoteLatency getRemoteLatency()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
    Optional<Duration> duration =
        bazelProfile
            .getThreads()
            .flatMap((thread) -> thread.getCompleteEvents().stream())
            .filter(
                (event) ->
                    BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION.equals(event.category)
                        || BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK.equals(
                            event.category))
            .map((event) -> event.duration)
            .min(Duration::compareTo);

    if (duration.isEmpty()) {
      return new RemoteLatency(EMPTY_REASON);
    }
    // The duration is the round-trip-time. Assume that the latency is equal to and from the
    // service.
    return new RemoteLatency(duration.get().dividedBy(2));
  }
}
