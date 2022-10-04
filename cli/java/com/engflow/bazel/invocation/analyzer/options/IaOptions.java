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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public final class IaOptions {
  private final CommandLine commandLine;
  private final Options options;

  public IaOptions(String[] args) throws ParseException {
    options = new Options();
    for (IaOption iaOption : IaOption.values()) {
      options.addOption(iaOption.option);
    }
    this.commandLine = new DefaultParser().parse(options, args);
  }

  /**
   * Returns true iff the boolean iaOption is supplied, else false.
   *
   * @param iaOption the option to check for
   * @return true iff the iaOption is supplied
   * @throws IllegalArgumentException if the iaOption is not of Boolean type
   */
  public Boolean hasOption(IaOption iaOption) {
    if (iaOption.option.getType() != Boolean.class) {
      throw new IllegalArgumentException(
          String.format(
              "Option %s does not have boolean type (actual: %s)",
              iaOption.option.getLongOpt(), iaOption.option.getType()));
    }
    return commandLine.hasOption(iaOption.option);
  }

  /**
   * Returns the option's value, or null if not set
   *
   * @param iaOption the option to retrieve
   * @return if set, the value of the option, otherwise null
   */
  public String getOption(IaOption iaOption) {
    return commandLine.getOptionValue(iaOption.option);
  }

  /**
   * Returns the option's values, or null if not set
   *
   * @param iaOption the option to retrieve
   * @return if set, the value of the option, otherwise null
   * @throws IllegalArgumentException if the passed in option does not support multiple values
   */
  public String[] getOptions(IaOption iaOption) {
    if (!iaOption.option.hasArgs()) {
      throw new IllegalArgumentException(
          "Can only call getOptions on options that accept multiple values");
    }
    return commandLine.getOptionValues(iaOption.option);
  }

  /**
   * @return all the positional arguments without options
   */
  public String[] getArguments() {
    return commandLine.getArgs();
  }

  public void printHelp() {
    final var helpFormatter = new HelpFormatter();
    helpFormatter.setLongOptSeparator("=");
    helpFormatter.printHelp("bazel-invocation-analyzer [OPTIONS...] FILE", options);
  }
}
