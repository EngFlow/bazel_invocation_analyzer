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

package com.engflow.bazel.invocation.analyzer.traceeventformat;

/**
 * Declares constants found in the Trace Event Format "specification" found at
 * https://docs.google.com/document/d/1CvAClvFfyA5R-PhYUmn5OOQtYMH4h6I0nSsKchNAySU/preview#heading=h.yr4qxyxotyw
 *
 * <p>This list is not comprehensive and only lists the constants necessary to parse Trace Event
 * Format profiles produced by the Bazel build system.
 */
public class TraceEventFormatConstants {
  public static final String SECTION_OTHER_DATA = "otherData";
  public static final String SECTION_TRACE_EVENTS = "traceEvents";

  public static final String EVENT_ARGUMENTS = "args";
  public static final String EVENT_CATEGORY = "cat";
  public static final String EVENT_DURATION = "dur";
  public static final String EVENT_NAME = "name";
  public static final String EVENT_PHASE = "ph";
  public static final String EVENT_PROCESS_ID = "pid";
  public static final String EVENT_THREAD_ID = "tid";
  public static final String EVENT_TIMESTAMP = "ts";

  public static final String PHASE_COMPLETE = "X";
  public static final String PHASE_COUNTER = "C";
  public static final String PHASE_INSTANT = "i";
  public static final String PHASE_METADATA = "M";

  public static final String METADATA_THREAD_NAME = "thread_name";
  public static final String METADATA_THREAD_SORT_INDEX = "thread_sort_index";
}
