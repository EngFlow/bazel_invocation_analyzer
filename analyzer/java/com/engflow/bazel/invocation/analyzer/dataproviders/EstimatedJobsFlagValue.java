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

/**
 * Estimated value of the Bazel flag `--jobs`. The Bazel profile includes information from which we
 * can infer a lower bound for the value of this flag. If the flag is not set, Bazel usually
 * defaults to the number of available cores. So if the value does not match the estimated available
 * cores, more likely than not the flag `--jobs` was set.
 */
public class EstimatedJobsFlagValue implements Datum {
  private final int lowerBound;
  private final boolean likelySet;

  public EstimatedJobsFlagValue(int lowerBound, boolean likelySet) {
    this.lowerBound = lowerBound;
    this.likelySet = likelySet;
  }

  public int getLowerBound() {
    return lowerBound;
  }

  public boolean isLikelySet() {
    return likelySet;
  }

  @Override
  public String getDescription() {
    return "Based on the Bazel profile, the estimated value that the Bazel flag `--jobs` was set"
        + " to.";
  }

  @Override
  public String getSummary() {
    return String.format(
        "Lower bound of %d; --jobs flag is likely %s", lowerBound, likelySet ? "set" : "not set");
  }
}
