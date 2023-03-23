workspace(
    name = "com_engflow_bazel_invocation_analyzer",
)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive", "http_file", "http_jar")

http_archive(
    name = "bazel_skylib",
    sha256 = "f7be3474d42aae265405a592bb7da8e171919d74c16f082a5457840f06054728",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/bazel-skylib/releases/download/1.2.1/bazel-skylib-1.2.1.tar.gz",
        "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/1.2.1/bazel-skylib-1.2.1.tar.gz",
        "https://github.com/bazelbuild/bazel-skylib/releases/download/1.2.1/bazel-skylib-1.2.1.tar.gz",
    ],
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()

http_archive(
    name = "rules_proto",
    sha256 = "e017528fd1c91c5a33f15493e3a398181a9e821a804eb7ff5acdd1d2d6c2b18d",
    strip_prefix = "rules_proto-4.0.0-3.20.0",
    urls = [
        "https://github.com/bazelbuild/rules_proto/archive/refs/tags/4.0.0-3.20.0.tar.gz",
    ],
)

load("@rules_proto//proto:repositories.bzl", "rules_proto_dependencies", "rules_proto_toolchains")

rules_proto_dependencies()

rules_proto_toolchains()

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
    sha256 = "067047714349e7789a5bdbfad9d1c0af9f3a1eb28c55a0ee3f68e682f905c4eb",
    url = "https://repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.15.0/error_prone_annotations-2.15.0.jar",
)

http_jar(
    name = "failureaccess",
    sha256 = "a171ee4c734dd2da837e4b16be9df4661afab72a41adaf31eb84dfdaf936ca26",
    url = "https://repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar",
)

http_jar(
    name = "gson",
    sha256 = "378534e339e6e6d50b1736fb3abb76f1c15d1be3f4c13cec6d536412e23da603",
    url = "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.9.1/gson-2.9.1.jar",
)

http_jar(
    name = "guava",
    sha256 = "a42edc9cab792e39fe39bb94f3fca655ed157ff87a8af78e1d6ba5b07c4a00ab",
    url = "https://repo1.maven.org/maven2/com/google/guava/guava/31.1-jre/guava-31.1-jre.jar",
)

http_jar(
    name = "hamcrest",
    sha256 = "094f5d92b4b7d9c8a2bf53cc69d356243ae89c3499457bcb4b92f7ed3bf95879",
    url = "https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/2.2/hamcrest-core-2.2.jar",
)

http_jar(
    name = "j2objc_annotations",
    sha256 = "21af30c92267bd6122c0e0b4d20cccb6641a37eaf956c6540ec471d584e64a7b",
    url = "https://repo1.maven.org/maven2/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3.jar",
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
    sha256 = "fc0b67782289a2aabfddfdf99eff1dcd5edc890d49143fcd489214b107b8f4f3",
    url = "https://repo1.maven.org/maven2/com/google/truth/truth/1.1.3/truth-1.1.3.jar",
)

http_jar(
    name = "truth_java8_extension",
    sha256 = "2bbd32dd2fa9470d17f1bbda4f52b33b60bce4574052c1d46610a0aa371fc446",
    url = "https://repo1.maven.org/maven2/com/google/truth/extensions/truth-java8-extension/1.1.3/truth-java8-extension-1.1.3.jar",
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
    sha256 = "82819a2c5f7067712e0233661b864c1c034f6657d63b8e718b4a50e39ab028f6",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/google/google-java-format/releases/download/v1.16.0/google-java-format-1.16.0-all-deps.jar",
        "https://github.com/google/google-java-format/releases/download/v1.16.0/google-java-format-1.16.0-all-deps.jar",
    ],
)

## Select right platform for buildifier src
http_archive(
    name = "platforms",
    sha256 = "379113459b0feaf6bfbb584a91874c065078aa673222846ac765f86661c27407",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/platforms/releases/download/0.0.5/platforms-0.0.5.tar.gz",
        "https://github.com/bazelbuild/platforms/releases/download/0.0.5/platforms-0.0.5.tar.gz",
    ],
)
