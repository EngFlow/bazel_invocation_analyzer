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

import static com.engflow.bazel.invocation.analyzer.core.DatumSupplier.memoized;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CounterEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** A {@link DataProvider} that supplies data on action counts, including bottleneck statistics. */
public class ActionStatsDataProvider extends DataProvider {
  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(ActionStats.class, memoized(this::getActionStats)));
  }

  public ActionStats getActionStats() throws MissingInputException, InvalidProfileException {
    BazelProfile bazelProfile = getDataManager().getDatum(BazelProfile.class);
    var actionCounts =
        bazelProfile.getMainThread().getCounts().get(BazelProfileConstants.COUNTER_ACTION_COUNT);
    if (actionCounts == null) {
      return new ActionStats(List.of());
    }

    var coreCount = getDataManager().getDatum(EstimatedCoresUsed.class).getEstimatedCores();

    List<Bottleneck.Builder> bottlenecks = new ArrayList<>();
    Bottleneck.Builder currentBottleneck = null;

    for (CounterEvent actionCount : actionCounts) {
      boolean isBottleneck = actionCount.getTotalValue() < coreCount;
      boolean bottleneckInProgress = currentBottleneck != null;

      if (bottleneckInProgress) {
        if (isBottleneck) {
          currentBottleneck.addActionCountSample(actionCount.getTotalValue());
          currentBottleneck.setEnd(actionCount.getTimestamp());
        } else {
          bottlenecks.add(currentBottleneck);
          currentBottleneck = null;
        }
      } else {
        if (isBottleneck) {
          currentBottleneck = Bottleneck.newBuilder(actionCount.getTimestamp());
          currentBottleneck.addActionCountSample(actionCount.getTotalValue());
        }
      }
    }

    if (currentBottleneck != null) {
      bottlenecks.add(currentBottleneck);
    }

    bazelProfile
        .getThreads()
        .flatMap(profileThread -> profileThread.getCompleteEvents().stream())
        .filter(event -> BazelProfileConstants.CAT_ACTION_PROCESSING.equals(event.category))
        .forEach(
            event ->
                bottlenecks.forEach(
                    bottleneck -> {
                      // Has this event started after the bottleneck?
                      if (event.start.compareTo(bottleneck.getEnd()) > 0) {
                        return;
                      }
                      // Has this event ended before the bottleneck?
                      if (event.end.compareTo(bottleneck.getStart()) < 0) {
                        return;
                      }
                      bottleneck.addEvent(event);
                    }));

    return new ActionStats(bottlenecks.stream().map(b -> b.build()).collect(Collectors.toList()));
  }
}
