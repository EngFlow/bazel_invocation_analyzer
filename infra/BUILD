sh_binary(
    name = "lint",
    srcs = [":lint.sh"],
    data = [
        "//third_party/buildifier",
        "//third_party/google-java-format",
    ],
    env = {
        "JAVA_FORMATTER": "$(rlocationpath //third_party/google-java-format)",
        "STARLARK_FORMATTER": "$(rlocationpath //third_party/buildifier)",
    },
    deps = ["@bazel_tools//tools/bash/runfiles"],
)
