java_test(
    name = "dataproviders",
    srcs = glob(
        ["**/*.java"],
        exclude = ["DataProviderUnitTestBase.java"],
    ),
    data = ["//analyzer/javatests/com/engflow/bazel/invocation/analyzer:profiles"],
    test_class = "com.engflow.bazel.invocation.analyzer.dataproviders.DataProvidersTestSuite",
    deps = [
        ":data_provider_test_base",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/dataproviders:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/traceeventformat",
        "//analyzer/javatests/com/engflow/bazel/invocation/analyzer:test_base",
        "//third_party/guava",
        "//third_party/junit",
        "//third_party/mockito",
        "//third_party/truth",
    ],
)

java_library(
    name = "data_provider_test_base",
    testonly = True,
    srcs = ["DataProviderUnitTestBase.java"],
    visibility = ["//analyzer/javatests/com/engflow/bazel/invocation/analyzer:__subpackages__"],
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/javatests/com/engflow/bazel/invocation/analyzer:test_base",
        "//third_party/junit",
        "//third_party/mockito",
    ],
)
