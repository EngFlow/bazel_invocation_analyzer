# Bazel Invocation Analyzer

The Bazel Invocation Analyzer is a library and terminal tool developed by EngFlow. It analyzes an invocation's Bazel profile and provides suggestions on how to speed up that invocation.

## Contact

You can get in touch with us

- by sending an email to <analyzer@engflow.com>
- by [creating an issue on GitHub](https://github.com/EngFlow/bazel_invocation_analyzer/issues)

## Documentation

- [Bazel Invocation Analyzer Architecture](docs/library-architecture.md)
- [Adding Suggestions to the Bazel Invocation Analyzer](docs/adding-suggestions.md) walk-through

You can also view this tool's documentation on <https://docs.engflow.com/bia/index.html>.

## Dependencies

[Bazel](https://bazel.build/) version 6.3+

## CLI

The Bazel Invocation Analyzer can be run in a terminal. In this mode it will print out the analysis results directly to the console.

### Usage

Pass in the path of a Bazel profile on your filesystem as the first argument. Use `-h` or `--help` to show all the available options.

```bash
bazel run //cli -- /path/to/bazel_profile.json.gz
```

## Integrations
The Bazel Invocation Analyzer can be integrated into other environments.

### Public Web UIs

On <https://analyzer.engflow.com> you can upload a Bazel profile to receive a rendered version of the library's output.

## Contributing

We welcome contributions from the community. Read our [guide to contributing](https://github.com/EngFlow/bazel_invocation_analyzer/blob/main/CONTRIBUTING.md) for details.

## References

- [Generating a Bazel profile](https://docs.engflow.com/docs/re/faq.html#how-do-i-capture-a-bazel-profile)
- [Interpreting a Bazel profile](https://bazel.build/rules/performance#performance-profiling)
