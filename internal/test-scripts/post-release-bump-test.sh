#!/usr/bin/env bash
#
# Tests for internal/post-release-bump.sh.
#
# The helpers are called directly. The rest is run from start to finish in a throwaway git
# repository that has its own origin and a stand-in for mvn on the PATH, so nothing reaches the real
# origin. See sandbox.sh.
#
# Usage:
#     internal/test-scripts/post-release-bump-test.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUMP="${HERE}/../post-release-bump.sh"

# shellcheck source=../post-release-bump.sh
source "$BUMP"
set +e +u   # the script turns these on, the harness needs them off

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# shellcheck source=sandbox.sh
source "${HERE}/sandbox.sh"

echo "== Working out the version =="

equals "the next snapshot version raises the last number" "1.0.1-SNAPSHOT" "$(next_snapshot_version 1.0.0)"
equals "the next snapshot version after a nine carries into two digits" "1.2.10-SNAPSHOT" \
  "$(next_snapshot_version 1.2.9)"
equals "the bump commit message names the released version" "build: bump version after 1.0.0" \
  "$(bump_commit_message 1.0.0)"

echo
echo "== Choosing the branch =="

equals "from main a bump branch is made" "bump/1_0_1" "$(bump_branch_for main 1.0.1)"
equals "the bump branch name uses underscores" "bump/1_2_10" "$(bump_branch_for main 1.2.10)"
equals "from another branch that branch is used" "release/1_0_0" "$(bump_branch_for release/1_0_0 1.0.1)"
equals "a branch named like main but longer is not main" "main-old" "$(bump_branch_for main-old 1.0.1)"

echo
echo "== Release notes for the coming version =="

NOTES_HEADER=$'# Release Notes\n\n'
NEW_SECTION=$'## Version 0.1.2\n\n**Date:** _not yet released_\n\n*\n\n'
OLD_SECTION=$'## Version 0.1.1\n\n**Date:** 2026-09-30\n\n* A fix.\n\nCopyright\n'

notes="${WORK}/notes.md"
printf '%s' "${NOTES_HEADER}${OLD_SECTION}" > "$notes"
add_release_notes_section 0.1.2 "$notes"
equals "the section is added above the latest version" "${NOTES_HEADER}${NEW_SECTION}${OLD_SECTION}x" \
  "$(cat "$notes"; printf x)"
equals "the section is not added twice" "${NOTES_HEADER}${NEW_SECTION}${OLD_SECTION}x" \
  "$(add_release_notes_section 0.1.2 "$notes"; cat "$notes"; printf x)"

add_release_notes_section 0.1.3 "$notes"
equals "a later section goes above the earlier ones" "## Version 0.1.3" "$(grep -m1 '^## Version' "$notes")"
equals "a later section is added once" "1" "$(grep -c '^## Version 0.1.3$' "$notes")"

printf '%s' "${NOTES_HEADER}## Version 0.1.20"$'\n' > "$notes"
add_release_notes_section 0.1.2 "$notes"
equals "a version that only starts like the coming one is not taken for it" "## Version 0.1.2" \
  "$(grep -m1 '^## Version' "$notes")"

printf '%s' "$NOTES_HEADER" > "$notes"
add_release_notes_section 0.1.2 "$notes"
equals "with no version yet the section is added at the end" "${NOTES_HEADER}${NEW_SECTION}x" \
  "$(cat "$notes"; printf x)"

cp "${HERE}/../../docs/release-notes.md" "$notes"
latest="$(grep -m1 '^## Version ' "$notes")"
add_release_notes_section 99.0.0 "$notes"
equals "in docs/release-notes.md the section goes above the latest version" \
  $'## Version 99.0.0\n\n**Date:** _not yet released_\n\n*\n\n'"$latest" \
  "$(awk '/^## Version /{found=1} found' "$notes" | sed -n '1,7p')"
equals "in docs/release-notes.md the rest is kept" "$(cat "${HERE}/../../docs/release-notes.md")" \
  "$(awk '/^## Version 99.0.0$/{skip=6} skip>0{skip--; next} {print}' "$notes")"

echo
echo "== Running the script =="

# --- a bump from main, after the release branch is merged ----------------------------------------

dir="$(new_sandbox 0.1.1 v0.1.0)"
main_before="$(in_work "$dir" git rev-parse main)"
run_in_sandbox "$dir" "$BUMP" $'\n'
status=$?

equals "from main: the script succeeds" "0" "$status"
equals "from main: a bump branch is made and checked out" "bump/0_1_2" "$(in_work "$dir" git branch --show-current)"
equals "from main: the bump branch starts from main" "$main_before" "$(in_work "$dir" git rev-parse bump/0_1_2~1)"
equals "from main: the bump is committed on the bump branch" "build: bump version after 0.1.1" \
  "$(in_work "$dir" git log -1 --format=%s bump/0_1_2)"
