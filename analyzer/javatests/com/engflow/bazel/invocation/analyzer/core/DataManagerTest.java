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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.engflow.bazel.invocation.analyzer.core.TestDatum.CharDatum;
import com.engflow.bazel.invocation.analyzer.core.TestDatum.DoubleDatum;
import com.engflow.bazel.invocation.analyzer.core.TestDatum.IntegerDatum;
import com.engflow.bazel.invocation.analyzer.core.TestDatum.StringDatum;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Test;

public class DataManagerTest {

  @Test
  public void shouldFetchRegisteredDataProvider() throws Exception {
    Character c = (char) (new Random().nextInt(26) + 'a');
    var dataManager = new DataManager();
    var dataProvider = new CharDataProvider(c);
    dataProvider.register(dataManager);

    assertThat(dataManager.getDatum(CharDatum.class).getMyChar()).isEqualTo(c);
  }

  @Test
  public void shouldThrowIfUnknownProvider() {
    var dataManager = new DataManager();

    var ex = assertThrows(MissingInputException.class, () -> dataManager.getDatum(CharDatum.class));

    assertThat(ex)
        .hasMessageThat()
        .isEqualTo(
            "Missing data provider for class"
                + " \"com.engflow.bazel.invocation.analyzer.core.TestDatum$CharDatum\". Please"
                + " register a DataProvider that supplies this type with the DataManager.");
  }

  @Test
  public void shouldThrowOnDuplicateProvider() throws Exception {
    var dataManager = new DataManager();
    new StringDataProvider().register(dataManager);

    var ex =
        assertThrows(
            DuplicateProviderException.class,
            () -> dataManager.registerProvider(new DuplicateStringDataProvider()));

    assertThat(ex)
        .hasMessageThat()
        .isEqualTo(
            "Duplicate providers found for type"
                + " \"com.engflow.bazel.invocation.analyzer.core.TestDatum$StringDatum\":"
                + " \"com.engflow.bazel.invocation.analyzer.core.DataManagerTest$StringDataProvider\""
                + " already registered and trying to add"
                + " \"com.engflow.bazel.invocation.analyzer.core.DataManagerTest$DuplicateStringDataProvider\"!");
  }

  @Test
  public void shouldThrowMissingDataExceptionWhenInputIsMissing() throws Exception {
    var dataManager = new DataManager();
    new StringDataProvider().register(dataManager);

    var ex =
        assertThrows(MissingInputException.class, () -> dataManager.getDatum(StringDatum.class));
    assertThat(ex)
        .hasMessageThat()
        .isEqualTo(
            "Missing data provider for class"
                + " \"com.engflow.bazel.invocation.analyzer.core.TestDatum$CharDatum\". Please"
                + " register a DataProvider that supplies this type with the DataManager.");
  }

  @Test
  public void shouldThrowMissingDataExceptionWhenNotRegistered() throws Exception {
    var dataManager = new DataManager();

    var ex =
        assertThrows(MissingInputException.class, () -> dataManager.getDatum(StringDatum.class));
    assertThat(ex)
        .hasMessageThat()
        .isEqualTo(
            "Missing data provider for class"
                + " \"com.engflow.bazel.invocation.analyzer.core.TestDatum$StringDatum\". Please"
                + " register a DataProvider that supplies this type with the DataManager.");
  }

  @Test
  public void shouldThrowNullDatumExceptionWhenSupplyingNull() throws Exception {
    var dataManager = new DataManager();
    new CharDataProvider(null).register(dataManager);

    var ex = assertThrows(NullDatumException.class, () -> dataManager.getDatum(CharDatum.class));
    assertThat(ex)
        .hasMessageThat()
        .isEqualTo(
            "The DataProvider registered with the DataManager for supplying"
                + " \"com.engflow.bazel.invocation.analyzer.core.TestDatum$CharDatum\" supplied"
                + " null.");
  }

  @Test
  public void shouldAllowDataProvidersToCallOneAnother() throws Exception {
    var dataManager = new DataManager();
    CharDataProvider charDataProvider = new CharDataProvider('a');
    charDataProvider.register(dataManager);
    new StringDataProvider().register(dataManager);

    StringDatum data = dataManager.getDatum(StringDatum.class);

    assertThat(data.getMyString())
        .isEqualTo(String.valueOf(charDataProvider.getReturnedChar().getMyChar()));
  }

