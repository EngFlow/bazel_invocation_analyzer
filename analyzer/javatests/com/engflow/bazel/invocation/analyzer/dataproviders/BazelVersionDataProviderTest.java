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

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.WriteBazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
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
  public void shouldReturnEmptyOnMissingBazelVersion()
      throws DuplicateProviderException,
          InvalidProfileException,
          MissingInputException,
          NullDatumException {
    useProfile(metaData(), trace(mainThread()));
    assertThat(provider.getBazelVersion().isEmpty()).isTrue();
  }

  @Test
  public void shouldReturnEmptyOnInvalidBazelVersion()
      throws DuplicateProviderException,
          InvalidProfileException,
          MissingInputException,
          NullDatumException {
    useProfile(
        metaData(
            WriteBazelProfile.Property.put(
                BazelProfileConstants.OTHER_DATA_BAZEL_VERSION, "invalid")),
        trace(mainThread()));
    assertThat(provider.getBazelVersion().isEmpty()).isTrue();
  }

  @Test
  public void shouldReturnVersionWithoutPreRelease()
      throws DuplicateProviderException,
          InvalidProfileException,
          MissingInputException,
          NullDatumException {
    String validBazelVersion = "release 6.1.0";
    useProfile(
        metaData(
            WriteBazelProfile.Property.put(
                BazelProfileConstants.OTHER_DATA_BAZEL_VERSION, validBazelVersion)),
        trace(mainThread()));

    BazelVersion version = provider.getBazelVersion();
    assertThat(version.isEmpty()).isFalse();
    assertThat(version.getSummary()).isEqualTo(validBazelVersion);
  }

  @Test
  public void shouldReturnBazelVersionWithPreRelease()
      throws DuplicateProviderException,
          InvalidProfileException,
          MissingInputException,
          NullDatumException {
    String validBazelVersion = "release 8.0.0-pre.20231030.2";
    useProfile(
        metaData(
            WriteBazelProfile.Property.put(
                BazelProfileConstants.OTHER_DATA_BAZEL_VERSION, validBazelVersion)),
        trace(mainThread()));

    BazelVersion version = provider.getBazelVersion();
    assertThat(version.isEmpty()).isFalse();
    assertThat(version.getSummary()).isEqualTo(validBazelVersion);
  }
}
