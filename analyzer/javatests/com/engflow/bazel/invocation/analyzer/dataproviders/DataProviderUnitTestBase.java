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

package com.engflow.bazel.invocation.analyzer.dataproviders;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.engflow.bazel.invocation.analyzer.UnitTestBase;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import org.junit.Assert;
import org.junit.Test;

public abstract class DataProviderUnitTestBase extends UnitTestBase {
  protected DataProvider dataProvider;

  @Test
  public void shouldThrowMissingInputExceptionOnMissingInputs() throws Exception {
    assertNotNull(
        "dataProvider must be set by the subclass. Set super.dataProvider to an instance of the"
            + " DataProvider in a @Before method.",
        dataProvider);

    // Save the class type requested for later comparison. Must be wrapped in a final object to use
    // in a lambda.
    final var savedParameter =
        new Object() {
          Class classRequested;
        };

    // Throw MissingInputException when any Datum is requested
    when(dataManager.getDatum(any()))
        .thenAnswer(
            x -> {
              savedParameter.classRequested = x.getArgument(0);
              throw new MissingInputException(savedParameter.classRequested);
            });

    // Check all DatumSuppliers on the DataProvider
    for (DatumSupplierSpecification<?> datumProvider : dataProvider.getSuppliers()) {
      var ex =
          assertThrows(
              MissingInputException.class, () -> datumProvider.getDatumSupplier().supply());
      Assert.assertEquals(ex.getMissingInputClass(), savedParameter.classRequested);
    }
  }
}
