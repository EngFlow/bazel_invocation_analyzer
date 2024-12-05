load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "time",
    srcs = glob(["**/*.java"]),
    test_class = "com.engflow.bazel.invocation.analyzer.time.TimeTestSuite",
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//third_party/guava",
        "//third_party/junit",
        "//third_party/truth",
    ],
)
