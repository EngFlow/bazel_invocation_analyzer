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

import static com.engflow.bazel.invocation.analyzer.core.DatumSupplier.memoized;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import java.time.Duration;
import java.util.List;

/** A {@link DataProvider} that supplies data on Bazel's garbage collection. */
public class GarbageCollectionStatsDataProvider extends DataProvider {
  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            GarbageCollectionStats.class, memoized(this::getGarbageCollectionStats)));
  }

  public GarbageCollectionStats getGarbageCollectionStats()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
    try {
      Duration majorGarbageCollection =
          bazelProfile
              .getThreads()
              .filter(BazelProfileConstants::isGarbageCollectorThread)
              .flatMap(profileThread -> profileThread.getCompleteEvents().stream())
              .filter(
                  event ->
                      BazelProfileConstants.COMPLETE_MAJOR_GARBAGE_COLLECTION.equals(event.name)
                          && BazelProfileConstants.CAT_GARBAGE_COLLECTION.equals(event.category))
              .map(event -> event.duration)
              .reduce(Duration.ZERO, Duration::plus);
      return new GarbageCollectionStats(majorGarbageCollection);
    } catch (Exception e) {
      throw new InvalidProfileException("Unable to find garbage collector thread.", e);
    }
  }
}
