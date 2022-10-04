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

package com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link DataProvider} that supplies the duration spent queuing for remote execution within the
 * critical path. For this, the sum over all queuing within critical path actions is computed.
 */
public class CriticalPathQueuingDurationDataProvider extends DataProvider {
  private static final Pattern CRITICAL_PATH_TO_EVENT_NAME = Pattern.compile("^action '(.*)'$");
  // When matching critical path events to events in the actual threads, timestamps and durations
  // do not match up entirely. Accept a difference of up to 1ms.
  private static final int ACCEPTABLE_DIVERGENCE_IN_MICROS = 1000;

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            CriticalPathQueuingDuration.class,
            DatumSupplier.memoized(this::getCriticalPathQueuingDuration)));
  }

  @VisibleForTesting
  CriticalPathQueuingDuration getCriticalPathQueuingDuration()
      throws MissingInputException, InvalidProfileException {
    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
    // For each event in critical path, first find the matching event by searching for
    // the relevant name, and further filtering by time interval.
    // Given the matching event, find a queuing event with the same tid and pid that fits
    // within the time interval.
    Set<CompleteEvent> criticalPathEventsInThreads = new HashSet<>();
    bazelProfile
        .getCriticalPath()
        .getCompleteEvents()
        .forEach(
            (criticalPathEvent) -> {
              Matcher m = CRITICAL_PATH_TO_EVENT_NAME.matcher(criticalPathEvent.name);
              if (m.matches()) {
                String eventNameToFind = m.group(1);
                Optional<CompleteEvent> matchingEvent =
                    bazelProfile
                        .getThreads()
                        .flatMap((thread) -> thread.getCompleteEvents().stream())
                        // Name should match, and event interval should be contained in
                        // criticalPathEvent interval.
                        .filter(
                            (event) ->
                                eventNameToFind.equals(event.name)
                                    && criticalPathEvent.start.almostEquals(event.start)
                                    && criticalPathEvent.end.almostEquals(event.end))
                        // If multiple matches are found, choose the one that matches the timestamp
                        // best.
                        .min(
                            Comparator.comparing(
                                e ->
                                    TimeUtil.getDurationBetween(criticalPathEvent.start, e.start)));
                if (matchingEvent.isPresent()) {
                  criticalPathEventsInThreads.add(matchingEvent.get());
                }
              }
            });
    Duration duration =
        bazelProfile
            .getThreads()
            .flatMap((thread) -> thread.getCompleteEvents().stream())
            // Restrict to queuing events.
            .filter(
                (event) ->
                    BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME.equals(event.category))
            // Restrict to events that are contained in one of the critical path events.
            .filter(
                (event) ->
                    criticalPathEventsInThreads.stream()
                        .anyMatch(
                            (cpEvent) ->
                                cpEvent.threadId == event.threadId
                                    && cpEvent.processId == event.processId
                                    && (cpEvent.start.compareTo(event.start) <= 0)
                                    && (event.end.compareTo(cpEvent.end) <= 0)))
            .map((event) -> event.duration)
            .reduce(Duration.ZERO, Duration::plus);
    return new CriticalPathQueuingDuration(duration);
  }
}