  @Test
  public void getAllDataByProvider() throws Exception {
    var dataManager = new DataManager();
    var charDataProvider = new CharDataProvider('a');
    charDataProvider.register(dataManager);
    var numericDataProvider = new NumericDataProvider();
    numericDataProvider.register(dataManager);

    var results = dataManager.getAllDataByProvider();

    var expected =
        new HashMap<Class<? extends DataProvider>, Map<Class<? extends Datum>, Datum>>() {
          {
            put(
                NumericDataProvider.class,
                new HashMap<>() {
                  {
                    put(IntegerDatum.class, numericDataProvider.returnedInt);
                    put(DoubleDatum.class, numericDataProvider.returnedDouble);
                  }
                });
            put(
                CharDataProvider.class,
                new HashMap<>() {
                  {
                    put(CharDatum.class, charDataProvider.returnedChar);
                  }
                });
          }
        };
    assertThat(results).isEqualTo(expected);
  }

  @Test
  public void getUsedDataByProvider() throws Exception {
    var dataManager = new DataManager();
    var charDataProvider = new CharDataProvider('a');
    charDataProvider.register(dataManager);
    var numericDataProvider = new NumericDataProvider();
    numericDataProvider.register(dataManager);

    var results = dataManager.getUsedDataByProvider();

    assertThat(results).isEmpty();

    dataManager.getDatum(IntegerDatum.class);

    results = dataManager.getUsedDataByProvider();
    var expected =
        new HashMap<Class<? extends DataProvider>, Map<Class<?>, Object>>() {
          {
            put(
                NumericDataProvider.class,
                new HashMap<>() {
                  {
                    put(IntegerDatum.class, numericDataProvider.returnedInt);
                  }
                });
          }
        };
    assertThat(results).isEqualTo(expected);

    dataManager.getDatum(CharDatum.class);

    results = dataManager.getUsedDataByProvider();
    expected.put(
        CharDataProvider.class,
        new HashMap<>() {
          {
            put(CharDatum.class, charDataProvider.returnedChar);
          }
        });
    assertThat(results).isEqualTo(expected);
  }

  private static class CharDataProvider extends DataProvider {
    @Nullable private final CharDatum returnedChar;

    public CharDataProvider(Character c) {
      this.returnedChar = c == null ? null : new CharDatum(c);
    }

    @Override
    public List<DatumSupplierSpecification<?>> getSuppliers() {
      return ImmutableList.of(
          DatumSupplierSpecification.of(CharDatum.class, () -> getReturnedChar()));
    }

    public CharDatum getReturnedChar() {
      return returnedChar;
    }
  }

  private static class StringDataProvider extends DataProvider {
    @Override
    public List<DatumSupplierSpecification<?>> getSuppliers() {
      return ImmutableList.of(
          DatumSupplierSpecification.of(
              StringDatum.class,
              () ->
                  new StringDatum(
                      String.valueOf(getDataManager().getDatum(CharDatum.class).getMyChar()))));
    }
  }

  private static class DuplicateStringDataProvider extends DataProvider {
    @Override
    public List<DatumSupplierSpecification<?>> getSuppliers() {
      var str = new StringDatum("foobar");
      return ImmutableList.of(DatumSupplierSpecification.of(StringDatum.class, () -> str));
    }
  }

  private static class NumericDataProvider extends DataProvider {
    private final IntegerDatum returnedInt;
    private final DoubleDatum returnedDouble;

    public NumericDataProvider() {
      var random = new Random();
      this.returnedInt = new IntegerDatum(random.nextInt());
      this.returnedDouble = new DoubleDatum(random.nextDouble());
    }

    @Override
    public List<DatumSupplierSpecification<?>> getSuppliers() {
      return ImmutableList.of(
          DatumSupplierSpecification.of(IntegerDatum.class, this::getReturnedInt),
          DatumSupplierSpecification.of(DoubleDatum.class, this::getReturnedDouble));
    }

    public IntegerDatum getReturnedInt() {
      return returnedInt;
    }

    public DoubleDatum getReturnedDouble() {
      return returnedDouble;
    }
  }
}
