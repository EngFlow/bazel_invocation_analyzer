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
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

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
    HashMultimap<PidTidKey, CompleteEvent> criticalPathEventsInThreads = HashMultimap.create();
    if (bazelProfile.getCriticalPath().isEmpty()) {
      return new CriticalPathQueuingDuration(EMPTY_REASON);
    }
    /*
     * Key: critical action path event name.
     * Value: the same critical action path, but in event form.
     * Used to efficiently look up critical path events by their parsed name.
     */
    HashMap<String, CompleteEvent> cPathActionNameToSelfEvent = new HashMap<>();
    /*
     * Key: critical action path event name.
     * Values: all profile 'action processing' events that matched by name with the critical path
     *  counterpart.
     * Used to keep track, for each critical path event, all the candidates that could be used to
     * find its real process and thread IDs.
     */
    HashMultimap<String, CompleteEvent> cPathActionNameToEventCandidates = HashMultimap.create();
    /*
     * Key: PidTidKey (process id and thread id in record form) of the remote queuing event
     * Value: The remote queuing event
     * This is used to efficiently match between a critical path event with the remote queuing
     * event, by the process and thread IDs.
     */
    HashMultimap<PidTidKey, CompleteEvent> remoteQueuingEvents = HashMultimap.create();

    for (var cPathEvent : bazelProfile.getCriticalPath().get().getCompleteEvents()) {
      if (Strings.isNullOrEmpty(cPathEvent.name)) {
        continue;
      }
      Matcher m = CRITICAL_PATH_TO_EVENT_NAME.matcher(cPathEvent.name);
      if (!m.matches()) {
        continue;
      }
      var eventNameToFind = m.group(1);
      cPathActionNameToSelfEvent.put(eventNameToFind, cPathEvent);
    }

    // This loop is extremely expensive. Make sure we only perform it once!
    bazelProfile
        .getThreads()
        .forEach(
            thread -> {
              for (var event : thread.getCompleteEvents()) {
                if (Strings.isNullOrEmpty(event.category)) {
                  continue;
                }
                switch (event.category) {
                  case BazelProfileConstants.CAT_ACTION_PROCESSING -> {
                    var cPathEvent = cPathActionNameToSelfEvent.get(event.name);
                    if (cPathEvent == null) {
                      continue;
                    }
                    // Found an event that matches a critical path event by name. Add it to the
                    // critical
                    // path event's list of candidates for later.
                    cPathActionNameToEventCandidates.put(event.name, event);
                  }
                  case BazelProfileConstants.CAT_REMOTE_EXECUTION_QUEUING_TIME -> {
                    // We'll need to iterate through these later to sum the queuing time. Keep track
                    // of this
                    // to avoid having to iterate through the full profile again.
                    remoteQueuingEvents.put(new PidTidKey(event.processId, event.threadId), event);
                  }
                  default -> {}
                }
              }
            });

    // For each critical path event, loop through its list of candidates that matched by name.
    // This is used to get the correct thread ID and process ID that will later be matched up to
    // the remote queuing events, to calculate critical path queuing.
    for (var cPathEventName : cPathActionNameToEventCandidates.keySet()) {
      var cPathEvent = cPathActionNameToSelfEvent.get(cPathEventName);
      if (cPathEvent == null) {
        continue;
      }

      @Nullable CompleteEvent found = null;
      var foundWithinBounds = false;
      for (CompleteEvent event : cPathActionNameToEventCandidates.get(cPathEventName)) {
        // If "action processing" is the first event, the timestamp
        // may be slightly out of sync with the critical path event.
        //
        // It may not be the first event, e.g.
        // "action dependency checking" may be reported before
        if (!cPathEvent.start.almostEquals(event.start)
            && cPathEvent.start.compareTo(event.start) <= 0) {
          continue;
        }
        // Keep this always-false-condition for documentation purposes!
        // We have found cases where the end time of the critical path event is less than the end
        // time of the processing event. This might be a bug / inconsistency in Bazel profile
        // writing.
        if (false
            && (!cPathEvent.end.almostEquals(event.end)
                && cPathEvent.end.compareTo(event.end) <= 0)) {
          continue;
        }

        if (found == null) {
          found = event;
          foundWithinBounds =
              cPathEvent.end.almostEquals(found.end) || cPathEvent.end.compareTo(found.end) > 0;
          continue;
        }

        // We expect to find just one event, but this may not be true for more generic action
        // names. Sort all thus far matching events to find the best match.
        var eventWithinBounds =
            cPathEvent.end.almostEquals(event.end) || cPathEvent.end.compareTo(event.end) > 0;

        if (foundWithinBounds && eventWithinBounds) {
          // Both events within bounds, prefer the longer one.
          if (event.duration.compareTo(found.duration) > 0) {
            found = event;
          }
          continue;
        }
        // If one of the events is within the bounds, prefer it.
        if (eventWithinBounds) {
          found = event;
          foundWithinBounds = true;
          continue;
        }
        // Neither event within bounds, prefer the one that extends the bounds
        // least.
        if (found.end.compareTo(event.end) < 0) {
          found = event;
          foundWithinBounds = false;
        }
      }
      if (found != null) {
        // As we could not check the end boundary above, adjust the duration here,
        // so that we can ensure queuing events do not exceed the boundaries of
        // the critical path entry.
        Timestamp end =
            Timestamp.ofMicros(Math.min(found.end.getMicros(), cPathEvent.end.getMicros()));
        criticalPathEventsInThreads.put(
            new PidTidKey(found.processId, found.threadId),
            new CompleteEvent(
                found.name,
                found.category,
                found.start,
                TimeUtil.getDurationBetween(found.start, end),
                found.threadId,
                found.processId,
                found.args));
      }
    }

    Duration duration = Duration.ZERO;
    for (var remoteQueuingEventEntry : remoteQueuingEvents.entries()) {
      var remoteQueuingEvent = remoteQueuingEventEntry.getValue();
      boolean found = false;
      for (var criticalPathEventInThread :
          criticalPathEventsInThreads.get(remoteQueuingEventEntry.getKey())) {
        if (criticalPathEventInThread.start.compareTo(remoteQueuingEvent.start) <= 0
            && (remoteQueuingEvent.end.almostEquals(criticalPathEventInThread.end)
                || (remoteQueuingEvent.end.compareTo(criticalPathEventInThread.end) <= 0))) {
          found = true;
          break;
        }
      }
      if (found) {
        duration = duration.plus(remoteQueuingEvent.duration);
      }
    }
    return new CriticalPathQueuingDuration(duration);
  }

  private record PidTidKey(int pid, int tid) {}
}
