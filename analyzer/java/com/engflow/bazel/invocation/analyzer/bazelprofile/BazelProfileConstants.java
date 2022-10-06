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

/** Constants that are specific to Bazel profiles. */
public class BazelProfileConstants {
  // Thread names: event.name == METADATA_THREAD_NAME && event.args.name == constant below
  public static final String THREAD_CRITICAL_PATH = "Critical Path";
  public static final String THREAD_GARBAGE_COLLECTOR = "Garbage Collector";
  public static final String THREAD_MAIN = "Main Thread";

  // CounterEvent names
  public static final String COUNTER_ACTION_COUNT = "action count";

  // Category names
  public static final String CAT_ACTION_PROCESSING = "action processing";
  public static final String CAT_BUILD_PHASE_MARKER = "build phase marker";
  public static final String CAT_GARBAGE_COLLECTION = "gc notification";
  public static final String CAT_GENERAL_INFORMATION = "general information";
  public static final String CAT_REMOTE_ACTION_CACHE_CHECK = "remote action cache check";
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

  // InstantEvent names
  public static final String INSTANT_FINISHING = "Finishing";
}
