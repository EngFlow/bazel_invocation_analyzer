load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "analyzer",
    srcs = glob(["*.java"]),
    visibility = [
        "//cli:__pkg__",
    ],
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders:util",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders",
        "//cli/java/com/engflow/bazel/invocation/analyzer/consoleoutput",
        "//cli/java/com/engflow/bazel/invocation/analyzer/options",
        "//proto:bazel_invocation_analyzer_java_proto",
        "//third_party/guava",
    ],
)
