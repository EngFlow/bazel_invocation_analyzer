load("@rules_java//java:defs.bzl", "java_binary")

java_binary(
    name = "cli",
    main_class = "com.engflow.bazel.invocation.analyzer.Main",
    runtime_deps = [
        "//cli/java/com/engflow/bazel/invocation/analyzer",
    ],
)

package_group(
    name = "default_visibility",
    packages = [
        "//cli/...",
    ],
)
