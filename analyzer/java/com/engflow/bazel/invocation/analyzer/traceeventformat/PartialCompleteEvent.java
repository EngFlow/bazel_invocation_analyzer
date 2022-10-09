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

import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.google.common.base.Preconditions;
import java.time.Duration;

/** A {@link CompleteEvent}, which is potentially cropped in the beginning and/or end. */
public class PartialCompleteEvent {
  public final CompleteEvent completeEvent;
  public final Timestamp croppedStart;
  public final Duration croppedDuration;
  public final Timestamp croppedEnd;

  public PartialCompleteEvent(
      CompleteEvent completeEvent, Timestamp croppedStart, Timestamp croppedEnd) {
    // Ensure that cropping does not extend the interval of the contained CompleteEvent.
    Preconditions.checkArgument(completeEvent.start.compareTo(croppedStart) <= 0);
    Preconditions.checkArgument(completeEvent.end.compareTo(croppedEnd) >= 0);
    this.completeEvent = completeEvent;
    this.croppedStart = croppedStart;
    this.croppedEnd = croppedEnd;
    this.croppedDuration = TimeUtil.getDurationBetween(croppedStart, croppedEnd);
  }

  /**
   * Returns whether the event is cropped. The even is cropped, if the timing information of this
   * event and the contained {@llink CompleteEvent} do not match.
   *
   * @return Whether the event is cropped.
   */
  public boolean isCropped() {
    return completeEvent.start.compareTo(croppedStart) != 0
        || completeEvent.end.compareTo(croppedEnd) != 0;
  }
}
