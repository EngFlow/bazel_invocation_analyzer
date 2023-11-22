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

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.complete;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.thread;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.WriteBazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class EstimatedCoresDataProviderTest extends DataProviderUnitTestBase {
  private EstimatedCoresDataProvider provider;

  @Before
  public void setupTest() throws DuplicateProviderException {
    provider = new EstimatedCoresDataProvider();
    provider.register(dataManager);
    new BazelPhasesDataProvider().register(dataManager);
    super.dataProvider = provider;
  }

  private WriteBazelProfile.TraceEvent skyFrameThread(
      int i, Timestamp timestamp, Duration duration) {
    return thread(
        i, i, String.format("skyframe-evaluator-%d", i), complete("", "", timestamp, duration));
  }

  @Test
  public void shouldReturnEstimatedCoresAvailableAllThreadsWithinRange() throws Exception {
    int maxIndexInRelevantPhase = 5;
    Timestamp start = Timestamp.ofMicros(20_000);
    Timestamp within = Timestamp.ofMicros(22_000);
    Timestamp end = Timestamp.ofMicros(30_000);
    useProfileWithDefaults(
        List.of(),
        List.of(
            skyFrameThread(0, start, Duration.ZERO),
            skyFrameThread(1, end, Duration.ZERO),
            skyFrameThread(2, within, Duration.ZERO),
            skyFrameThread(maxIndexInRelevantPhase, within, Duration.ZERO)));

    BazelPhaseDescriptions bazelPhaseDescriptions =
        BazelPhaseDescriptions.newBuilder()
            .add(BazelProfilePhase.EVALUATE, new BazelPhaseDescription(start, within))
            .add(BazelProfilePhase.DEPENDENCIES, new BazelPhaseDescription(within, end))
            .build();

    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenReturn(bazelPhaseDescriptions);

    EstimatedCoresAvailable estimatedCores = provider.getEstimatedCoresAvailable();
    assertThat(estimatedCores.getEstimatedCores().get()).isEqualTo(maxIndexInRelevantPhase + 1);
  }

  @Test
  public void shouldReturnEstimatedCoresAvailableSomeThreadsWithinRange() throws Exception {
    int maxIndexInRelevantPhase = 3;
    Timestamp start = Timestamp.ofMicros(20_000);
    Timestamp within = Timestamp.ofMicros(22_000);
    Timestamp end = Timestamp.ofMicros(30_000);
    Timestamp outsideRange = Timestamp.ofMicros(30_001);
    useProfileWithDefaults(
        List.of(),
        List.of(
            skyFrameThread(maxIndexInRelevantPhase - 1, start, Duration.ZERO),
            skyFrameThread(maxIndexInRelevantPhase - 2, within, Duration.ZERO),
            skyFrameThread(maxIndexInRelevantPhase, end, Duration.ZERO),
            skyFrameThread(maxIndexInRelevantPhase + 3, outsideRange, Duration.ZERO)));

    BazelPhaseDescriptions bazelPhaseDescriptions =
        BazelPhaseDescriptions.newBuilder()
            .add(BazelProfilePhase.EVALUATE, new BazelPhaseDescription(start, within))
            .add(BazelProfilePhase.DEPENDENCIES, new BazelPhaseDescription(within, end))
            .build();

    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenReturn(bazelPhaseDescriptions);

    EstimatedCoresAvailable estimatedCores = provider.getEstimatedCoresAvailable();
    assertThat(estimatedCores.getEstimatedCores().get()).isEqualTo(maxIndexInRelevantPhase + 1);
  }

  @Test
  public void shouldReturnEstimatedCoresAvailableEvaluatePhaseMarkerMissing() throws Exception {
    int maxIndexInRelevantPhase = 3;
    Timestamp start = Timestamp.ofMicros(20_000);
    Timestamp within1 = Timestamp.ofMicros(22_000);
    Timestamp end = Timestamp.ofMicros(30_000);
    Timestamp outsideRange = Timestamp.ofMicros(30_001);
    useProfileWithDefaults(
        List.of(),
        List.of(
            skyFrameThread(maxIndexInRelevantPhase, within1, Duration.ZERO),
            skyFrameThread(maxIndexInRelevantPhase + 3, outsideRange, Duration.ZERO)));

    BazelPhaseDescriptions bazelPhaseDescriptions =
        BazelPhaseDescriptions.newBuilder()
            .add(BazelProfilePhase.DEPENDENCIES, new BazelPhaseDescription(start, end))
            .build();

    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenReturn(bazelPhaseDescriptions);

    assertThat(provider.getEstimatedCoresAvailable().getEstimatedCores().get())
        .isEqualTo(maxIndexInRelevantPhase + 1);
  }

  @Test
  public void shouldReturnEstimatedCoresAvailableDependenciesPhaseMarkerMissing() throws Exception {
    int maxIndexInRelevantPhase = 3;
    Timestamp start = Timestamp.ofMicros(20_000);
    Timestamp within1 = Timestamp.ofMicros(22_000);
    Timestamp end = Timestamp.ofMicros(30_000);
    Timestamp outsideRange = Timestamp.ofMicros(30_001);
    useProfileWithDefaults(
        List.of(),
        List.of(
            skyFrameThread(maxIndexInRelevantPhase, within1, Duration.ZERO),
            skyFrameThread(maxIndexInRelevantPhase + 3, outsideRange, Duration.ZERO)));

    BazelPhaseDescriptions bazelPhaseDescriptions =
        BazelPhaseDescriptions.newBuilder()
            .add(BazelProfilePhase.EVALUATE, new BazelPhaseDescription(start, end))
            .build();

    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenReturn(bazelPhaseDescriptions);

    assertThat(provider.getEstimatedCoresAvailable().getEstimatedCores().get())
        .isEqualTo(maxIndexInRelevantPhase + 1);
  }

  @Test
  public void shouldReturnEmptyEstimatedCoresAvailablePhaseMarkersMissing() throws Exception {
    int maxIndexInRelevantPhase = 3;
    Timestamp start = Timestamp.ofMicros(20_000);
    Timestamp within1 = Timestamp.ofMicros(22_000);
    Timestamp within2 = Timestamp.ofMicros(28_000);
    Timestamp end = Timestamp.ofMicros(30_000);
    Timestamp outsideRange = Timestamp.ofMicros(30_001);
    useProfileWithDefaults(
        List.of(),
        List.of(
            skyFrameThread(maxIndexInRelevantPhase, within1, Duration.ZERO),
            skyFrameThread(maxIndexInRelevantPhase + 3, outsideRange, Duration.ZERO)));

    BazelPhaseDescriptions bazelPhaseDescriptions =
        BazelPhaseDescriptions.newBuilder()
            .add(BazelProfilePhase.INIT, new BazelPhaseDescription(start, within1))
            .add(BazelProfilePhase.EXECUTE, new BazelPhaseDescription(within2, end))
            .build();

    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenReturn(bazelPhaseDescriptions);

    assertThat(provider.getEstimatedCoresAvailable().getEstimatedCores().isEmpty()).isTrue();
  }

  @Test
  public void shouldReturnEmptyEstimatedCoresAvailableWhenMissingEvents() throws Exception {
    Timestamp start = Timestamp.ofMicros(20_000);
    Timestamp within1 = Timestamp.ofMicros(22_000);
    Timestamp within2 = Timestamp.ofMicros(28_000);
    Timestamp end = Timestamp.ofMicros(30_000);
    Timestamp outsideRange = Timestamp.ofMicros(30_001);
    useProfileWithDefaults(List.of(), List.of(skyFrameThread(3, outsideRange, Duration.ZERO)));

    BazelPhaseDescriptions bazelPhaseDescriptions =
        BazelPhaseDescriptions.newBuilder()
            .add(BazelProfilePhase.EVALUATE, new BazelPhaseDescription(start, within1))
            .add(BazelProfilePhase.DEPENDENCIES, new BazelPhaseDescription(within2, end))
            .build();

    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenReturn(bazelPhaseDescriptions);

    assertThat(provider.getEstimatedCoresAvailable().getEstimatedCores().isEmpty()).isTrue();
  }

  @Test
  public void shouldReturnEstimatedCoresUsedAllThreadsWithinRange() throws Exception {
    Timestamp start = Timestamp.ofMicros(20_000);
    Timestamp end = Timestamp.ofMicros(30_000);
    useProfileWithDefaults(
        List.of(),
        List.of(
            skyFrameThread(0, start, Duration.ZERO),
            skyFrameThread(1, start, Duration.ZERO),
            skyFrameThread(2, start, Duration.ZERO),
            skyFrameThread(3, start, Duration.ZERO)));
    BazelPhaseDescriptions bazelPhaseDescriptions =
        BazelPhaseDescriptions.newBuilder()
            .add(BazelProfilePhase.EXECUTE, new BazelPhaseDescription(start, end))
            .build();

    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenReturn(bazelPhaseDescriptions);

    EstimatedCoresUsed estimatedCores = provider.getEstimatedCoresUsed();
    assertThat(estimatedCores.getEstimatedCores().get()).isEqualTo(4);
  }

  @Test
  public void shouldReturnEstimatedCoresUsedSomeThreadsWithinRange() throws Exception {
    Timestamp outsideRangeBefore = Timestamp.ofMicros(19_999);
    Timestamp start = Timestamp.ofMicros(20_000);
    Timestamp end = Timestamp.ofMicros(30_000);
    Timestamp outsideRangeAfter = Timestamp.ofMicros(30_001);
    useProfileWithDefaults(
        List.of(),
        List.of(
            skyFrameThread(0, outsideRangeAfter, Duration.ZERO),
            skyFrameThread(1, start, Duration.ZERO),
            skyFrameThread(2, start, Duration.ZERO),
            skyFrameThread(3, outsideRangeBefore, Duration.ZERO)));
    BazelPhaseDescriptions bazelPhaseDescriptions =
        BazelPhaseDescriptions.newBuilder()
            .add(BazelProfilePhase.EXECUTE, new BazelPhaseDescription(start, end))
            .build();

    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenReturn(bazelPhaseDescriptions);

    EstimatedCoresUsed estimatedCores = provider.getEstimatedCoresUsed();
    assertThat(estimatedCores.getEstimatedCores().get()).isEqualTo(2);
  }

  @Test
  public void shouldReturnEmptyEstimatedCoresUsedWhenMissingEvents() throws Exception {
    Timestamp outsideRangeBefore = Timestamp.ofMicros(19_999);
    Timestamp start = Timestamp.ofMicros(20_000);
    Timestamp end = Timestamp.ofMicros(30_000);
    Timestamp outsideRangeAfter = Timestamp.ofMicros(30_001);
    useProfileWithDefaults(
        List.of(),
        List.of(
            skyFrameThread(1, outsideRangeBefore, Duration.ZERO),
            skyFrameThread(2, outsideRangeAfter, Duration.ZERO)));
    BazelPhaseDescriptions bazelPhaseDescriptions =
        BazelPhaseDescriptions.newBuilder()
            .add(BazelProfilePhase.EXECUTE, new BazelPhaseDescription(start, end))
            .build();

    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenReturn(bazelPhaseDescriptions);

    assertThat(provider.getEstimatedCoresUsed().isEmpty()).isTrue();
  }

  @Test
  public void shouldReturnEmptyEstimatedCoresUsedWhenExecutePhaseMarkerIsMissing()
      throws Exception {
    Timestamp outsideRangeBefore = Timestamp.ofMicros(19_999);
    Timestamp start = Timestamp.ofMicros(20_000);
    Timestamp within1 = Timestamp.ofMicros(22_000);
    Timestamp within2 = Timestamp.ofMicros(28_000);
    Timestamp end = Timestamp.ofMicros(30_000);
    Timestamp outsideRangeAfter = Timestamp.ofMicros(30_001);
    useProfileWithDefaults(
        List.of(),
        List.of(
            skyFrameThread(0, outsideRangeBefore, Duration.ZERO),
            skyFrameThread(1, within2, Duration.ZERO),
            skyFrameThread(2, outsideRangeAfter, Duration.ZERO),
            skyFrameThread(3, within1, Duration.ZERO)));
    BazelPhaseDescriptions bazelPhaseDescriptions =
        BazelPhaseDescriptions.newBuilder()
            .add(BazelProfilePhase.DEPENDENCIES, new BazelPhaseDescription(start, within1))
            .add(BazelProfilePhase.FINISH, new BazelPhaseDescription(within2, end))
            .build();

    when(dataManager.getDatum(BazelPhaseDescriptions.class)).thenReturn(bazelPhaseDescriptions);

    assertThat(provider.getEstimatedCoresUsed().isEmpty()).isTrue();
  }

  @Test
  public void getGapsShouldRecognizeGapsWhenSetIsEmpty() {
    // 0, 1, 2 are missing.
    Set<Integer> values = Sets.newHashSet();
    assertThat(provider.getGaps(values, 2)).isEqualTo(3);
  }

  @Test
  public void getGapsShouldRecognizeNoGaps() {
    // All present.
    Set<Integer> values = Sets.newHashSet(0, 1, 2, 3, 4, 5);
    assertThat(provider.getGaps(values, 5)).isEqualTo(0);
  }

  @Test
  public void getGapsShouldRecognizeGapsInTheStart() {
    // 0 is missing.
    Set<Integer> values = Sets.newHashSet(1, 2, 3, 4, 5);
    assertThat(provider.getGaps(values, 5)).isEqualTo(1);
  }

  @Test
  public void getGapsShouldRecognizeGapsInMiddle() {
    // 1, 2, 4 are missing.
    Set<Integer> values = Sets.newHashSet(0, 3, 5);
    assertThat(provider.getGaps(values, 5)).isEqualTo(3);
  }

  @Test
  public void getGapsShouldRecognizeGapsInTheEnd() {
    // 4, 5 are missing.
    Set<Integer> values = Sets.newHashSet(0, 1, 2, 3);
    assertThat(provider.getGaps(values, 5)).isEqualTo(2);
  }

  @Test
  public void getGapsShouldRecognizeGapsThroughout() {
    // 0, 2, 3, 4, 5 are missing.
    Set<Integer> values = Sets.newHashSet(1);
    assertThat(provider.getGaps(values, 5)).isEqualTo(5);
  }
}
