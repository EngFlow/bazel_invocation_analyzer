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

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;

/**
 * A mapping from {@link BazelProfilePhase} to its {@link BazelPhaseDescription} in the provided
 * Bazel profile.
 */
public class BazelPhaseDescriptions implements Datum {
  private final ImmutableMap<BazelProfilePhase, BazelPhaseDescription> phaseToDescription;

  private BazelPhaseDescriptions(Map<BazelProfilePhase, BazelPhaseDescription> phaseToDescription) {
    this.phaseToDescription = ImmutableMap.copyOf(phaseToDescription);
  }

  public static BazelPhaseDescriptions.Builder newBuilder() {
    return new BazelPhaseDescriptions.Builder();
  }

  public BazelPhaseDescription get(BazelProfilePhase phase) {
    return phaseToDescription.get(phase);
  }

  public boolean has(BazelProfilePhase phase) {
    return phaseToDescription.containsKey(phase);
  }

  public BazelPhaseDescription getOrClosestBefore(BazelProfilePhase phase) {
    if (phaseToDescription.containsKey(phase)) {
      return phaseToDescription.get(phase);
    }
    while (phase != BazelProfilePhase.LAUNCH) {
      phase = phase.getPrevious();
      if (phaseToDescription.containsKey(phase)) {
        return phaseToDescription.get(phase);
      }
    }
    return null;
  }

  public BazelPhaseDescription getOrClosestAfter(BazelProfilePhase phase) {
    if (phaseToDescription.containsKey(phase)) {
      return phaseToDescription.get(phase);
    }
    while (phase != BazelProfilePhase.FINISH) {
      phase = phase.getNext();
      if (phaseToDescription.containsKey(phase)) {
        return phaseToDescription.get(phase);
      }
    }
    return null;
  }

  @Override
  public String getDescription() {
    return "The Bazel Profile's various phases and their timing information.";
  }

  @Override
  public String getSummary() {
    var sb = new StringBuilder();
    String durationHeading = "Duration";
    String timestampHeading = "Timestamp (μs)";
    int durationWidth = durationHeading.length();
    int timestampWidth = timestampHeading.length();
    for (var phase : BazelProfilePhase.values()) {
      BazelPhaseDescription desc = phaseToDescription.get(phase);
      if (desc != null) {
        durationWidth =
            Math.max(durationWidth, DurationUtil.formatDuration(desc.getDuration()).length());
        timestampWidth =
            Math.max(timestampWidth, String.valueOf(desc.getStart().getMicros()).length());
      }
    }

    String format = "%" + durationWidth + "s\t%" + timestampWidth + "s\t%s";
    sb.append(String.format(format, durationHeading, timestampHeading, "Description"));
    String entryFormat = "\n" + format;
    for (var phase : BazelProfilePhase.values()) {
      BazelPhaseDescription desc = phaseToDescription.get(phase);
      if (desc != null) {
        sb.append(
            String.format(
                entryFormat,
                DurationUtil.formatDuration(desc.getDuration()),
                desc.getStart().getMicros(),
                phase.name));
      }
    }
    return sb.toString();
  }

  public static class Builder {
    private final Map<BazelProfilePhase, BazelPhaseDescription> phaseToDescription;

    private Builder() {
      this.phaseToDescription = new HashMap<>();
    }

    public Builder add(BazelProfilePhase phase, BazelPhaseDescription description) {
      phaseToDescription.put(phase, description);
      return this;
    }

    public BazelPhaseDescriptions build() {
      return new BazelPhaseDescriptions(phaseToDescription);
    }
  }
}
