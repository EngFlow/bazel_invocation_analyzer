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

package com.engflow.bazel.invocation.analyzer.time;

public class Range {

  public static Range between(Timestamp start, Timestamp end) {
    return new Range(start, end);
  }

  private final Timestamp start;
  private final Timestamp end;

  private Range(Timestamp start, Timestamp end) {
    this.start = start;
    this.end = end;
  }

  public boolean contains(Timestamp... values) {
    for (Timestamp value : values) {
      if (start.plus(Timestamp.ACCEPTABLE_DIVERGENCE.negated()).compareTo(value) > 0
          || end.plus(Timestamp.ACCEPTABLE_DIVERGENCE).compareTo(value) < 0) {
        return false;
      }
    }
    return true;
  }
}
