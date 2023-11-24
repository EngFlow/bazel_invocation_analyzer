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

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.complete;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;

public class SkymeldUsedDataProviderTest extends DataProviderUnitTestBase {
  private SkymeldUsedDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new SkymeldUsedDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnSkymeldNotUsed() throws Exception {
    when(dataManager.getDatum(BazelPhaseDescriptions.class))
        .thenReturn(BazelPhaseDescriptions.newBuilder().build());
    assertThat(provider.getSkymeldUsed().isSkymeldUsed()).isFalse();
  }

  @Test
  public void shouldReturnSkymeldUsedWithoutExecutionPhase() throws Exception {
    Timestamp analysisAndExecutionStart = Timestamp.ofSeconds(2);
    Timestamp analysisAndExecutionEnd = Timestamp.ofSeconds(6);
    when(dataManager.getDatum(BazelPhaseDescriptions.class))
        .thenReturn(
            BazelPhaseDescriptions.newBuilder()
                .add(
                    BazelProfilePhase.ANALYZE_AND_EXECUTE,
                    new BazelPhaseDescription(analysisAndExecutionStart, analysisAndExecutionEnd))
                .build());
    useProfile(metaData(), trace(mainThread()));

    assertThat(provider.getSkymeldUsed().isSkymeldUsed()).isTrue();
    assertThat(provider.getSkymeldUsed().getAnalysisAndExecutionPhase().isPresent()).isTrue();
    assertThat(provider.getSkymeldUsed().getAnalysisAndExecutionPhase().get().getStart())
        .isEqualTo(analysisAndExecutionStart);
    assertThat(provider.getSkymeldUsed().getAnalysisAndExecutionPhase().get().getEnd())
        .isEqualTo(analysisAndExecutionEnd);
    assertThat(provider.getSkymeldUsed().getExecutionPhase().isEmpty()).isTrue();
  }

  @Test
  public void shouldReturnSkymeldUsedWithExecutionPhase() throws Exception {
    Timestamp analysisAndExecutionStart = Timestamp.ofSeconds(2);
    Timestamp executionStart = Timestamp.ofSeconds(3);
    Timestamp analysisAndExecutionEnd = Timestamp.ofSeconds(6);
    when(dataManager.getDatum(BazelPhaseDescriptions.class))
        .thenReturn(
            BazelPhaseDescriptions.newBuilder()
                .add(
                    BazelProfilePhase.ANALYZE_AND_EXECUTE,
                    new BazelPhaseDescription(analysisAndExecutionStart, analysisAndExecutionEnd))
                .build());
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                10,
                10,
                String.format("thread-%d", 10),
                complete(
                    "some execution event",
                    BazelProfileConstants.CAT_ACTION_PROCESSING,
                    executionStart,
                    Duration.ofSeconds(1)))));

    assertThat(provider.getSkymeldUsed().isSkymeldUsed()).isTrue();
    assertThat(provider.getSkymeldUsed().getAnalysisAndExecutionPhase().isPresent()).isTrue();
    assertThat(provider.getSkymeldUsed().getAnalysisAndExecutionPhase().get().getStart())
        .isEqualTo(analysisAndExecutionStart);
    assertThat(provider.getSkymeldUsed().getAnalysisAndExecutionPhase().get().getEnd())
        .isEqualTo(analysisAndExecutionEnd);
    assertThat(provider.getSkymeldUsed().getExecutionPhase().isPresent()).isTrue();
    assertThat(provider.getSkymeldUsed().getExecutionPhase().get().getStart())
        .isEqualTo(executionStart);
    assertThat(provider.getSkymeldUsed().getExecutionPhase().get().getEnd())
        .isEqualTo(analysisAndExecutionEnd);
  }
}
