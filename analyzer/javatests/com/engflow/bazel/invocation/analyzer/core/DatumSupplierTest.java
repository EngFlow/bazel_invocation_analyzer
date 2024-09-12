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

import com.engflow.bazel.invocation.analyzer.core.TestDatum.IntegerDatum;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class DatumSupplierTest {

  @Test
  public void shouldMemoize() throws Exception {
    AtomicInteger callCount = new AtomicInteger();

    DatumSupplier<IntegerDatum> supplier =
        DatumSupplier.memoized(() -> new IntegerDatum(callCount.incrementAndGet()));

    assertThat(callCount.get()).isEqualTo(0);
    assertThat(supplier.supply().getMyInt()).isEqualTo(1);
    assertThat(callCount.get()).isEqualTo(1);
    assertThat(supplier.supply().getMyInt()).isEqualTo(1);
    assertThat(callCount.get()).isEqualTo(1);
  }

  @Test(timeout = 1_000)
  public void memoizationIsThreadSafe() throws Exception {
    AtomicInteger callCount = new AtomicInteger();
    AtomicInteger retrievalCount = new AtomicInteger();

    DatumSupplier<IntegerDatum> supplier =
        DatumSupplier.memoized(() -> new IntegerDatum(callCount.incrementAndGet()));

    ExecutorService executorService = Executors.newFixedThreadPool(100);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(100);
    for (int i = 0; i < 100; ++i) {
      var unused =
          executorService.submit(
              () -> {
                try {
                  startLatch.await();
                  supplier.supply();
                  retrievalCount.getAndIncrement();
                  endLatch.countDown();
                } catch (Throwable e) {
                  throw new IllegalStateException(e);
                }
              });
    }
    startLatch.countDown();
    endLatch.await();
    executorService.shutdown();

    assertThat(supplier.supply().getMyInt()).isEqualTo(1);
    assertThat(callCount.get()).isEqualTo(1);
    assertThat(retrievalCount.get()).isEqualTo(100);
  }
}
