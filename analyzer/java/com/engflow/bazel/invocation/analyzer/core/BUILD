load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "core",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//proto:bazel_invocation_analyzer_java_proto",
        "//third_party/jsr305",
    ],
)
