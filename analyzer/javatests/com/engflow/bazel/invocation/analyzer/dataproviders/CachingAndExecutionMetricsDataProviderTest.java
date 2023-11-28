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

import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_LOCAL_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.EventThreadBuilder;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CachingAndExecutionMetricsDataProviderTest extends DataProviderUnitTestBase {
  private CachingAndExecutionMetricsDataProvider provider;
  private LocalActions localActions;

  @Before
  public void setupTest() throws Exception {
    provider = new CachingAndExecutionMetricsDataProvider();
    provider.register(dataManager);
    when(dataManager.getDatum(LocalActions.class)).thenAnswer(i -> localActions);
    super.dataProvider = provider;
  }

  @Test
  public void shouldReturnEmptyOnEmptyLocalActions()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    localActions = LocalActions.create(ImmutableList.of());

    var metrics = provider.getMetrics();
    assertThat(metrics.getActions()).isEqualTo(0);
    assertThat(metrics.getActionsWithRcHit()).isEqualTo(0);
    assertThat(metrics.getActionsWithRcMiss()).isEqualTo(0);
    assertThat(metrics.getActionsWithRcMissLocallyExecuted()).isEqualTo(0);
    assertThat(metrics.getActionsWithRcMissRemotelyExecuted()).isEqualTo(0);
    assertThat(metrics.getActionsWithRcMissExecNotReported()).isEqualTo(0);
    assertThat(metrics.getActionsWithoutRc()).isEqualTo(0);
    assertThat(metrics.getActionsWithoutRcLocallyExecuted()).isEqualTo(0);
    assertThat(metrics.getActionsWithoutRcRemotelyExecuted()).isEqualTo(0);
    assertThat(metrics.getActionsWithoutRcExecNotReported()).isEqualTo(0);
    assertThat(metrics.getActionsExecutedLocally()).isEqualTo(0);
    assertThat(metrics.getActionsExecutedRemotely()).isEqualTo(0);
  }

  @Test
  public void shouldReturnMetrics()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    var thread = new EventThreadBuilder(1, 1);
    List all = new ArrayList<LocalActions.LocalAction>();
    int cacheHits = 1;
    int cacheMissesLocalExec = 2;
    int cacheMissesRemoteExec = 4;
    int cacheMissesUnreportedExec = 8;
    int nocacheLocalExec = 16;
    int nocacheRemoteExec = 32;
    int nocacheUnreportedExec = 64;
    for (int i = 0; i < cacheHits; i++) {
      all.add(
          new LocalActions.LocalAction(
              thread.actionProcessingAction("cache hit " + i, "Work", 0, 5),
              List.of(
                  thread.related(0, 2, CAT_REMOTE_ACTION_CACHE_CHECK),
                  thread.related(2, 3, CAT_REMOTE_OUTPUT_DOWNLOAD))));
    }
    for (int i = 0; i < cacheMissesLocalExec; i++) {
      all.add(
          new LocalActions.LocalAction(
              thread.actionProcessingAction("cache miss local exec " + i, "Work", 0, 5),
              List.of(
                  thread.related(0, 2, CAT_REMOTE_ACTION_CACHE_CHECK),
                  thread.related(2, 3, CAT_LOCAL_ACTION_EXECUTION))));
    }
    for (int i = 0; i < cacheMissesRemoteExec; i++) {
      all.add(
          new LocalActions.LocalAction(
              thread.actionProcessingAction("cache miss remote exec " + i, "Work", 0, 5),
              List.of(
                  thread.related(0, 2, CAT_REMOTE_ACTION_CACHE_CHECK),
                  thread.related(2, 1, CAT_REMOTE_ACTION_EXECUTION),
                  thread.related(3, 2, CAT_REMOTE_OUTPUT_DOWNLOAD))));
    }
    for (int i = 0; i < cacheMissesUnreportedExec; i++) {
      all.add(
          new LocalActions.LocalAction(
              thread.actionProcessingAction("cache miss unreported exec " + i, "Work", 0, 5),
              List.of(thread.related(0, 2, CAT_REMOTE_ACTION_CACHE_CHECK))));
    }
    for (int i = 0; i < nocacheLocalExec; i++) {
      all.add(
          new LocalActions.LocalAction(
              thread.actionProcessingAction("mo cache check exec " + i, "Work", 0, 5),
              List.of(thread.related(2, 3, CAT_LOCAL_ACTION_EXECUTION))));
    }
    for (int i = 0; i < nocacheRemoteExec; i++) {
      all.add(
          new LocalActions.LocalAction(
              thread.actionProcessingAction("no cache check remote exec " + i, "Work", 0, 5),
              List.of(
                  thread.related(2, 1, CAT_REMOTE_ACTION_EXECUTION),
                  thread.related(3, 2, CAT_REMOTE_OUTPUT_DOWNLOAD))));
    }
    for (int i = 0; i < nocacheUnreportedExec; i++) {
      all.add(
          new LocalActions.LocalAction(
              thread.actionProcessingAction("no cache check unreported exec " + i, "Work", 0, 5),
              List.of()));
    }
    localActions = LocalActions.create(all);

    var metrics = provider.getMetrics();
    assertThat(metrics.getActions())
        .isEqualTo(
            cacheHits
                + cacheMissesLocalExec
                + cacheMissesRemoteExec
                + cacheMissesUnreportedExec
                + nocacheLocalExec
                + nocacheRemoteExec
                + nocacheUnreportedExec);
    assertThat(metrics.getActionsWithRcHit()).isEqualTo(cacheHits);
    assertThat(metrics.getActionsWithRcMiss())
        .isEqualTo(cacheMissesLocalExec + cacheMissesRemoteExec + cacheMissesUnreportedExec);
    assertThat(metrics.getActionsWithRcMissLocallyExecuted()).isEqualTo(cacheMissesLocalExec);
    assertThat(metrics.getActionsWithRcMissRemotelyExecuted()).isEqualTo(cacheMissesRemoteExec);
    assertThat(metrics.getActionsWithRcMissExecNotReported()).isEqualTo(cacheMissesUnreportedExec);
    assertThat(metrics.getActionsWithoutRc())
        .isEqualTo(nocacheLocalExec + nocacheRemoteExec + nocacheUnreportedExec);
    assertThat(metrics.getActionsWithoutRcLocallyExecuted()).isEqualTo(nocacheLocalExec);
    assertThat(metrics.getActionsWithoutRcRemotelyExecuted()).isEqualTo(nocacheRemoteExec);
    assertThat(metrics.getActionsWithoutRcExecNotReported()).isEqualTo(nocacheUnreportedExec);
    assertThat(metrics.getActionsExecutedLocally())
        .isEqualTo(cacheMissesLocalExec + nocacheLocalExec);
    assertThat(metrics.getActionsExecutedRemotely())
        .isEqualTo(cacheMissesRemoteExec + nocacheRemoteExec);
  }
}
