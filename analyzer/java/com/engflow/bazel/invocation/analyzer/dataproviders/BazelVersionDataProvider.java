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
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link DataProvider} that supplies data on the Bazel version used when the Bazel profile was
 * generated.
 */
public class BazelVersionDataProvider extends DataProvider {
  // See https://bazel.build/release#bazel-versioning
  // See
  // https://github.com/bazelbuild/bazel/blob/aab19f75cd383c4b09a6ae720f9fa436bf89d271/src/main/java/com/google/devtools/build/lib/profiler/JsonTraceFileWriter.java#L179
  // See
  // https://github.com/bazelbuild/bazel/blob/c637041ec145e0964982a2cbf8d5693f0d1d4be0/src/main/java/com/google/devtools/build/lib/analysis/BlazeVersionInfo.java#L119
  private static final Pattern BAZEL_VERSION_PATTERN =
      Pattern.compile("^release (\\d+)\\.(\\d+)\\.(\\d+)(|-.*)$");

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(BazelVersion.class, memoized(this::getBazelVersion)));
  }

  public BazelVersion getBazelVersion()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
    String bazelVersion =
        bazelProfile.getOtherData().get(BazelProfileConstants.OTHER_DATA_BAZEL_VERSION);
    return parse(bazelVersion);
  }

  @VisibleForTesting
  static BazelVersion parse(String version) {
    if (version == null) {
      // The version metadata was introduced in https://github.com/bazelbuild/bazel/pull/17562 and
      // added to release 6.1.0.
      return new BazelVersion(
          "No Bazel version was found. Bazel versions before 6.1.0 did not report the version.");
    }
    Matcher m = BAZEL_VERSION_PATTERN.matcher(version);
    if (m.matches()) {
      return new BazelVersion(
          Integer.valueOf(m.group(1)),
          Integer.valueOf(m.group(2)),
          Integer.valueOf(m.group(3)),
          m.group(4));
    } else {
      return new BazelVersion(
          String.format("The provided Bazel version could not be parsed: '%s'", version));
    }
  }
}
