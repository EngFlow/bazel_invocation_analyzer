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

import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_GENERAL_INFORMATION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_SUBPROCESS_RUN;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.EventThreadBuilder;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions.LocalAction;
import java.time.Duration;
import java.util.List;
import org.junit.Test;

public class LocalActionsTest {
  @Test
  public void getDurationWithoutCacheCheck() {
    int otherEvent1 = 1;
    int cacheCheck1 = 2;
    int cacheCheck2 = 4;
    int otherEvent2 = 8;
    int totalInSeconds = otherEvent1 + cacheCheck1 + cacheCheck2 + otherEvent2;
    var thread = new EventThreadBuilder(1, 1);
    var action =
        new LocalAction(
            thread.actionProcessingAction("my action", "foo", 10, totalInSeconds),
            List.of(
                thread.related(otherEvent1, CAT_GENERAL_INFORMATION),
                thread.related(cacheCheck1, CAT_REMOTE_ACTION_CACHE_CHECK),
                thread.related(cacheCheck2, CAT_REMOTE_ACTION_CACHE_CHECK),
                thread.related(otherEvent2, CAT_REMOTE_ACTION_EXECUTION)));
    assertThat(action.getDurationWithoutCacheCheck())
        .isEqualTo(
            Duration.ofSeconds(totalInSeconds).minusSeconds(cacheCheck1).minusSeconds(cacheCheck2));
  }

  @Test
  public void getDurationWithoutCacheCheckNoEvent() {
    int durationInSeconds = 1234;
    var thread = new EventThreadBuilder(1, 1);
    var action =
        new LocalAction(
            thread.actionProcessingAction("my action", "foo", 10, durationInSeconds),
            List.of(
                thread.related(2, CAT_GENERAL_INFORMATION),
                thread.related(4, CAT_REMOTE_ACTION_EXECUTION)));
    assertThat(action.getDurationWithoutCacheCheck())
        .isEqualTo(Duration.ofSeconds(durationInSeconds));
  }

  @Test
  public void isExecutedLocallyIsTrue() {
    var thread = new EventThreadBuilder(1, 1);
    var action =
        new LocalAction(
            thread.actionProcessingAction("my action", "foo", 10, 20),
            List.of(thread.related(12, 2, CAT_GENERAL_INFORMATION, COMPLETE_SUBPROCESS_RUN)));
    assertThat(action.isExecutedLocally()).isTrue();
  }

  @Test
  public void isExecutedLocallyIsFalseNoEvent() {
    var thread = new EventThreadBuilder(1, 1);
    var action =
        new LocalAction(thread.actionProcessingAction("my action", "foo", 10, 20), List.of());
    assertThat(action.isExecutedLocally()).isFalse();
  }

  @Test
  public void isExecutedLocallyIsFalseWrongCategory() {
    var thread = new EventThreadBuilder(1, 1);
    var action =
        new LocalAction(
            thread.actionProcessingAction("my action", "foo", 10, 20),
            List.of(thread.related(12, 2, CAT_REMOTE_ACTION_CACHE_CHECK, COMPLETE_SUBPROCESS_RUN)));
    assertThat(action.isExecutedLocally()).isFalse();
  }

  @Test
  public void isExecutedLocallyIsFalseWrongName() {
    var thread = new EventThreadBuilder(1, 1);
    var action =
        new LocalAction(
            thread.actionProcessingAction("my action", "foo", 10, 20),
            List.of(thread.related(12, 2, CAT_GENERAL_INFORMATION, COMPLETE_SUBPROCESS_RUN + "x")));
    assertThat(action.isExecutedLocally()).isFalse();
  }

  @Test
  public void isExecutedLocallyIsFalseEventOutOfRange() {
    var thread = new EventThreadBuilder(1, 1);
    var action =
        new LocalAction(
            thread.actionProcessingAction("my action", "foo", 10, 20),
            List.of(thread.related(22, 1, CAT_GENERAL_INFORMATION, COMPLETE_SUBPROCESS_RUN)));
    assertThat(action.isExecutedLocally()).isTrue();
  }
}
