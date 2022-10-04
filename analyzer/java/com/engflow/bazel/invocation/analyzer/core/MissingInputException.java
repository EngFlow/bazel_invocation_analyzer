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

/**
 * Thrown when an entity requests a type of datum from a {@link DataManager} that has no registered
 * {@link DataProvider}s that supply such data.
 */
public class MissingInputException extends Exception {
  private static final long serialVersionUID = 1L;

  private final Class<? extends Datum> missingInputClass;

  public MissingInputException(Class<? extends Datum> missingInputClass) {
    this.missingInputClass = missingInputClass;
  }

  @Override
  public String getMessage() {
    return String.format(
        "Missing data provider for class \"%s\". Please register a DataProvider that supplies this"
            + " type with the DataManager.",
        missingInputClass.getName());
  }

  public Class<? extends Datum> getMissingInputClass() {
    return missingInputClass;
  }
}
