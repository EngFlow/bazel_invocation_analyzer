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
 * Thrown when an entity requests a type of {@link Datum} from a {@link DataManager} and null is
 * supplied by the registered {@link DataProvider}.
 */
public class NullDatumException extends Exception {
  private static final long serialVersionUID = 1L;

  private final Class<? extends Datum> datumClass;

  public NullDatumException(Class<? extends Datum> datumClass) {
    this.datumClass = datumClass;
  }

  @Override
  public String getMessage() {
    return String.format(
        "The DataProvider registered with the DataManager for supplying \"%s\" supplied null.",
        datumClass.getName());
  }

  public Class<? extends Datum> getDatumClass() {
    return datumClass;
  }
}
