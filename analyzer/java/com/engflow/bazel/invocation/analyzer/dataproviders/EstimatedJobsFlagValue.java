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

import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Estimated value of the Bazel flag `--jobs`. The Bazel profile includes information from which we
 * can infer a lower bound for the value of this flag. If the flag is not set, Bazel usually
 * defaults to the number of available cores. So if the value does not match the estimated available
 * cores, more likely than not the flag `--jobs` was set.
 */
public class EstimatedJobsFlagValue implements Datum {
  private final Optional<Integer> lowerBound;
  private final boolean likelySet;
  @Nullable private final String emptyReason;

  public EstimatedJobsFlagValue(String emptyReason) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(emptyReason));
    this.lowerBound = Optional.empty();
    this.likelySet = false;
    this.emptyReason = emptyReason;
  }

  public EstimatedJobsFlagValue(Integer lowerBound, boolean likelySet) {
    Preconditions.checkNotNull(lowerBound);
    this.lowerBound = Optional.ofNullable(lowerBound);
    this.likelySet = likelySet;
    this.emptyReason = null;
  }

  public Optional<Integer> getLowerBound() {
    return lowerBound;
  }

  public boolean isLikelySet() {
    return likelySet;
  }

  @Override
  public boolean isEmpty() {
    return lowerBound.isEmpty();
  }

  @Override
  public String getEmptyReason() {
    return emptyReason;
  }

  @Override
  public String getDescription() {
    return "Based on the Bazel profile, the estimated value that the Bazel flag `--jobs` was set"
        + " to.";
  }

  @Override
  public String getSummary() {
    return isEmpty()
        ? null
        : String.format(
            "Lower bound of %d; --jobs flag is likely %s",
            lowerBound.get(), likelySet ? "set" : "not set");
  }
}
