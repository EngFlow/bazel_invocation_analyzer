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

import com.engflow.bazel.invocation.analyzer.core.Datum;
import javax.annotation.Nullable;

/**
 * Statistics on the number of actions included and their caching and execution status.
 *
 * <p>While it is not enforced, the expectancy is that:
 * <li>All actions have some RC status:<br>
 *     {@link #getActions} = {@link #getActionsWithRcMiss()} + {@link #getActionsWithRcHit()} +
 *     {@link #getActionsWithoutRc()}
 * <li>All actions that were not cache hits are executed:<br>
 *     {@link #getActionsWithRcMiss()} + {@link #getActionsWithoutRc()} = {@link
 *     #getActionsExecutedLocally()} + {@link #getActionsExecutedRemotely()}
 */
public class CachingAndExecutionMetrics implements Datum {
  private final long actions;
  private final long actionsWithRcHit;
  private final long actionsWithRcMissLocallyExecuted;
  private final long actionsWithRcMissRemotelyExecuted;
  private final long actionsWithRcMissExecNotReported;
  private final long actionsWithoutRcLocallyExecuted;
  private final long actionsWithoutRcRemotelyExecuted;
  private final long actionsWithoutRcExecNotReported;

  public CachingAndExecutionMetrics(
      long actions,
      long actionsWithRcHit,
      long actionsWithRcMissLocallyExecuted,
      long actionsWithRcMissRemotelyExecuted,
      long actionsWithRcMissExecNotReported,
      long actionsWithoutRcLocallyExecuted,
      long actionsWithoutRcRemotelyExecuted,
      long actionsWithoutRcExecNotReported) {
    this.actions = actions;
    this.actionsWithRcHit = actionsWithRcHit;
    this.actionsWithRcMissLocallyExecuted = actionsWithRcMissLocallyExecuted;
    this.actionsWithRcMissRemotelyExecuted = actionsWithRcMissRemotelyExecuted;
    this.actionsWithRcMissExecNotReported = actionsWithRcMissExecNotReported;
    this.actionsWithoutRcLocallyExecuted = actionsWithoutRcLocallyExecuted;
    this.actionsWithoutRcRemotelyExecuted = actionsWithoutRcRemotelyExecuted;
    this.actionsWithoutRcExecNotReported = actionsWithoutRcExecNotReported;
  }

  /** The number of actions processed. */
  public long getActions() {
    return actions;
  }

  /** The number of actions processed that checked a remote cache and there was a match. */
  public long getActionsWithRcHit() {
    return actionsWithRcHit;
  }

  /** The number of actions processed that checked a remote cache and there was no match. */
  public long getActionsWithRcMiss() {
    return actionsWithRcMissLocallyExecuted
        + actionsWithRcMissRemotelyExecuted
        + actionsWithRcMissExecNotReported;
  }

  /**
   * The number of actions processed that had a remote cache miss and that were executed locally.
   */
  public long getActionsWithRcMissLocallyExecuted() {
    return actionsWithRcMissLocallyExecuted;
  }

  /**
   * The number of actions processed that had a remote cache miss and that were executed remotely.
   */
  public long getActionsWithRcMissRemotelyExecuted() {
    return actionsWithRcMissRemotelyExecuted;
  }

  /**
   * The number of actions processed that had a remote cache miss and where no event indicating
   * execution location was found.
   */
  public long getActionsWithRcMissExecNotReported() {
    return actionsWithRcMissExecNotReported;
  }

  /** The number of actions processed that did not check a remote cache. */
  public long getActionsWithoutRc() {
    return actionsWithoutRcLocallyExecuted
        + actionsWithoutRcRemotelyExecuted
        + actionsWithoutRcExecNotReported;
  }

  /**
   * The number of actions processed that did not check a remote cache and that were executed
   * locally.
   */
  public long getActionsWithoutRcLocallyExecuted() {
    return actionsWithoutRcLocallyExecuted;
  }

  /**
   * The number of actions processed that did not check a remote cache and that were executed
   * remotely.
   */
  public long getActionsWithoutRcRemotelyExecuted() {
    return actionsWithoutRcRemotelyExecuted;
  }

  /**
   * The number of actions processed that did not check a remote cache and where no event indicating
   * execution location was found. This includes internal Bazel events, such as creating symlinks.
   */
  public long getActionsWithoutRcExecNotReported() {
    return actionsWithoutRcExecNotReported;
  }

  /** The number of actions processed that were executed locally. */
  public long getActionsExecutedLocally() {
    return actionsWithRcMissLocallyExecuted + actionsWithoutRcLocallyExecuted;
  }

  /** The number of actions processed that were executed remotely. */
  public long getActionsExecutedRemotely() {
    return actionsWithRcMissRemotelyExecuted + actionsWithoutRcRemotelyExecuted;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public String getEmptyReason() {
    return null;
  }

  @Override
  public String getDescription() {
    return "Metrics on the processed actions' caching and execution status.";
  }

  private static String summarize(int padTo, long part, long total, @Nullable String suffix) {
    return String.format(
        "%" + padTo + "d    %,6.2f%%%s",
        part,
        100f * part / total,
        suffix == null ? "" : " " + suffix);
  }

  @Override
  public String getSummary() {
    int width = String.valueOf(actions).length();
    StringBuilder sb = new StringBuilder();
    sb.append("Actions: ____________________________ ");
    sb.append(actions);
    sb.append("\n    Remote cache hits: ______________ ");
    sb.append(summarize(width, actionsWithRcHit, actions, "of all actions"));
    sb.append("\n    Remote cache misses: ____________ ");
    sb.append(summarize(width, getActionsWithRcMiss(), actions, "of all actions"));
    sb.append("\n        Executed locally:             ");
    sb.append(
        summarize(
            width,
            actionsWithRcMissLocallyExecuted,
            getActionsWithRcMiss(),
            "of all cache misses"));
    sb.append("\n        Executed remotely:            ");
    sb.append(
        summarize(
            width,
            actionsWithRcMissRemotelyExecuted,
            getActionsWithRcMiss(),
            "of all cache misses"));
    sb.append("\n        Not reported:                 ");
    sb.append(
        summarize(
            width,
            actionsWithRcMissExecNotReported,
            getActionsWithRcMiss(),
            "of all cache misses"));
    sb.append("\n    Remote cache not checked: _______ ");
    sb.append(summarize(width, getActionsWithoutRc(), actions, "of all actions"));
    sb.append("\n        Executed locally:             ");
    sb.append(
        summarize(
            width, actionsWithoutRcLocallyExecuted, getActionsWithoutRc(), "of all cache skips"));
    sb.append("\n        Executed remotely:            ");
    sb.append(
        summarize(
            width, actionsWithoutRcRemotelyExecuted, getActionsWithoutRc(), "of all cache skips"));
    sb.append("\n        Not reported (e.g. internal): ");
    sb.append(
        summarize(
            width, actionsWithoutRcExecNotReported, getActionsWithoutRc(), "of all cache skips"));
    sb.append("\n\n    Executed locally: _______________ ");
    sb.append(summarize(width, getActionsExecutedLocally(), actions, "of all actions"));
    sb.append("\n    Executed remotely: ______________ ");
    sb.append(summarize(width, getActionsExecutedRemotely(), actions, "of all actions"));
    sb.append("\n    Execution not reported: _________ ");
    sb.append(
        summarize(
            width,
            actions - getActionsExecutedLocally() - getActionsExecutedRemotely(),
            actions,
            "of all actions"));
    return sb.toString();
  }
}
