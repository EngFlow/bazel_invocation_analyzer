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

/** Thrown when a data expected to be in a Bazel profile is missing or in an unexpected format. */
public class InvalidProfileException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Constructor with exception
   *
   * @param errorMessage Message detailing what was found to be missing or invalid
   * @param err Error encountered while parsing
   */
  public InvalidProfileException(String errorMessage, Throwable err) {
    super("This does not appear to be a valid Bazel profile. " + errorMessage, err);
  }

  /**
   * Constructor with no exception
   *
   * @param errorMessage Message detailing what was found to be missing or invalid
   */
  public InvalidProfileException(String errorMessage) {
    super("This does not appear to be a valid Bazel profile. " + errorMessage);
  }
}
