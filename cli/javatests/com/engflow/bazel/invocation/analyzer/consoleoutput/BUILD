load("@rules_java//java:defs.bzl", "java_test")

java_test(
    name = "consoleoutput",
    srcs = glob(["*.java"]),
    test_class = "com.engflow.bazel.invocation.analyzer.consoleoutput.ConsoleOutputSuite",
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/javatests/com/engflow/bazel/invocation/analyzer:test_base",
        "//analyzer/javatests/com/engflow/bazel/invocation/analyzer/core:types",
        "//cli/java/com/engflow/bazel/invocation/analyzer/consoleoutput",
        "//proto:bazel_invocation_analyzer_java_proto",
        "//third_party/guava",
        "//third_party/junit",
        "//third_party/truth",
    ],
)
