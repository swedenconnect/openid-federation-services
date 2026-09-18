# Release Instructions

How to cut a release of OpenID Federation Services.

## Prerequisites

- Clean working tree, checked out on `main` with the latest changes pulled.
- Push access to `origin`.
- Maven installed and able to build the project locally.

## Run the release script

Run the release script from the repository root:

```
./internal/release.sh
```

The script will:

1. Verify the working tree is clean.
2. Fetch tags from `origin` and suggest the next version (latest `vX.Y.Z` tag, patch bumped by one).
3. Ask you to confirm the suggested version or enter a different one.
4. Create a `release_X_Y_Z` branch (underscores, e.g. `release_0_11_15`).
5. Set the `service-revision` property to the new version in every `pom.xml`, via `mvn versions:set-property`.
6. Run `mvn clean install` to build and test the release version.
7. Pause and remind you to update [`docs/release-notes.md`](../docs/release-notes.md) with the changes in this release — do this now, before continuing.
8. Commit the version bump and release notes as `choir: Prepare release X.Y.Z`.
9. Ask whether to push the branch to `origin`.
10. Pause again and wait for you to open a pull request from `release_X_Y_Z` into `main`, get it reviewed, and merge it. Press Enter once it's merged (or Ctrl+C to abort here — nothing below this point has run yet).
11. Check out `main`, pull the latest, tag the merge commit `vX.Y.Z`, and push the tag to `origin`. Pushing the tag triggers the Docker release workflow (`.github/workflows/release.yml` → `docker-release.yml`).
12. Bump `service-revision` in every `pom.xml` to the next patch version with a `-SNAPSHOT` suffix, commit as `choir: new version`, and push directly to `main`.

That's the whole release — nothing to run manually afterward.

## Version scheme

- Tags are `vX.Y.Z` (e.g. `v0.11.14`).
- The `service-revision` property (declared independently in every module's `pom.xml`) always matches the tag without the `v` prefix, with a `-SNAPSHOT` suffix while in development.
- Releases are patch bumps unless a change explicitly warrants a minor/major bump — if so, just answer the version prompt in `release.sh` with the version you want instead of accepting the suggestion.

## Troubleshooting

- **"Working tree has untracked or modified files"** — the script refuses to start with a dirty working tree. Commit, stash, or clean up first.
- **"Branch ... already exists"** — a `release_X_Y_Z` branch already exists locally or on `origin`. Delete it or pick a different version.
- **"Tag ... already exists"** — `vX.Y.Z` is already tagged locally or on `origin`. This shouldn't happen unless a release was already cut for that version, or a previous run of the script got interrupted after tagging.
- If `mvn clean install` fails during step 6, fix the issue on the release branch, commit, and re-run `mvn versions:set-property` / `mvn clean install` manually — no need to restart the whole script.
- If the script is interrupted after the branch was merged but before tagging (step 11–12), you can safely re-run `./internal/release.sh` from `main`: since `release_X_Y_Z` already exists it will fail fast at branch creation, so instead run the tag/version-bump commands shown in step 11–12 above by hand.
