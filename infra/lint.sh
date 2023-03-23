#!/usr/bin/env bash

# Copyright 2022 EngFlow Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This script bootstraps building a Bazel binary without Bazel then
# use this compiled Bazel to bootstrap Bazel itself. It can also
# be provided with a previous version of Bazel to bootstrap Bazel
# itself.
# The resulting binary can be found at output/bazel.

# --- begin runfiles.bash initialization v2 ---
# Copy-pasted from the Bazel Bash runfiles library v2.
set -uo pipefail; f=bazel_tools/tools/bash/runfiles/runfiles.bash
source "${RUNFILES_DIR:-/dev/null}/$f" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "${RUNFILES_MANIFEST_FILE:-/dev/null}" | cut -f2- -d' ')" 2>/dev/null || \
  source "$0.runfiles/$f" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "$0.runfiles_manifest" | cut -f2- -d' ')" 2>/dev/null || \
  source "$(grep -sm1 "^$f " "$0.exe.runfiles_manifest" | cut -f2- -d' ')" 2>/dev/null || \
  { echo>&2 "ERROR: cannot find $f"; exit 1; }; f=; set -e
# --- end runfiles.bash initialization v2 ---

function help() {
  cat <<-'EOT'
Usage bazel run //infra:lint -- [OPTION]... [PATH]

Options:
  -f|--fix          format the code
  -h|--help         print this help message

Arguments:
  PATH              the path to the repository

Examples:
  bazel run //infra:lint -- --fix "${HOME}/bazel_invocation_analyzer"
EOT
}

JAVA_FORMATTER="$(pwd)/third_party/google-java-format/google-java-format"
STARLARK_FORMATTER="$(pwd)/third_party/buildifier/buildifier.exe"
POSITIONAL_ARGS=()
FIX=

while [[ $# -gt 0 ]]; do
  case $1 in
  -f | --fix)
    FIX=1
    shift
    ;;
  -h | --help)
    help
    exit 0
    ;;
  -*)
    echo >&2 "Unknown option $1"
    help >&2
    exit 1
    ;;
  *)
    POSITIONAL_ARGS+=("$1")
    shift
    ;;
  esac
done

set -- "${POSITIONAL_ARGS[@]}" # restore positional parameters

function java_files() {
    find "${1}" -name *.java
}

function lint_java() {
  readonly files="$(java_files "${1}")"
  "${JAVA_FORMATTER}" --dry-run ${files}
}

function format_java() {
  readonly files="$(java_files "${1}")"
  "${JAVA_FORMATTER}" --replace ${files}
}

function lint_starlark() {
  local first_line=
  # buildifier prints the files to reformat to stderr and returns a non-zero exit code if anything has to be formatted
  for starlark_file in "$("${STARLARK_FORMATTER}" -mode check -r "${1}" 2>&1 >/dev/null || true)"; do
    if [[ -z "${first_line}" ]]; then echo; fi
    echo "${starlark_file%" # reformat"}"
  done
}

function format_starlark() {
  "${STARLARK_FORMATTER}" -mode fix -r "${1}"
}

if [[ -n "${FIX}" ]]; then
  # fix
  format_java "${1}"
  format_starlark "${1}"

  echo "Files reformatted, running \`git status\`"
  cd "${1}"
  git status
else
  # dry-run
  FILES_TO_CHANGE="$(lint_java "${1}")"
  FILES_TO_CHANGE+="$(lint_starlark "${1}")"

  if [[ -n "${FILES_TO_CHANGE}" ]]; then
    echo "The following files need formatting: "
    echo "${FILES_TO_CHANGE}"
    echo
    echo "Please run:"
    echo "bazel run //infra:lint -- --fix ${1}"
    exit 1
  fi
fi

