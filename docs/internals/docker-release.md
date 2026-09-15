![Sweden Connect](../images/sweden-connect.png)

# Building a Release Docker Image

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

-----

Every push to `main` triggers the [`publish.yml`](../../.github/workflows/publish.yml) GitHub Actions
workflow, which builds and publishes a **snapshot** Docker image to the GitHub Container Registry
(`ghcr.io`). This image always carries whatever version is currently set in `pom.xml`'s
`service-revision` property (for example `0.11.13`).

To publish a **versioned release** image, push a git tag matching `v<version>`, for example:

```bash
git tag v0.0.0
git push origin v0.0.0
```

Pushing a tag of the form `v*` triggers [`release.yml`](../../.github/workflows/release.yml), which calls
the reusable [`docker-release.yml`](../../.github/workflows/docker-release.yml) workflow. It:

1. Sets the `service-revision` Maven property to the tag name with the leading `v` stripped
   (`v0.0.3` &rarr; `0.0.3`).
2. Builds `oidf-services` and publishes a Docker image to `ghcr.io` tagged with that version.

The tag name must start with `v` (e.g. `v1.2.0`, `v0.0.3-rc1`) — tags that do not match this pattern will
not trigger a release build.

-----

Copyright &copy; 2024-2026, [Sweden Connect](https://swedenconnect.se). Licensed under version 2.0 of the
[Apache License](https://www.apache.org/licenses/LICENSE-2.0).
