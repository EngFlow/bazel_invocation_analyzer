load("@rules_java//java:defs.bzl", "java_library")

TYPES = [
    "ActionStats.java",
    "BazelPhaseDescription.java",
    "BazelPhaseDescriptions.java",
    "BazelVersion.java",
    "Bottleneck.java",
    "CriticalPathDuration.java",
    "EstimatedCores.java",
    "EstimatedCoresAvailable.java",
    "EstimatedCoresUsed.java",
    "EstimatedJobsFlagValue.java",
    "FlagValueExperimentalProfileIncludeTargetLabel.java",
    "GarbageCollectionStats.java",
    "LocalActions.java",
    "MergedEventsPresent.java",
    "SkymeldUsed.java",
    "TotalDuration.java",
]

UTIL = [
    "DataProviderUtil.java",
]

java_library(
    name = "dataproviders",
    srcs = glob(
        ["*.java"],
        exclude = TYPES + UTIL,
    ),
    visibility = ["//visibility:public"],
    deps = [
        ":types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:util",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/traceeventformat",
        "//third_party/guava",
        "//third_party/jsr305",
    ],
)

java_library(
    name = "util",
    srcs = UTIL,
    visibility = ["//visibility:public"],
    deps = [
        ":dataproviders",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders/remoteexecution",
    ],
)

java_library(
    name = "types",
    srcs = TYPES,
    visibility = ["//visibility:public"],
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:util",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/traceeventformat",
        "//third_party/guava",
        "//third_party/jsr305",
    ],
)
