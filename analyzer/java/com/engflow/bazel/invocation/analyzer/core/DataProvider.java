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

import java.util.List;
import javax.annotation.Nullable;

/**
 * Provides groupings of analysis data to the {@link DataManager} that are more efficient to
 * calculate together. {@link DataProvider}s may consume other pieces of data from the DataManager
 * and perform calculation on them.
 */
public abstract class DataProvider {
  @Nullable private DataManager dataManager;

  /**
   * Registers the {@link DataProvider} with the supplied {@link DataManager} and saves it for use
   * by the {@link DatumSupplier}s.
   *
   * @param dataManager The DataManager that tracks this DataProvider and provides data from other
   *     DataProviders.
   * @throws DuplicateProviderException If a DataProvider that supplies the same data types is
   *     already registered with the {@link DataManager}.
   */
  public void register(DataManager dataManager) throws DuplicateProviderException {
    this.dataManager = dataManager;
    this.dataManager.registerProvider(this);
  }

  /**
   * Retrieve the {@link DataManager} this DataProvider is registered with.
   *
   * @return {@link DataManager} this DataProvider is registered with.
   * @throws IllegalStateException if called before {@link #register(DataManager) register} is
   *     called.
   */
  protected DataManager getDataManager() {
    if (dataManager == null) {
      throw new IllegalStateException(
          String.format(
              "%s tried to access DataManager before it was registered.",
              this.getClass().getName()));
    }
    return dataManager;
  }

  /**
   * Returns the specification of all the datum suppliers provided by this DataProvider.
   *
   * @return The list of supplier specifications that are calculated and provided by this
   *     DataProvider.
   */
  public abstract List<DatumSupplierSpecification<?>> getSuppliers();
}
