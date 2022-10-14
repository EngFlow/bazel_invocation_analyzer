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

/** Estimate of the number of cores used or available. */
public abstract class EstimatedCores implements Datum {
  private final Optional<Integer> estimatedCores;
  private final Optional<Integer> gaps;
  @Nullable private final String emptyReason;

  EstimatedCores(Integer estimatedCores, Integer gaps) {
    Preconditions.checkNotNull(estimatedCores);
    Preconditions.checkNotNull(gaps);
    this.estimatedCores = Optional.of(estimatedCores);
    this.gaps = Optional.of(gaps);
    this.emptyReason = null;
  }

  EstimatedCores(String emptyReason) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(emptyReason));
    this.estimatedCores = Optional.empty();
    this.gaps = Optional.empty();
    this.emptyReason = emptyReason;
  }

  @Override
  public boolean isEmpty() {
    return estimatedCores.isEmpty() && gaps.isEmpty();
  }

  @Override
  public String getEmptyReason() {
    return emptyReason;
  }

  public Optional<Integer> getEstimatedCores() {
    return estimatedCores;
  }

  public boolean hasGaps() {
    return gaps.isPresent() && gaps.get() > 0;
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
  public Optional<Integer> getGaps() {
    return gaps;
  }

  public String getSummary() {
    return isEmpty()
        ? null
        : String.format("%d cores (with %d gaps)", estimatedCores.get(), gaps.get());
  }
}
