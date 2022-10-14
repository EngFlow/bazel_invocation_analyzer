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

import java.util.HashMap;
import java.util.Map;

/** Tracks all registered {@link DataProvider}s and maps their output types to */
public class DataManager {
  private final Map<Class<? extends Datum>, DatumSupplierEntry<?>> suppliers = new HashMap<>();
  private final Map<Class<? extends Datum>, DatumSupplierEntry<?>> usedSuppliers = new HashMap<>();

  /**
   * Tracks a {@link DataProvider} and all data associated with its {@link DatumSupplier}s.
   *
   * <p>This should only be called by the abstract DataProvider and need not be called otherwise.
   *
   * @param dataProvider The DataProvider whose {@link DatumSupplier}s to track.
   * @throws DuplicateProviderException If a DataProvider that supplies the same output type has
   *     already been registered.
   */
  public void registerProvider(DataProvider dataProvider) throws DuplicateProviderException {
    for (var entry : dataProvider.getSuppliers()) {
      addSupplier(dataProvider.getClass(), entry);
    }
  }

  /**
   * Returns a piece of data by type calculated by the registered {@link DataProvider}s.
   *
   * @param clazz The class type of the datum to return.
   * @param <DatumType> The type of the datum to return.
   * @return The datum that was calculated or pre-cached.
   * @throws MissingInputException If no {@link DataProvider} has been registered that supplies such
   *     data.
   */
  public <DatumType extends Datum> DatumType getDatum(Class<DatumType> clazz)
      throws InvalidProfileException, MissingInputException, NullDatumException {
    var entry = suppliers.get(clazz);
    if (entry == null) {
      throw new MissingInputException(clazz);
    }

    Object datum = entry.supplier.supply();
    if (datum == null) {
      throw new NullDatumException(entry.dataProviderClass, clazz);
    }
    if (!clazz.equals(datum.getClass())) {
      throw new IllegalStateException(
          String.format(
              "The provider \"%s\" registered a DatumProvider that claims to produce a \"%s\" but"
                  + " actually produces a \"%s\"!",
              entry.dataProviderClass.getName(), clazz.getName(), datum.getClass().getName()));
    }

    usedSuppliers.put(clazz, entry);

    // Note: This cast is safe due to the check above.
    return (DatumType) datum;
  }

  /**
   * Returns all available data organized by provider name, then by class name.
   *
   * <p>Note that any unavailable data is excluded from results.
   *
   * @return All available datum entries organized by provider name, then by class name
   */
  public Map<Class<? extends DataProvider>, Map<Class<? extends Datum>, Datum>>
      getAllDataByProvider() {
    return organizeByProvider(suppliers);
  }

  /**
   * Returns data which has been retrieved organized by provider name, then by class name.
   *
   * @return All previously requested datum entries organized by provider name, then by class name
   */
  public Map<Class<? extends DataProvider>, Map<Class<? extends Datum>, Datum>>
      getUsedDataByProvider() {
    return organizeByProvider(usedSuppliers);
  }

  private Map<Class<? extends DataProvider>, Map<Class<? extends Datum>, Datum>> organizeByProvider(
      Map<Class<? extends Datum>, DatumSupplierEntry<?>> source) {
    var result = new HashMap<Class<? extends DataProvider>, Map<Class<? extends Datum>, Datum>>();
    source.forEach(
        (clazz, supplierEntry) -> {
          try {
            var byProvider = result.get(supplierEntry.dataProviderClass);
            // Do this before adding any missing provider entries in case it fails
            var obj = getDatum(clazz);
            // Now add the provider, if it is new
            if (byProvider == null) {
              byProvider = new HashMap<>();
              result.put(supplierEntry.dataProviderClass, byProvider);
            }
            byProvider.put(clazz, obj);
          }
          // Ignore results with errors
          catch (Exception ex) {
          }
        });
    return result;
  }

  private <DatumType extends Datum> void addSupplier(
      Class<? extends DataProvider> dataProviderClass,
      DatumSupplierSpecification<DatumType> supplierSpec)
      throws DuplicateProviderException {
    Class<DatumType> datumType = supplierSpec.getSupplierOutputClass();
    var existing = suppliers.get(datumType);
    if (existing != null) {
      throw new DuplicateProviderException(
          datumType, existing.dataProviderClass, dataProviderClass);
    }

    suppliers.put(
        datumType, new DatumSupplierEntry<>(dataProviderClass, supplierSpec.getDatumSupplier()));
  }

  private static class DatumSupplierEntry<DatumType extends Datum> {
    public final Class<? extends DataProvider> dataProviderClass;
    public final DatumSupplier<DatumType> supplier;

    public DatumSupplierEntry(
        Class<? extends DataProvider> dataProviderClass, DatumSupplier<DatumType> supplier) {
      this.dataProviderClass = dataProviderClass;
      this.supplier = supplier;
    }
  }
}
