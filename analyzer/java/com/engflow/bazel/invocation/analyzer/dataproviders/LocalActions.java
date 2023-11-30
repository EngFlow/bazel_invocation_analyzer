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

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelEventsUtil;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions.LocalAction;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** Organizes events into {@link LocalAction} by category and time period */
public class LocalActions implements Datum, Iterable<LocalAction> {
  @VisibleForTesting
  static final Set<String> INTERNAL_ACTION_MNEMONICS =
      ImmutableSet.of(
          BazelProfileConstants.MNEMONIC_BAZEL_WORKSPACE_STATUS_ACTION,
          BazelProfileConstants.MNEMONIC_EXECUTABLE_SYMLINK,
          BazelProfileConstants.MNEMONIC_SYMLINK);

  private final ImmutableList<LocalAction> actions;

  private LocalActions(List<LocalAction> actions) {
    Preconditions.checkNotNull(actions);
    this.actions = actions.stream().sorted().collect(ImmutableList.toImmutableList());
  }

  @VisibleForTesting
  public static LocalActions create(List<LocalAction> actions) {
    var duplicated = Lists.newArrayList();
    var actionEvents = Sets.newHashSet();
    for (LocalAction action : actions) {
      if (actionEvents.contains(action.action)) {
        duplicated.add(action.action);
      } else {
        actionEvents.add(action.action);
      }
    }
    Preconditions.checkArgument(
        duplicated.isEmpty(), "A profile should not contain duplicated local events.");
    return new LocalActions(actions);
  }

  @Override
  public String toString() {
    return "LocalActions{actions=" + actions.size() + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocalActions that = (LocalActions) o;
    return Objects.equal(actions, that.actions);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(actions);
  }

  @Override
  public boolean isEmpty() {
    return actions.isEmpty();
  }

  @Override
  public String getEmptyReason() {
    if (isEmpty()) {
      return "No local actions executed";
    }
    return null;
  }

  @Override
  public String getDescription() {
    return "A collection of actions executed locally with the related events.";
  }

  @Override
  public String getSummary() {
    return String.format("Found %d local actions", actions.size());
  }

  @Override
  public Iterator<LocalAction> iterator() {
    return actions.iterator();
  }

  public int size() {
    return actions.size();
  }

  public Stream<LocalAction> stream() {
    return actions.stream();
  }

  public Stream<LocalAction> parallelStream() {
    return actions.parallelStream();
  }

  /** An event and the events related by thread and time period. */
  public static class LocalAction implements Comparable<LocalAction> {

    private final CompleteEvent action;
    private final ImmutableList<CompleteEvent> relatedEvents;

    private final boolean checksRemoteCache;
    private final boolean executedLocally;
    private final boolean executedRemotely;
    private final boolean remoteCacheHit;

    @VisibleForTesting
    public LocalAction(CompleteEvent action, List<CompleteEvent> relatedEvents) {
      this.action = action;
      this.relatedEvents = ImmutableList.copyOf(relatedEvents);
      this.checksRemoteCache =
          relatedEvents.stream().anyMatch(BazelEventsUtil::indicatesRemoteCacheCheck);
      this.executedLocally =
          relatedEvents.stream().anyMatch(BazelEventsUtil::indicatesLocalExecution);
      this.executedRemotely =
          relatedEvents.stream().anyMatch(BazelEventsUtil::indicatesRemoteExecution);
      this.remoteCacheHit =
          !executedRemotely
              && relatedEvents.stream().anyMatch(BazelEventsUtil::indicatesRemoteDownloadOutputs);
    }

    public CompleteEvent getAction() {
      return action;
    }

    public Duration getDurationWithoutCacheCheck() {
      var cacheCheckDuration =
          relatedEvents.stream()
              .filter(BazelEventsUtil::indicatesRemoteCacheCheck)
              .map(e -> e.duration)
              .reduce(Duration.ZERO, Duration::plus);
      return action.duration.minus(cacheCheckDuration);
    }

    public List<CompleteEvent> getRelatedEvents() {
      return relatedEvents;
    }

    public boolean hasRemoteCacheCheck() {
      return checksRemoteCache;
    }

    public boolean isExecutedLocally() {
      return executedLocally;
    }

    public boolean isExecutedRemotely() {
      return executedRemotely;
    }

    public boolean isRemoteCacheHit() {
      return remoteCacheHit;
    }

    /**
     * Returns whether the action looks like it is Bazel-internal.
     *
     * @return true if it is likely internal, false if it is not, and empty if it is unclear
     */
    @Nullable
    public Optional<Boolean> isInternal() {
      if (hasRemoteCacheCheck() || isExecutedLocally() || isExecutedRemotely()) {
        return Optional.of(false);
      }
      var mnemonic = action.args.get(BazelProfileConstants.ARGS_CAT_ACTION_PROCESSING_MNEMONIC);
      if (mnemonic == null) {
        return Optional.empty();
      }
      // TODO: Find other strong signals for internal actions.
      return INTERNAL_ACTION_MNEMONICS.contains(mnemonic) ? Optional.of(true) : Optional.empty();
    }

    @Override
    public String toString() {
      return "LocalAction{action=" + action + ", relatedEvents=" + relatedEvents + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LocalAction action1 = (LocalAction) o;
      return Objects.equal(action, action1.action)
          && Objects.equal(relatedEvents, action1.relatedEvents);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(action, relatedEvents);
    }

    @Override
    public int compareTo(LocalAction o) {
      int i;
      assert action.name != null;
      assert o.action.name != null;
      i = action.name.compareTo(o.action.name);
      if (i != 0) return i;
      assert action.category != null;
      assert o.action.category != null;
      i = action.category.compareTo(o.action.category);
      if (i != 0) return i;
      i = Integer.compare(action.processId, o.action.processId);
      if (i != 0) return i;
      i = Integer.compare(action.threadId, o.action.threadId);
      if (i != 0) return i;
      return action.start.compareTo(o.action.start);
    }
  }
}
