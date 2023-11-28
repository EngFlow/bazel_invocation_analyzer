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

import com.engflow.bazel.invocation.analyzer.EventThreadBuilder;
import org.junit.Before;
import org.junit.Test;

public class FlagValueDataProviderTest extends DataProviderUnitTestBase {
  private FlagValueDataProvider provider;

  @Before
  public void setupTest() throws Exception {
    provider = new FlagValueDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void flagValueExperimentalProfileIncludeTargetLabelIsFalseWhenThereAreNoActions()
      throws Exception {
    useProfile(metaData(), trace(mainThread()));

    assertThat(provider.getExperimentalProfileIncludeTargetLabel())
        .isEqualTo(new FlagValueExperimentalProfileIncludeTargetLabel(false));
  }

  @Test
  public void
      flagValueExperimentalProfileIncludeTargetLabelIsFalseWhenActionProcessingDoesNotHaveALabel()
          throws Exception {
    var thread = new EventThreadBuilder(1, 1);
    thread.actionProcessingAction("foo", null, null, 0, 1);

    useProfile(metaData(), trace(mainThread(), thread.asEvent()));

    assertThat(provider.getExperimentalProfileIncludeTargetLabel())
        .isEqualTo(new FlagValueExperimentalProfileIncludeTargetLabel(false));
  }

  @Test
  public void
      flagValueExperimentalProfileIncludeTargetLabelIsTrueWhenActionProcessingHasAnEmptyLabel()
          throws Exception {
    var thread = new EventThreadBuilder(1, 1);
    thread.actionProcessingAction("foo", "", null, 0, 1);

    useProfile(metaData(), trace(mainThread(), thread.asEvent()));

    assertThat(provider.getExperimentalProfileIncludeTargetLabel())
        .isEqualTo(new FlagValueExperimentalProfileIncludeTargetLabel(true));
  }

  @Test
  public void
      flagValueExperimentalProfileIncludeTargetLabelIsTrueWhenActionProcessingHasANonemptyLabel()
          throws Exception {
    var thread = new EventThreadBuilder(1, 1);
    thread.actionProcessingAction("foo", "my target", null, 0, 1);

    useProfile(metaData(), trace(mainThread(), thread.asEvent()));

    assertThat(provider.getExperimentalProfileIncludeTargetLabel())
        .isEqualTo(new FlagValueExperimentalProfileIncludeTargetLabel(true));
  }
}
