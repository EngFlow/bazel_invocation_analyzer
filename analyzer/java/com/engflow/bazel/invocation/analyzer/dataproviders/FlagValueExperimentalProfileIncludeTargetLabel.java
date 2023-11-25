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
import java.util.Objects;

/**
 * Whether the Bazel flag `--experimental_profile_include_target_label` was enabled when the Bazel
 * profile was generated.
 *
 * @see <a
 *     href="https://bazel.build/reference/command-line-reference#flag--experimental_profile_include_target_label">Bazel
 *     Command-Line Reference</a>
 */
public class FlagValueExperimentalProfileIncludeTargetLabel implements Datum {
  public static final String FLAG_NAME = "--experimental_profile_include_target_label";
  public static final String COMMAND_LINE_REFERENCE_URL =
      "https://bazel.build/reference/command-line-reference#flag--experimental_profile_include_target_label";
  private final boolean profileIncludeTargetLabelEnabled;

  public FlagValueExperimentalProfileIncludeTargetLabel(boolean profileIncludeTargetLabelEnabled) {
    this.profileIncludeTargetLabelEnabled = profileIncludeTargetLabelEnabled;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public String getEmptyReason() {
    return null;
  }

  public boolean isProfileIncludeTargetLabelEnabled() {
    return profileIncludeTargetLabelEnabled;
  }

  @Override
  public String getDescription() {
    return "Whether the Bazel flag `--experimental_profile_include_target_label` was enabled when"
        + " the Bazel profile was generated.";
  }

  public String getSummary() {
    return String.valueOf(profileIncludeTargetLabelEnabled);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FlagValueExperimentalProfileIncludeTargetLabel that =
        (FlagValueExperimentalProfileIncludeTargetLabel) o;
    return profileIncludeTargetLabelEnabled == that.profileIncludeTargetLabelEnabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(profileIncludeTargetLabelEnabled);
  }

  @Override
  public String toString() {
    return String.valueOf(profileIncludeTargetLabelEnabled);
  }
}
