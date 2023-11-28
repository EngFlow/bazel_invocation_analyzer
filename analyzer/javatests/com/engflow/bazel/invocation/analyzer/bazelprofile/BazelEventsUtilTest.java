package com.engflow.bazel.invocation.analyzer.bazelprofile;

import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_ACTION_PROCESSING;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_GENERAL_INFORMATION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_LOCAL_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_CACHE_CHECK;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_ACTION_EXECUTION;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_EXECUTION_UPLOAD_TIME;
import static com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants.CAT_REMOTE_OUTPUT_DOWNLOAD;
import static com.google.common.truth.Truth.assertThat;

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
  public void indicatesRemoteDownloadOutputsCat() {
    assertThat(
            BazelEventsUtil.indicatesRemoteDownloadOutputs(
                completeEvent("random name", CAT_REMOTE_OUTPUT_DOWNLOAD)))
        .isTrue();
  }

  @Test
  public void indicatesRemoteDownloadOutputsName() {
    assertThat(
            BazelEventsUtil.indicatesRemoteDownloadOutputs(
                completeEvent("Remote.download", CAT_GENERAL_INFORMATION)))
        .isTrue();
    assertThat(
            BazelEventsUtil.indicatesRemoteDownloadOutputs(
                completeEvent("NoRemote.download", CAT_GENERAL_INFORMATION)))
        .isFalse();
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

  private static CompleteEvent completeEvent(String name, String category) {
    return new CompleteEvent(
        name, category, Timestamp.ofSeconds(1), Duration.ofSeconds(1), 1, 1, ImmutableMap.of());
  }
}
