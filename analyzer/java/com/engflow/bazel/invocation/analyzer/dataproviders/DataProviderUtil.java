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

import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.CriticalPathQueuingDurationDataProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.QueuingObservedDataProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteCachingUsedDataProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionUsedDataProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.TotalQueuingDurationDataProvider;
import java.util.List;

public class DataProviderUtil {
  /**
   * Convenience method for retrieving all available {@link DataProvider}s. When adding a new
   * DataProvider, also add it to the list returned by this method.
   *
   * @return The list of all available {@link DataProvider}s.
   */
  public static List<DataProvider> getAllDataProviders() {
    return List.of(
        new ActionStatsDataProvider(),
        new BazelPhasesDataProvider(),
        new CriticalPathDurationDataProvider(),
        new EstimatedCoresDataProvider(),
        new GarbageCollectionStatsDataProvider(),
        new MergedEventsPresentDataProvider(),
        new LocalActionsDataProvider(),

        // RemoteExecution
        new CriticalPathQueuingDurationDataProvider(),
        new QueuingObservedDataProvider(),
        new RemoteCachingUsedDataProvider(),
        new RemoteExecutionUsedDataProvider(),
        new TotalQueuingDurationDataProvider(),

        // RemoteCache
        new RemoteCacheMetricsDataProvider());
  }
}
