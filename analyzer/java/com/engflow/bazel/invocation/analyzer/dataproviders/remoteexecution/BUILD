TYPES = [
    "CriticalPathQueuingDuration.java",
    "QueuingObserved.java",
    "RemoteCachingUsed.java",
    "RemoteExecutionUsed.java",
    "RemoteLatency.java",
    "TotalQueuingDuration.java",
]

java_library(
    name = "remoteexecution",
    srcs = glob(
        ["*.java"],
        exclude = TYPES,
    ),
    visibility = ["//visibility:public"],
    deps = [
        ":types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/traceeventformat",
        "//third_party/guava",
    ],
)

java_library(
    name = "types",
    srcs = TYPES,
    visibility = ["//visibility:public"],
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//third_party/guava",
        "//third_party/jsr305",
    ],
)
