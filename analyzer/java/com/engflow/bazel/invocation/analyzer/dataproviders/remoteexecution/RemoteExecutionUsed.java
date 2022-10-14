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

package com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution;

import com.engflow.bazel.invocation.analyzer.core.Datum;

/** Whether evidence was found of remote execution being used */
public class RemoteExecutionUsed implements Datum {
  private final boolean remoteExecutionUsed;

  public RemoteExecutionUsed(boolean remoteExecutionUsed) {
    this.remoteExecutionUsed = remoteExecutionUsed;
  }

  public boolean isRemoteExecutionUsed() {
    return remoteExecutionUsed;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public String getEmptyReason() {
    return null;
  }

  @Override
  public String getDescription() {
    return "Whether the Bazel Profile includes events indicating that remote execution was used.";
  }

  @Override
  public String getSummary() {
    return String.valueOf(remoteExecutionUsed);
  }
}
