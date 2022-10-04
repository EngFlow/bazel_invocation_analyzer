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

/** Estimate of the number of cores used or available. */
public abstract class EstimatedCores implements Datum {
  private final int estimatedCores;
  private final int gaps;

  EstimatedCores(int estimatedCoresUsed, int gaps) {
    this.estimatedCores = estimatedCoresUsed;
    this.gaps = gaps;
  }

  public int getEstimatedCores() {
    return estimatedCores;
  }

  public boolean hasGaps() {
    return gaps > 0;
  }

  /**
   * The estimated cores are based on skyframe-evaluators listed in the Bazel profile. These are
   * numbered starting with 0 up to the determined maximum allowed. However, if they are not used at
   * all, they may be dropped from the profile, which leads to gaps in the list of numbered
   * skyframe-evaluators.<br>
   * This method returns how many such numbered skyframe-evaluators are not present in the profile,
   * compared to all numbers from 0 up to the maximum found in the profile.<br>
   * Dropped evaluators with a number above the maximum found are not detected. In that, the maximum
   * found only represent a lower bound for the maximum allowed.
   */
  public int getGaps() {
    return gaps;
  }

  public String getSummary() {
    return String.format("%d cores (with %d gaps)", estimatedCores, gaps);
  }
}
