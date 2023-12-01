workspace(
    name = "com_engflow_bazel_invocation_analyzer",
)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file", "http_jar")

http_jar(
    name = "byte_buddy",
    sha256 = "6a688bff5b0da4f4f26a672be6623efef94837f1dd49ef2d1f5f6fe07c06699c",
    url = "https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.12.13/byte-buddy-1.12.13.jar",
)

http_jar(
    name = "commons_cli",
    sha256 = "bc8bb01fc0fad250385706e20f927ddcff6173f6339b387dc879237752567ac6",
    url = "https://repo1.maven.org/maven2/commons-cli/commons-cli/1.5.0/commons-cli-1.5.0.jar",
)

http_jar(
    name = "error_prone_annotations",
    sha256 = "ec6f39f068b6ff9ac323c68e28b9299f8c0a80ca512dccb1d4a70f40ac3ec054",
    url = "https://repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.23.0/error_prone_annotations-2.23.0.jar",
)

http_jar(
    name = "failureaccess",
    sha256 = "8a8f81cf9b359e3f6dfa691a1e776985c061ef2f223c9b2c80753e1b458e8064",
    url = "https://repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar",
)

http_jar(
    name = "gson",
    sha256 = "4241c14a7727c34feea6507ec801318a3d4a90f070e4525681079fb94ee4c593",
    url = "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar",
)

http_jar(
    name = "guava",
    sha256 = "6d4e2b5a118aab62e6e5e29d185a0224eed82c85c40ac3d33cf04a270c3b3744",
    url = "https://repo1.maven.org/maven2/com/google/guava/guava/32.1.3-jre/guava-32.1.3-jre.jar",
)

http_jar(
    name = "hamcrest",
    sha256 = "094f5d92b4b7d9c8a2bf53cc69d356243ae89c3499457bcb4b92f7ed3bf95879",
    url = "https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/2.2/hamcrest-core-2.2.jar",
)

http_jar(
    name = "j2objc_annotations",
    sha256 = "f02a95fa1a5e95edb3ed859fd0fb7df709d121a35290eff8b74dce2ab7f4d6ed",
    url = "https://repo1.maven.org/maven2/com/google/j2objc/j2objc-annotations/2.8/j2objc-annotations-2.8.jar",
)

http_jar(
    name = "jsr305",
    sha256 = "766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7",
    url = "https://repo1.maven.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar",
)

http_jar(
    name = "junit",
    sha256 = "8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3",
    url = "https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar",
)

http_jar(
    name = "mockito",
    sha256 = "ee3b91cdf4c23cff92960c32364371c683ee6415f1ec4678317bcea79c9f9819",
    url = "https://repo1.maven.org/maven2/org/mockito/mockito-core/4.6.1/mockito-core-4.6.1.jar",
)

http_jar(
    name = "objenesis",
    sha256 = "02dfd0b0439a5591e35b708ed2f5474eb0948f53abf74637e959b8e4ef69bfeb",
    url = "https://repo1.maven.org/maven2/org/objenesis/objenesis/3.3/objenesis-3.3.jar",
)

http_jar(
    name = "truth",
    sha256 = "7f6d50d6f43a102942ef2c5a05f37a84f77788bb448cf33cceebf86d34e575c0",
    url = "https://repo1.maven.org/maven2/com/google/truth/truth/1.1.5/truth-1.1.5.jar",
)

http_jar(
    name = "truth_java8_extension",
    sha256 = "9e3c437ef76c0028d1c87d9f81d599301459333cfb3b50e5bf815ed712745140",
    url = "https://repo1.maven.org/maven2/com/google/truth/extensions/truth-java8-extension/1.1.5/truth-java8-extension-1.1.5.jar",
)

# Dependencies required for linting

## BUILD files
http_file(
    name = "buildifier_darwin_amd64",
    executable = True,
    sha256 = "c9378d9f4293fc38ec54a08fbc74e7a9d28914dae6891334401e59f38f6e65dc",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-darwin-amd64",
        "https://github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-darwin-amd64",
    ],
)

http_file(
    name = "buildifier_darwin_arm64",
    executable = True,
    sha256 = "745feb5ea96cb6ff39a76b2821c57591fd70b528325562486d47b5d08900e2e4",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-darwin-arm64",
        "https://github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-darwin-arm64",
    ],
)

http_file(
    name = "buildifier_linux_amd64",
    executable = True,
    sha256 = "52bf6b102cb4f88464e197caac06d69793fa2b05f5ad50a7e7bf6fbd656648a3",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-linux-amd64",
        "https://github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-linux-amd64",
    ],
)

http_file(
    name = "buildifier_linux_arm64",
    executable = True,
    sha256 = "917d599dbb040e63ae7a7e1adb710d2057811902fdc9e35cce925ebfd966eeb8",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-linux-arm64",
        "https://github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-linux-arm64",
    ],
)

http_file(
    name = "buildifier_windows_amd64",
    executable = True,
    sha256 = "2f039125e2fbef4c804e43dc11c71866cf444306ac6d0f5e38c592854458f425",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-windows-amd64.exe",
        "https://github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-windows-amd64.exe",
    ],
)

## Java files
http_jar(
    name = "google_java_format",
    sha256 = "bed3bad433f7df427700648f24b798db3c338d6dcb2cc5c08bc542b55610f910",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/google/google-java-format/releases/download/v1.18.1/google-java-format-1.18.1-all-deps.jar",
        "https://github.com/google/google-java-format/releases/download/v1.18.1/google-java-format-1.18.1-all-deps.jar",
    ],
)
