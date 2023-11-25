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
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_GENERAL_INFORMATION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_SUBPROCESS_RUN;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.EventThreadBuilder;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions.LocalAction;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.truth.Expect;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.Subject.Factory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LocalActionsDataProviderTest extends DataProviderUnitTestBase {

  private static class LocalActionsSubject extends IterableSubject {

    private final LocalActions got;

    protected LocalActionsSubject(FailureMetadata metadata, LocalActions got) {
      super(metadata, got);
      this.got = got;
    }

    public void isEqualTo(LocalActions want) {
      check("actions").that(index(got)).isEqualTo(index(want));
    }

    private Map<CompleteEvent, List<CompleteEvent>> index(LocalActions actions) {
      return actions.stream()
          .collect(Collectors.toMap(a -> a.getAction(), a -> a.getRelatedEvents()));
    }
  }

  private static final Factory<LocalActionsSubject, LocalActions> localActions =
      LocalActionsSubject::new;

  @Rule public final Expect expect = Expect.create();

  private LocalActionsDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new LocalActionsDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void extractLocalActionsFromEmptyProfile()
      throws InvalidProfileException,
          MissingInputException,
          DuplicateProviderException,
          NullDatumException {
    useProfile(metaData(), trace(mainThread()));
    expect.about(localActions).that(provider.derive()).isEqualTo(LocalActions.create(List.of()));
  }

  @Test
  public void extractLocalActionsFromSingleThread()
      throws InvalidProfileException,
          MissingInputException,
          DuplicateProviderException,
          NullDatumException {
    var thread = new EventThreadBuilder(1, 1);
    var want =
        LocalActions.create(
            List.of(
                new LocalAction(
                    thread.actionProcessingAction("cached", "Work", 0, 5),
                    List.of(
                        thread.related(0, 2, CAT_REMOTE_ACTION_CACHE_CHECK),
                        thread.related(0, 3, CAT_REMOTE_OUTPUT_DOWNLOAD))),
                new LocalAction(
                    thread.actionProcessingAction("not_cached", "Work2", 5, 5),
                    List.of(thread.related(5, 2, CAT_REMOTE_ACTION_CACHE_CHECK)))));

    useProfile(metaData(), trace(mainThread(), thread.asEvent()));
    expect.about(localActions).that(provider.derive()).isEqualTo(want);
  }

  @Test
  public void extractLocalActionsFromMultipleThreads()
      throws InvalidProfileException,
          MissingInputException,
          DuplicateProviderException,
          NullDatumException {
    var one = new EventThreadBuilder(1, 1);
    var two = new EventThreadBuilder(2, 2);
    var want =
        LocalActions.create(
            List.of(
                new LocalAction(
                    one.actionProcessingAction("one cached", "Work", 0, 5),
                    List.of(
                        one.related(0, 2, CAT_REMOTE_ACTION_CACHE_CHECK),
                        one.related(0, 3, CAT_REMOTE_OUTPUT_DOWNLOAD))),
                new LocalAction(
                    one.actionProcessingAction("one cache miss", "Work2", 5, 5),
                    List.of(one.related(5, 2, CAT_REMOTE_ACTION_CACHE_CHECK))),
                new LocalAction(
                    two.actionProcessingAction("two cached", "Work", 1, 7),
                    List.of(
                        two.related(2, 2, CAT_REMOTE_ACTION_CACHE_CHECK),
                        two.related(4, 3, CAT_REMOTE_OUTPUT_DOWNLOAD))),
                new LocalAction(
                    two.actionProcessingAction("two cache miss", "Work3", 8, 5),
                    List.of(two.related(8, 2, CAT_REMOTE_ACTION_CACHE_CHECK)))));

    useProfile(metaData(), trace(mainThread(), one.asEvent(), two.asEvent()));
    expect.about(localActions).that(provider.derive()).isEqualTo(want);
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
