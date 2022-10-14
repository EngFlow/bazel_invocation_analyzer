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

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.InstantEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * A {@link DataProvider} that supplies information about the various phases for a Bazel invocation,
 * including:
 *
 * <ul>
 *   <li>start times and durations of each phase
 *   <li>total duration of the invocation
 * </ul>
 */
public class BazelPhasesDataProvider extends DataProvider {
  private static final String TOTAL_DURATION_EMPTY_REASON_LAUNCH =
      "The Bazel profile does not include a launch marker, which is required for determining the"
          + " invocation's total duration. All Bazel profiles should include this data. Try"
          + " creating a new profile.";
  private static final String TOTAL_DURATION_EMPTY_REASON_COMPLETE =
      "The Bazel profile does not include a completion marker, which is required for determining"
          + " the invocation's total duration. All Bazel profiles should include this data. Try"
          + " creating a new profile.";
  private static final Logger logger = Logger.getLogger(BazelProfile.class.getName());

  private Timestamp launchStart;
  private Timestamp finishEnd;

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            BazelPhaseDescriptions.class, DatumSupplier.memoized(this::getBazelPhaseDescriptions)),
        DatumSupplierSpecification.of(
            TotalDuration.class, DatumSupplier.memoized(this::getTotalDuration)));
  }

  private void determineStartAndEndTimestamps()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    if (launchStart == null || finishEnd == null) {
      BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);

      try {
        // Launch is a CompleteEvent whereas all the other phase indicators are InstantEvents
        launchStart =
            bazelProfile.getMainThread().getCompleteEvents().stream()
                .filter(x -> x.name.equals(BazelProfilePhase.LAUNCH.name))
                .findAny()
                .get()
                .start;
      } catch (Exception ex) {
        throw new InvalidProfileException(
            String.format(
                "Unable to find complete event named \"%s\".", BazelProfilePhase.LAUNCH.name),
            ex);
      }

      try {
        List<InstantEvent> generalInfo =
            bazelProfile
                .getMainThread()
                .getInstants()
                .get(BazelProfileConstants.CAT_GENERAL_INFORMATION);
        Preconditions.checkNotNull(generalInfo);
        finishEnd =
            generalInfo.stream()
                .filter(i -> BazelProfileConstants.INSTANT_FINISHING.equals(i.getName()))
                .findAny()
                .get()
                .getTimestamp();
      } catch (Exception ex) {
        throw new InvalidProfileException(
            String.format(
                "Unable to find instant event named \"%s\".",
                BazelProfileConstants.INSTANT_FINISHING),
            ex);
      }
    }
  }

  @VisibleForTesting
  TotalDuration getTotalDuration()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    determineStartAndEndTimestamps();
    if (launchStart == null) {
      return new TotalDuration(TOTAL_DURATION_EMPTY_REASON_LAUNCH);
    }
    if (finishEnd == null) {
      return new TotalDuration(TOTAL_DURATION_EMPTY_REASON_COMPLETE);
    }
    return new TotalDuration(TimeUtil.getDurationBetween(launchStart, finishEnd));
  }

  @VisibleForTesting
  BazelPhaseDescriptions getBazelPhaseDescriptions()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    determineStartAndEndTimestamps();

    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);

    Map<Timestamp, BazelProfilePhase> startToPhase = new TreeMap<>();

    List<InstantEvent> buildPhases;
    try {
      buildPhases =
          bazelProfile
              .getMainThread()
              .getInstants()
              .get(BazelProfileConstants.CAT_BUILD_PHASE_MARKER);
    } catch (Exception ex) {
      throw new InvalidProfileException(
          String.format(
              "No instant events of category \"%s\" found",
              BazelProfileConstants.CAT_BUILD_PHASE_MARKER),
          ex);
    }

    if (buildPhases != null) {
      for (InstantEvent buildPhaseMarker : buildPhases) {
        String phaseName = buildPhaseMarker.getName();
        try {
          BazelProfilePhase phase = BazelProfilePhase.parse(phaseName);
          if (startToPhase.containsKey(buildPhaseMarker.getTimestamp())) {
            throw new InvalidProfileException(
                String.format(
                    "Two phase markers have the same timestamp %dμs: \"%s\" and \"%s\"",
                    buildPhaseMarker.getTimestamp().getMicros(),
                    phase.name,
                    startToPhase.get(buildPhaseMarker.getTimestamp()).name));
          }
          startToPhase.put(buildPhaseMarker.getTimestamp(), phase);
        } catch (IllegalArgumentException e) {
          logger.warning(String.format("Found unrecognized Bazel phase %s", phaseName));
        }
      }
    }

    BazelPhaseDescriptions.Builder resultBuilder = BazelPhaseDescriptions.newBuilder();
    Timestamp previousTimestamp = launchStart;
    BazelProfilePhase previousPhase = BazelProfilePhase.LAUNCH;
    for (Timestamp timestamp : startToPhase.keySet()) {
      if (previousPhase != null) {
        resultBuilder.add(previousPhase, new BazelPhaseDescription(previousTimestamp, timestamp));
      }
      previousTimestamp = timestamp;
      previousPhase = startToPhase.get(timestamp);
    }
    resultBuilder.add(
        BazelProfilePhase.FINISH, new BazelPhaseDescription(previousTimestamp, finishEnd));
    return resultBuilder.build();
  }
}
