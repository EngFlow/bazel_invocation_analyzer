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

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.regex.Pattern;

/** A {@link DataProvider} that supplies whether the Bazel profile includes merged events. */
public class MergedEventsPresentDataProvider extends DataProvider {
  private static final Pattern MERGED_EVENTS_PATTERN = Pattern.compile("^merged\\s\\d+\\sevents$");

  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(
            MergedEventsPresent.class, DatumSupplier.memoized(this::getMergedEventsPresent)));
  }

  @VisibleForTesting
  MergedEventsPresent getMergedEventsPresent()
      throws MissingInputException, InvalidProfileException {
    BazelProfile profile = getDataManager().getDatum(BazelProfile.class);
    return new MergedEventsPresent(
        profile
            .getThreads()
            .flatMap((thread) -> thread.getCompleteEvents().stream())
            .anyMatch((event) -> MERGED_EVENTS_PATTERN.matcher(event.name).matches()));
  }
}
