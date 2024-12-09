load("@protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@protobuf//bazel:proto_library.bzl", "proto_library")

proto_library(
    name = "bazel_invocation_analyzer_proto",
    srcs = [
        "bazel_invocation_analyzer.proto",
    ],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "bazel_invocation_analyzer_java_proto",
    visibility = ["//visibility:public"],
    deps = [
        ":bazel_invocation_analyzer_proto",
    ],
)
