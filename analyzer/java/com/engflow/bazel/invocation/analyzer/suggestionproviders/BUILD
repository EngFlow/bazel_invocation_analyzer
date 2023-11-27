java_library(
    name = "suggestionproviders",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:util",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders/remoteexecution:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/traceeventformat",
        "//proto:bazel_invocation_analyzer_java_proto",
        "//third_party/guava",
        "//third_party/jsr305",
    ],
)
