#!/usr/bin/env bash
#
# Tests for internal/release.sh.
#
# The helpers that work out the version and the branch are called directly. The rest is run from
# start to finish in a throwaway git repository that has its own origin and a stand-in for mvn on
# the PATH, so nothing reaches the real origin and no real tag is made.
#
# Usage:
#     internal/test-scripts/release-test.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE="${HERE}/../release.sh"

# shellcheck source=release.sh
source "$RELEASE"
set +e +u   # release.sh turns these on, the harness needs them off

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

passed=0
failed=0

ok() {
  echo "PASS $1"
  passed=$((passed + 1))
}

no() {
  echo "FAIL $1: $2"
  failed=$((failed + 1))
}

# equals <name> <expected> <actual>
equals() {
  if [ "$2" = "$3" ]; then ok "$1"; else no "$1" "expected '$2', got '$3'"; fi
}

# accepted <name> <version>
accepted() {
  if is_valid_version "$2"; then ok "$1"; else no "$1" "'$2' was rejected"; fi
}

# rejected <name> <version>
rejected() {
  if is_valid_version "$2"; then no "$1" "'$2' was accepted"; else ok "$1"; fi
}

# contains <name> <file> <text>
contains() {
  if grep -qF -- "$3" "$2"; then ok "$1"; else no "$1" "the output does not mention '$3'"; fi
}

# absent <name> <file> <text>
absent() {
  if grep -qF -- "$3" "$2"; then no "$1" "the output mentions '$3'"; else ok "$1"; fi
}

echo "== Working out the version =="

equals "the next version raises the last number" "0.11.17" "$(suggest_next_version v0.11.16)"
equals "the next version after a nine carries into two digits" "1.2.10" "$(suggest_next_version v1.2.9)"
equals "the next version also works without the v" "0.1.1" "$(suggest_next_version 0.1.0)"
equals "the next snapshot version" "0.11.17-SNAPSHOT" "$(next_snapshot_version 0.11.16)"

echo
echo "== Commit messages =="

equals "the release commit message" "build: 0.11.17 release" "$(release_commit_message 0.11.17)"
equals "the bump commit message names the released version" "build: bump version after 0.11.17" \
  "$(bump_commit_message 0.11.17)"

accepted "three numbers are a version" "0.11.16"
accepted "two digits in a number are a version" "1.2.10"
rejected "two numbers are not a version" "0.11"
rejected "four numbers are not a version" "0.11.16.1"
rejected "a tag is not a version" "v0.11.16"
rejected "a snapshot is not a version" "0.11.16-SNAPSHOT"
rejected "empty is not a version" ""
rejected "a letter is not a version" "0.11.x"

echo
echo "== Release notes for the coming version =="

NOTES_HEADER=$'# Release notes\n\n'
NEW_SECTION=$'## Version 0.1.2\n\n**Date:** _not yet released_\n\n*\n\n'
OLD_SECTION=$'## Version 0.1.1\n\n**Date:** 2026-09-30\n\n* A fix.\n'

notes="${WORK}/notes.md"
printf '%s' "${NOTES_HEADER}${OLD_SECTION}" > "$notes"
add_release_notes_section 0.1.2 "$notes"
equals "the section is added above the latest version" "${NOTES_HEADER}${NEW_SECTION}${OLD_SECTION}x" \
  "$(cat "$notes"; printf x)"
equals "the section is not added twice" "${NOTES_HEADER}${NEW_SECTION}${OLD_SECTION}x" \
  "$(add_release_notes_section 0.1.2 "$notes"; cat "$notes"; printf x)"

printf '%s' "$NOTES_HEADER" > "$notes"
add_release_notes_section 0.1.2 "$notes"
equals "with no version yet the section is added at the end" "${NOTES_HEADER}${NEW_SECTION}x" \
  "$(cat "$notes"; printf x)"

echo
echo "== Choosing the branch =="

equals "from main a release branch is made" "release/0_11_16" "$(release_branch_for main 0.11.16)"
equals "the release branch name uses underscores" "release/1_2_10" "$(release_branch_for main 1.2.10)"
equals "from another branch that branch is used" "feature/thing" "$(release_branch_for feature/thing 0.11.16)"
equals "from a release branch that branch is used" "release/0_11_16" "$(release_branch_for release/0_11_16 0.11.16)"

echo
echo "== Running the script =="

