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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.dataproviders.DataProviderUnitTestBase;
import org.junit.Before;
import org.junit.Test;

public class RemoteExecutionUsedDataProviderTest extends DataProviderUnitTestBase {
  private RemoteExecutionUsedDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new RemoteExecutionUsedDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnRemoteExecutionUsed() throws Exception {
    // TODO: Generate a small json with remote execution.
    String profilePath = RUNFILES.rlocation(ROOT + "bazel-profile-with_queuing.json.gz");
    BazelProfile bazelProfile = BazelProfile.createFromPath(profilePath);
    when(dataManager.getDatum(BazelProfile.class)).thenReturn(bazelProfile);

    RemoteExecutionUsed remoteExecutionUsed = provider.getRemoteExecutionUsed();
    verify(dataManager).registerProvider(provider);
    verify(dataManager).getDatum(BazelProfile.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(remoteExecutionUsed.isRemoteExecutionUsed()).isTrue();
  }

  @Test
  public void shouldReturnRemoteExecutionNotUsed() throws Exception {
    String profilePath = RUNFILES.rlocation(ROOT + "tiny.json.gz");
    BazelProfile bazelProfile = BazelProfile.createFromPath(profilePath);
    when(dataManager.getDatum(BazelProfile.class)).thenReturn(bazelProfile);

    RemoteExecutionUsed remoteExecutionUsed = provider.getRemoteExecutionUsed();
    verify(dataManager).registerProvider(provider);
    verify(dataManager).getDatum(BazelProfile.class);
    verifyNoMoreInteractions(dataManager);

    assertThat(remoteExecutionUsed.isRemoteExecutionUsed()).isFalse();
  }
}
