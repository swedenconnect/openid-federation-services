#!/usr/bin/env bash
#
# Tests for internal/release.sh.
#
# The helpers that work out the version and the branch are called directly. The rest is run from
# start to finish in a throwaway git repository that has its own origin and a stand-in for mvn on
# the PATH, so nothing reaches the real origin and no real tag is made. See sandbox.sh.
#
# Usage:
#     internal/test-scripts/release-test.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE="${HERE}/../release.sh"

# shellcheck source=../release.sh
source "$RELEASE"
set +e +u   # release.sh turns these on, the harness needs them off

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# shellcheck source=sandbox.sh
source "${HERE}/sandbox.sh"

# accepted <name> <version>
accepted() {
  if is_valid_version "$2"; then ok "$1"; else no "$1" "'$2' was rejected"; fi
}

# rejected <name> <version>
rejected() {
  if is_valid_version "$2"; then no "$1" "'$2' was accepted"; else ok "$1"; fi
}

echo "== Working out the version =="

equals "the next version raises the last number" "1.0.1" "$(suggest_next_version v1.0.0)"
equals "the next version after a nine carries into two digits" "1.2.10" "$(suggest_next_version v1.2.9)"
equals "the next version also works without the v" "0.1.1" "$(suggest_next_version 0.1.0)"
equals "the release commit message" "build: 1.0.1 release" "$(release_commit_message 1.0.1)"

accepted "three numbers are a version" "1.0.0"
accepted "two digits in a number are a version" "1.2.10"
rejected "two numbers are not a version" "1.0"
rejected "four numbers are not a version" "1.0.0.1"
rejected "a tag is not a version" "v1.0.0"
rejected "a snapshot is not a version" "1.0.0-SNAPSHOT"
rejected "empty is not a version" ""
rejected "a letter is not a version" "1.0.x"
rejected "a leading space is not a version" " 1.0.0"

echo
echo "== Choosing the branch =="

equals "from main a release branch is made" "release/1_0_0" "$(release_branch_for main 1.0.0)"
equals "the release branch name uses underscores" "release/1_2_10" "$(release_branch_for main 1.2.10)"
equals "from another branch that branch is used" "feature/thing" "$(release_branch_for feature/thing 1.0.0)"
equals "from a release branch that branch is used" "release/1_0_0" "$(release_branch_for release/1_0_0 1.0.0)"
equals "a branch named like main but longer is not main" "main-old" "$(release_branch_for main-old 1.0.0)"

echo
echo "== Running the script =="

ANSWER_FULL_RELEASE=$'y\n\ny\n'

# --- a release from main -------------------------------------------------------------------------

dir="$(new_sandbox 0.1.1-SNAPSHOT v0.1.0)"
in_work "$dir" git tag -a v0.0.9 -m "A stray tag" >/dev/null 2>&1
run_in_sandbox "$dir" "$RELEASE" "$ANSWER_FULL_RELEASE"
status=$?

equals "from main: the script succeeds" "0" "$status"
contains "from main: the latest tag is the base of the suggestion" "$dir/output" "Suggested version: 0.1.1"
equals "from main: the release branch is checked out" "release/0_1_1" "$(in_work "$dir" git branch --show-current)"
equals "from main: the release is tagged" "v0.1.1" "$(in_work "$dir" git tag -l v0.1.1)"
equals "from main: the tag is annotated" "tag" "$(in_work "$dir" git cat-file -t v0.1.1)"
equals "from main: the tag is on the release commit" "build: 0.1.1 release" \
  "$(in_work "$dir" git log -1 --format=%s v0.1.1)"
equals "from main: the branch ends with the release commit" "$(in_work "$dir" git rev-parse 'v0.1.1^{commit}')" \
  "$(in_work "$dir" git rev-parse release/0_1_1)"
equals "from main: the branch is pushed" "release/0_1_1" \
  "$(in_work "$dir" git ls-remote --heads origin release/0_1_1 | awk '{print $2}' | sed 's|refs/heads/||')"
equals "from main: the pushed branch is the release commit" "$(in_work "$dir" git rev-parse release/0_1_1)" \
  "$(in_work "$dir" git ls-remote --heads origin release/0_1_1 | awk '{print $1}')"
equals "from main: the tag is pushed" "v0.1.1" \
  "$(in_work "$dir" git ls-remote --tags origin v0.1.1 | awk '{print $2}' | sed 's|refs/tags/||' | head -1)"
equals "from main: only the new tag is pushed" "" \
  "$(in_work "$dir" git ls-remote --tags origin v0.0.9 | awk '{print $2}')"
