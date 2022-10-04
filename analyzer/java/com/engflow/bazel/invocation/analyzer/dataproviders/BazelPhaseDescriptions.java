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
import java.util.HashMap;
import java.util.Map;

/**
 * A mapping from {@link BazelProfilePhase} to its {@link BazelPhaseDescription} in the provided
 * Bazel profile.
 */
public class BazelPhaseDescriptions implements Datum {
  private final Map<BazelProfilePhase, BazelPhaseDescription> phaseToDescription = new HashMap<>();

  public void add(BazelProfilePhase phase, BazelPhaseDescription description) {
    phaseToDescription.put(phase, description);
  }

  public BazelPhaseDescription get(BazelProfilePhase phase) {
    return phaseToDescription.get(phase);
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
    // Remove the final newline
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }
}
