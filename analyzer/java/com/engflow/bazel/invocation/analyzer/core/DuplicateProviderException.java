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
 * Thrown when a {@link DataProvider} that provides a specific type of datum is registered to a
 * {@link DataManager} that already has a DataProvider registered that provides the same type of
 * datum.
 */
public class DuplicateProviderException extends Exception {
  private static final long serialVersionUID = 1L;

  private final Class<? extends Datum> datumType;
  private final Class<? extends DataProvider> existing;
  private final Class<? extends DataProvider> duplicate;

  public DuplicateProviderException(
      Class<? extends Datum> datumType,
      Class<? extends DataProvider> existing,
      Class<? extends DataProvider> duplicate) {
    this.datumType = datumType;
    this.existing = existing;
    this.duplicate = duplicate;
  }

  @Override
  public String getMessage() {
    return String.format(
        "Duplicate providers found for type \"%s\": \"%s\" already registered and trying to add"
            + " \"%s\"!",
        datumType.getName(), existing.getName(), duplicate.getName());
  }
}