equals "from main: the branch is left on the release version" \
  "<project><version>0.1.1</version></project>" "$(in_work "$dir" cat pom.xml)"
equals "from main: the module poms are committed too" \
  "<project><version>0.1.1</version></project>" "$(in_work "$dir" git show v0.1.1:modules/pom.xml)"
equals "from main: the release notes are not touched by the script" "${SANDBOX_NOTES}x" \
  "$(in_work "$dir" git show v0.1.1:docs/release-notes.md; printf x)"
equals "from main: the working tree is clean" "" "$(in_work "$dir" git status --porcelain)"
equals "from main: main is untouched" "First commit" "$(in_work "$dir" git log -1 --format=%s main)"
equals "from main: the project is built once" "1" "$(grep -c 'clean install' "$dir/mvn.log")"
equals "from main: the version is set once" "1" "$(grep -c 'versions:set' "$dir/mvn.log")"
contains "from main: the bump script is pointed at" "$dir/output" "internal/post-release-bump.sh"
contains "from main: the merge is explained" "$dir/output" "Create a merge"
contains "from main: the workflow run is pointed at" "$dir/output" \
  "swedenconnect/openid-federation-services/actions/workflows/maven-github-deploy.yml"

# --- the version is typed instead of confirmed -----------------------------------------------------

dir="$(new_sandbox 0.1.1-SNAPSHOT v0.1.0)"
run_in_sandbox "$dir" "$RELEASE" $'0.2.0\n\ny\n'
equals "a typed version: it is released" "v0.2.0" "$(in_work "$dir" git tag -l v0.2.0)"
equals "a typed version: on a branch named after it" "release/0_2_0" "$(in_work "$dir" git branch --show-current)"

dir="$(new_sandbox 0.1.1-SNAPSHOT v0.1.0)"
run_in_sandbox "$dir" "$RELEASE" $'n\n1.0.0\n\ny\n'
equals "no and then a version: it is released" "v1.0.0" "$(in_work "$dir" git tag -l v1.0.0)"

# --- the first release, with no tag yet ------------------------------------------------------------

dir="$(new_sandbox 1.0.0-SNAPSHOT)"
run_in_sandbox "$dir" "$RELEASE" $'1.0.0\n\n\ny\n'
status=$?
equals "first release: the script succeeds" "0" "$status"
contains "first release: the missing tag is explained" "$dir/output" "There is no tag of the form vX.Y.Z."
equals "first release: the release is tagged" "v1.0.0" "$(in_work "$dir" git tag -l v1.0.0)"

# --- the newest vX.Y.Z tag is found by version, not by name, and other tags are ignored -----------

dir="$(new_sandbox 0.1.1-SNAPSHOT v0.1.0 v0.9.0 v0.10.0 v1.0.0-RC1)"
run_in_sandbox "$dir" "$RELEASE" $'n\n0.0.1\n'
contains "tags are ordered by version: 0.10.0 is newer than 0.9.0" "$dir/output" "Latest tag: v0.10.0"
contains "a tag that is not vX.Y.Z is ignored" "$dir/output" "Suggested version: 0.10.1"

# --- a release from another branch ---------------------------------------------------------------

dir="$(new_sandbox 0.1.1-SNAPSHOT v0.1.0)"
in_work "$dir" git checkout --quiet -b feature/thing
run_in_sandbox "$dir" "$RELEASE" "$ANSWER_FULL_RELEASE"
status=$?

equals "from another branch: the script succeeds" "0" "$status"
equals "from another branch: the release stays on that branch" "feature/thing" \
  "$(in_work "$dir" git branch --show-current)"
equals "from another branch: no release branch is made" "" \
  "$(in_work "$dir" git for-each-ref --format='%(refname:short)' 'refs/heads/release/*')"
equals "from another branch: the release is tagged" "v0.1.1" "$(in_work "$dir" git tag -l v0.1.1)"
equals "from another branch: the branch is pushed" "feature/thing" \
  "$(in_work "$dir" git ls-remote --heads origin feature/thing | awk '{print $2}' | sed 's|refs/heads/||')"

# --- the tag is not made when the user says no ----------------------------------------------------

