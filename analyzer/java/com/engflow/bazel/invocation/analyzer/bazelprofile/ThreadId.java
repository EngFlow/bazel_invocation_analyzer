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

import java.util.Objects;

public class ThreadId {
  private final int processId;
  private final int threadId;

  public ThreadId(int processId, int threadId) {
    this.processId = processId;
    this.threadId = threadId;
  }

  public int getThreadId() {
    return threadId;
  }

  public int getProcessId() {
    return processId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ThreadId other = (ThreadId) o;
    return processId == other.processId && threadId == other.threadId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(processId, threadId);
  }

  @Override
  public String toString() {
    return String.format("processId: %d, threadId: %d", processId, threadId);
  }
}
