#!/usr/bin/env bash
#
# Checks that the project may be published under a given release tag.
#
# The tag must be "v" followed by the version in the POMs, and that version must not be a snapshot.
# Every module that is actually deployed is checked, not only the root, so a module left behind at
# an older parent version stops the release before anything is uploaded.
#
# Usage:
#     internal/check-release-version.sh <tag> [<modules-file>]
#
# <modules-file> exists for the tests. It replaces the interrogation of the POMs with pre-computed
# lines of "<pom path> <version> <deploy skip>". Without it the versions are read from the POMs
# with the Maven help plugin.
#
# Exit status: 0 when the release may go ahead, 1 when it must not, 2 on a usage error.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Prints a failure so that it also shows up as an annotation when running in GitHub Actions.
fail() {
  if [ "${GITHUB_ACTIONS:-}" = "true" ]; then
    echo "::error::$1"
  else
    echo "$1" >&2
  fi
}

# Prints one "<pom path> <version> <deploy skip>" line per module in the reactor. The deploy skip
# column is "true" for modules that set maven.deploy.skip, and the Maven help plugin's placeholder
# text for the rest.
list_modules() {
  local pom version skip
  while IFS= read -r pom; do
    if ! version="$(mvn -q --no-transfer-progress -f "$pom" -Dexpression=project.version -DforceStdout help:evaluate)" ||
       ! skip="$(mvn -q --no-transfer-progress -f "$pom" -Dexpression=maven.deploy.skip -DforceStdout help:evaluate)"; then
      echo "Could not read the version of ${pom}" >&2
      exit 1
    fi
    echo "${pom} ${version} ${skip}"
  done < <(find . -name pom.xml -not -path '*/target/*' | sort)
}

TAG="${1:-}"
MODULES_FILE="${2:-}"

if [ -z "$TAG" ]; then
  echo "Usage: $0 <tag> [<modules-file>]" >&2
  exit 2
fi

if [ "${TAG#v}" = "$TAG" ]; then
  fail "The tag ${TAG} does not start with 'v' - a release tag is 'v' followed by the version"
  exit 1
fi

EXPECTED_VERSION="${TAG#v}"

if [ -z "$EXPECTED_VERSION" ]; then
  fail "The tag ${TAG} carries no version - a release tag is 'v' followed by the version"
  exit 1
fi

if [ -n "$MODULES_FILE" ]; then
  MODULES="$(cat "$MODULES_FILE")"
else
  cd "$REPO_ROOT"
  MODULES="$(list_modules)"
fi

echo "Checking that every deployed module is at version ${EXPECTED_VERSION}, as required by the tag ${TAG}."

deployed=0
errors=0

while read -r pom version skip; do
  [ -z "$pom" ] && continue
  if [ "$skip" = "true" ]; then
    echo "  ${pom}: ${version} - not deployed, not checked"
    continue
  fi
  deployed=$((deployed + 1))
  if [ "${version%-SNAPSHOT}" != "$version" ]; then
    fail "The version of ${pom} is the snapshot ${version} - set the release version in the POMs before tagging"
    errors=$((errors + 1))
  elif [ "$version" != "$EXPECTED_VERSION" ]; then
    fail "The version of ${pom} is ${version}, but the tag ${TAG} requires ${EXPECTED_VERSION}"
    errors=$((errors + 1))
  else
    echo "  ${pom}: ${version} - ok"
  fi
done <<< "$MODULES"

if [ "$deployed" -eq 0 ]; then
  fail "No module would be deployed - there is nothing to publish"
  exit 1
fi

if [ "$errors" -gt 0 ]; then
  fail "${errors} module(s) do not match the tag ${TAG} - nothing has been published"
  exit 1
fi

echo "All ${deployed} deployed module(s) are at version ${EXPECTED_VERSION}."
