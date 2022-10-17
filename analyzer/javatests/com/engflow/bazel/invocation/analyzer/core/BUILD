TYPES = [
    "TestDatum.java",
]

java_test(
    name = "core",
    srcs = glob(
        ["**/*.java"],
        exclude = TYPES,
    ),
    data = ["//analyzer/javatests/com/engflow/bazel/invocation/analyzer:profiles"],
    test_class = "com.engflow.bazel.invocation.analyzer.core.CoreTestSuite",
    deps = [
        ":types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//third_party/guava",
        "//third_party/jsr305",
        "//third_party/junit",
        "//third_party/truth",
    ],
)

java_library(
    name = "types",
    testonly = True,
    srcs = TYPES,
    visibility = ["//visibility:public"],
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
    ],
)
