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
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

public class BazelProfile implements Datum {
  // Best effort to get somewhat good alignment when outputting a list of thread names.
  private static final int THREAD_NAME_MIN_OUTPUT_LENGTH =
      "\"skyframe-evaluator-cpu-heavy-12345\"".length();

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

      profile
          .get(TraceEventFormatConstants.SECTION_TRACE_EVENTS)
          .getAsJsonArray()
          .forEach(
              element -> {
                JsonObject object = element.getAsJsonObject();
                int pid;
                int tid;
                try {
                  pid = object.get(TraceEventFormatConstants.EVENT_PROCESS_ID).getAsInt();
                  tid = object.get(TraceEventFormatConstants.EVENT_THREAD_ID).getAsInt();
                } catch (Exception e) {
                  // Skip events that do not have a valid pid or tid.
                  return;
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
  }

  /**
   * This method is called from the constructor. Either it needs to stay private or it must be
   * declared final, so that it cannot be overridden.
   */
  private boolean containsMainThread() {
    return threads.values().stream().anyMatch(BazelProfile::isMainThread);
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

  public ImmutableMap<String, String> getOtherData() {
    return ImmutableMap.copyOf(otherData);
  }

  public Stream<ProfileThread> getThreads() {
    return threads.values().stream();
  }

  public Optional<ProfileThread> getCriticalPath() {
    return threads.values().stream()
        .filter(t -> BazelProfileConstants.THREAD_CRITICAL_PATH.equals(t.getName()))
        .findAny();
  }

  public ProfileThread getMainThread() {
    return threads.values().stream().filter(BazelProfile::isMainThread).findAny().get();
  }

  public Optional<ProfileThread> getGarbageCollectorThread() {
    return threads.values().stream().filter(BazelProfile::isGarbageCollectorThread).findAny();
  }

  public Optional<ImmutableList<CounterEvent>> getActionCounts() {
    var actionCounts = getMainThread().getCounts().get(BazelProfileConstants.COUNTER_ACTION_COUNT);
    if (actionCounts == null) {
      actionCounts =
          getMainThread().getCounts().get(BazelProfileConstants.COUNTER_ACTION_COUNT_OLD);
    }
    return Optional.ofNullable(actionCounts);
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

  @Override
  public String getSummary() {
    StringBuilder sb = new StringBuilder();
    sb.append("Threads:\n");
    getThreads()
        .map(
            (thread) -> {
              StringBuilder threadSb = new StringBuilder();
              threadSb.append(
                  Strings.padEnd(
                      String.format("\"%s\"", thread.getName()),
                      THREAD_NAME_MIN_OUTPUT_LENGTH,
                      ' '));
              if (!thread.getCompleteEvents().isEmpty()) {
                threadSb.append("\tCompleteEvents: ");
                threadSb.append(thread.getCompleteEvents().size());
              }
              if (!thread.getCounts().isEmpty()) {
                threadSb.append("\tCounts: ");
                threadSb.append(thread.getCounts().size());
              }
              if (!thread.getInstants().isEmpty()) {
                threadSb.append("\tInstants: ");
                threadSb.append(thread.getInstants().size());
              }
              if (!thread.getExtraEvents().isEmpty()) {
                threadSb.append("\tExtra: ");
                threadSb.append(thread.getExtraEvents().size());
              }
              threadSb.append("\n");
              return threadSb.toString();
            })
        .sorted()
        .forEach(s -> sb.append(s));
    sb.append("\n");
    Optional<ProfileThread> criticalPath = getCriticalPath();
    if (criticalPath.isPresent() && !criticalPath.get().getCompleteEvents().isEmpty()) {
      String durationHeading = "Duration";
      Integer maxFormattedDurationLength =
          criticalPath.get().getCompleteEvents().stream()
              .map(event -> DurationUtil.formatDuration(event.duration).length())
              .max(Integer::compareTo)
              .orElse(0)
              .intValue();
      int durationWidth = Math.max(maxFormattedDurationLength, durationHeading.length());
      String format = "%" + durationWidth + "s\t%s";
      sb.append("Critical Path:\n");
      sb.append(String.format(format, durationHeading, "Description"));
      String entryFormat = "\n" + format;
      criticalPath.get().getCompleteEvents().stream()
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
