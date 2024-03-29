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

/**
 * Estimate of the number of cores available, irrespective of restrictions imposed by setting the
 * flag `--jobs`. This value may both be higher or lower than {@link EstimatedCoresUsed}.
 */
public class EstimatedCoresAvailable extends EstimatedCores {
  public EstimatedCoresAvailable(String emptyReason) {
    super(emptyReason);
  }

  public EstimatedCoresAvailable(Integer estimatedCoresAvailable, Integer gaps) {
    super(estimatedCoresAvailable, gaps);
  }

  @Override
  public String getDescription() {
    return "The estimated number of cores available on the machine that ran the Bazel client, as"
        + " well as how many numbers were skipped when naming skyframe-evaluators. Extracted"
        + " from the Bazel profile.";
  }
}
