We welcome contributions from the community.

# Communication
- Before starting to work on a new feature, reach out to us via GitHub or email
    <analyzer@engflow.com>.
- If no [issue](/bazel_invocation_analyzer/issues) exists for a new feature yet, crease an issue.
  Assign yourself to signal that you are actively working on the feature.
- Small patches and bug fixes do not require prior communication.

# Breaking Changes
- Various clients may depend on this code base, so implementation stability is important to us.
- Where possible, avoid breaking changes. For example, instead of changing the signature of a
  method, mark it as deprecated and add a separate method for the new functionality.
- The following changes are breaking, the list may not be exhaustive.
    - Adding methods or changing the signature of methods in [`core`][1] interfaces and abstract
      classes without a default implementation
    - Removing methods or changing the signature of methods that [`DataProvider`][2]s and
      [`SuggestionProvider`][3]s may depend on, e.g.
        - various `DataProvider`s
        - utility and base classes such as [`SuggestionProviderUtil`][4] and
          [`SuggestionProviderBase`][5]
      Note that not all `DataProvider`s and `SuggestionProvider`s are necessarily upstreamed.
    - Renaming, removing and renumbering [protocol buffer][6] fields and methods, as well as adding
      required fields and methods.

# Submitting a PR
## Prerequisites

You need to have:
- [Bazelisk][7]: A version manager for Bazel. This ensures we
  are all using the same version of Bazel.
- JVM 11 or higher

## Process
- Fork the repository.
- In your local repository, install the git hooks by running
    ```bash
    ./infra/setup.sh
    ```
- DCO: Sign off all commits
    - The sign-off is a simple line at the end of the commit message, which certifies that you wrote
      the patch or otherwise have the right to pass it on as an open-source patch. See
      <https://developercertificate.org/> for details.
    - You can manually add the following line to all of your commit messages:
      ```text
      Signed-off-by: So-and-so <soandso@example.com>
      ```
      using your real name (no pseudonyms) and email address. We do not accept anonymous
      contributions.
    - You can sign off your patch when creating the git commit using `git commit -s`.
    - If you want this to be automatic, you can set up aliases:
      ```bash
      git config --add alias.amend "commit -s --amend"
      git config --add alias.c "commit -s"
      ```
    - If your PR fails the DCO check, you have to fix the entire commit history in the PR. Ideally,
      [squash][8] the commit history to a single commit, append the DCO sign-off as described above,
      and [force push][9]. For example, if you have 3 commits
      in your history:
      ```bash
      git rebase -i HEAD~3
      (interactive squash + DCO append)
      git push origin -f
      ```
      Avoid rewriting the history as much as possible. It complicates the review process.
- Create your PR.
    - Draft PRs may not be reviewed, so do not create your PR as a draft if you want prompt reviews.
    - When opening a PR, you are expected to actively work on until it is merged or closed. We
        reserve the right to close PRs that are not making progress. PRs that are closed due to lack
        of activity can be reopened later.
    - The PR title should be descriptive and ideally starts with an identifier, followed by a colon
        and more information, e.g.
        - "CLI: support relative paths for passed in Bazel profile"
        - "Docs: Add guidance on contributing"
    - The PR description should have details on what the PR does. If it fixes an existing issue, it
        should include "Fixes #XXX".
    - The PR description will be used as the commit message when the PR is merged. Update this field
        if your PR diverges during review.
    - If your PR is co-authored or based on an earlier PR from another contributor, attribute them
      with `Co-authored-by: name <name@example.com>`. See [GitHub's multiple author guidance][10]
      for details.
    - When adding new code, add tests covering it.
    - PRs are only merged if all tests pass.

[1]: /tree/main/analyzer/java/com/engflow/bazel/invocation/analyzer/core
[2]: /blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/core/DataProvider.java
[3]: /blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/core/SuggestionProvider.java
[4]: /blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders/SuggestionProviderUtil.java
[5]: /blob/main/analyzer/java/com/engflow/bazel/invocation/analyzer/suggestionproviders/SuggestionProviderBase.java
[6]: /blob/main/proto/bazel_invocation_analyzer.proto
[7]: https://github.com/bazelbuild/bazelisk
[8]: https://gitready.com/advanced/2009/02/10/squashing-commits-with-rebase
[9]: https://git-scm.com/docs/git-push#Documentation/git-push.txt--f
[10]: https://docs.github.com/en/pull-requests/committing-changes-to-your-project/creating-and-editing-commits/creating-a-commit-with-multiple-authors