for tag_answer in n "" yes-please; do
  dir="$(new_sandbox 0.1.1-SNAPSHOT v0.1.0)"
  run_in_sandbox "$dir" "$RELEASE" $'y\n\n'"${tag_answer}"$'\n'
  status=$?

  equals "no tag on '${tag_answer}': the script ends without an error" "0" "$status"
  equals "no tag on '${tag_answer}': nothing is tagged" "" "$(in_work "$dir" git tag -l v0.1.1)"
  equals "no tag on '${tag_answer}': nothing is tagged on the remote" "" \
    "$(in_work "$dir" git ls-remote --tags origin v0.1.1 | awk '{print $2}')"
  equals "no tag on '${tag_answer}': the branch is still pushed" "release/0_1_1" \
    "$(in_work "$dir" git ls-remote --heads origin release/0_1_1 | awk '{print $2}' | sed 's|refs/heads/||')"
  equals "no tag on '${tag_answer}': the branch holds the release version" \
    "<project><version>0.1.1</version></project>" "$(in_work "$dir" cat pom.xml)"
  contains "no tag on '${tag_answer}': the state is explained" "$dir/output" "Stopped before tagging."
  contains "no tag on '${tag_answer}': the bump script is pointed at" "$dir/output" "internal/post-release-bump.sh"
done

# --- checks that stop the release before anything changes ------------------------------------------

stops_early "a dirty working tree" "$RELEASE" "The working tree has changed or untracked files" \
  "$ANSWER_FULL_RELEASE" 0.1.1-SNAPSHOT \
  bash -c 'echo leftover > leftover.txt'

stops_early "a changed tracked file" "$RELEASE" "The working tree has changed or untracked files" \
  "$ANSWER_FULL_RELEASE" 0.1.1-SNAPSHOT \
  bash -c 'echo more >> docs/release-notes.md'

stops_early "no branch checked out" "$RELEASE" "No branch is checked out" \
  "$ANSWER_FULL_RELEASE" 0.1.1-SNAPSHOT \
  git checkout --quiet --detach

stops_early "a version that is not X.Y.Z" "$RELEASE" "is not a version of the form X.Y.Z" \
  $'n\n0.11\n' 0.1.1-SNAPSHOT

stops_early "a typed tag instead of a version" "$RELEASE" "is not a version of the form X.Y.Z" \
  $'v0.1.1\n' 0.1.1-SNAPSHOT

stops_early "a typed snapshot instead of a version" "$RELEASE" "is not a version of the form X.Y.Z" \
  $'0.1.1-SNAPSHOT\n' 0.1.1-SNAPSHOT

stops_early "an empty version after no" "$RELEASE" "is not a version of the form X.Y.Z" \
  $'n\n\n' 0.1.1-SNAPSHOT

stops_early "a version that is already tagged" "$RELEASE" "The tag 'v0.1.0' already exists" \
  $'0.1.0\n' 0.1.1-SNAPSHOT

stops_early "a version that is already tagged, from another branch" "$RELEASE" "The tag 'v0.1.0' already exists" \
  $'0.1.0\n' 0.1.1-SNAPSHOT \
  git checkout --quiet -b feature/thing

stops_early "a release branch that already exists" "$RELEASE" "The branch 'release/0_1_1' already exists" \
  "$ANSWER_FULL_RELEASE" 0.1.1-SNAPSHOT \
  git branch release/0_1_1

stops_early "a release branch that exists only on the remote" "$RELEASE" \
  "The branch 'release/0_1_1' already exists" \
  "$ANSWER_FULL_RELEASE" 0.1.1-SNAPSHOT \
  bash -c 'git branch release/0_1_1 && git push -q origin release/0_1_1 && git branch -D release/0_1_1'

# The two checks also look at the remote, not only at what is here. Running the whole script does
# not reach that for tags, because the fetch at the start brings a remote tag in first, so they are
# called directly against refs that exist only on the remote.

dir="$(new_sandbox 0.1.1-SNAPSHOT v0.1.0)"
in_work "$dir" bash -c 'git branch release/0_1_1 && git push -q origin release/0_1_1 && git branch -D release/0_1_1' \
  >/dev/null 2>&1
in_work "$dir" bash -c 'git tag -a v0.1.1 -m "Version 0.1.1" && git push -q origin v0.1.1 && git tag -d v0.1.1' \
  >/dev/null 2>&1

# free <name> <free|taken> <function> <ref>
free() {
  local result
  if in_work "$dir" bash -c "source '$RELEASE'; $3 '$4'" >/dev/null 2>&1; then
    result="free"
  else
    result="taken"
  fi
  equals "$1" "$2" "$result"
}

free "a branch that is only on the remote is taken" "taken" branch_is_free "release/0_1_1"
free "a tag that is only on the remote is taken" "taken" tag_is_free "v0.1.1"
free "a branch that exists nowhere is free" "free" branch_is_free "release/9_9_9"
free "a tag that exists nowhere is free" "free" tag_is_free "v9.9.9"
free "a branch that is here is taken" "taken" branch_is_free "main"
free "a tag that is here is taken" "taken" tag_is_free "v0.1.0"

summary
