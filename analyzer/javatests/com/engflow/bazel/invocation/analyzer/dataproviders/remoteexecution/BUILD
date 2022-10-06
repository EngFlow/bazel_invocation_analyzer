java_test(
    name = "remoteexecution",
    srcs = glob(["**/*.java"]),
    data = ["//analyzer/javatests/com/engflow/bazel/invocation/analyzer:profiles"],
    test_class = "com.engflow.bazel.invocation.analyzer.dataproviders.remoteexecution.RemoteExecutionDataProviderSuite",
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders/remoteexecution",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders/remoteexecution:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//analyzer/javatests/com/engflow/bazel/invocation/analyzer:test_base",
        "//analyzer/javatests/com/engflow/bazel/invocation/analyzer/dataproviders:data_provider_test_base",
        "//third_party/junit",
        "//third_party/mockito",
        "//third_party/truth",
    ],
)
