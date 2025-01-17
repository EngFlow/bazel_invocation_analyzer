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

import static com.engflow.bazel.invocation.analyzer.bazelprofile.ProfileThread.ofCategoryTypes;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelEventsUtil;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.bazelprofile.ProfileThread;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplier;
import com.engflow.bazel.invocation.analyzer.core.DatumSupplierSpecification;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions.LocalAction;
import com.engflow.bazel.invocation.analyzer.time.Range;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalActionsDataProvider extends DataProvider {
  @Override
  public List<DatumSupplierSpecification<?>> getSuppliers() {
    return List.of(
        DatumSupplierSpecification.of(LocalActions.class, DatumSupplier.memoized(this::derive)));
  }

  @VisibleForTesting
  LocalActions derive() throws InvalidProfileException, MissingInputException, NullDatumException {
    return LocalActions.create(
        getDataManager()
            .getDatum(BazelProfile.class)
            .getThreads()
            .parallel()
            .flatMap(this::coalesce)
            .collect(Collectors.toList()));
  }

  private static boolean isRelatedEvent(CompleteEvent event) {
    return BazelEventsUtil.indicatesRemoteCacheCheck(event)
        || BazelEventsUtil.indicatesRemoteUploadOutputs(event)
        || BazelEventsUtil.indicatesRemoteDownloadOutputs(event)
        || BazelEventsUtil.indicatesLocalExecution(event)
        || BazelEventsUtil.indicatesRemoteExecution(event);
  }

  Stream<LocalAction> coalesce(ProfileThread thread) {
    var localActionEvents =
        ofCategoryTypes(thread.getCompleteEvents(), BazelProfileConstants.CAT_ACTION_PROCESSING);
    var relatedEvents =
        Iterators.peekingIterator(
            thread.getCompleteEvents().stream().filter(event -> isRelatedEvent(event)).iterator());

    var out = Stream.<LocalAction>builder();

    localActionEvents.forEachRemaining(
        action -> {
          var range = Range.between(action.start, action.end);
          var related = new ArrayList<CompleteEvent>();
          while (relatedEvents.hasNext()) {
            // events are sorted by start time, so when an event doesn't fit, break
            if (!range.contains(relatedEvents.peek().start, relatedEvents.peek().end)) {
              break;
            }
            Preconditions.checkArgument(
                action.threadId == relatedEvents.peek().threadId,
                "Thread ids must match %s != %s",
                action.threadId, relatedEvents.peek().threadId);
            related.add(relatedEvents.next());
          }
          out.accept(new LocalAction(action, related));
        });
    return out.build();
  }
}
