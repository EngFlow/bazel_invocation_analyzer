load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "traceeventformat",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//analyzer/java/com/engflow/bazel/invocation/analyzer/time",
        "//third_party/gson",
        "//third_party/guava",
        "//third_party/jsr305",
    ],
)
