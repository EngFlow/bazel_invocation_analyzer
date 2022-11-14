---
name: 🐛 Bug Report
about: Report a bug.
title: "{short headline describing the bug, not the fix}"
labels:
  - type/bug
---

## Description

A clear and concise description of what the bug is.

### Observed behavior

A clear and concise description of the observed behavior.

### Expected behavior

A clear and concise description of what the behavior should be.

### Step-by-step guide on how to reproduce the bug

1. Use numbered steps for easier later reference.
1. If you used the CLI, include the command you ran:
    ```bash
    the command run
    ```

## Additional context

Supplemental data helps debug issues more quickly and reliably.

Provide as many details as you feel comfortable sharing publicly.
Alternatively, email data to <analyzer@engflow.com> while referencing the
created issue, or opt to send the bug report entirely by email.

### Environment

Details about the environment this bug was observed in.

- Bazel Invocation Analyzer: [version number or commit id]
- Browser: [e.g. Chrome, Safari]
- Operating System: [e.g. Linux, macOS, Windows including version]
- Other:

### Output

Optionally include the tool's output. You can add a screenshot, or copy and paste the text output
either in its entirety or only the relevant section(s). Inline the output as a code block here or
attach a text file.

```
example output
```

### File(s) scanned

Optionally provide file(s) that demonstrate the bug when running the tool.

Ideally, do not attach files directly. Bazel profiles can include sensitive data, such as exposing
which dependencies your project includes.

Instead, upload files to the cloud storage provider of your choice and grant appropriate access.
This allows you to adjust who can access them now and in the future. For example, you may want to
remove access to the files once the issue has been resolved.

### Further details

Provide other data related to this issue that may prove helpful.