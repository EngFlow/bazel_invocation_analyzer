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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.Before;
import org.junit.Test;

public class CriticalPathDurationDataProviderTest extends DataProviderUnitTestBase {
  private CriticalPathDurationDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new CriticalPathDurationDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnCriticalPathDuration() throws Exception {
    // TODO: Generate a small json.
    String profilePath = RUNFILES.rlocation(ROOT + "tiny.json.gz");
    BazelProfile bazelProfile = BazelProfile.createFromPath(profilePath);
    when(dataManager.getDatum(BazelProfile.class)).thenReturn(bazelProfile);

    CriticalPathDuration duration = provider.getCriticalPathDuration();
    verify(dataManager).registerProvider(provider);
    verify(dataManager).getDatum(BazelProfile.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(duration.getCriticalPathDuration().truncatedTo(ChronoUnit.SECONDS))
        .isEqualTo(Duration.ofSeconds(13));
  }
}
