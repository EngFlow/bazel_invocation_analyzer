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
import com.engflow.bazel.invocation.analyzer.traceeventformat.TraceEventFormatConstants;
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
    JsonObject bazelProfile =
        JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
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
    if (!containsMainThread()) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid Bazel profile, JSON file missing \"%s\".",
              BazelProfileConstants.THREAD_MAIN));
    }
  }

  /* This method is final, as it is called from the constructor and must not be overridden. */
  private final boolean containsMainThread() {
    return threads.values().stream().anyMatch(BazelProfileConstants::isMainThread);
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
    return threads.values().stream().filter(BazelProfileConstants::isMainThread).findAny().get();
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
