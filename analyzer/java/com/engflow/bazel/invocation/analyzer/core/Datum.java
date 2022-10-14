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

package com.engflow.bazel.invocation.analyzer.core;

/** Base class for all datum elements returned by {@link DataProvider}s */
public interface Datum {

  /**
   * Check whether this datum includes any data. It may be that the input sources do not include the
   * data required for providing information.
   *
   * @return Whether the datum includes no data.
   */
  boolean isEmpty();

  /**
   * If the datum {@link #isEmpty}, retrieve a reason explaining why it is empty.
   *
   * @return The reason why the datum is empty, or null if non-empty.
   */
  String getEmptyReason();

  /**
   * Get a description of the kind of data this datum provides. This description should be
   * independent of the data, i.e. static.
   *
   * @return A description of this datum for display to the user.
   */
  String getDescription();

  /**
   * Get a summary of the actual data this datum provides. May return null if the datum is empty.
   *
   * @return A summary of this datum for display to the user, or null.
   */
  String getSummary();
}
