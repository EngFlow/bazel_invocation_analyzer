java_library(
    name = "test_base",
    testonly = True,
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/bazelprofile:types",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/core",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/traceeventformat",
        "//third_party/gson",
        "//third_party/guava",
        "//third_party/jsr305",
        "//third_party/junit",
        "//third_party/mockito",
        "@bazel_tools//tools/java/runfiles",
    ],
)

filegroup(
    name = "profiles",
    srcs = glob([
        "profiles/**",
    ]),
    visibility = ["//analyzer/javatests/com/engflow/bazel/invocation/analyzer:__subpackages__"],
)
