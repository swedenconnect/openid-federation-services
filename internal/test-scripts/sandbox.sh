#!/usr/bin/env bash
#
# Helpers shared by release-test.sh and post-release-bump-test.sh. Sourced, not run.
#
# A sandbox is a throwaway git repository with its own origin and a stand-in for mvn on the PATH, so
# a script under test never reaches the real origin, never makes a real tag and never changes this
# repository. The caller sets WORK to a temporary directory that it removes when done.

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

# contains <name> <file> <text>
contains() {
  if grep -qF -- "$3" "$2"; then ok "$1"; else no "$1" "the output does not mention '$3'"; fi
}

# absent <name> <file> <text>
absent() {
  if grep -qF -- "$3" "$2"; then no "$1" "the output mentions '$3'"; else ok "$1"; fi
}

# The release notes of a new sandbox, in the layout of docs/release-notes.md.
SANDBOX_NOTES=$'# Release Notes\n\n## Version 0.1.0\n\n**Date:** 2026-01-01\n\n* First.\n\nCopyright\n'

# new_sandbox <version> [<tag> ...] - builds a throwaway repository whose POMs are at <version>, with
# the given tags pushed to its origin. Prints its directory, which holds "work" (the working copy)
# and "origin.git" (what the scripts push to).
new_sandbox() {
  local version="$1" dir
  shift
  dir="$(mktemp -d "${WORK}/sandbox.XXXXXX")"

  mkdir -p "$dir/bin"
  cat > "$dir/bin/mvn" <<'MVN'
#!/usr/bin/env bash
# Stands in for Maven: records the call, prints the version of the root pom.xml for help:evaluate,
# and for versions:set rewrites every pom.xml, the way versions:set -DprocessAllModules=true does.
echo "$*" >> "${MVN_LOG}"
for arg in "$@"; do
  case "$arg" in
    help:evaluate)
      sed -n 's|.*<version>\(.*\)</version>.*|\1|p' pom.xml | head -1 | tr -d '\n'
      ;;
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
    printf '<project><version>%s</version></project>\n' "$version" > pom.xml
    printf '<project><version>%s</version></project>\n' "$version" > modules/pom.xml
    printf '%s' "$SANDBOX_NOTES" > docs/release-notes.md
    git add -A
    git commit --quiet -m "First commit"
    git remote add origin "$dir/origin.git"
    git push --quiet -u origin main
    local tag
    for tag in "$@"; do
      git tag -a "$tag" -m "Version ${tag#v}"
      git push --quiet origin "$tag"
    done
  ) >/dev/null 2>&1

  echo "$dir"
}

# run_in_sandbox <sandbox> <script> <answers> - runs the script with those answers on standard input.
# The output lands in <sandbox>/output and the exit status is returned.
run_in_sandbox() {
  local dir="$1" script="$2" answers="$3"
  (
    cd "$dir/work" || exit 1
    export HOME="$dir"
    export GIT_CONFIG_NOSYSTEM=1
    export PATH="$dir/bin:$PATH"
    export MVN_LOG="$dir/mvn.log"
    printf '%s' "$answers" | bash "$script"
  ) > "$dir/output" 2>&1
  return $?
}

# Everything a script could change, as one string, so a run that must change nothing can be checked
# by comparing before and after.
state_of() {
  (
    cd "$1/work" || exit 1
    echo "head $(git rev-parse HEAD)"
    echo "branch $(git branch --show-current)"
    echo "branches $(git for-each-ref --format='%(refname:short)' refs/heads | sort | tr '\n' ' ')"
    echo "tags $(git tag | sort | tr '\n' ' ')"
    echo "poms $(cat pom.xml modules/pom.xml)"
    echo "notes $(cat docs/release-notes.md)"
    echo "status $(git status --porcelain)"
    echo "remote $(git ls-remote "$1/origin.git" | sort | tr '\n' ' ')"
  )
}

in_work() {
  local dir="$1"
  shift
  (cd "$dir/work" && "$@")
}

# stops_early <name> <script> <text the output must hold> <answers> <sandbox version> <setup command ...>
# Runs the script in a new sandbox, after the setup command, and checks that it stops with an error,
# gives the reason, never runs Maven and leaves the repository exactly as it was.
stops_early() {
  local name="$1" script="$2" text="$3" answers="$4" version="$5"
  shift 5
  local dir before after status
  dir="$(new_sandbox "$version" v0.1.0)"
  if [ "$#" -gt 0 ]; then
    in_work "$dir" "$@" >/dev/null 2>&1
  fi
  before="$(state_of "$dir")"
  run_in_sandbox "$dir" "$script" "$answers"
  status=$?
  after="$(state_of "$dir")"

  equals "${name}: the script stops with an error" "1" "$status"
  contains "${name}: the reason is given" "$dir/output" "$text"
  if grep -qE 'versions:set|clean install' "$dir/mvn.log" 2>/dev/null; then
    no "${name}: nothing is built or set" "Maven was run: $(cat "$dir/mvn.log")"
  else
    ok "${name}: nothing is built or set"
  fi
  if [ "$before" = "$after" ]; then
    ok "${name}: nothing is changed"
  else
    no "${name}: nothing is changed" "the repository changed:
$(diff <(echo "$before") <(echo "$after") | sed 's/^/     /')"
  fi
}

# Prints the summary and fails if any test failed.
summary() {
  echo
  echo "${passed} passed, ${failed} failed"
  [ "$failed" -eq 0 ]
}
