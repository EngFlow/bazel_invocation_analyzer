package com.engflow.bazel.invocation.analyzer.dataproviders;

import com.engflow.bazel.invocation.analyzer.core.Datum;
import com.engflow.bazel.invocation.analyzer.dataproviders.LocalActions.LocalAction;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Organizes events into {@link LocalAction} by category and time period
 */
public class LocalActions implements Datum, Iterable<LocalAction> {

  private final ImmutableList<LocalAction> actions;

  private LocalActions(List<LocalAction> actions) {
    this.actions = actions.stream().sorted().collect(ImmutableList.toImmutableList());
  }

  static LocalActions create(List<LocalAction> actions) {
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
    if(isEmpty()) {
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

  public Stream<LocalAction> stream() {
    return actions.stream();
  }

  public Stream<LocalAction> parallelStream() {
    return actions.parallelStream();
  }

  /**
   * An event and the events related by thread and time period.
   */
  public static class LocalAction implements Comparable<LocalAction> {

    public final CompleteEvent action;
    public final ImmutableList<CompleteEvent> relatedEvents;

    LocalAction(CompleteEvent action, List<CompleteEvent> relatedEvents) {
      this.action = action;
      this.relatedEvents = ImmutableList.copyOf(relatedEvents);
    }

    public CompleteEvent getAction() {
      return action;
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
