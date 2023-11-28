/*
 * Copyright 2023 EngFlow Inc.
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

import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_GENERAL_INFORMATION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_LOCAL_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_EXECUTION_UPLOAD_TIME;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD_OUTPUTS;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.COMPLETE_SUBPROCESS_RUN;

import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;

public final class BazelEventsUtil {
  private BazelEventsUtil() {}

  /** The event indicates that an action was executed locally. */
  public static boolean indicatesLocalExecution(CompleteEvent event) {
    return CAT_LOCAL_ACTION_EXECUTION.equals(event.category)
        || CAT_GENERAL_INFORMATION.equals(event.category)
            && COMPLETE_SUBPROCESS_RUN.equals(event.name);
  }

  /** The event indicates that an action was executed remotely. */
  public static boolean indicatesRemoteExecution(CompleteEvent event) {
    return CAT_REMOTE_ACTION_EXECUTION.equals(event.category);
  }

  /**
   * The event documents a remote cache check. This includes cache checks against remote caches
   * configured by {@code --remote_cache} or {@code --disk_cache}.
   */
  public static boolean indicatesRemoteCacheCheck(CompleteEvent event) {
    return CAT_REMOTE_ACTION_CACHE_CHECK.equals(event.category);
  }

  /**
   * The event documents downloading outputs from a remote cache. This includes data downloaded
   * after an action was executed remotely.
   */
  public static boolean indicatesRemoteDownloadOutputs(CompleteEvent event) {
    return CAT_REMOTE_OUTPUT_DOWNLOAD.equals(event.category);
  }

  /** The event documents uploading outputs to a remote cache. */
  public static boolean indicatesRemoteUploadOutputs(CompleteEvent event) {
    // See
    // https://github.com/bazelbuild/bazel/blob/7d10999fc0357596824f2b6022bbbd895f245a3c/src/main/java/com/google/devtools/build/lib/remote/RemoteExecutionService.java#L1417
    // and
    // https://github.com/bazelbuild/bazel/blob/7d10999fc0357596824f2b6022bbbd895f245a3c/src/main/java/com/google/devtools/build/lib/remote/RemoteSpawnRunner.java#L359
    return CAT_REMOTE_EXECUTION_UPLOAD_TIME.equals(event.category)
        && (COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD_OUTPUTS.equals(event.name)
            || COMPLETE_REMOTE_EXECUTION_UPLOAD_TIME_UPLOAD.equals(event.name));
  }
}