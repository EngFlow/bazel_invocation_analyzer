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

import com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfile;
import com.engflow.bazel.invocation.analyzer.consoleoutput.ConsoleOutput;
import com.engflow.bazel.invocation.analyzer.core.DataManager;
import com.engflow.bazel.invocation.analyzer.core.DataProvider;
import com.engflow.bazel.invocation.analyzer.core.SuggestionProvider;
import com.engflow.bazel.invocation.analyzer.dataproviders.DataProviderUtil;
import com.engflow.bazel.invocation.analyzer.options.IaOption;
import com.engflow.bazel.invocation.analyzer.options.IaOptions;
import com.engflow.bazel.invocation.analyzer.options.Mode;
import com.engflow.bazel.invocation.analyzer.suggestionproviders.SuggestionProviderUtil;
import java.io.File;
import java.nio.file.FileSystems;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class Main {
  public static void main(String[] args) throws Exception {
    IaOptions options = new IaOptions(args);

    if (options.hasOption(IaOption.HELP)) {
      options.printHelp();
      System.exit(0);
    }

    if (options.getArguments().length != 1) {
      System.err.println(
          "You need to pass a valid path of a Bazel profile as the first and only argument.");
      System.exit(1);
    }

    HashSet<Mode> modes = new HashSet<Mode>();
    var modeOptions = options.getOptions(IaOption.OUTPUT_MODE);
    if (modeOptions != null && modeOptions.length > 0) {
      for (var modeStr : modeOptions) {
        try {
          var mode = Mode.valueOf(modeStr.toUpperCase(Locale.US));
          modes.add(mode);
        } catch (IllegalArgumentException ex) {
          System.err.println(String.format("Invalid mode \"%s\" specified.", modeStr));
          System.exit(1);
        }
      }
    } else {
      modes.add(Mode.SUGGESTIONS);
    }

    final boolean verbose = options.hasOption(IaOption.OUTPUT_VERBOSE);
    ConsoleOutput consoleOutput =
        new ConsoleOutput(options.hasOption(IaOption.OUTPUT_PLAINTEXT), verbose);
    consoleOutput.outputHeader();

    try {
      String bazelProfilePath = options.getArguments()[0];
      File file = new File(bazelProfilePath);
      if (!file.isAbsolute()) {
        String buildWorkingDirectory = System.getenv("BUILD_WORKING_DIRECTORY");
        if (buildWorkingDirectory != null) {
          bazelProfilePath =
              buildWorkingDirectory + FileSystems.getDefault().getSeparator() + bazelProfilePath;
        }
      }
      consoleOutput.outputAnalysisInput(bazelProfilePath);

      DataManager dataManager = new DataManager();
      BazelProfile bazelProfile = BazelProfile.createFromPath(bazelProfilePath);
      bazelProfile.registerWithDataManager(dataManager);

      // We do not use forEach to retain the checked DuplicateProviderException.
      for (DataProvider dataProvider : DataProviderUtil.getAllDataProviders()) {
        dataProvider.register(dataManager);
      }

      // Only gather suggestions if they're requested, or if USED_DATA is requested without ALL_DATA
      if (modes.contains(Mode.SUGGESTIONS)
          || (modes.contains(Mode.USED_DATA) && !modes.contains(Mode.ALL_DATA))) {
        List<SuggestionProvider> suggestionProviders =
            SuggestionProviderUtil.getAllSuggestionProviders(verbose);

        var suggestionStream =
            suggestionProviders.stream().map((a) -> a.getSuggestions(dataManager));
        if (modes.contains(Mode.SUGGESTIONS)) {
          consoleOutput.outputSuggestions(suggestionStream);
        } else {
          // USED_DATA was requested; just cycle through them to allow the DataManager to record
          // which data was used
          suggestionStream.forEach(output -> {});
        }
      }

      if (modes.contains(Mode.ALL_DATA)) {
        consoleOutput.outputAnalysisData(dataManager.getAllDataByProvider());
      } else if (modes.contains(Mode.USED_DATA)) {
        consoleOutput.outputAnalysisData(dataManager.getUsedDataByProvider());
      }
    } catch (Throwable t) {
      consoleOutput.outputError(t);
      System.exit(1);
    }
  }
}
