/*
 * Copyright 2023 EngFlow Inc.
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
import java.util.List;

/**
 * A {@link DataProvider} that supplies data on whether specific Bazel flags were used when the
 * Bazel profile was generated.
 */
public class FlagValueDataProvider extends DataProvider {
  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            FlagValueExperimentalProfileIncludeTargetLabel.class,
            memoized(this::getExperimentalProfileIncludeTargetLabel)));
  }

  public FlagValueExperimentalProfileIncludeTargetLabel getExperimentalProfileIncludeTargetLabel()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
    // See
    // https://github.com/bazelbuild/bazel/blob/7d10999fc0357596824f2b6022bbbd895f245a3c/src/main/java/com/google/devtools/build/lib/runtime/BlazeRuntime.java#L395
    // https://github.com/bazelbuild/bazel/blob/7d10999fc0357596824f2b6022bbbd895f245a3c/src/main/java/com/google/devtools/build/lib/profiler/Profiler.java#L818
    // https://github.com/bazelbuild/bazel/blob/7d10999fc0357596824f2b6022bbbd895f245a3c/src/main/java/com/google/devtools/build/lib/profiler/Profiler.java#L206-L208
    var hasTarget =
        bazelProfile
            .getThreads()
            .flatMap(profileThread -> profileThread.getCompleteEvents().stream())
            .anyMatch(
                event ->
                    BazelProfileConstants.CAT_ACTION_PROCESSING.equals(event.category)
                        && event.args.containsKey(
                            BazelProfileConstants.ARGS_CAT_ACTION_PROCESSING_TARGET));
    return new FlagValueExperimentalProfileIncludeTargetLabel(hasTarget);
  }
}
