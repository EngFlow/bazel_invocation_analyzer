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

package com.engflow.bazel.invocation.analyzer.integrationtests;

import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.SuggestionOutput;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelPhasesDataProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresAvailable;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresDataProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedCoresUsed;
import com.engflow.bazel.invocation.analyzer.dataproviders.EstimatedJobsFlagValue;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteCachingUsedDataProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionUsedDataProvider;
import com.engflow.bazel.invocation.analyzer.suggestionproviders.JobsSuggestionProvider;
import org.junit.Before;
import org.junit.Test;

public class JobsSuggestionProviderIntegrationTest extends IntegerationTestBase {
  private JobsSuggestionProvider provider;

  @Before
  public void before() throws Exception {
    provider = new JobsSuggestionProvider();

    new BazelPhasesDataProvider().register(dataManager);
    new EstimatedCoresDataProvider().register(dataManager);
    new RemoteCachingUsedDataProvider().register(dataManager);
    new RemoteExecutionUsedDataProvider().register(dataManager);
  }

  public void withProfile(String filename) throws Exception {
    BazelProfile bazelProfile = BazelProfile.createFromPath(RUNFILES.rlocation(ROOT + filename));
    bazelProfile.registerWithDataManager(dataManager);
  }

  @Test
  public void shouldRecognizeInvocationWith16CoresAndUnsetJobs() throws Exception {
    withProfile("local-jobs-none.json.gz");

    EstimatedCoresAvailable estimatedCoresAvailable =
        dataManager.getDatum(EstimatedCoresAvailable.class);
    assertThat(estimatedCoresAvailable.getEstimatedCores()).isEqualTo(16);
    assertThat(estimatedCoresAvailable.getGaps()).isEqualTo(0);

    EstimatedCoresUsed estimatedCoresUsed = dataManager.getDatum(EstimatedCoresUsed.class);
    assertThat(estimatedCoresUsed.getEstimatedCores()).isEqualTo(16);
    assertThat(estimatedCoresUsed.getGaps()).isEqualTo(0);

    EstimatedJobsFlagValue estimatedJobsFlagValue =
        dataManager.getDatum(EstimatedJobsFlagValue.class);
    assertThat(estimatedJobsFlagValue.isLikelySet()).isFalse();
    assertThat(estimatedJobsFlagValue.getLowerBound()).isEqualTo(16);

    SuggestionOutput output = provider.getSuggestions(dataManager);
    assertThat(output.hasFailure()).isFalse();
    assertThat(output.getMissingInputList()).isEmpty();
    assertThat(output.getSuggestionList()).isEmpty();
  }

  @Test
  public void shouldRecognizeInvocationWith16CoresAndJobsSetTo4() throws Exception {
    withProfile("local-jobs-4.json.gz");

    EstimatedCoresAvailable estimatedCoresAvailable =
        dataManager.getDatum(EstimatedCoresAvailable.class);
    assertThat(estimatedCoresAvailable.getEstimatedCores()).isEqualTo(16);
    assertThat(estimatedCoresAvailable.getGaps()).isEqualTo(0);

    EstimatedCoresUsed estimatedCoresUsed = dataManager.getDatum(EstimatedCoresUsed.class);
    assertThat(estimatedCoresUsed.getEstimatedCores()).isEqualTo(4);
    assertThat(estimatedCoresUsed.getGaps()).isEqualTo(0);

    EstimatedJobsFlagValue estimatedJobsFlagValue =
        dataManager.getDatum(EstimatedJobsFlagValue.class);
    assertThat(estimatedJobsFlagValue.isLikelySet()).isTrue();
    assertThat(estimatedJobsFlagValue.getLowerBound()).isEqualTo(4);

    SuggestionOutput output = provider.getSuggestions(dataManager);
    assertThat(output.hasFailure()).isFalse();
    assertThat(output.getSuggestionList().size()).isEqualTo(1);
    assertThat(String.join(" ", output.getSuggestionList().get(0).getRationaleList()))
        .contains(JobsSuggestionProvider.RATIONALE_FOR_LOCAL_TOO_LOW_JOBS_VALUE);
  }

  @Test
  public void shouldRecognizeInvocationWith16CoresAndJobsSetTo100() throws Exception {
    withProfile("local-jobs-100.json.gz");

    EstimatedCoresAvailable estimatedCoresAvailable =
        dataManager.getDatum(EstimatedCoresAvailable.class);
    assertThat(estimatedCoresAvailable.getEstimatedCores()).isEqualTo(16);
    assertThat(estimatedCoresAvailable.getGaps()).isEqualTo(0);

    EstimatedCoresUsed estimatedCoresUsed = dataManager.getDatum(EstimatedCoresUsed.class);
    assertThat(estimatedCoresUsed.getEstimatedCores()).isEqualTo(51);
    assertThat(estimatedCoresUsed.getGaps()).isEqualTo(46);

    EstimatedJobsFlagValue estimatedJobsFlagValue =
        dataManager.getDatum(EstimatedJobsFlagValue.class);
    assertThat(estimatedJobsFlagValue.isLikelySet()).isTrue();
    assertThat(estimatedJobsFlagValue.getLowerBound()).isEqualTo(97);

    SuggestionOutput output = provider.getSuggestions(dataManager);
    assertThat(output.hasFailure()).isFalse();
    assertThat(output.getSuggestionList().size()).isEqualTo(1);
    assertThat(String.join(" ", output.getSuggestionList().get(0).getRationaleList()))
        .contains(JobsSuggestionProvider.RATIONALE_FOR_LOCAL_TOO_HIGH_JOBS_VALUE);
  }
}
