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

package com.engflow.bazel.invocation.analyzer.bazelprofile;

import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.dataproviders.BazelVersion;
import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CounterEvent;
import com.engflow.bazel.invocation.analyzer.traceeventformat.TraceEventFormatConstants;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

public class BazelProfile implements Datum {
  // Best effort to get somewhat good alignment when outputting a list of thread names.
  private static final int THREAD_NAME_MIN_OUTPUT_LENGTH = "\"Garbage Collector\"".length() + 1;

  @VisibleForTesting static final int MIN_BAZEL_MAJOR_VERSION_FOR_COUNTER_SERIES_IN_NULL_THREAD = 7;

  public static BazelProfile createFromPath(String path) throws IllegalArgumentException {
    File bazelProfileFile = new File(path);

    InputStream inputStream;
    try {
      inputStream = new FileInputStream(bazelProfileFile);
      if (path.endsWith(".gz")) {
        try {
          inputStream = new GZIPInputStream(inputStream);
        } catch (ZipException ex) {
          throw new IllegalArgumentException(
              String.format("bazel_profile is set to %s, but it could not be gunzipped.", path),
              ex);
        }
      }
    } catch (FileNotFoundException ex) {
      throw new IllegalArgumentException(
          String.format("bazel_profile is set to %s, which does not appear to be a file.", path),
          ex);
    } catch (IOException ex) {
      throw new IllegalArgumentException(
          String.format("Could not parse bazel_profile at %s.", path), ex);
    }

    return createFromInputStream(inputStream);
  }

  public static BazelProfile createFromInputStream(InputStream inputStream)
      throws IllegalArgumentException {
    JsonObject bazelProfile;
    try {
      bazelProfile = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
    } catch (IllegalStateException e) {
      throw new IllegalArgumentException("Could not parse Bazel profile.", e);
    }
    return new BazelProfile(bazelProfile);
  }

  private final Map<String, String> otherData = new HashMap<>();
  private final BazelVersion bazelVersion;
  private final Map<ThreadId, ProfileThread> threads = new HashMap<>();

