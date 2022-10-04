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
 * Describes a supplier of a single class of data.
 *
 * <p>The main function of this class is to safely couple the Java class object to the supplier
 * function that produces the same class type. This is necessary because suppliers are keyed based
 * off of their class types at runtime. Due to Java type erasure these types are not retained at
 * runtime so they must be explicitly set in the code.
 *
 * @param <DatumType> The type of datum supplied.
 */
public class DatumSupplierSpecification<DatumType extends Datum> {
  private final Class<DatumType> clazz;
  private final DatumSupplier<DatumType> datumSupplier;

  /**
   * Creates a new specification.
   *
   * @param clazz The return class of the datumSupplier.
   * @param datumSupplier A supplier that calculates and returns a piece of data.
   * @param <T> The type supplied by datumSupplier.
   * @return A new {@link DatumSupplierSpecification} with a type-safe class object and supplier
   *     function.
   */
  public static <T extends Datum> DatumSupplierSpecification<T> of(
      Class<T> clazz, DatumSupplier<T> datumSupplier) {
    return new DatumSupplierSpecification<>(clazz, datumSupplier);
  }

  private DatumSupplierSpecification(
      Class<DatumType> clazz, DatumSupplier<DatumType> datumSupplier) {
    this.clazz = clazz;
    this.datumSupplier = datumSupplier;
  }

  public Class<DatumType> getSupplierOutputClass() {
    return clazz;
  }

  public DatumSupplier<DatumType> getDatumSupplier() {
    return datumSupplier;
  }
}
