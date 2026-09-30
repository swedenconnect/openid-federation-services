#!/usr/bin/env bash
#
# Runs a release from start to finish, on a branch.
#
# The script works out the next version from the tags that exist, lets you confirm it or enter
# another one, sets that version in every pom.xml, builds the project, commits, pushes the branch,
# tags the release commit, pushes the tag, and then opens the next snapshot version on the same
# branch. Merging the branch into main is left to you, afterwards.
#
# Run from main, the release is made on a new release/X_Y_Z branch. Run from any other branch, the
# release is made on that branch.
#
# Pushing the tag is what starts publishing, so the script asks before it does that. Publishing to
# Maven Central is done by a GitHub workflow when the tag is pushed, see internal/release.md.
set -euo pipefail

REMOTE="origin"
MAIN_BRANCH="main"
WORKFLOW_URL="https://github.com/swedenconnect/openid-federation-services/actions/workflows/maven-central-deploy.yml"

# Prints the version that follows the given tag or version: the last number raised by one.
suggest_next_version() {
  local version="${1#v}" major minor patch
  major="$(echo "$version" | cut -d. -f1)"
  minor="$(echo "$version" | cut -d. -f2)"
  patch="$(echo "$version" | cut -d. -f3)"
  echo "${major}.${minor}.$((patch + 1))"
}

