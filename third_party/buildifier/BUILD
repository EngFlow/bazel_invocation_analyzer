package(default_visibility = ["//infra:__subpackages__"])

load("@bazel_skylib//rules:copy_file.bzl", "copy_file")

copy_file(
    name = "buildifier",
    src = select({
        "//conditions:darwin_arm64": "@buildifier_darwin_arm64//file",
        "//conditions:darwin_x86_64": "@buildifier_darwin_amd64//file",
        "//conditions:linux_aarch64": "@buildifier_linux_arm64//file",
        "//conditions:linux_x86_64": "@buildifier_linux_amd64//file",
        "//conditions:windows": "@buildifier_windows_amd64//file",
        "//conditions:default": "@buildifier_linux_amd64//file",
    }),
    out = "buildifier.exe",
    allow_symlink = True,
    is_executable = True,
)
