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

public enum BazelProfilePhase {
  // The order of these is important, it reflects in which order we expect the timestamps of the
  // phases markers in the Bazel profile to be.
  LAUNCH("Launch Blaze"),
  INIT("Initialize command"),
  EVALUATE("Evaluate target patterns"),
  DEPENDENCIES("Load and analyze dependencies"),
  PREPARE("Prepare for build"),
  EXECUTE("Build artifacts"),
  FINISH("Complete build");

  public final String name;

  BazelProfilePhase(String name) {
    this.name = name;
  }

  /**
   * Returns the previous Bazel phase.
   *
   * @throws UnsupportedOperationException for the first phase
   */
  public BazelProfilePhase getPrevious() throws UnsupportedOperationException {
    if (this.ordinal() == 0) {
      throw new UnsupportedOperationException();
    }
    return values()[this.ordinal() - 1];
  }

  /**
   * Returns the next Bazel phase.
   *
   * @throws UnsupportedOperationException for the last phase
   */
  public BazelProfilePhase getNext() throws UnsupportedOperationException {
    if (this.ordinal() == values().length - 1) {
      throw new UnsupportedOperationException();
    }
    return values()[this.ordinal() + 1];
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
