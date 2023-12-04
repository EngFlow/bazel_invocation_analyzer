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

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_EXECUTION_UPLOAD_TIME;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD_MISSING_INPUTS;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD_OUTPUTS;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.EventThreadBuilder;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.google.common.truth.Expect;
import com.google.common.truth.Truth;
import java.time.Duration;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RemoteCacheMetricsDataProviderTest extends DataProviderUnitTestBase {
  @Rule public final Expect expect = Expect.create();
  private RemoteCacheMetricsDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    new LocalActionsDataProvider().register(dataManager);
    provider = new RemoteCacheMetricsDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void shouldBeEmptyWhenNoCachingIsIncluded() throws Exception {
    useProfile(metaData(), trace(mainThread()));
    when(dataManager.getDatum(LocalActions.class)).thenReturn(LocalActions.create(List.of()));

    assertThat(provider.derive().getCacheCheckDuration()).isEqualTo(Duration.ZERO);
    assertThat(provider.derive().getDownloadOutputsDuration()).isEqualTo(Duration.ZERO);
    assertThat(provider.derive().getUploadOutputsDuration()).isEqualTo(Duration.ZERO);
  }

  @Test
  public void summarizeCacheData()
      throws InvalidProfileException,
          MissingInputException,
          DuplicateProviderException,
          NullDatumException {
    useProfile(metaData(), trace(mainThread()));

    var thread = new EventThreadBuilder(1, 1);
    when(dataManager.getDatum(LocalActions.class))
        .thenReturn(
            LocalActions.create(
                List.of(
                    new LocalActions.LocalAction(
                        thread.actionProcessingAction("Cached Work", "WorkC", 5),
                        List.of(
                            thread.related(1, CAT_REMOTE_ACTION_CACHE_CHECK),
                            thread.related(2, CAT_REMOTE_OUTPUT_DOWNLOAD))),
                    new LocalActions.LocalAction(
                        thread.actionProcessingAction("Cache Miss Work", "WorkC", 5),
                        List.of(
                            thread.related(4, CAT_REMOTE_ACTION_CACHE_CHECK),
                            thread.related(
                                3,
                                CAT_REMOTE_EXECUTION_UPLOAD_TIME,
                                COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD_OUTPUTS))),
                    new LocalActions.LocalAction(
                        thread.actionProcessingAction("Cache Miss Work RE", "WorkC", 5),
                        List.of(
                            thread.related(8, CAT_REMOTE_ACTION_CACHE_CHECK),
                            thread.related(
                                5,
                                CAT_REMOTE_EXECUTION_UPLOAD_TIME,
                                COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD_MISSING_INPUTS),
                            thread.related(
                                7,
                                CAT_REMOTE_EXECUTION_UPLOAD_TIME,
                                COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD))),
                    new LocalActions.LocalAction(
                        thread.actionProcessingAction("Work without cache check", "LocalWorkC", 5),
                        List.of()))));

    Truth.assertThat(provider.derive())
        .isEqualTo(
            new RemoteCacheMetrics(
                Duration.ofSeconds(1 + 4 + 8), Duration.ofSeconds(2), Duration.ofSeconds(3 + 7)));
  }
}