# Builds a throwaway repository with its own origin and a stand-in for mvn. Prints its directory,
# which holds "work" (the working copy) and "origin.git" (what the script pushes to).
new_sandbox() {
  local dir
  dir="$(mktemp -d "${WORK}/sandbox.XXXXXX")"

  mkdir -p "$dir/bin"
  cat > "$dir/bin/mvn" <<'MVN'
#!/usr/bin/env bash
# Stands in for Maven: records the call, and for versions:set rewrites every pom.xml, the way
# versions:set -DprocessAllModules=true does.
echo "$*" >> "${MVN_LOG}"
for arg in "$@"; do
  case "$arg" in
    -DnewVersion=*)
      while IFS= read -r pom; do
        printf '<project><version>%s</version></project>\n' "${arg#-DnewVersion=}" > "$pom"
      done < <(find . -name pom.xml -not -path './.git/*')
      ;;
  esac
done
MVN
  chmod +x "$dir/bin/mvn"

  git init --quiet --bare "$dir/origin.git"
  git init --quiet --initial-branch=main "$dir/work"
  (
    cd "$dir/work" || exit 1
    git config user.email "release-test@example.com"
    git config user.name "Release Test"
    git config commit.gpgsign false
    git config tag.gpgSign false
    mkdir docs modules
    printf '<project><version>0.1.0-SNAPSHOT</version></project>\n' > pom.xml
    printf '<project><version>0.1.0-SNAPSHOT</version></project>\n' > modules/pom.xml
    printf '# Release notes\n\n## Version 0.1.0\n\n**Date:** 2026-01-01\n\n* First.\n' > docs/release-notes.md
    git add -A
    git commit --quiet -m "First commit"
    git remote add origin "$dir/origin.git"
    git push --quiet -u origin main
    git tag -a v0.1.0 -m "Version 0.1.0"
    git push --quiet origin v0.1.0
  ) >/dev/null 2>&1

  echo "$dir"
}

# run_release <sandbox> <answers> - runs the script with those answers on standard input. The
# output lands in <sandbox>/output and the exit status is returned.
run_release() {
  local dir="$1" answers="$2"
  (
    cd "$dir/work" || exit 1
    export HOME="$dir"
    export PATH="$dir/bin:$PATH"
    export MVN_LOG="$dir/mvn.log"
    printf '%s' "$answers" | bash "$RELEASE"
  ) > "$dir/output" 2>&1
  return $?
}

# Everything the release could change, as one string, so a run that must change nothing can be
# checked by comparing before and after.
state_of() {
  (
    cd "$1/work" || exit 1
    echo "head $(git rev-parse HEAD)"
    echo "branch $(git branch --show-current)"
    echo "branches $(git for-each-ref --format='%(refname:short)' refs/heads | sort | tr '\n' ' ')"
    echo "tags $(git tag | sort | tr '\n' ' ')"
    echo "poms $(cat pom.xml modules/pom.xml)"
    echo "remote $(git ls-remote "$1/origin.git" | sort | tr '\n' ' ')"
  )
}

in_work() {
  local dir="$1"
  shift
  (cd "$dir/work" && "$@")
}

ANSWER_FULL_RELEASE=$'y\n\ny\n'

# --- a release from main -------------------------------------------------------------------------

dir="$(new_sandbox)"
in_work "$dir" git tag -a v0.0.9 -m "A stray tag" >/dev/null 2>&1
run_release "$dir" "$ANSWER_FULL_RELEASE"
status=$?

equals "from main: the script succeeds" "0" "$status"
equals "from main: the release branch is checked out" "release/0_1_1" "$(in_work "$dir" git branch --show-current)"
equals "from main: the release is tagged" "v0.1.1" "$(in_work "$dir" git tag -l v0.1.1)"
equals "from main: the tag is on the release commit" "build: 0.1.1 release" \
  "$(in_work "$dir" git log -1 --format=%s v0.1.1)"
equals "from main: the branch ends with the bump commit" "build: bump version after 0.1.1" \
  "$(in_work "$dir" git log -1 --format=%s release/0_1_1)"
equals "from main: the bump commit follows the release commit" "$(in_work "$dir" git rev-parse v0.1.1^{commit})" \
  "$(in_work "$dir" git rev-parse release/0_1_1~1)"
equals "from main: the branch is pushed" "release/0_1_1" \
  "$(in_work "$dir" git ls-remote --heads origin release/0_1_1 | awk '{print $2}' | sed 's|refs/heads/||')"
equals "from main: the tag is pushed" "v0.1.1" \
  "$(in_work "$dir" git ls-remote --tags origin v0.1.1 | awk '{print $2}' | sed 's|refs/tags/||' | head -1)"
