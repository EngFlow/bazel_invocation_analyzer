load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "consoleoutput",
    srcs = glob(
        ["*.java"],
    ),
    visibility = [
        "//cli:default_visibility",
    ],
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders",
        "//proto:bazel_invocation_analyzer_java_proto",
        "//third_party/guava",
    ],
)