  private BazelProfile(JsonObject profile) {
    if (!profile.has(TraceEventFormatConstants.SECTION_OTHER_DATA)
        || !profile.has(TraceEventFormatConstants.SECTION_TRACE_EVENTS)) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid profile, JSON file missing \"%s\" and/or \"%s\"",
              TraceEventFormatConstants.SECTION_OTHER_DATA,
              TraceEventFormatConstants.SECTION_TRACE_EVENTS));
    }
    try {
      profile
          .get(TraceEventFormatConstants.SECTION_OTHER_DATA)
          .getAsJsonObject()
          .entrySet()
          .forEach(entry -> otherData.put(entry.getKey(), entry.getValue().getAsString()));
      this.bazelVersion =
          BazelVersion.parse(otherData.get(BazelProfileConstants.OTHER_DATA_BAZEL_VERSION));

      profile
          .get(TraceEventFormatConstants.SECTION_TRACE_EVENTS)
          .getAsJsonArray()
          .forEach(
              element -> {
                JsonObject object = element.getAsJsonObject();
                int pid;
                try {
                  pid = object.get(TraceEventFormatConstants.EVENT_PROCESS_ID).getAsInt();
                } catch (Exception e) {
                  // Skip events that do not have a valid pid.
                  return;
                }
                Integer tid;
                try {
                  tid = object.get(TraceEventFormatConstants.EVENT_THREAD_ID).getAsInt();
                } catch (Exception e) {
                  // Collect all events without a tid into one thread.
                  tid = null;
                }
                ThreadId threadId = new ThreadId(pid, tid);
                ProfileThread profileThread =
                    threads.compute(
                        threadId,
                        (key, t) -> {
                          if (t == null) {
                            t = new ProfileThread(threadId);
                          }
                          return t;
                        });
                // TODO: Use success response to take action on errant events.
                profileThread.addEvent(object);
              });
    } catch (IllegalStateException e) {
      throw new IllegalArgumentException("Could not parse Bazel profile.", e);
    }

    if (!containsMainThread()) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid Bazel profile, JSON file missing \"%s\".",
              BazelProfileConstants.THREAD_MAIN));
    }

    if (!containsCounterSeriesThread()) {
      throw new IllegalArgumentException(
          "Invalid Bazel profile, JSON file missing counter series.");
    }
  }

  /**
   * This method is called from the constructor. Either it needs to stay private or it must be
   * declared final, so that it cannot be overridden.
   */
  private boolean containsMainThread() {
    return threads.values().stream().anyMatch(BazelProfile::isMainThread);
  }

  /**
   * This method is called from the constructor. Either it needs to stay private or it must be
   * declared final, so that it cannot be overridden.
   */
  private boolean containsCounterSeriesThread() {
    return threads.values().stream()
        .anyMatch(thread -> isCounterSeriesThread(bazelVersion, thread));
  }

  /**
   * Returns whether the passed-in thread looks like the main thread, both for newer and older
   * versions of Bazel. See
   * https://github.com/bazelbuild/bazel/commit/a03674e6297ed5f6f740889cba8780d7c4ffe05c for when
   * the naming of the main thread was changed.
   *
   * @param thread the thread to check
   * @return whether the thread looks like it is the main thread
   */
  @VisibleForTesting
  static boolean isMainThread(ProfileThread thread) {
    String name = thread.getName();
    if (Strings.isNullOrEmpty(name)) {
      return false;
    }
    return name.equals(BazelProfileConstants.THREAD_MAIN)
        || name.startsWith(BazelProfileConstants.THREAD_MAIN_OLD_PREFIX);
  }

  /**
   * Returns whether the passed-in thread looks like the garbage collection, both for newer and
   * older versions of Bazel. See
   * https://github.com/bazelbuild/bazel/commit/a03674e6297ed5f6f740889cba8780d7c4ffe05c for when
   * the naming of the garbage collection thread was changed.
   *
   * @param thread the thread to check
   * @return whether the thread looks like it is the garbage collection thread
   */
  @VisibleForTesting
  static boolean isGarbageCollectorThread(ProfileThread thread) {
    String name = thread.getName();
    if (Strings.isNullOrEmpty(name)) {
      return false;
    }
    return name.equals(BazelProfileConstants.THREAD_GARBAGE_COLLECTOR)
        || name.equals(BazelProfileConstants.THREAD_GARBAGE_COLLECTOR_OLD);
  }

  /**
   * Returns whether the passed-in thread looks like the thread that includes counter series, which
   * includes counters for data such as action count, CPU usage or memory usage.
   *
   * @param thread the thread to check
   * @return whether the thread looks like it includes Bazel's counter series
   */
  @VisibleForTesting
  static boolean isCounterSeriesThread(BazelVersion bazelVersion, ProfileThread thread) {
    if (!thread.getCounts().containsKey(BazelProfileConstants.COUNTER_ACTION_COUNT)
        && !thread.getCounts().containsKey(BazelProfileConstants.COUNTER_ACTION_COUNT_OLD)) {
      // Any counter series thread should at least include an action count event.
      return false;
    }
    if (bazelVersion.getMajor().isPresent()
        && bazelVersion.getMajor().get()
            >= MIN_BAZEL_MAJOR_VERSION_FOR_COUNTER_SERIES_IN_NULL_THREAD) {
      // As of https://github.com/bazelbuild/bazel/commit/610b56f50d98181ee5051f2644295e0fea43560a
      // counter series no longer include a `tid`.
      return thread.getThreadId().getThreadId() == null;
    } else {
      // Prior https://github.com/bazelbuild/bazel/commit/610b56f50d98181ee5051f2644295e0fea43560a
      // counter series were included in the main thread.
      return isMainThread(thread);
    }
  }

  /**
   * Returns whether the passed-in thread looks like the critical path thread.
   *
   * @param thread the thread to check
   * @return whether the thread looks like it is the critical path thread
   */
  private static boolean isCriticalPathThread(ProfileThread thread) {
    return BazelProfileConstants.THREAD_CRITICAL_PATH.equals(thread.getName());
  }

  public ImmutableMap<String, String> getOtherData() {
    return ImmutableMap.copyOf(otherData);
  }

  /**
   * For performance reasons, prefer getting the Datum {@link BazelVersion} provided by {@link
   * com.engflow.bazel.invocation.analyzer.dataproviders.BazelVersionDataProvider}, which memoizes
   * the value.
   *
   * @return the BazelVersion included in the profile, if any.
   */
  public BazelVersion getBazelVersion() {
    return bazelVersion;
  }

  public Stream<ProfileThread> getThreads() {
    return threads.values().stream();
  }

  public Optional<ProfileThread> getCriticalPath() {
    return threads.values().stream().filter(BazelProfile::isCriticalPathThread).findAny();
  }

  public ProfileThread getMainThread() {
    return threads.values().stream().filter(BazelProfile::isMainThread).findAny().get();
  }

  public Optional<ProfileThread> getGarbageCollectorThread() {
    return threads.values().stream().filter(BazelProfile::isGarbageCollectorThread).findAny();
  }

  public ProfileThread getCounterSeriesThread() {
    return threads.values().stream()
        .filter(thread -> isCounterSeriesThread(bazelVersion, thread))
        .findAny()
        .get();
  }

  public ImmutableList<CounterEvent> getActionCounts() {
    ProfileThread counterSeriesThread = getCounterSeriesThread();
    var actionCounts =
        counterSeriesThread.getCounts().get(BazelProfileConstants.COUNTER_ACTION_COUNT);
    if (actionCounts == null) {
      actionCounts =
          counterSeriesThread.getCounts().get(BazelProfileConstants.COUNTER_ACTION_COUNT_OLD);
    }
    return actionCounts;
  }

  /**
   * Registers a {@link DataProvider} with the supplied {@link DataManager} that supplies this
   * {@link BazelProfile}.
   *
   * @param dataManager The DataManager to register this BazelProfile with.
   * @throws DuplicateProviderException If another DataProvider has already been registered with the
   *     supplied DataManager that supplies a Bazel Profile.
   */
  public void registerWithDataManager(DataManager dataManager) throws DuplicateProviderException {
    BazelProfile profile = this;

    var provider =
        new DataProvider() {
          @Override
          public List<DatumSupplierSpecification<?>> getSuppliers() {
            return ImmutableList.of(
                DatumSupplierSpecification.of(BazelProfile.class, () -> profile));
          }
        };

    provider.register(dataManager);
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public String getEmptyReason() {
    return null;
  }

  @Override
  public String getDescription() {
    return "The profile written by Bazel.";
  }

  private void appendThreadSummary(StringBuilder sb, ProfileThread profileThread) {
    appendThreadSummary(
        sb,
        String.format("\"%s\"", profileThread.getName()),
        profileThread.getCompleteEvents().size(),
        profileThread.getCounts().size(),
        profileThread.getInstants().size(),
        profileThread.getExtraEvents().size());
  }

  private void appendThreadSummary(
      StringBuilder sb,
      String threadName,
      long completeEvents,
      long counts,
      long instants,
      long extraEvents) {
    sb.append(Strings.padEnd(threadName, THREAD_NAME_MIN_OUTPUT_LENGTH, ' '));
    if (completeEvents > 0) {
      sb.append("\tCompleteEvents: ");
      sb.append(completeEvents);
    }
    if (counts > 0) {
      sb.append("\tCounts: ");
      sb.append(counts);
    }
    if (instants > 0) {
      sb.append("\tInstants: ");
      sb.append(instants);
    }
    if (extraEvents > 0) {
      sb.append("\tExtra: ");
      sb.append(extraEvents);
    }
    sb.append("\n");
  }

  @Override
  public String getSummary() {
    StringBuilder sb = new StringBuilder();
    sb.append("Bazel version:\n");
    sb.append(getBazelVersion().toString());
    sb.append("\n\nThreads:\n");
    appendThreadSummary(sb, getMainThread());
    Optional<ProfileThread> optionalCriticalPath = getCriticalPath();
    if (optionalCriticalPath.isPresent()) {
      appendThreadSummary(sb, optionalCriticalPath.get());
    }
    Optional<ProfileThread> optionalGarbageCollector = getGarbageCollectorThread();
    if (optionalGarbageCollector.isPresent()) {
      appendThreadSummary(sb, optionalGarbageCollector.get());
    }
    AtomicLong completeEvents = new AtomicLong();
    AtomicLong counts = new AtomicLong();
    AtomicLong instants = new AtomicLong();
    AtomicLong extraEvents = new AtomicLong();
    getThreads()
        .filter(
            thread ->
                !isGarbageCollectorThread(thread)
                    && !isMainThread(thread)
                    && !isCriticalPathThread(thread))
        .forEach(
            (thread) -> {
              if (!thread.getCompleteEvents().isEmpty()) {
                completeEvents.getAndAdd(thread.getCompleteEvents().size());
              }
              if (!thread.getCounts().isEmpty()) {
                counts.getAndAdd(thread.getCounts().size());
              }
              if (!thread.getInstants().isEmpty()) {
                instants.getAndAdd(thread.getInstants().size());
              }
              if (!thread.getExtraEvents().isEmpty()) {
                extraEvents.getAndAdd(thread.getExtraEvents().size());
              }
            });
    appendThreadSummary(
        sb,
        "Other (aggregated)",
        completeEvents.get(),
        counts.get(),
        instants.get(),
        extraEvents.get());
    sb.append("\n");

    if (optionalCriticalPath.isPresent()
        && !optionalCriticalPath.get().getCompleteEvents().isEmpty()) {
      String durationHeading = "Duration";
      Integer maxFormattedDurationLength =
          optionalCriticalPath.get().getCompleteEvents().stream()
              .map(event -> DurationUtil.formatDuration(event.duration).length())
              .max(Integer::compareTo)
              .orElse(0)
              .intValue();
      int durationWidth = Math.max(maxFormattedDurationLength, durationHeading.length());
      String format = "%" + durationWidth + "s\t%s";
      sb.append("Critical Path:\n");
      sb.append(String.format(format, durationHeading, "Description"));
      String entryFormat = "\n" + format;
      optionalCriticalPath.get().getCompleteEvents().stream()
          .forEach(
              event -> {
                sb.append(
                    String.format(
                        entryFormat, DurationUtil.formatDuration(event.duration), event.name));
              });
    }
    return sb.toString();
  }
}
