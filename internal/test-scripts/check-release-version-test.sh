#!/usr/bin/env bash
#
# Tests for internal/check-release-version.sh.
#
# Most cases feed the check a pre-computed module list, so they run without Maven. The last case
# runs the real thing against this repository, which covers reading the versions out of the POMs.
#
# Usage:
#     internal/test-scripts/check-release-version-test.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECK="${HERE}/../check-release-version.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

passed=0
failed=0

# expect <name> <expected exit status> <expected text in the output> <tag> [<module lines>]
expect() {
  local name="$1" expected_status="$2" expected_text="$3" tag="$4" modules="${5-}"
  local modules_file="" output status

  if [ -n "$modules" ]; then
    modules_file="${WORK}/modules.txt"
    printf '%s\n' "$modules" > "$modules_file"
  fi

  output="$(GITHUB_ACTIONS= "$CHECK" "$tag" $modules_file 2>&1)"
  status=$?

  if [ "$status" -ne "$expected_status" ]; then
    echo "FAIL ${name}: exit status ${status}, expected ${expected_status}"
    echo "${output}" | sed 's/^/     /'
    failed=$((failed + 1))
    return
  fi
  if [ -n "$expected_text" ] && [[ "$output" != *"$expected_text"* ]]; then
    echo "FAIL ${name}: the output does not mention '${expected_text}'"
    echo "${output}" | sed 's/^/     /'
    failed=$((failed + 1))
    return
  fi
  echo "PASS ${name}"
  passed=$((passed + 1))
}

ALL_MATCH='./pom.xml 1.2.3 null object or invalid expression
./oidf-modules/pom.xml 1.2.3 null object or invalid expression
./oidf-modules/common/pom.xml 1.2.3 null object or invalid expression
./oidf-services/pom.xml 1.2.3 true'

expect "every deployed module matches the tag" 0 "All 3 deployed module(s) are at version 1.2.3" \
  v1.2.3 "$ALL_MATCH"

expect "a snapshot version is rejected" 1 "is the snapshot 1.2.3-SNAPSHOT" \
  v1.2.3 './pom.xml 1.2.3-SNAPSHOT null object or invalid expression'

expect "a snapshot is rejected even when the tag names the snapshot" 1 "is the snapshot 1.2.3-SNAPSHOT" \
  v1.2.3-SNAPSHOT './pom.xml 1.2.3-SNAPSHOT null object or invalid expression'

expect "a module left behind at an older version is rejected" 1 "is 1.2.2, but the tag v1.2.3 requires 1.2.3" \
  v1.2.3 './pom.xml 1.2.3 null object or invalid expression
./oidf-modules/common/pom.xml 1.2.2 null object or invalid expression'

expect "a tag that is a prefix of the version is rejected" 1 "but the tag v1.2.3 requires 1.2.3" \
  v1.2.3 './pom.xml 1.2.30 null object or invalid expression'

expect "a version that is a prefix of the tag is rejected" 1 "but the tag v1.2.30 requires 1.2.30" \
  v1.2.30 './pom.xml 1.2.3 null object or invalid expression'

expect "a tag without the v prefix is rejected" 1 "does not start with 'v'" \
  1.2.3 './pom.xml 1.2.3 null object or invalid expression'

expect "a tag of nothing but the prefix is rejected" 1 "carries no version" \
  v './pom.xml 1.2.3 null object or invalid expression'

expect "a version with a qualifier is accepted when the tag carries it too" 0 "All 1 deployed module(s)" \
  v1.2.3-RC1 './pom.xml 1.2.3-RC1 null object or invalid expression'

expect "a module that is not deployed is not checked" 0 "All 1 deployed module(s)" \
  v1.2.3 './pom.xml 1.2.3 null object or invalid expression
./oidf-services/pom.xml 9.9.9-SNAPSHOT true'

expect "nothing to publish is rejected" 1 "No module would be deployed" \
  v1.2.3 './oidf-services/pom.xml 1.2.3 true'

expect "a missing tag is a usage error" 2 "Usage:" ""

echo
echo "Reading the versions out of the POMs of this repository (needs Maven) ..."
POM_VERSION="$(mvn -q --no-transfer-progress -f "${HERE}/../../pom.xml" -Dexpression=project.version -DforceStdout help:evaluate)"
if [ "${POM_VERSION%-SNAPSHOT}" != "$POM_VERSION" ]; then
  expect "this repository, on a snapshot version, is rejected" 1 "is the snapshot ${POM_VERSION}" "v${POM_VERSION%-SNAPSHOT}"
else
  expect "this repository, on the release version, is accepted" 0 "are at version ${POM_VERSION}" "v${POM_VERSION}"
  expect "this repository, under a tag for another version, is rejected" 1 "requires 0.0.1" "v0.0.1"
fi

echo
echo "${passed} passed, ${failed} failed"
[ "$failed" -eq 0 ]
