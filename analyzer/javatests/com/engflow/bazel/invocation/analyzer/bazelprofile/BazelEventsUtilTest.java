package com.engflow.bazel.invocation.analyzer.bazelprofile;

import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_ACTION_PROCESSING;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_LOCAL_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_EXECUTION_UPLOAD_TIME;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;
import static com.google.common.truth.Truth.assertThat;

import com.engflow.bazel.invocation.analyzer.time.DurationUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.engflow.bazel.invocation.analyzer.traceeventformat.CompleteEvent;
import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import org.junit.Test;

public class BazelEventsUtilTest {
  @Test
  public void indicatesLocalExecutionSpecificCategory() {
    assertThat(
            BazelEventsUtil.indicatesLocalExecution(
                completeEvent("random name", CAT_LOCAL_ACTION_EXECUTION)))
        .isTrue();
  }

  @Test
  public void indicatesLocalExecutionGeneralCategory() {
    assertThat(
            BazelEventsUtil.indicatesLocalExecution(
                completeEvent("subprocess.run", CAT_LOCAL_ACTION_EXECUTION)))
        .isTrue();
  }

  @Test
  public void doesNotIndicateLocalExecutionGeneralCategory() {
    assertThat(
            BazelEventsUtil.indicatesLocalExecution(
                completeEvent("subprocess.run", CAT_ACTION_PROCESSING)))
        .isFalse();
  }

  @Test
  public void indicatesRemoteExecution() {
    assertThat(
            BazelEventsUtil.indicatesRemoteExecution(
                completeEvent("random name", CAT_REMOTE_ACTION_EXECUTION)))
        .isTrue();
  }

  @Test
  public void doesNotIndicateRemoteExecution() {
    assertThat(
            BazelEventsUtil.indicatesRemoteExecution(
                completeEvent("random name", CAT_ACTION_PROCESSING)))
        .isFalse();
  }

  @Test
  public void indicatesRemoteCacheCheck() {
    assertThat(
            BazelEventsUtil.indicatesRemoteCacheCheck(
                completeEvent("random name", CAT_REMOTE_ACTION_CACHE_CHECK)))
        .isTrue();
  }

  @Test
  public void doesNotIndicateRemoteCacheCheck() {
    assertThat(
            BazelEventsUtil.indicatesRemoteCacheCheck(
                completeEvent("random name", CAT_ACTION_PROCESSING)))
        .isFalse();
  }

  @Test
  public void indicatesRemoteDownloadOutputs() {
    assertThat(
            BazelEventsUtil.indicatesRemoteDownloadOutputs(
                completeEvent("random name", CAT_REMOTE_OUTPUT_DOWNLOAD)))
        .isTrue();
  }

  @Test
  public void doesNotIndicateRemoteDownloadOutputs() {
    assertThat(
            BazelEventsUtil.indicatesRemoteDownloadOutputs(
                completeEvent("random name", CAT_ACTION_PROCESSING)))
        .isFalse();
  }

  @Test
  public void indicatesRemoteUploadOutputsRC() {
    assertThat(
            BazelEventsUtil.indicatesRemoteUploadOutputs(
                completeEvent("upload outputs", CAT_REMOTE_EXECUTION_UPLOAD_TIME)))
        .isTrue();
  }

  @Test
  public void indicatesRemoteUploadOutputsRE() {
    assertThat(
            BazelEventsUtil.indicatesRemoteUploadOutputs(
                completeEvent("upload", CAT_REMOTE_EXECUTION_UPLOAD_TIME)))
        .isTrue();
  }

  @Test
  public void doesNotRemoteUploadOutputs() {
    assertThat(
            BazelEventsUtil.indicatesRemoteUploadOutputs(
                completeEvent("random name", CAT_REMOTE_EXECUTION_UPLOAD_TIME)))
        .isFalse();
  }

  @Test
  public void summarizeCompleteActionWithoutTargetName() {
    var eventName = "some random name";
    var eventCategory = "that specific category";
    var eventDuration = Duration.ofSeconds(1234);
    var event =
        new CompleteEvent(
            eventName,
            eventCategory,
            Timestamp.ofSeconds(0),
            eventDuration,
            1,
            1,
            ImmutableMap.of());
    var summary = BazelEventsUtil.summarizeCompleteEvent(event);
    assertThat(summary).contains(eventName);
    assertThat(summary).contains(DurationUtil.formatDuration(eventDuration));
    assertThat(summary).doesNotContain(eventCategory);
  }

  @Test
  public void summarizeCompleteActionWithTargetName() {
    var eventName = "some random name";
    var eventCategory = "that specific category";
    var eventDuration = Duration.ofSeconds(1234);
    var targetName = "for //target:foo";
    var event =
        new CompleteEvent(
            eventName,
            eventCategory,
            Timestamp.ofSeconds(0),
            eventDuration,
            1,
            1,
            ImmutableMap.of(BazelProfileConstants.ARGS_CAT_ACTION_PROCESSING_TARGET, targetName));
    var summary = BazelEventsUtil.summarizeCompleteEvent(event);
    assertThat(summary).contains(eventName);
    assertThat(summary).contains(DurationUtil.formatDuration(eventDuration));
    assertThat(summary).contains(targetName);
    assertThat(summary).doesNotContain(eventCategory);
  }

  private static CompleteEvent completeEvent(String name, String category) {
    return new CompleteEvent(
        name, category, Timestamp.ofSeconds(1), Duration.ofSeconds(1), 1, 1, ImmutableMap.of());
  }
}
