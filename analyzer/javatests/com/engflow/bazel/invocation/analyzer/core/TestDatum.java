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

package com.engflow.bazel.invocation.analyzer.core;

/** Collection of datum elements useful for unit testing */
public class TestDatum {
  public static class IntegerDatum implements Datum {
    private int myInt;

    public IntegerDatum(int i) {
      myInt = i;
    }

    int getMyInt() {
      return myInt;
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
      return "An integer.";
    }

    @Override
    public String getSummary() {
      return String.format("My int is %d", myInt);
    }
  }

  public static class DoubleDatum implements Datum {
    private double myDouble;

    public DoubleDatum(double d) {
      myDouble = d;
    }

    public double getMyDouble() {
      return myDouble;
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
      return "A double.";
    }

    @Override
    public String getSummary() {
      return String.format("My double is %f", myDouble);
    }
  }

  public static class CharDatum implements Datum {
    private char myChar;

    public CharDatum(char c) {
      myChar = c;
    }

    public char getMyChar() {
      return myChar;
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
      return "A char.";
    }

    @Override
    public String getSummary() {
      return String.format("My character is %s", myChar);
    }
  }

  public static class StringDatum implements Datum {
    private String myString;

    public StringDatum(String s) {
      myString = s;
    }

    public String getMyString() {
      return myString;
    }

    @Override
    public boolean isEmpty() {
      return myString == null;
    }

    @Override
    public String getEmptyReason() {
      return isEmpty() ? "because" : null;
    }

    @Override
    public String getDescription() {
      return "A string.";
    }

    @Override
    public String getSummary() {
      return isEmpty() ? null : String.format("My string is %s", myString);
    }
  }
}
