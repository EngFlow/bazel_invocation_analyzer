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

package com.engflow.bazel.invocation.analyzer.consoleoutput;

public enum ConsoleOutputStyle {
  TEXT_BOLD(1),
  TEXT_DIM(2),
  TEXT_UNDERLINE(4),
  TEXT_BG_INVERSE(7),
  TEXT_BLACK(30),
  TEXT_RED(31),
  TEXT_GREEN(32),
  TEXT_YELLOW(33),
  TEXT_BLUE(34),
  TEXT_MAGENTA(35),
  TEXT_CYAN(36),
  TEXT_WHITE(37),
  BG_BLACK(40),
  BG_RED(41),
  BG_GREEN(42),
  BG_YELLOW(43),
  BG_BLUE(44),
  BG_MAGENTA(45),
  BG_CYAN(46),
  BG_WHITE(47);

  public final int code;

  ConsoleOutputStyle(int code) {
    this.code = code;
  }
}
