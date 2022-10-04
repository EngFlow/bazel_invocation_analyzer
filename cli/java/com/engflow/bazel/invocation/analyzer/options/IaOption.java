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

package com.engflow.bazel.invocation.analyzer.options;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.cli.Option;

public enum IaOption {
  HELP(
      Option.builder()
          .option("h")
          .longOpt("help")
          .desc("Print this help message.")
          .type(Boolean.class)
          .build()),
  OUTPUT_PLAINTEXT(
      Option.builder()
          .longOpt("plaintext")
          .desc("Enable to output unformatted console output.")
          .type(Boolean.class)
          .build()),
  OUTPUT_VERBOSE(
      Option.builder()
          .longOpt("verbose")
          .desc("Enable to output verbose console output.")
          .type(Boolean.class)
          .build()),
  OUTPUT_MODE(
      Option.builder()
          .longOpt("mode")
          .hasArgs()
          .valueSeparator(',')
          .desc(
              "Specify what information to generate. One or more of "
                  + Arrays.stream(Mode.values())
                      .map(Mode::toString)
                      .map(s -> s.toLowerCase(Locale.US))
                      .collect(Collectors.joining(","))
                  + " separated by commas.")
          .type(String.class)
          .build());

  public final Option option;

  IaOption(Option option) {
    this.option = option;
  }
}
