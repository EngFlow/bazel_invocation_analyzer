package com.engflow.bazel.invocation.analyzer.dataproviders;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.mainThread;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.metaData;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.trace;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_EXECUTION_UPLOAD_TIME;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;

import com.engflow.bazel.invocation.analyzer.WriteBazelProfile;
import com.engflow.bazel.invocation.analyzer.WriteBazelProfile.ProfileSection;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.google.common.truth.Expect;
import com.google.common.truth.Truth;
import java.time.Duration;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;

public class RemoteCacheMetricsDataProviderTest {
  @Rule public final Expect expect = Expect.create();

  private RemoteCacheMetricsDataProvider useProfile(ProfileSection... sections)
      throws DuplicateProviderException {
    var manager = new DataManager();
    manager.registerProvider(
        new DataProvider() {
          @Override
          public List<DatumSupplierSpecification<?>> getSuppliers() {
            return List.of(
                DatumSupplierSpecification.of(
                    BazelProfile.class,
                    () ->
                        BazelProfile.createFromInputStream(
                            WriteBazelProfile.toInputStream(sections))));
          }
        });
    new LocalActionsDataProvider().register(manager);
    var provider = new RemoteCacheMetricsDataProvider();
    provider.register(manager);

    return provider;
  }

  @Test
  public void summarizeCacheData()
      throws InvalidProfileException, MissingInputException, DuplicateProviderException,
          NullDatumException {
    var thread = new EventThreadBuilder(1, 1);
    thread.action("Cached Work", "WorkC", 5);
    thread.related( 2, CAT_REMOTE_ACTION_CACHE_CHECK);
    thread.related(2, CAT_REMOTE_OUTPUT_DOWNLOAD);
    thread.action("More Cached Work", "WorkC", 5);
    thread.related( 2, CAT_REMOTE_ACTION_CACHE_CHECK);
    thread.related(2, CAT_REMOTE_OUTPUT_DOWNLOAD);
    thread.action("Cache Miss Work", "WorkC", 5);
    thread.related(2, CAT_REMOTE_ACTION_CACHE_CHECK);
    thread.related( 2, CAT_REMOTE_EXECUTION_UPLOAD_TIME);
    thread.action("UnCached Work", "LocalWorkC", 5);

    var provider = useProfile(metaData(), trace(mainThread(), thread.asEvent()));
    Truth.assertThat(provider.derive())
        .isEqualTo(
            new RemoteCacheMetrics(
                Duration.ofSeconds(6), Duration.ofSeconds(4), Duration.ofSeconds(2), 25.0f));
  }
}
