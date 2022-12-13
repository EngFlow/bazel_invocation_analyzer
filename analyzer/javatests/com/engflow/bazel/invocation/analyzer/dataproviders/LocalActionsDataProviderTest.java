package com.engflow.bazel.invocation.analyzer.dataproviders;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;

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
      return actions.stream().collect(Collectors.toMap(a -> a.action, a -> a.relatedEvents));
    }
  }

  private static final Factory<LocalActionsSubject, LocalActions> localActions = LocalActionsSubject::new;

  @Rule public final Expect expect = Expect.create();

  private LocalActionsDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new LocalActionsDataProvider();
    provider.register(dataManager);
    super.dataProvider = provider;
  }

  @Test
  public void extractLocalActionsFromSingleThread()
      throws InvalidProfileException, MissingInputException, DuplicateProviderException,
          NullDatumException {
    var thread = new EventThreadBuilder(1, 1);
    var want =
        LocalActions.create(
            List.of(
                new LocalAction(
                    thread.action("cached", "Work", 0, 5),
                    List.of(
                        thread.related(0, 2, CAT_REMOTE_ACTION_CACHE_CHECK),
                        thread.related(0, 3, CAT_REMOTE_OUTPUT_DOWNLOAD))),
                new LocalAction(
                    thread.action("cached", "Work2", 5, 5),
                    List.of(thread.related(5, 2, CAT_REMOTE_ACTION_CACHE_CHECK)))));

    useProfile(metaData(), trace(mainThread(), thread.asEvent()));
    expect.about(localActions).that(provider.derive()).isEqualTo(want);
  }

  @Test
  public void extractLocalActionsFromMultipleThreads()
      throws InvalidProfileException, MissingInputException, DuplicateProviderException,
          NullDatumException {
    var one = new EventThreadBuilder(1, 1);
    var two = new EventThreadBuilder(2, 2);
    var want =
        LocalActions.create(
            List.of(
                new LocalAction(
                    one.action("one cached", "Work", 0, 5),
                    List.of(
                        one.related(0, 2, CAT_REMOTE_ACTION_CACHE_CHECK),
                        one.related(0, 3, CAT_REMOTE_OUTPUT_DOWNLOAD))),
                new LocalAction(
                    one.action("one cache miss", "Work2", 5, 5),
                    List.of(one.related(5, 2, CAT_REMOTE_ACTION_CACHE_CHECK))),
                new LocalAction(
                    two.action("two cached", "Work", 1, 7),
                    List.of(
                        two.related(2, 2, CAT_REMOTE_ACTION_CACHE_CHECK),
                        two.related(4, 3, CAT_REMOTE_OUTPUT_DOWNLOAD))),
                new LocalAction(
                    two.action("two cache miss", "Work3", 8, 5),
                    List.of(two.related(8, 2, CAT_REMOTE_ACTION_CACHE_CHECK)))));

    useProfile(metaData(), trace(mainThread(), one.asEvent(), two.asEvent()));
    expect.about(localActions).that(provider.derive()).isEqualTo(want);
  }
}
