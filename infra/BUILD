sh_binary(
    name = "lint",
    srcs = [":lint.sh"],
    data = [
        "//third_party/buildifier",
        "//third_party/google-java-format",
    ],
    deps = ["@bazel_tools//tools/bash/runfiles"],
)
