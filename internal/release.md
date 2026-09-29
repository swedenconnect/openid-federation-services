# Release Instructions

How to cut a release of OpenID Federation Services.

Maintainer documentation. Creating release tags is restricted to the maintainers.

## Tagging rules

Four rules, all of which exist because a tag is a permanent record of where the released
artifacts came from:

1. **Prefix with `v`.** The tag for version `0.11.16` is `v0.11.16`.

2. **Make the tag annotated**, never lightweight, so it records who tagged it, when and why:

   ```bash
   git tag -a v0.11.16 -m "Version 0.11.16"
   ```

   The release script does this for you.

3. **Tag a commit on `main`.** The release is prepared on a branch, so merge it first and tag the
   merged commit. A tag on a commit that never reaches `main` leaves the released artifacts with
   no reachable source.

4. **Land the version bump on `main` before tagging.** The version in the POMs at the tagged
   commit must equal the version being released. The Docker release workflow enforces this: it
   fails if the tag does not match the Maven version, or if the version is a snapshot.

### Never re-point or delete a release tag

Once a tag has been pushed and artifacts have been built from it, do not move it to a tidier
commit and do not delete it, even if its commit is not on `main`. Reproducibility matters more
than a neat history. If a tag is wrong, release a new version.

### Never reuse a published version

Maven Central is immutable. A version that has been published cannot be altered or withdrawn, so
two different artifact sets sharing one set of coordinates is a permanent inconsistency.

## Prerequisites

- Clean working tree, checked out on `main` with the latest changes pulled.
- Push access to `origin`.
- Maven installed and able to build the project locally.
- A running Docker daemon, since the build runs the integration test suites.

## Run the release script

Run the release script from the repository root:

```
./internal/release.sh
```

The script will:

1. Verify the working tree is clean.
2. Fetch tags from `origin` and suggest the next version (latest `vX.Y.Z` tag, patch bumped by one).
3. Ask you to confirm the suggested version or enter a different one.
4. Create a `release_X_Y_Z` branch (underscores, e.g. `release_0_11_16`).
5. Set the new version in every `pom.xml`, via `mvn versions:set -DprocessAllModules=true`.
6. Run `mvn clean install` to build and test the release version.
7. Pause and remind you to update [`docs/release-notes.md`](../docs/release-notes.md) with the
   changes in this release. Do this now, before continuing.
8. Commit the version bump and release notes as `choir: Prepare release X.Y.Z`.
9. Ask whether to push the branch to `origin`.
10. Pause again and wait for you to open a pull request from `release_X_Y_Z` into `main`, get it
    reviewed, and merge it. Press Enter once it is merged (or Ctrl+C to abort here, nothing below
    this point has run yet).
11. Check out `main`, pull the latest, create the annotated tag `vX.Y.Z` on the merge commit, and
    push the tag to `origin`. Pushing the tag triggers the Docker release workflow
    ([`docker-release.yml`](../.github/workflows/docker-release.yml)) and the GitHub release
    workflow ([`github-release.yml`](../.github/workflows/github-release.yml)).
12. Print a reminder that the Maven Central deploy is manual, and pause.
13. Bump the version in every `pom.xml` to the next patch version with a `-SNAPSHOT` suffix,
    commit as `choir: new version`, and push directly to `main`.

## Publishing to Maven Central

This is the one step the script does not do. Run it by hand, from the tagged commit:

```bash
git checkout v0.11.16
mvn -Prelease clean deploy
```

Running it from the tagged commit matters: the artifacts that reach Central must come from the
same source as the tag they claim to be built from.

### What the `release` profile does

The `release` profile in the parent POM attaches the sources and javadoc jars Central requires,
signs every artifact with GPG, and uploads the bundle through the Sonatype Central publishing
plugin. `autoPublish` is on, so a bundle that passes validation goes live without a manual step in
the Central portal.

A plain `mvn deploy`, without the profile, still publishes to GitHub Packages as it always has.

### What must be set up first

1. **A Central portal token**, as a server with id `central` in `~/.m2/settings.xml`:

   ```xml
   <server>
     <id>central</id>
     <username><!-- token username --></username>
     <password><!-- token password --></password>
   </server>
   ```

2. **A published GPG key.** The key must be available to `gpg` on the machine and its public half
   uploaded to a keyserver Central checks, otherwise validation rejects the bundle.

### What is published

| Published | Not published |
|-----------|----------------|
| `oidf-parent` | `oidf-services` |
| `oidf-modules` and `common`, `resolver`, `trustanchor`, `trustmarkissuer` | |
| `oidf-starters` and `federation-starter`, `trustanchor-starter`, `trustmarkissuer-starter`, `resolver-starter` | |

`oidf-services` is the deployable application, not a library, and sets `maven.deploy.skip`. It is
shipped as a Docker image instead.

The POMs are published as they are written, not flattened, so the parent POMs have to be published
along with the modules that inherit from them. That is why `oidf-parent`, `oidf-modules` and
`oidf-starters` are in the list.

`resolver` also publishes a test jar, which `oidf-services` needs for its tests. It is signed along
with everything else.

### Dry run first

Central is immutable, so verify the bundle before running `deploy`:

```bash
mvn -Prelease -Dgpg.skip=true clean verify
```

This builds every artifact the release would upload, including the sources and javadoc jars, but
signs nothing and uploads nothing.

## Version scheme

- Tags are `vX.Y.Z` (e.g. `v0.11.16`).
- Only the root `pom.xml` states a version of its own. Every module inherits it from its parent
  and states only the parent version, as a literal value. There is no version property, so the only
  supported way to change the version is `mvn versions:set`, which moves the root and every parent
  reference at once.
- The version on `main` always matches the last tag without the `v` prefix, with the patch number
  bumped and a `-SNAPSHOT` suffix, while in development.
- Releases are patch bumps unless a change explicitly warrants a minor or major bump. If so, answer
  the version prompt in `release.sh` with the version you want instead of accepting the suggestion.

## Troubleshooting

- **"Working tree has untracked or modified files"**: the script refuses to start with a dirty
  working tree. Commit, stash, or clean up first.
- **"Branch ... already exists"**: a `release_X_Y_Z` branch already exists locally or on `origin`.
  Delete it or pick a different version.
- **"Tag ... already exists"**: `vX.Y.Z` is already tagged locally or on `origin`. This should not
  happen unless a release was already cut for that version, or a previous run of the script got
  interrupted after tagging.
- If `mvn clean install` fails during step 6, fix the issue on the release branch, commit, and
  re-run `mvn versions:set` and `mvn clean install` manually. There is no need to restart the whole
  script.
- If the script is interrupted after the branch was merged but before tagging (steps 11 to 13), do
  not re-run it from `main`: since `release_X_Y_Z` already exists it fails at branch creation. Run
  the tag and version bump commands from steps 11 and 13 above by hand instead.
- **The Docker release workflow failed on the version check.** The tag does not match the version
  in the POMs, or the version is still a snapshot. Nothing was published. Fix the version on `main`
  and release under a new version number rather than moving the tag.
