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

import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.UnitTestBase;
import org.junit.Test;

public class BazelProfileConstantsTest extends UnitTestBase {
  @Test
  public void isMainThreadShouldReturnFalseOnUnnamedProfileThread() {
    ProfileThread thread = new ProfileThread(new ThreadId(0, 0));
    assertThat(BazelProfileConstants.isMainThread(thread)).isFalse();
  }

  @Test
  public void isMainThreadShouldReturnFalseOnOtherProfileThread() {
    ProfileThread thread =
        new ProfileThread(
            new ThreadId(0, 0), "skyframe-evaluator-1", null, null, null, null, null, null);
    assertThat(BazelProfileConstants.isMainThread(thread)).isFalse();
  }

  @Test
  public void isMainThreadShouldReturnTrueOnNewName() {
    ProfileThread thread =
        new ProfileThread(new ThreadId(0, 0), "Main Thread", null, null, null, null, null, null);
    assertThat(BazelProfileConstants.isMainThread(thread)).isTrue();
  }

  @Test
  public void isMainThreadShouldReturnTrueOnOldName() {
    ProfileThread thread =
        new ProfileThread(new ThreadId(0, 0), "grpc-command-3", null, null, null, null, null, null);
    assertThat(BazelProfileConstants.isMainThread(thread)).isTrue();
  }

  @Test
  public void isGarbageCollectorThreadShouldReturnFalseOnUnnamedProfileThread() {
    ProfileThread thread = new ProfileThread(new ThreadId(0, 0));
    assertThat(BazelProfileConstants.isGarbageCollectorThread(thread)).isFalse();
  }

  @Test
  public void isGarbageCollectorShouldReturnFalseOnOtherProfileThread() {
    ProfileThread thread =
        new ProfileThread(
            new ThreadId(0, 0), "skyframe-evaluator-1", null, null, null, null, null, null);
    assertThat(BazelProfileConstants.isGarbageCollectorThread(thread)).isFalse();
  }

  @Test
  public void isGarbageCollectorShouldReturnTrueOnNewName() {
    ProfileThread thread =
        new ProfileThread(
            new ThreadId(0, 0), "Garbage Collector", null, null, null, null, null, null);
    assertThat(BazelProfileConstants.isGarbageCollectorThread(thread)).isTrue();
  }

  @Test
  public void isGarbageCollectorShouldReturnTrueOnOldName() {
    ProfileThread thread =
        new ProfileThread(new ThreadId(0, 0), "Service Thread", null, null, null, null, null, null);
    assertThat(BazelProfileConstants.isGarbageCollectorThread(thread)).isTrue();
  }
}
