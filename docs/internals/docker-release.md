![Sweden Connect](../images/sweden-connect.png)

# Building a Release Docker Image

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

-----

The service is shipped as a Docker image in the GitHub Container Registry (`ghcr.io`), built from
the `oidf-services` module with jib. Two workflows publish it, and neither one changes the Maven
version: the version in the POMs is what the image is tagged with.

## Snapshot images

Every push to `main` triggers
[`docker-snapshot.yml`](../../.github/workflows/docker-snapshot.yml), which publishes an image
tagged with the version in the POMs, for example `0.11.16-SNAPSHOT`.

When `main` holds a release version (no `-SNAPSHOT` suffix), nothing is published, so a release
image is never overwritten by a later commit.

## Release images

To publish a versioned release image:

1. Set the release version in the POMs, e.g. `0.11.16`, and commit it:

   ```bash
   mvn versions:set -DnewVersion=0.11.16 -DprocessAllModules=true -DgenerateBackupPoms=false
   ```

2. Push an annotated git tag named `v` followed by that version:

   ```bash
   git tag -a v0.11.16 -m "Version 0.11.16"
   git push origin v0.11.16
   ```

3. Set the next snapshot version in the POMs, e.g. `0.11.17-SNAPSHOT`.

In practice all three steps are driven by [`internal/release.sh`](../../internal/release.sh), see
[internal/release.md](../../internal/release.md).

Pushing a tag of the form `v*` triggers
[`docker-release.yml`](../../.github/workflows/docker-release.yml), which:

1. Checks that the tag matches the version in the POMs (`v0.11.16` &harr; `0.11.16`) and that the
   version is not a snapshot. If either check fails, the workflow fails and nothing is published.
2. Builds the reactor and publishes a Docker image to `ghcr.io` tagged with the version and with
   `latest`.

The same tag also triggers [`github-release.yml`](../../.github/workflows/github-release.yml),
which creates a GitHub release pointing at the release notes. It builds nothing and attaches no
assets.

The tag name must start with `v`. Tags that do not match this pattern trigger neither workflow.

## Building an image locally

```bash
mvn clean compile jib:dockerBuild@local -Djib.from.platforms=linux/amd64
```

Use `linux/arm64` instead for an ARM image. The local build tags the image `local/oidf-services`
and pushes nothing.

-----

Copyright &copy; 2024-2026, [Sweden Connect](https://swedenconnect.se). Licensed under version 2.0 of the
[Apache License](https://www.apache.org/licenses/LICENSE-2.0).
