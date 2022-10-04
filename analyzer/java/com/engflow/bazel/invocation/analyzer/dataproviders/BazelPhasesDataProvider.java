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
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.InstantEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
      throws MissingInputException, InvalidProfileException {
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
                "Unable to find instant event event named \"%s\".",
                BazelProfileConstants.INSTANT_FINISHING),
            ex);
      }
    }
  }

  @VisibleForTesting
  TotalDuration getTotalDuration() throws MissingInputException, InvalidProfileException {
    determineStartAndEndTimestamps();
    return new TotalDuration(TimeUtil.getDurationBetween(launchStart, finishEnd));
  }

  @VisibleForTesting
  BazelPhaseDescriptions getBazelPhaseDescriptions()
      throws MissingInputException, InvalidProfileException {
    determineStartAndEndTimestamps();

    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);

    Map<BazelProfilePhase, Timestamp> phaseToStart = new HashMap<>();
    phaseToStart.put(BazelProfilePhase.LAUNCH, launchStart);

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

    buildPhases.stream()
        .forEach(
            (action) -> {
              String phaseName = action.getName();
              try {
                BazelProfilePhase phase = BazelProfilePhase.parse(phaseName);
                phaseToStart.put(phase, action.getTimestamp());
              } catch (IllegalArgumentException e) {
                logger.warning(String.format("Found unrecognized Bazel phase %s", phaseName));
              }
            });

    if (phaseToStart.size() != BazelProfilePhase.values().length) {
      throw new InvalidProfileException(
          String.format(
              "Missing build phases: %d/%d found.",
              phaseToStart.size(), BazelProfilePhase.values().length));
    }

    BazelPhaseDescriptions result = new BazelPhaseDescriptions();
    result.add(
        BazelProfilePhase.LAUNCH,
        new BazelPhaseDescription(
            phaseToStart.get(BazelProfilePhase.LAUNCH), phaseToStart.get(BazelProfilePhase.INIT)));
    result.add(
        BazelProfilePhase.INIT,
        new BazelPhaseDescription(
            phaseToStart.get(BazelProfilePhase.INIT),
            phaseToStart.get(BazelProfilePhase.EVALUATE)));
    result.add(
        BazelProfilePhase.EVALUATE,
        new BazelPhaseDescription(
            phaseToStart.get(BazelProfilePhase.EVALUATE),
            phaseToStart.get(BazelProfilePhase.DEPENDENCIES)));
    result.add(
        BazelProfilePhase.DEPENDENCIES,
        new BazelPhaseDescription(
            phaseToStart.get(BazelProfilePhase.DEPENDENCIES),
            phaseToStart.get(BazelProfilePhase.PREPARE)));
    result.add(
        BazelProfilePhase.PREPARE,
        new BazelPhaseDescription(
            phaseToStart.get(BazelProfilePhase.PREPARE),
            phaseToStart.get(BazelProfilePhase.EXECUTE)));
    result.add(
        BazelProfilePhase.EXECUTE,
        new BazelPhaseDescription(
            phaseToStart.get(BazelProfilePhase.EXECUTE),
            phaseToStart.get(BazelProfilePhase.FINISH)));
    result.add(
        BazelProfilePhase.FINISH,
        new BazelPhaseDescription(phaseToStart.get(BazelProfilePhase.FINISH), finishEnd));
    return result;
  }
}
