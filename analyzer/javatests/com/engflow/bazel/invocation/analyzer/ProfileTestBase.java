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

package com.engflow.bazel.invocation.analyzer;

import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.complete;
import static com.engflow.bazel.invocation.analyzer.WriteBazelProfile.instant;
import static java.nio.file.Files.createTempDirectory;

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileConstants;
import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfilePhase;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.DuplicateProviderException;
import com.engflow.bazel.invocation.analyzer.core.InvalidProfileException;
import com.engflow.bazel.invocation.analyzer.core.MissingInputException;
import com.engflow.bazel.invocation.analyzer.core.NullDatumException;
import com.engflow.bazel.invocation.analyzer.time.TimeUtil;
import com.engflow.bazel.invocation.analyzer.time.Timestamp;
import com.google.devtools.build.runfiles.Runfiles;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.BeforeClass;

public abstract class ProfileTestBase {
  private static final String WORKSPACE_ROOT = "com_engflow_bazel_invocation_analyzer/";

  protected static Runfiles RUNFILES;
  protected static final String ROOT =
      WORKSPACE_ROOT + "analyzer/javatests/com/engflow/bazel/invocation/analyzer/profiles/";

  @Nullable private Path tempPath;

  @BeforeClass
  public static void setupClass() throws IOException {
    RUNFILES = Runfiles.create();
  }

  @After
  public void teardownInstance() {
    if (tempPath != null) {
      for (File file : tempPath.toFile().listFiles()) {
        file.delete();
      }
      tempPath.toFile().delete();
    }
    tempPath = null;
  }

  /**
   * Get a {@link Path} to a temporary directory that can be used for testing.
   *
   * @return {@link Path} to a temporary directory that can be used for testing
   */
  protected Path getTempPath() {
    if (tempPath == null) {
      try {
        tempPath = createTempDirectory(this.getClass().getName());
      } catch (IOException ex) {
        throw new IllegalStateException("Unable to create temporary directory", ex);
      }
    }
    return tempPath;
  }

  /**
   * Register the BazelProfile with the DataManager implementation in the derived class.
   *
   * @param bazelProfile BazelProfile to register
   */
  protected abstract void registerBazelProfile(BazelProfile bazelProfile)
      throws InvalidProfileException,
          MissingInputException,
          DuplicateProviderException,
          NullDatumException;

  /**
   * Load the specified Bazel profile file and set up the {@link DataManager} to return the loaded
   * profile.
   *
   * @param path location of the Bazel profile file to load
   */
  protected void withProfile(String path) throws Exception {
    String profilePath = RUNFILES.rlocation(ROOT + path);
    BazelProfile bazelProfile = BazelProfile.createFromPath(profilePath);
    registerBazelProfile(bazelProfile);
  }

  /**
   * Create a Bazel profile file from the supplied profile sections, load it into a {@link
   * BazelProfile}, and set up the {@link DataManager} to return the loaded profile.
   *
   * @param profileSections sections to include in the created Bazel profile file
   * @return the {@link BazelProfile} created
   */
  protected BazelProfile useProfile(WriteBazelProfile.ProfileSection... profileSections)
      throws DuplicateProviderException,
          InvalidProfileException,
          MissingInputException,
          NullDatumException {
    var profile =
        BazelProfile.createFromInputStream(WriteBazelProfile.toInputStream(profileSections));
    registerBazelProfile(profile);
    return profile;
  }

  /**
   * Create the Bazel profile events that establish the phases of invocation processing. For a valid
   * profile, at least launchStart and finishTime have to be non-null.
   *
   * @param launchStart timestamp of the Launch Blaze event
   * @param initStart timestamp of the Initialize command event
   * @param evalStart timestamp of the Evaluate target patterns event
   * @param depStart timestamp of the Load and analyze dependencies event
   * @param prepStart timestamp of the Prepare for build event
   * @param execStart timestamp of the Build artifacts event
   * @param finishStart timestamp of the Complete build event
   * @param finishTime timestamp of the Finishing event
   * @return array of phase marker events
   */
  protected WriteBazelProfile.ThreadEvent[] createPhaseEvents(
      @Nullable Timestamp launchStart,
      @Nullable Timestamp initStart,
      @Nullable Timestamp evalStart,
      @Nullable Timestamp depStart,
      @Nullable Timestamp prepStart,
      @Nullable Timestamp execStart,
      @Nullable Timestamp finishStart,
      @Nullable Timestamp finishTime) {
    List<WriteBazelProfile.ThreadEvent> threadEvents = new ArrayList<>();
    if (launchStart != null) {
      threadEvents.add(
          complete(
              BazelProfilePhase.LAUNCH.name,
              BazelProfileConstants.CAT_BUILD_PHASE_MARKER,
              launchStart,
              TimeUtil.getDurationBetween(
                  launchStart, initStart == null ? Timestamp.ofMicros(0) : initStart)));
    }
    if (initStart != null) {
      threadEvents.add(
          instant(
              BazelProfilePhase.INIT.name,
              BazelProfileConstants.CAT_BUILD_PHASE_MARKER,
              initStart));
    }
    if (evalStart != null) {
      threadEvents.add(
          instant(
              BazelProfilePhase.EVALUATE.name,
              BazelProfileConstants.CAT_BUILD_PHASE_MARKER,
              evalStart));
    }
    if (depStart != null) {
      threadEvents.add(
          instant(
              BazelProfilePhase.DEPENDENCIES.name,
              BazelProfileConstants.CAT_BUILD_PHASE_MARKER,
              depStart));
    }
    if (prepStart != null) {
      threadEvents.add(
          instant(
              BazelProfilePhase.PREPARE.name,
              BazelProfileConstants.CAT_BUILD_PHASE_MARKER,
              prepStart));
    }
    if (execStart != null) {
      threadEvents.add(
          instant(
              BazelProfilePhase.EXECUTE.name,
              BazelProfileConstants.CAT_BUILD_PHASE_MARKER,
              execStart));
    }
    if (finishStart != null) {
      threadEvents.add(
          instant(
              BazelProfilePhase.FINISH.name,
              BazelProfileConstants.CAT_BUILD_PHASE_MARKER,
              finishStart));
    }
    if (finishTime != null) {
      threadEvents.add(
          instant(
              BazelProfileConstants.INSTANT_FINISHING,
              BazelProfileConstants.CAT_GENERAL_INFORMATION,
              finishTime));
    }
    return threadEvents.toArray(new WriteBazelProfile.ThreadEvent[0]);
  }
}
