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

package com.engflow.bazel.invocation.analyzer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import org.junit.Before;

public abstract class UnitTestBase extends ProfileTestBase {
  /** DataManager to coordinate the transfer of data between Providers */
  protected DataManager dataManager;

  @Before
  public void setupTestBase() {
    dataManager = mock(DataManager.class);
  }

  @Override
  protected void registerBazelProfile(BazelProfile bazelProfile)
      throws InvalidProfileException, MissingInputException, NullDatumException {
    when(dataManager.getDatum(BazelProfile.class)).thenReturn(bazelProfile);
  }
}
