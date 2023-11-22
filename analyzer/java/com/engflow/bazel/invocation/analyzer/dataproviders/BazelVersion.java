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
import javax.annotation.Nullable;

/** The Bazel version used when the analyzed inputs were generated. */
public class BazelVersion implements Datum {
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
