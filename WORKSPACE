workspace(
    name = "com_engflow_bazel_invocation_analyzer",
)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")

# Dependencies required for linting

## BUILD files
http_file(
    name = "buildifier_darwin_amd64",
    executable = True,
    sha256 = "c9378d9f4293fc38ec54a08fbc74e7a9d28914dae6891334401e59f38f6e65dc",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-darwin-amd64",
        "https://github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-darwin-amd64",
    ],
)

http_file(
    name = "buildifier_darwin_arm64",
    executable = True,
    sha256 = "745feb5ea96cb6ff39a76b2821c57591fd70b528325562486d47b5d08900e2e4",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-darwin-arm64",
        "https://github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-darwin-arm64",
    ],
)

http_file(
    name = "buildifier_linux_amd64",
    executable = True,
    sha256 = "52bf6b102cb4f88464e197caac06d69793fa2b05f5ad50a7e7bf6fbd656648a3",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-linux-amd64",
        "https://github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-linux-amd64",
    ],
)

http_file(
    name = "buildifier_linux_arm64",
    executable = True,
    sha256 = "917d599dbb040e63ae7a7e1adb710d2057811902fdc9e35cce925ebfd966eeb8",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-linux-arm64",
        "https://github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-linux-arm64",
    ],
)

http_file(
    name = "buildifier_windows_amd64",
    executable = True,
    sha256 = "2f039125e2fbef4c804e43dc11c71866cf444306ac6d0f5e38c592854458f425",
    urls = [
        "https://storage.googleapis.com/engflow-tools-public/github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-windows-amd64.exe",
        "https://github.com/bazelbuild/buildtools/releases/download/5.1.0/buildifier-windows-amd64.exe",
    ],
)
