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

import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/** The Bazel version used when the analyzed inputs were generated. */
public class BazelVersion implements Datum {
  // See https://bazel.build/release#bazel-versioning
  // See
  // https://github.com/bazelbuild/bazel/blob/aab19f75cd383c4b09a6ae720f9fa436bf89d271/src/main/java/com/google/devtools/build/lib/profiler/JsonTraceFileWriter.java#L179
  // See
  // https://github.com/bazelbuild/bazel/blob/c637041ec145e0964982a2cbf8d5693f0d1d4be0/src/main/java/com/google/devtools/build/lib/analysis/BlazeVersionInfo.java#L119
  private static final Pattern BAZEL_VERSION_PATTERN =
      Pattern.compile("^release (\\d+)\\.(\\d+)\\.(\\d+)(|-.*)$");

  private final Optional<Integer> major;
  private final Optional<Integer> minor;
  private final Optional<Integer> patch;
  private final Optional<String> preReleaseAnnotation;
  @Nullable private final String emptyReason;

  BazelVersion(int major, int minor, int patch, String preReleaseAnnotation) {
    Preconditions.checkNotNull(preReleaseAnnotation);
    this.major = Optional.of(major);
    this.minor = Optional.of(minor);
    this.patch = Optional.of(patch);
    this.preReleaseAnnotation = Optional.of(preReleaseAnnotation);
    this.emptyReason = null;
  }

  BazelVersion(String emptyReason) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(emptyReason));
    this.major = Optional.empty();
    this.minor = Optional.empty();
    this.patch = Optional.empty();
    this.preReleaseAnnotation = Optional.empty();
    this.emptyReason = emptyReason;
  }

  public static BazelVersion parse(String version) {
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

  @Override
  public boolean isEmpty() {
    return emptyReason != null;
  }

  @Override
  public String getEmptyReason() {
    return emptyReason;
  }

  public Optional<Integer> getMajor() {
    return major;
  }

  public Optional<Integer> getMinor() {
    return minor;
  }

  public Optional<Integer> getPatch() {
    return patch;
  }

  public Optional<String> getPreReleaseAnnotation() {
    return preReleaseAnnotation;
  }

  @Override
  public String getDescription() {
    return "The Bazel version used when the Bazel profile was generated. Extracted from the Bazel"
        + " profile.";
  }

  public String getSummary() {
    return isEmpty()
        ? null
        : String.format(
            "release %d.%d.%d%s",
            major.get(), minor.get(), patch.get(), preReleaseAnnotation.get());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BazelVersion that = (BazelVersion) o;
    return major.equals(that.major)
        && minor.equals(that.minor)
        && patch.equals(that.patch)
        && preReleaseAnnotation.equals(that.preReleaseAnnotation)
        && Objects.equals(emptyReason, that.emptyReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch, preReleaseAnnotation, emptyReason);
  }

  @Override
  public String toString() {
    return isEmpty() ? emptyReason : getSummary();
  }
}
