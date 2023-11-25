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

import com.google.common.annotations.VisibleForTesting;

/** Constants that are specific to Bazel profiles. */
public class BazelProfileConstants {
  // Thread names: event.name == METADATA_THREAD_NAME && event.args.name == constant below
  // These constants should not be used outside this package.
  @VisibleForTesting public static final String THREAD_CRITICAL_PATH = "Critical Path";

  /**
   * Deprecated.
   *
   * <p>The thread that contains GC events has been renamed multiple times, so it's discouraged to
   * use it to filter for GC events. Instead, match the event category against {@link
   * #CAT_GARBAGE_COLLECTION}.
   *
   * <p>Used thread names over time include "Service Thread", "Garbage Collector" and "Notification
   * Thread". E.g. see
   * https://github.com/bazelbuild/bazel/commit/a03674e6297ed5f6f740889cba8780d7c4ffe05c
   */
  @Deprecated @VisibleForTesting
  public static final String THREAD_GARBAGE_COLLECTOR = "Garbage Collector";

  /**
   * Deprecated.
   *
   * <p>The thread that contains GC events has been renamed multiple times, so it's discouraged to
   * use it to filter for GC events. Instead, match the event category against {@link
   * #CAT_GARBAGE_COLLECTION}.
   *
   * <p>Used thread names over time include "Service Thread", "Garbage Collector" and "Notification
   * Thread". E.g. see
   * https://github.com/bazelbuild/bazel/commit/a03674e6297ed5f6f740889cba8780d7c4ffe05c
   */
  @Deprecated static final String THREAD_GARBAGE_COLLECTOR_OLD = "Service Thread";

  @VisibleForTesting public static final String THREAD_MAIN = "Main Thread";
  // See https://github.com/bazelbuild/bazel/commit/a03674e6297ed5f6f740889cba8780d7c4ffe05c
  static final String THREAD_MAIN_OLD_PREFIX = "grpc-command";

  // CounterEvent names
  // These constants should not be used outside this package.
  @VisibleForTesting public static final String COUNTER_ACTION_COUNT = "action count";
  // See
  // https://github.com/bazelbuild/bazel/commit/ec2eda1b56a5197ee2d019f58d89a68b17974b13#diff-f8db96cce91c612e82faa11be7a835199fd31777cf6bf7ce39a069e140a199b2
  static final String COUNTER_ACTION_COUNT_OLD = "action counters";

  // Category names
  // See
  // https://github.com/bazelbuild/bazel/blob/master/src/main/java/com/google/devtools/build/lib/profiler/ProfilerTask.java#L23-L101
  public static final String CAT_ACTION_PROCESSING = "action processing";
  public static final String CAT_BUILD_PHASE_MARKER = "build phase marker";
  public static final String CAT_CRITICAL_PATH_COMPONENT = "critical path component";
  public static final String CAT_GARBAGE_COLLECTION = "gc notification";
  public static final String CAT_GENERAL_INFORMATION = "general information";

  public static final String CAT_LOCAL_ACTION_EXECUTION = "local action execution";
  public static final String CAT_REMOTE_ACTION_CACHE_CHECK = "remote action cache check";
  public static final String CAT_REMOTE_ACTION_EXECUTION = "remote action execution";
  public static final String CAT_REMOTE_OUTPUT_DOWNLOAD = "remote output download";
  public static final String CAT_REMOTE_EXECUTION_PROCESS_WALL_TIME =
      "Remote execution process wall time";
  public static final String CAT_REMOTE_EXECUTION_QUEUING_TIME = "Remote execution queuing time";
  public static final String CAT_REMOTE_EXECUTION_SETUP = "Remote execution setup";

  /**
   * This event does not necessarily imply remote execution was enabled. For example, it is also
   * reported when setting the Bazel flag `--disk_cache`.
   */
  public static final String CAT_REMOTE_EXECUTION_UPLOAD_TIME = "Remote execution upload time";

  // CompleteEvent names
  public static final String COMPLETE_MAJOR_GARBAGE_COLLECTION = "major GC";
  public static final String COMPLETE_MINOR_GARBAGE_COLLECTION = "minor GC";

  // One of the events written when an action is executed locally, see
  // https://github.com/bazelbuild/bazel/blob/36614cf29867c7ca86ae44b60ac2d92dcf03f204/src/main/java/com/google/devtools/build/lib/sandbox/AbstractSandboxSpawnRunner.java#L147
  public static final String COMPLETE_SUBPROCESS_RUN = "subprocess.run";
  public static final String COMPLETE_EXECUTE_REMOTELY = "execute remotely";

  // https://github.com/bazelbuild/bazel/blob/7d10999fc0357596824f2b6022bbbd895f245a3c/src/main/java/com/google/devtools/build/lib/remote/RemoteRepositoryRemoteExecutor.java#L160
  /**
   * For complete events of category {@link #CAT_REMOTE_EXECUTION_UPLOAD_TIME}, this name indicates
   * that missing inputs were uploaded to a remote execution service.
   */
  public static final String COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD_MISSING_INPUTS =
      "upload missing inputs";

  // https://github.com/bazelbuild/bazel/blob/7d10999fc0357596824f2b6022bbbd895f245a3c/src/main/java/com/google/devtools/build/lib/remote/RemoteExecutionService.java#L1417
  /**
   * For complete events of category {@link #CAT_REMOTE_EXECUTION_UPLOAD_TIME}, this name indicates
   * that outputs were uploaded to a remote cache. This includes local "uploads" when using
   * `disk_cache`.
   */
  public static final String COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD_OUTPUTS =
      "upload outputs";

  // https://github.com/bazelbuild/bazel/blob/7d10999fc0357596824f2b6022bbbd895f245a3c/src/main/java/com/google/devtools/build/lib/remote/RemoteSpawnRunner.java#L359
  /**
   * For complete events of category {@link #CAT_REMOTE_EXECUTION_UPLOAD_TIME}, this name indicates
   * that outputs were uploaded to a remote cache while using a remote execution service.
   */
  public static final String COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD = "upload";

  // InstantEvent names
  public static final String INSTANT_FINISHING = "Finishing";

  // otherData key names
  // See
  // https://github.com/bazelbuild/bazel/blob/aab19f75cd383c4b09a6ae720f9fa436bf89d271/src/main/java/com/google/devtools/build/lib/profiler/JsonTraceFileWriter.java#L179-L183
  public static final String OTHER_DATA_BAZEL_VERSION = "bazel_version";
}
