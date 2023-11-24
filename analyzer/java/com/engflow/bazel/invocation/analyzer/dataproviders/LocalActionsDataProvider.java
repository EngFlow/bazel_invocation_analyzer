package com.engflow.bazel.invocation.analyzer.dataproviders;

import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_EXECUTION_UPLOAD_TIME;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.ProfileThread.ofCategoryTypes;

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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalActionsDataProvider extends DataProvider {
  private static final Set<String> RELATED_EVENTS_CATEGORIES =
      Set.of(
          CAT_REMOTE_ACTION_CACHE_CHECK,
          CAT_REMOTE_OUTPUT_DOWNLOAD,
          CAT_REMOTE_EXECUTION_UPLOAD_TIME);

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
    return RELATED_EVENTS_CATEGORIES.contains(event.category)
        || LocalActions.LocalAction.indicatesLocalExecution(event);
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
                action.threadId == relatedEvents.peek().threadId);
            related.add(relatedEvents.next());
          }
          out.accept(new LocalAction(action, related));
        });
    return out.build();
  }
}
