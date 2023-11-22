/*
 * Copyright 2023 EngFlow Inc.
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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class BazelVersionTest {
  @Test
  public void parseReturnsEmpty() {
    assertThat(BazelVersion.parse("").isEmpty()).isTrue();
    assertThat(BazelVersion.parse("1.2.3").isEmpty()).isTrue();
    assertThat(BazelVersion.parse("1.2.3-foo").isEmpty()).isTrue();
    assertThat(BazelVersion.parse("release 1").isEmpty()).isTrue();
    assertThat(BazelVersion.parse("release 1.2").isEmpty()).isTrue();
    assertThat(BazelVersion.parse("release 1.2.3foo").isEmpty()).isTrue();
  }

  @Test
  public void parseReturnsNonempty() {
    assertThat(BazelVersion.parse("release 1.23.4")).isEqualTo(new BazelVersion(1, 23, 4, ""));
    assertThat(BazelVersion.parse("release 12.3.45-foo"))
        .isEqualTo(new BazelVersion(12, 3, 45, "-foo"));
  }
}