# Succeeds if the given version is three numbers separated by dots.
is_valid_version() {
  [[ "$1" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]
}

# Prints the snapshot version that development continues on after the given release version.
next_snapshot_version() {
  echo "$(suggest_next_version "$1")-SNAPSHOT"
}

# Prints the message of the commit that holds the given release version.
release_commit_message() {
  echo "build: $1 release"
}

# Prints the message of the commit that opens the next snapshot version after the given release.
bump_commit_message() {
  echo "build: bump version after $1"
}

# Adds a section for the given coming version at the top of the given release notes file, above the
# first "## Version" heading, with the date set to not yet released. If there is no such heading the
# section is added at the end. If the file already has a section for that version it is left as it is.
add_release_notes_section() {
  local version="$1" file="$2" tmp
  if grep -qxF "## Version $version" "$file"; then
    return 0
  fi
  tmp="$(mktemp)"
  awk -v version="$version" '
    function section() {
      print "## Version " version
      print ""
      print "**Date:** _not yet released_"
      print ""
      print "*"
      print ""
    }
    !done && /^## Version / { section(); done = 1 }
    { print; last = $0 }
    END { if (!done) { if (last != "") print ""; section() } }
  ' "$file" > "$tmp"
  cat "$tmp" > "$file"
  rm -f "$tmp"
}

# Prints the branch the release is made on. From main that is a new release/X_Y_Z branch, from any
# other branch it is that branch, which is then used as it is.
release_branch_for() {
  local current_branch="$1" version="$2"
  if [ "$current_branch" = "$MAIN_BRANCH" ]; then
    echo "release/${version//./_}"
  else
    echo "$current_branch"
  fi
}

# Succeeds if no branch of that name exists, neither here nor on the remote.
branch_is_free() {
  ! git show-ref --verify --quiet "refs/heads/$1" &&
    ! git ls-remote --exit-code --heads "$REMOTE" "$1" >/dev/null 2>&1
}

# Succeeds if no tag of that name exists, neither here nor on the remote.
tag_is_free() {
  ! git show-ref --verify --quiet "refs/tags/$1" &&
    ! git ls-remote --exit-code --tags "$REMOTE" "$1" >/dev/null 2>&1
}

main() {
  local repo_root
  repo_root="$(git rev-parse --show-toplevel)"
  cd "$repo_root"

  echo "== Release =="

  # From here down to the branch is created, nothing is changed. Every check that can stop the
  # release runs first, so a release that cannot go through leaves the repository as it was.

  if [ -n "$(git status --porcelain)" ]; then
    echo "The working tree has changed or untracked files. Commit, stash or remove them first." >&2
    git status --short >&2
    exit 1
  fi

  local current_branch
  current_branch="$(git branch --show-current)"
  if [ -z "$current_branch" ]; then
    echo "No branch is checked out. Check out $MAIN_BRANCH, or the branch you want to release from." >&2
    exit 1
  fi

  echo "Fetching tags from $REMOTE ..."
  git fetch --tags --quiet "$REMOTE" || echo "Could not fetch tags from $REMOTE, using the local tags."

  local latest_tag suggested_version
  latest_tag="$(git tag -l 'v[0-9]*.[0-9]*.[0-9]*' | sort -V | tail -1)"

  if [ -z "$latest_tag" ]; then
    echo "There is no tag of the form vX.Y.Z."
    read -r -p "Enter the first version (X.Y.Z): " suggested_version
  else
    suggested_version="$(suggest_next_version "$latest_tag")"
    echo "Latest tag: $latest_tag"
  fi

  echo "Suggested version: $suggested_version"
  read -r -p "Use this version? [Y/n/type another version]: " answer

  local version
  case "$answer" in
    ""|y|Y|yes|Yes|YES)
      version="$suggested_version"
      ;;
    n|N|no|No|NO)
      read -r -p "Enter the version (X.Y.Z): " version
      ;;
    *)
      version="$answer"
      ;;
  esac

  if ! is_valid_version "$version"; then
    echo "'$version' is not a version of the form X.Y.Z." >&2
    exit 1
  fi

  local branch tag next_version
  branch="$(release_branch_for "$current_branch" "$version")"
  tag="v$version"
  next_version="$(next_snapshot_version "$version")"

  if [ "$branch" != "$current_branch" ] && ! branch_is_free "$branch"; then
    echo "The branch '$branch' already exists here or on $REMOTE. Remove it, or pick another version." >&2
    exit 1
  fi

  if ! tag_is_free "$tag"; then
    echo "The tag '$tag' already exists here or on $REMOTE. Version $version has been released." >&2
    exit 1
  fi

  # The checks are done. From here on the repository is changed.

  if [ "$branch" != "$current_branch" ]; then
    echo "Creating the branch '$branch' from '$current_branch' ..."
    git checkout -b "$branch"
  else
    echo "Releasing on the current branch '$branch'."
  fi

  echo "Setting the version to $version in every pom.xml ..."
  mvn versions:set -DnewVersion="$version" -DprocessAllModules=true -DgenerateBackupPoms=false

  echo "Building the project ..."
  mvn clean install

  echo
  echo "== Release notes =="
  echo "Update docs/release-notes.md with what is in version $version now, before you continue."
  read -r -p "Press Enter once the release notes are updated (or Ctrl+C to stop here) ..." _

  git add -- '**/pom.xml' pom.xml docs/release-notes.md
  git commit -m "$(release_commit_message "$version")"

  echo "Pushing '$branch' to $REMOTE ..."
  git push -u "$REMOTE" "$branch"

  echo
  echo "== Tagging =="
  echo "Pushing the tag $tag starts publishing. The artifacts go to Maven Central and the Docker"
  echo "image is pushed. A version that has been published cannot be removed or replaced."
  read -r -p "Create the tag $tag and push it? [y/N]: " tag_answer

  case "$tag_answer" in
    y|Y|yes|Yes|YES)
      ;;
    *)
      echo
      echo "Stopped before tagging."
      echo "The branch '$branch' is pushed to $REMOTE and holds version $version."
      echo "There is no tag and nothing has been published."
      echo "To tag later, from that commit:"
      echo
      echo "    git tag -a $tag -m \"Version $version\""
      echo "    git push $REMOTE $tag"
      exit 0
      ;;
  esac

  echo "Tagging $tag and pushing it ..."
  git tag -a "$tag" -m "Version $version"
  git push "$REMOTE" "$tag"

  echo
  echo "== Publishing =="
  echo "The tag started the Maven Central workflow, the Docker release workflow and the GitHub"
  echo "release workflow. Follow the Maven Central run here:"
  echo
  echo "    $WORKFLOW_URL"
  echo
  echo "It publishes nothing unless every deployed module is at version $version."

  echo
  echo "== Next snapshot version =="
  echo "Setting the version to $next_version in every pom.xml ..."
  mvn versions:set -DnewVersion="$next_version" -DprocessAllModules=true -DgenerateBackupPoms=false

  echo "Adding version ${next_version%-SNAPSHOT} to docs/release-notes.md ..."
  add_release_notes_section "${next_version%-SNAPSHOT}" docs/release-notes.md

  git add -- '**/pom.xml' pom.xml docs/release-notes.md
  git commit -m "$(bump_commit_message "$version")"
  git push "$REMOTE" "$branch"

  echo
  echo "Done. Version $version is tagged as $tag, and '$branch' is now on $next_version."
  echo
  echo "== What is left =="
  echo "Open a pull request from '$branch' into $MAIN_BRANCH and merge it with \"Create a merge"
  echo "commit\". The other two buttons, \"Squash and merge\" and \"Rebase and merge\", write new"
  echo "commits onto $MAIN_BRANCH. The commit that $tag points at would then not be part of the"
  echo "history of $MAIN_BRANCH, and GitHub would not show $tag on $MAIN_BRANCH."
  echo
  echo "Check that the Maven Central workflow for $tag succeeded."
}

if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
  main "$@"
fi
