# Release Instructions

How to cut a release of OpenID Federation Services.

## Prerequisites

- Clean working tree, checked out on `main` with the latest changes pulled.
- Push access to `origin`.
- Maven installed and able to build the project locally.

## 1. Prepare the release branch

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
10. Print the follow-up commands for tagging and bumping to the next development version (see below) — these are printed only, not run automatically.

## 2. Open a pull request

Open a PR from `release_X_Y_Z` into `main`. Get it reviewed and merged like any other change.

## 3. Tag the release

After the release branch is merged into `main`, tag the merge commit on `main` and push the tag:

```
git checkout main
git pull
git tag vX.Y.Z
git push origin vX.Y.Z
```

Tagging the release triggers the publish/Docker release workflows (see `.github/workflows/publish.yml` and `.github/workflows/docker-release.yml`).

## 4. Bump to the next development version

Still on `main`, move the `service-revision` property in every `pom.xml` to the next patch version with a `-SNAPSHOT` suffix:

```
mvn versions:set-property -Dproperty=service-revision -DnewVersion=X.Y.(Z+1)-SNAPSHOT -DgenerateBackupPoms=false
```

Commit and push this directly to `main` (or via a small PR, per team preference), e.g.:

```
git add -- '**/pom.xml' pom.xml
git commit -m "choir: new version"
git push origin main
```

## Version scheme

- Tags are `vX.Y.Z` (e.g. `v0.11.14`).
- The `service-revision` property (declared independently in every module's `pom.xml`) always matches the tag without the `v` prefix, with a `-SNAPSHOT` suffix while in development.
- Releases are patch bumps unless a change explicitly warrants a minor/major bump — if so, just answer the version prompt in `release.sh` with the version you want instead of accepting the suggestion.

## Troubleshooting

- **"Arbetskatalogen har ospårade eller ändrade filer"** — the script refuses to start with a dirty working tree. Commit, stash, or clean up first.
- **"Branchen ... finns redan"** — a `release_X_Y_Z` branch already exists locally or on `origin`. Delete it or pick a different version.
- If `mvn clean install` fails, fix the issue on the release branch, commit, and re-run `mvn versions:set-property` / `mvn clean install` manually — no need to restart the whole script.
