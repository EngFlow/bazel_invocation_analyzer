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

import javax.annotation.Nullable;

/**
 * A Function that produces a specific type of data.
 *
 * <p>This is distinct form the {@link java.util.function.Supplier} interface as it must throw an
 * exception.
 *
 * @param <DatumType> The type of the data returned.
 */
@FunctionalInterface
public interface DatumSupplier<DatumType extends Datum> {
  /**
   * Calculates and returns a type of data.
   *
   * <p>Note: This method MUST be thread-safe. Consider using `synchronized`.
   *
   * @return The calculated data.
   * @throws MissingInputException If the required input data cannot be fetched from the {@link
   *     DataManager}.
   * @throws InvalidProfileException If the required input data cannot be parsed from the Bazel
   *     profile.
   */
  DatumType supply() throws MissingInputException, InvalidProfileException;

  /**
   * Returns a memoized version of a {@link DatumSupplier}.
   *
   * <p>The supplier will only ever be called once and a cached value will be returned for every
   * call after the first.
   *
   * @param supplier The supplier that performs a calculation and returns the result.
   * @param <T> The return type of the supplier.
   * @return A DatumSupplier of the same signature as the input supplier that memoizes the internal
   *     supplier.
   */
  static <T extends Datum> DatumSupplier<T> memoized(DatumSupplier<T> supplier) {
    return new DatumSupplier<>() {
      @Nullable private T cachedOutput;

      @Override
      public T supply() throws MissingInputException, InvalidProfileException {
        // If we already have the value return early.
        if (cachedOutput != null) {
          return cachedOutput;
        } else {
          synchronized (supplier) {
            // Need to check again to make sure someone didn't set it while we were waiting on the
            // `supplier` lock.
            if (cachedOutput == null) {
              cachedOutput = supplier.supply();
            }
            return cachedOutput;
          }
        }
      }
    };
  }
}
