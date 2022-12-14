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
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link DataProvider} that supplies the duration spent queuing for remote execution within the
 * critical path. For this, the sum over all queuing within critical path actions is computed.
 */
public class CriticalPathQueuingDurationDataProvider extends DataProvider {
  private static final Pattern CRITICAL_PATH_TO_EVENT_NAME = Pattern.compile("^action '(.*)'$");
  public static final String EMPTY_REASON =
      "The Bazel profile does not include a critical path, which is required for determining"
          + " whether it has queuing. Try analyzing a profile that processes actions, for example a"
          + " build or test.";

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            CriticalPathQueuingDuration.class,
            DatumSupplier.memoized(this::getCriticalPathQueuingDuration)));
  }

  @VisibleForTesting
  CriticalPathQueuingDuration getCriticalPathQueuingDuration()
      throws InvalidProfileException, MissingInputException, NullDatumException {
    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
    // For each event in critical path, first find the matching event by searching for
    // the relevant name, and further filtering by time interval.
    // Given the matching event, find a queuing event with the same tid and pid that fits
    // within the time interval.
    Set<CompleteEvent> criticalPathEventsInThreads = new HashSet<>();
    if (bazelProfile.getCriticalPath().isEmpty()) {
      return new CriticalPathQueuingDuration(EMPTY_REASON);
    }
    bazelProfile
        .getCriticalPath()
        .get()
        .getCompleteEvents()
        .forEach(
            (criticalPathEvent) -> {
              Matcher m = CRITICAL_PATH_TO_EVENT_NAME.matcher(criticalPathEvent.name);
              if (m.matches()) {
                String eventNameToFind = m.group(1);
                bazelProfile
                    .getThreads()
                    .flatMap((thread) -> thread.getCompleteEvents().stream())
                    // Name should match, and event interval should be contained in
                    // criticalPathEvent interval.
                    .filter(
                        (event) ->
                            BazelProfileConstants.CAT_ACTION_PROCESSING.equals(event.category)
                                && eventNameToFind.equals(event.name)
                                // If "action processing" is the first event, the timestamp
                                // may be slightly out of sync with the critical path event.
                                && (criticalPathEvent.start.almostEquals(event.start)
                                    ||
                                    // It may not be the first event, e.g.
                                    // "action dependency checking" may be reported before
                                    criticalPathEvent.start.compareTo(event.start) > 0)
                                // Keep this always-true-condition for documentation purposes!
                                // We have found cases where the end time of the critical path
                                // event is less than the end time of the processing event.
                                // This might be a bug / inconsistency in Bazel profile writing.
                                && (true
                                    || criticalPathEvent.end.almostEquals(event.end)
                                    || criticalPathEvent.end.compareTo(event.end) > 0))
                    // We expect to find just one event, but this may not be true for more
                    // generic action names. Sort all thus far matching events to find the best
                    // match.
                    .sorted(
                        (a, b) -> {
                          boolean aWithinBounds =
                              criticalPathEvent.end.almostEquals(a.end)
                                  || criticalPathEvent.end.compareTo(a.end) > 0;
                          boolean bWithinBounds =
                              criticalPathEvent.end.almostEquals(b.end)
                                  || criticalPathEvent.end.compareTo(b.end) > 0;
                          if (aWithinBounds && bWithinBounds) {
                            // Both events within bounds, prefer the longer one.
                            return b.duration.compareTo(a.duration);
                          }
                          // If one of the events is within the bounds, prefer it.
                          if (aWithinBounds) {
                            return -1;
                          }
                          if (bWithinBounds) {
                            return 1;
                          }
                          // Neither event within bounds, prefer the one that extends the bounds
                          // least.
                          return a.end.compareTo(b.end);
                        })
                    .limit(1)
                    .forEach(
                        e -> {
                          // As we could not check the end boundary above, adjust the duration here,
                          // so that we can ensure queuing events do not exceed the boundaries of
                          // the critical path entry.
                          Timestamp end =
                              Timestamp.ofMicros(
                                  Math.min(e.end.getMicros(), criticalPathEvent.end.getMicros()));
                          criticalPathEventsInThreads.add(
                              new CompleteEvent(
                                  e.name,
                                  e.category,
                                  e.start,
                                  TimeUtil.getDurationBetween(e.start, end),
                                  e.threadId,
                                  e.processId,
                                  e.args));
                        });
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
                                    && (event.end.almostEquals(cpEvent.end)
                                        || (event.end.compareTo(cpEvent.end) <= 0))))
            .map((event) -> event.duration)
            .reduce(Duration.ZERO, Duration::plus);
    return new CriticalPathQueuingDuration(duration);
  }
}