equals "from main: only the new tag is pushed" "" \
  "$(in_work "$dir" git ls-remote --tags origin v0.0.9 | awk '{print $2}')"
equals "from main: the branch is left on the next snapshot version" \
  "<project><version>0.1.2-SNAPSHOT</version></project>" "$(in_work "$dir" cat pom.xml)"
equals "from main: the module poms are committed too" \
  "<project><version>0.1.1</version></project>" "$(in_work "$dir" git show v0.1.1:modules/pom.xml)"
equals "from main: the bump commit adds the coming version to the release notes" \
  $'## Version 0.1.2\n\n**Date:** _not yet released_\n\n*\n\n## Version 0.1.0' \
  "$(in_work "$dir" git show release/0_1_1:docs/release-notes.md | sed -n '3,9p')"
equals "from main: the release commit has no section for the coming version" "" \
  "$(in_work "$dir" git show v0.1.1:docs/release-notes.md | grep -F 'Version 0.1.2')"
equals "from main: main is untouched" "First commit" "$(in_work "$dir" git log -1 --format=%s main)"
contains "from main: the merge is explained" "$dir/output" "Create a merge"
contains "from main: the workflow run is pointed at" "$dir/output" "maven-central-deploy.yml"

# --- a release from another branch ---------------------------------------------------------------

dir="$(new_sandbox)"
in_work "$dir" git checkout --quiet -b feature/thing
run_release "$dir" "$ANSWER_FULL_RELEASE"
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

dir="$(new_sandbox)"
run_release "$dir" $'y\n\nn\n'
status=$?

equals "no tag: the script ends without an error" "0" "$status"
equals "no tag: nothing is tagged" "" "$(in_work "$dir" git tag -l v0.1.1)"
equals "no tag: nothing is tagged on the remote" "" \
  "$(in_work "$dir" git ls-remote --tags origin v0.1.1 | awk '{print $2}')"
equals "no tag: the branch is still pushed" "release/0_1_1" \
  "$(in_work "$dir" git ls-remote --heads origin release/0_1_1 | awk '{print $2}' | sed 's|refs/heads/||')"
equals "no tag: the branch holds the release version" \
  "<project><version>0.1.1</version></project>" "$(in_work "$dir" cat pom.xml)"
contains "no tag: the state is explained" "$dir/output" "Stopped before tagging."

# --- checks that stop the release before anything changes ------------------------------------------

# stops_early <name> <text the output must hold> <answers> <setup command ...>
stops_early() {
  local name="$1" text="$2" answers="$3"
  shift 3
  local dir before after status
  dir="$(new_sandbox)"
  if [ "$#" -gt 0 ]; then
    in_work "$dir" "$@" >/dev/null 2>&1
  fi
  before="$(state_of "$dir")"
  run_release "$dir" "$answers"
  status=$?
  after="$(state_of "$dir")"

  equals "${name}: the script stops with an error" "1" "$status"
  contains "${name}: the reason is given" "$dir/output" "$text"
  absent "${name}: nothing is built" "$dir/output" "Building the project"
  if [ "$before" = "$after" ]; then
    ok "${name}: nothing is changed"
  else
    no "${name}: nothing is changed" "the repository changed:
$(diff <(echo "$before") <(echo "$after") | sed 's/^/     /')"
  fi
}

stops_early "a dirty working tree" "The working tree has changed or untracked files" \
  "$ANSWER_FULL_RELEASE" \
  bash -c 'echo leftover > leftover.txt'

stops_early "no branch checked out" "No branch is checked out" \
  "$ANSWER_FULL_RELEASE" \
  git checkout --quiet --detach

stops_early "a version that is not X.Y.Z" "is not a version of the form X.Y.Z" \
  $'n\n0.11\n'

stops_early "a version that is already tagged" "The tag 'v0.1.0' already exists" \
  $'0.1.0\n'

stops_early "a release branch that already exists" "The branch 'release/0_1_1' already exists" \
  "$ANSWER_FULL_RELEASE" \
  git branch release/0_1_1

stops_early "a release branch that exists only on the remote" "The branch 'release/0_1_1' already exists" \
  "$ANSWER_FULL_RELEASE" \
  bash -c 'git branch release/0_1_1 && git push -q origin release/0_1_1 && git branch -D release/0_1_1'

# The two checks also look at the remote, not only at what is here. Running the whole script does
# not reach that, because the fetch at the start brings a remote tag in first, so they are called
# directly against refs that exist only on the remote.

dir="$(new_sandbox)"
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

echo
echo "${passed} passed, ${failed} failed"
[ "$failed" -eq 0 ]
