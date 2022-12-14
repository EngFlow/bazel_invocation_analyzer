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

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.complete;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.dataproviders.DataProviderUnitTestBase;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;

public class QueuingObservedDataProviderTest extends DataProviderUnitTestBase {
  private QueuingObservedDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new QueuingObservedDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnQueuingObserved() throws Exception {
    useProfile(
        metaData(),
        trace(
            mainThread(),
            thread(
                0,
                0,
                "foo",
                complete(
                    "bar",
                    BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME,
                    Timestamp.ofMicros(123),
                    Duration.ZERO))));

    assertThat(provider.getQueuingObserved().isQueuingObserved()).isTrue();
  }

  @Test
  public void shouldReturnQueuingNotObserved() throws Exception {
    useProfile(metaData(), trace(mainThread()));

    assertThat(provider.getQueuingObserved().isQueuingObserved()).isFalse();
  }
}
