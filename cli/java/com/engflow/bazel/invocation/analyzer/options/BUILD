load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "options",
    srcs = glob(
        ["*.java"],
    ),
    visibility = [
        "//cli:default_visibility",
    ],
    deps = [
        "//third_party/commons-cli:commons_cli",
    ],
)
