java_test(
    name = "bazelprofile",
    srcs = glob(["**/*.java"]),
    data = ["//analyzer/javatests/com/engflow/bazel/invocation/analyzer:profiles"],
    test_class = "com.engflow.bazel.invocation.analyzer.bazelprofile.BazelProfileTestSuite",
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:util",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/traceeventformat",
        "//analyzer/javatests/com/engflow/bazel/invocation/analyzer:test_base",
        "//third_party/guava",
        "//third_party/junit",
        "//third_party/truth",
    ],
)
