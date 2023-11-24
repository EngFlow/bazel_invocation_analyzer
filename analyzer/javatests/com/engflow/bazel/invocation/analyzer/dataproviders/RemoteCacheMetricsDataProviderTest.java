package com.engflow.bazel.invocation.analyzer.dataproviders;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_EXECUTION_UPLOAD_TIME;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;
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

    assertThat(provider.derive().isEmpty()).isTrue();
    assertThat(provider.derive().getEmptyReason()).isNotNull();
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
                        thread.actionProcessingAction("More Cached Work", "WorkC", 5),
                        List.of(
                            thread.related(4, CAT_REMOTE_ACTION_CACHE_CHECK),
                            thread.related(8, CAT_REMOTE_OUTPUT_DOWNLOAD))),
                    new LocalActions.LocalAction(
                        thread.actionProcessingAction("Cache Miss Work", "WorkC", 5),
                        List.of(
                            thread.related(16, CAT_REMOTE_ACTION_CACHE_CHECK),
                            thread.related(32, CAT_REMOTE_EXECUTION_UPLOAD_TIME))),
                    new LocalActions.LocalAction(
                        thread.actionProcessingAction("UnCached Work", "LocalWorkC", 5),
                        List.of()))));

    Truth.assertThat(provider.derive())
        .isEqualTo(
            new RemoteCacheMetrics(
                Duration.ofSeconds(1 + 4 + 16),
                Duration.ofSeconds(2 + 8),
                Duration.ofSeconds(32),
                25.0f));
  }
}
