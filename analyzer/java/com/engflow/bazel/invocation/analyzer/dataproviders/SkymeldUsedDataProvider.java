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
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;

/**
 * A {@link DataProvider} that returns whether the profile looks like it was generated while using
 * Skymeld.
 *
 * @see <a href="https://github.com/bazelbuild/bazel/issues/14057">Project Skymeld GitHub issue</a>
 */
public class SkymeldUsedDataProvider extends DataProvider {
  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            SkymeldUsed.class, DatumSupplier.memoized(this::getSkymeldUsed)));
  }

  @VisibleForTesting
  SkymeldUsed getSkymeldUsed()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    BazelPhaseDescriptions bazelPhaseDescriptions =
        getDataManager().getDatum(BazelPhaseDescriptions.class);
    var interleavedAnalysisAndExecutionPhase =
        bazelPhaseDescriptions.get(BazelProfilePhase.ANALYZE_AND_EXECUTE);
    if (!interleavedAnalysisAndExecutionPhase.isPresent()) {
      return new SkymeldUsed();
    }
    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
    var firstActionProcessing =
        bazelProfile
            .getThreads()
            // Find the first action processing event.
            .flatMap(thread -> thread.getCompleteEvents().stream())
            .filter(event -> BazelProfileConstants.CAT_ACTION_PROCESSING.equals(event.category))
            .map(event -> event.start)
            .min(Timestamp::compareTo);
    return new SkymeldUsed(interleavedAnalysisAndExecutionPhase.get(), firstActionProcessing);
  }
}
