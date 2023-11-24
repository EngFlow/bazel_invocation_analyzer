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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import org.junit.Test;

public class BazelProfilePhaseTest {
  @Test
  public void getPreviousWorks() {
    assertThat(BazelProfilePhase.FINISH.getPrevious()).isEqualTo(BazelProfilePhase.EXECUTE);
    assertThat(BazelProfilePhase.EXECUTE.getPrevious()).isEqualTo(BazelProfilePhase.PREPARE);
    assertThat(BazelProfilePhase.PREPARE.getPrevious()).isEqualTo(BazelProfilePhase.LICENSE);
    assertThat(BazelProfilePhase.LICENSE.getPrevious())
        .isEqualTo(BazelProfilePhase.ANALYZE_AND_EXECUTE);
    assertThat(BazelProfilePhase.ANALYZE_AND_EXECUTE.getPrevious())
        .isEqualTo(BazelProfilePhase.ANALYZE);
    assertThat(BazelProfilePhase.ANALYZE.getPrevious())
        .isEqualTo(BazelProfilePhase.TARGET_PATTERN_EVAL);
    assertThat(BazelProfilePhase.TARGET_PATTERN_EVAL.getPrevious())
        .isEqualTo(BazelProfilePhase.INIT);
    assertThat(BazelProfilePhase.INIT.getPrevious()).isEqualTo(BazelProfilePhase.LAUNCH);
    assertThrows(UnsupportedOperationException.class, () -> BazelProfilePhase.LAUNCH.getPrevious());
  }

  @Test
  public void getNextWorks() {
    assertThat(BazelProfilePhase.LAUNCH.getNext()).isEqualTo(BazelProfilePhase.INIT);
    assertThat(BazelProfilePhase.INIT.getNext()).isEqualTo(BazelProfilePhase.TARGET_PATTERN_EVAL);
    assertThat(BazelProfilePhase.TARGET_PATTERN_EVAL.getNext())
        .isEqualTo(BazelProfilePhase.ANALYZE);
    assertThat(BazelProfilePhase.ANALYZE.getNext())
        .isEqualTo(BazelProfilePhase.ANALYZE_AND_EXECUTE);
    assertThat(BazelProfilePhase.ANALYZE_AND_EXECUTE.getNext())
        .isEqualTo(BazelProfilePhase.LICENSE);
    assertThat(BazelProfilePhase.LICENSE.getNext()).isEqualTo(BazelProfilePhase.PREPARE);
    assertThat(BazelProfilePhase.PREPARE.getNext()).isEqualTo(BazelProfilePhase.EXECUTE);
    assertThat(BazelProfilePhase.EXECUTE.getNext()).isEqualTo(BazelProfilePhase.FINISH);
    assertThrows(UnsupportedOperationException.class, () -> BazelProfilePhase.FINISH.getNext());
  }
}
