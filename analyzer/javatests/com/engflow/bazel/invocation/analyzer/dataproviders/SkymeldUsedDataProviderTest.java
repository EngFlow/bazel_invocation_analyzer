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
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
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
  public void shouldReturnSkymeldUsed() throws Exception {
    when(dataManager.getDatum(BazelPhaseDescriptions.class))
        .thenReturn(
            BazelPhaseDescriptions.newBuilder()
                .add(
                    BazelProfilePhase.ANALYZE_AND_EXECUTE,
                    new BazelPhaseDescription(Timestamp.ofSeconds(2), Timestamp.ofSeconds(3)))
                .build());
    assertThat(provider.getSkymeldUsed().isSkymeldUsed()).isTrue();
  }
}