equals "from main: the bump branch holds the next snapshot version" \
  "<project><version>0.1.2-SNAPSHOT</version></project>" "$(in_work "$dir" git show bump/0_1_2:modules/pom.xml)"
equals "from main: the bump branch is pushed" "$(in_work "$dir" git rev-parse bump/0_1_2)" \
  "$(in_work "$dir" git ls-remote --heads origin bump/0_1_2 | awk '{print $1}')"
equals "from main: main is untouched here" "$main_before" "$(in_work "$dir" git rev-parse main)"
equals "from main: main is untouched on the remote" "$main_before" \
  "$(in_work "$dir" git ls-remote --heads origin main | awk '{print $1}')"
equals "from main: the working tree is clean" "" "$(in_work "$dir" git status --porcelain)"
contains "from main: the merge is asked for" "$dir/output" "Open a pull request from 'bump/0_1_2' into main"
absent "from main: no merge button is required" "$dir/output" "Create a merge commit"

dir="$(new_sandbox 0.1.1 v0.1.0)"
run_in_sandbox "$dir" "$BUMP" $'0.2.0\n'
equals "from main, a typed version: the branch is named after it" "bump/0_2_0" \
  "$(in_work "$dir" git branch --show-current)"

dir="$(new_sandbox 0.1.1 v0.1.0)"
in_work "$dir" git remote set-url origin "$dir/nowhere.git"
run_in_sandbox "$dir" "$BUMP" $'\n'
status=$?
contains "from main, no remote: the local main is used" "$dir/output" "using the local main"
equals "from main, no remote: the bump is still committed" "build: bump version after 0.1.1" \
  "$(in_work "$dir" git log -1 --format=%s bump/0_1_2)"
equals "from main, no remote: the failed push is an error" "failed" "$([ "$status" -ne 0 ] && echo failed)"

# --- a bump on a release branch that is not merged yet ---------------------------------------------

dir="$(new_sandbox 0.1.1 v0.1.0)"
in_work "$dir" bash -c 'git checkout -q -b release/0_1_1 && git push -q -u origin release/0_1_1' >/dev/null 2>&1
run_in_sandbox "$dir" "$BUMP" $'\n'
status=$?

equals "a bump: the script succeeds" "0" "$status"
contains "a bump: the released version is read from the POMs" "$dir/output" "Released version: 0.1.1"
contains "a bump: the next snapshot version is suggested" "$dir/output" "Suggested version: 0.1.2-SNAPSHOT"
equals "a bump: it stays on the branch" "release/0_1_1" "$(in_work "$dir" git branch --show-current)"
equals "a bump: the root pom is on the next snapshot version" \
  "<project><version>0.1.2-SNAPSHOT</version></project>" "$(in_work "$dir" git show HEAD:pom.xml)"
equals "a bump: the module poms are committed too" \
  "<project><version>0.1.2-SNAPSHOT</version></project>" "$(in_work "$dir" git show HEAD:modules/pom.xml)"
equals "a bump: the commit names the released version" "build: bump version after 0.1.1" \
  "$(in_work "$dir" git log -1 --format=%s)"
equals "a bump: one commit is added" "First commit" "$(in_work "$dir" git log -1 --format=%s HEAD~1)"
equals "a bump: the coming version is added to the release notes" \
  $'## Version 0.1.2\n\n**Date:** _not yet released_\n\n*\n\n## Version 0.1.0' \
  "$(in_work "$dir" git show HEAD:docs/release-notes.md | sed -n '3,9p')"
equals "a bump: the branch is pushed" "$(in_work "$dir" git rev-parse HEAD)" \
  "$(in_work "$dir" git ls-remote --heads origin release/0_1_1 | awk '{print $1}')"
equals "a bump: main is untouched on the remote" "$(in_work "$dir" git rev-parse main)" \
  "$(in_work "$dir" git ls-remote --heads origin main | awk '{print $1}')"
equals "a bump: no tag is made" "v0.1.0" "$(in_work "$dir" git tag | tr '\n' ' ' | sed 's/ $//')"
equals "a bump: the working tree is clean" "" "$(in_work "$dir" git status --porcelain)"
contains "a bump: the merge commit is explained" "$dir/output" "Create a merge commit"

# --- other ways of answering ------------------------------------------------------------------------

# answered <name> <answers> <expected pom version>
answered() {
  local dir
  dir="$(new_sandbox 0.1.1 v0.1.0)"
  run_in_sandbox "$dir" "$BUMP" "$2"
  equals "$1" "<project><version>$3</version></project>" "$(in_work "$dir" git show HEAD:pom.xml)"
}

