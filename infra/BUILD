sh_binary(
    name = "lint",
    srcs = [":lint.sh"],
    data = [
        "//third_party/buildifier",
        "@google_java_format//jar",
    ],
    deps = ["@bazel_tools//tools/bash/runfiles"],
)
