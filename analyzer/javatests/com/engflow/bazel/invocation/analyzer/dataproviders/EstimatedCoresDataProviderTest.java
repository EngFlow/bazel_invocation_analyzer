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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.google.common.collect.Sets;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class EstimatedCoresDataProviderTest extends DataProviderUnitTestBase {
  private EstimatedCoresDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new EstimatedCoresDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnEstimatedCores() throws Exception {
    // TODO: Generate a small json.
    String profilePath = RUNFILES.rlocation(ROOT + "tiny.json.gz");
    BazelProfile bazelProfile = BazelProfile.createFromPath(profilePath);
    when(dataManager.getDatum(BazelProfile.class)).thenReturn(bazelProfile);

    EstimatedCoresUsed estimatedCores = provider.getEstimatedCoresUsed();
    verify(dataManager).registerProvider(provider);
    verify(dataManager).getDatum(BazelProfile.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(estimatedCores.getEstimatedCores()).isEqualTo(8);
  }

  @Test
  public void getGapsShouldRecognizeGapsWhenSetIsEmpty() {
    // 0, 1, 2 are missing.
    Set<Integer> values = Sets.newHashSet();
    assertThat(provider.getGaps(values, 2)).isEqualTo(3);
  }

  @Test
  public void getGapsShouldRecognizeNoGaps() {
    // All present.
    Set<Integer> values = Sets.newHashSet(0, 1, 2, 3, 4, 5);
    assertThat(provider.getGaps(values, 5)).isEqualTo(0);
  }

  @Test
  public void getGapsShouldRecognizeGapsInTheStart() {
    // 0 is missing.
    Set<Integer> values = Sets.newHashSet(1, 2, 3, 4, 5);
    assertThat(provider.getGaps(values, 5)).isEqualTo(1);
  }

  @Test
  public void getGapsShouldRecognizeGapsInMiddle() {
    // 1, 2, 4 are missing.
    Set<Integer> values = Sets.newHashSet(0, 3, 5);
    assertThat(provider.getGaps(values, 5)).isEqualTo(3);
  }

  @Test
  public void getGapsShouldRecognizeGapsInTheEnd() {
    // 4, 5 are missing.
    Set<Integer> values = Sets.newHashSet(0, 1, 2, 3);
    assertThat(provider.getGaps(values, 5)).isEqualTo(2);
  }

  @Test
  public void getGapsShouldRecognizeGapsThroughout() {
    // 0, 2, 3, 4, 5 are missing.
    Set<Integer> values = Sets.newHashSet(1);
    assertThat(provider.getGaps(values, 5)).isEqualTo(5);
  }
}