answered "y takes the suggestion" $'y\n' "0.1.2-SNAPSHOT"
answered "a typed version gets -SNAPSHOT" $'0.2.0\n' "0.2.0-SNAPSHOT"
answered "a typed snapshot version is taken as it is" $'0.2.0-SNAPSHOT\n' "0.2.0-SNAPSHOT"
answered "no and then a version" $'n\n1.0.0\n' "1.0.0-SNAPSHOT"

dir="$(new_sandbox 0.1.1 v0.1.0)"
run_in_sandbox "$dir" "$BUMP" $'0.2.0\n'
equals "a typed version: the release notes get that version" "## Version 0.2.0" \
  "$(in_work "$dir" git show HEAD:docs/release-notes.md | grep -m1 '^## Version')"

# --- a branch that is not yet on the remote ----------------------------------------------------------

dir="$(new_sandbox 0.1.1 v0.1.0)"
in_work "$dir" git checkout -q -b feature/thing
run_in_sandbox "$dir" "$BUMP" $'\n'
equals "a new branch: it is pushed" "$(in_work "$dir" git rev-parse HEAD)" \
  "$(in_work "$dir" git ls-remote --heads origin feature/thing | awk '{print $1}')"
equals "a new branch: it tracks the remote" "origin/feature/thing" \
  "$(in_work "$dir" git rev-parse --abbrev-ref '@{upstream}')"

# --- the release notes already have the coming version ----------------------------------------------

dir="$(new_sandbox 0.1.1 v0.1.0)"
in_work "$dir" bash -c "printf '%s' '# Release Notes

## Version 0.1.2

**Date:** _not yet released_

* Already here.
' > docs/release-notes.md && git commit -q -am 'Notes'" >/dev/null 2>&1
before="$(in_work "$dir" cat docs/release-notes.md)"
run_in_sandbox "$dir" "$BUMP" $'\n'
equals "an existing section: the script succeeds" "0" "$?"
equals "an existing section: the release notes are left as they are" "$before" \
  "$(in_work "$dir" git show HEAD:docs/release-notes.md)"
equals "an existing section: the poms are still bumped" "<project><version>0.1.2-SNAPSHOT</version></project>" \
  "$(in_work "$dir" git show HEAD:pom.xml)"

# --- checks that stop the bump before anything changes ----------------------------------------------

stops_early "a dirty working tree" "$BUMP" "The working tree has changed or untracked files" \
  $'\n' 0.1.1 \
  bash -c 'echo leftover > leftover.txt'

stops_early "a changed tracked file" "$BUMP" "The working tree has changed or untracked files" \
  $'\n' 0.1.1 \
  bash -c 'echo more >> docs/release-notes.md'

stops_early "no branch checked out" "$BUMP" "No branch is checked out" \
  $'\n' 0.1.1 \
  git checkout --quiet --detach

stops_early "the POMs are already on a snapshot" "$BUMP" "already the snapshot 0.1.1-SNAPSHOT" \
  $'\n' 0.1.1-SNAPSHOT

stops_early "the POMs are on a version that is not X.Y.Z" "$BUMP" "is not a released version of the form X.Y.Z" \
  $'\n' 0.1.1-RC1

stops_early "a typed version that is not X.Y.Z" "$BUMP" "is not a version of the form X.Y.Z" \
  $'0.2\n' 0.1.1

stops_early "a typed tag instead of a version" "$BUMP" "is not a version of the form X.Y.Z" \
  $'v0.2.0\n' 0.1.1

stops_early "an empty version after no" "$BUMP" "is not a version of the form X.Y.Z" \
  $'n\n\n' 0.1.1

stops_early "a typed version with another qualifier" "$BUMP" "is not a version of the form X.Y.Z" \
  $'0.2.0-RC1\n' 0.1.1

stops_early "a bump branch that already exists" "$BUMP" "The branch 'bump/0_1_2' already exists" \
  $'\n' 0.1.1 \
  git branch bump/0_1_2

stops_early "a bump branch that exists only on the remote" "$BUMP" "The branch 'bump/0_1_2' already exists" \
  $'\n' 0.1.1 \
  bash -c 'git branch bump/0_1_2 && git push -q origin bump/0_1_2 && git branch -D bump/0_1_2'

stops_early "main is behind the remote" "$BUMP" "main is behind origin/main" \
  $'\n' 0.1.1 \
  bash -c 'git commit -q --allow-empty -m Merged && git push -q origin main && git reset -q --hard HEAD~1'

stops_early "main is behind the remote, also with the merged release" "$BUMP" "main is behind origin/main" \
  $'\n' 0.1.1-SNAPSHOT \
  bash -c 'git commit -q --allow-empty -m Merged && git push -q origin main && git reset -q --hard HEAD~1'

summary
