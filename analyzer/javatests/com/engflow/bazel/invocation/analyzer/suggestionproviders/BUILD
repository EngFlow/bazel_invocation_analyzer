java_test(
    name = "suggestionproviders",
    srcs = glob(["**/*.java"]),
    data = ["//analyzer/javatests/com/engflow/bazel/invocation/analyzer:profiles"],
    test_class = "com.engflow.bazel.invocation.analyzer.suggestionproviders.SuggestionProvidersTestSuite",
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders/remoteexecution:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//proto:bazel_invocation_analyzer_java_proto",
        "//third_party/jsr305",
        "//third_party/junit",
        "//third_party/mockito",
        "//third_party/truth",
    ],
)
