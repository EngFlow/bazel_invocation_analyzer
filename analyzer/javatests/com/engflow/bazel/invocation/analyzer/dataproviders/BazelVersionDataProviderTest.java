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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import org.junit.Before;
import org.junit.Test;

public class BazelVersionDataProviderTest extends DataProviderUnitTestBase {
  private BazelVersionDataProvider provider;

  @Before
  public void setupTest() throws Exception {
    provider = new BazelVersionDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnBazelVersionFromBazelProfile()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    BazelVersion expected = BazelVersion.parse("release 1.2.3");

    BazelProfile mockProfile = mock(BazelProfile.class);
    when(dataManager.getDatum(BazelProfile.class)).thenReturn(mockProfile);
    when(mockProfile.getBazelVersion()).thenReturn(expected);

    assertThat(provider.getBazelVersion()).isEqualTo(expected);
  }
}
