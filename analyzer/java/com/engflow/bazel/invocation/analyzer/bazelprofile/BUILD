load("@rules_java//java:defs.bzl", "java_library")

TYPES = [
    "BazelProfileConstants.java",
    "BazelProfilePhase.java",
    "ThreadId.java",
]

java_library(
    name = "bazelprofile",
    srcs = glob(
        ["*.java"],
        exclude = TYPES + ["BazelEventsUtil.java"],
    ),
    visibility = ["//visibility:public"],
    deps = [
        ":types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/traceeventformat",
        "//third_party/gson",
        "//third_party/guava",
        "//third_party/jsr305",
    ],
)

java_library(
    name = "types",
    srcs = TYPES,
    visibility = ["//visibility:public"],
    deps = [
        "//third_party/guava",
    ],
)

java_library(
    name = "util",
    srcs = ["BazelEventsUtil.java"],
    visibility = ["//visibility:public"],
    deps = [
        ":types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/traceeventformat",
        "//third_party/guava",
    ],
)
