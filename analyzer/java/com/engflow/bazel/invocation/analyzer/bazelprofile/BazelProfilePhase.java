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

package com.engflow.bazel.invocation.analyzer.bazelprofile;

import java.util.Comparator;
import java.util.stream.Stream;

public enum BazelProfilePhase {
  LAUNCH("Launch Blaze", 0),
  INIT("Initialize command", 1),
  EVALUATE("Evaluate target patterns", 2),
  DEPENDENCIES("Load and analyze dependencies", 3),
  PREPARE("Prepare for build", 4),
  EXECUTE("Build artifacts", 5),
  FINISH("Complete build", 6);

  public final String name;
  public final int order;

  BazelProfilePhase(String name, int order) {
    this.name = name;
    this.order = order;
  }

  /**
   * Returns the Bazel phase with the largest order smaller than the order of this phase.
   *
   * @throws UnsupportedOperationException for the first phase
   */
  public BazelProfilePhase getPrevious() throws UnsupportedOperationException {
    return Stream.of(values())
        .filter((a) -> a.order < this.order)
        .max(Comparator.comparingInt(a -> a.order))
        .orElseThrow(() -> new UnsupportedOperationException());
  }

  /**
   * Returns the Bazel phase with the smallest order larger than the order of this phase.
   *
   * @throws UnsupportedOperationException for the last phase
   */
  public BazelProfilePhase getNext() throws UnsupportedOperationException {
    return Stream.of(values())
        .filter((a) -> a.order > this.order)
        .min(Comparator.comparingInt(a -> a.order))
        .orElseThrow(() -> new UnsupportedOperationException());
  }

  public static BazelProfilePhase parse(String name) {
    for (BazelProfilePhase phase : values()) {
      if (phase.name.equals(name)) {
        return phase;
      }
    }
    return null;
  }
}
