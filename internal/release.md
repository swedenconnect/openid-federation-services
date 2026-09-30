# Releasing

How to make a release of OpenID Federation Services.

This is for maintainers. Only maintainers can create release tags.

## How a release works

A release is made on a branch. The script sets the release version, builds it, commits it, pushes
the branch, and puts the tag `vX.Y.Z` on the release commit. Pushing that tag is what starts
publishing. The branch is merged into `main` afterwards, by you, through a pull request.

The tag is created before the branch is merged, so the tag does not sit on a commit on `main` at
the time it is made. That is fine. The tag keeps the released commit available whatever happens to
the branch.

## Before you start

- The working tree has no changes and no untracked files.
- You can push to `origin`.
- Maven works on your machine.
- Docker is running, because the build runs the integration tests.

## Run the release script

From the root of the repository:

```bash
./internal/release.sh
```

It does the whole release in one run:

1. Checks that the working tree is clean and that a branch is checked out.
2. Fetches the tags from `origin`, takes the newest `vX.Y.Z` tag and suggests the next version,
   which is the last number raised by one. You confirm it, or type another version.
3. Works out which branch to use. See [Which branch the release is made on](#which-branch-the-release-is-made-on).
4. Checks that the version is of the form X.Y.Z, that the branch does not exist yet, here or on
   `origin`, and that the tag `vX.Y.Z` does not exist either. Everything that can stop the release
   is checked at this point, before anything is changed. If a check fails the script stops and the
   repository is exactly as it was.
5. Creates the release branch, if it is making one.
6. Sets the version in every `pom.xml`, with `mvn versions:set -DprocessAllModules=true`.
7. Builds and tests with `mvn clean install`.
8. Stops and asks you to write the release notes for this version in
   [`docs/release-notes.md`](../docs/release-notes.md). Do that now, then press Enter.
9. Commits the version and the release notes as `choir: Prepare release X.Y.Z`, and pushes the
   branch to `origin`.
10. Asks whether to create the tag and push it, and says that this starts publishing and that a
    published version cannot be removed or replaced. If you say no the script stops here. The
    branch is pushed, there is no tag, and nothing is published. It prints the two commands you
    need to tag later.
11. Creates the annotated tag `vX.Y.Z` on the release commit and pushes that one tag. This starts
    the Maven Central workflow
    ([`maven-central-deploy.yml`](../.github/workflows/maven-central-deploy.yml)), the Docker
    release workflow ([`docker-release.yml`](../.github/workflows/docker-release.yml)) and the
    GitHub release workflow ([`github-release.yml`](../.github/workflows/github-release.yml)). It
    prints where to follow the Maven Central run.
12. Sets the next snapshot version, commits it as `choir: new version`, and pushes the branch.
13. Tells you to open a pull request from the branch into `main`.

Only three points wait for you: the version, the release notes, and the question before tagging.

Only the new tag is pushed, with `git push origin vX.Y.Z`. Nothing pushes all tags, so a tag you
happen to have locally cannot start a release workflow by accident.

[`internal/release-test.sh`](release-test.sh) tests the script: how it picks the version and the
branch, and that each check stops it before anything is changed.

## Which branch the release is made on

- **From `main`**, the script makes a new branch named `release/X_Y_Z`, with underscores. Version
  0.11.16 is released on `release/0_11_16`.
- **From any other branch**, the release is made on that branch. No new branch is made.

## Merge the pull request with "Create a merge commit"

When you merge the release branch into `main`, use the "Create a merge commit" button.

The other two buttons write new commits. "Squash and merge" replaces the branch with one new
commit, and "Rebase and merge" copies the commits onto `main` as new ones. Either way the commit
the tag points at is not part of the history of `main`, so GitHub shows the tag as not being on
`main`. The code on `main` is the same in all three cases, and the tag keeps the released commit
available, so this is a recommendation and nothing enforces it.

## Rules for tags

1. **Start with `v`.** Version `0.11.16` is tagged `v0.11.16`.

2. **Use an annotated tag**, never a lightweight one, so the tag records who made it, when and why:

   ```bash
   git tag -a v0.11.16 -m "Version 0.11.16"
   ```

   The script does this for you.

3. **The version in the POMs must match the tag.** The Maven Central workflow and the Docker
   release workflow both check this, and both stop if the tag does not match the version, or if the
   version is still a snapshot.

4. **Never move or delete a tag that has been pushed.** Artifacts have been built from it. Being
   able to find the exact source of a release matters more than a tidy history. If a tag is wrong,
   release a new version.

5. **Never release the same version twice.** Maven Central does not let a published version be
   changed or taken down. Two different builds published under the same version can never be told
   apart again.

## Publishing to Maven Central

Pushing the tag starts the Maven Central workflow
([`maven-central-deploy.yml`](../.github/workflows/maven-central-deploy.yml)), which builds the
tagged commit and publishes the files as the `swedenconnect-bot` Central user. Nothing is published
by hand.

The workflow runs next to the Docker release workflow and the GitHub release workflow, which the
same tags start, and does not depend on them. There is no approval step, publishing starts as soon
as the tag is pushed.

### What the workflow does

1. Checks out the tag and sets up Java 25 from Temurin, as the other workflows do.
2. Checks the version, before anything is built, so that a mismatch publishes nothing. Every module
   that is published must be at the version the tag names, and that version must not be a snapshot.
3. Builds the whole project with `mvn -Prelease clean deploy`, tests included. The integration
   tests run as usual, so a failing test stops the release.
4. Signs every file and uploads them to Central.

The version check is [`internal/check-release-version.sh`](check-release-version.sh). You can run
it yourself before tagging:

```bash
./internal/check-release-version.sh v0.11.16
```

It reads the version of every `pom.xml` in the project, skips the modules that are not published,
and lists every module that does not match instead of stopping at the first one.
[`internal/check-release-version-test.sh`](check-release-version-test.sh) tests it.

### What the `release` profile does

The `release` profile in the parent POM adds the source and javadoc files that Central requires,
signs everything with GPG, and uploads it through the Sonatype Central publishing plugin.
`autoPublish` is on, so an upload that passes Central's checks goes live without anyone pressing a
button in the Central portal.

A plain `mvn deploy`, without the profile, still publishes to GitHub Packages as it always has.

### Signing

The workflow signs with the Bouncy Castle signer of the Maven GPG plugin, chosen on the command
line with `-Dgpg.signer=bc`. It reads the key and its password from the environment, so the machine
running the workflow needs no `gpg` program and no imported key. The profile itself is not changed
by this, so signing from your own machine still works the way it always has.

### Secrets

These are organisation secrets and they already exist. The workflow reads them and adds nothing of
its own:

| Secret | What it holds |
|--------|---------------|
| `MAVEN_CENTRAL_USERNAME` | the user name half of a Central portal token, not the bot's login name |
| `MAVEN_CENTRAL_TOKEN_PASSWORD` | the password half of the same token |
| `BOT_GPG_PRIVATE_KEY` | the private key, in ASCII armour |
| `BOT_GPG_PASSWORD` | the password for that key |

The public half of the key is already on a key server, so the workflow does not need it. The two
Central values reach Maven as environment references in a generated `settings.xml`, so no secret is
written to a file or shown in the log.

That generated `settings.xml` holds two servers: `central` for publishing, and `github` for
fetching dependencies from GitHub Packages, which the build needs just as the other workflows do.

### If the run fails

Maven Central does not let a published version be removed or replaced, so what to do depends on how
far the run got. In every case, release a new version instead of moving the tag.

- **The version check failed.** Nothing was built and nothing was uploaded. The tag does not match
  the version in the POMs, or the version is still a snapshot. Fix the version on `main` and
  release a new version.
- **The build or the tests failed.** Nothing was uploaded. Fix the problem on `main` and release a
  new version.
- **The upload or Central's own checks failed.** An upload that fails those checks never goes live,
  so nothing was published. Look at the deployment in the Central portal, fix the cause, and
  release a new version. Running the workflow again on the same tag only works if the upload never
  reached Central at all.
- **The files went live but are wrong.** They cannot be replaced or taken down. Release a new
  version.

### Publishing by hand

Only if the workflow cannot be used. You need a Central portal token as a server with the id
`central` in your `~/.m2/settings.xml`, and a GPG key that `gpg` can find on your machine, with its
public half on a key server that Central checks. Run it from the tagged commit, so that what
reaches Central is built from exactly the source the tag points at:

```bash
git checkout v0.11.16
mvn -Prelease clean deploy
```

### What is published

| Published | Not published |
|-----------|----------------|
| `oidf-parent` | `oidf-services` |
| `oidf-modules` and `common`, `resolver`, `trustanchor`, `trustmarkissuer` | |
| `oidf-starters` and `federation-starter`, `trustanchor-starter`, `trustmarkissuer-starter`, `resolver-starter` | |

`oidf-services` is the application, not a library, and sets `maven.deploy.skip`. It ships as a
Docker image instead.

The POMs are published as they are written, so the parent POMs have to be published together with
the modules that inherit from them. That is why `oidf-parent`, `oidf-modules` and `oidf-starters`
are in the list.

`resolver` also publishes a test jar, which `oidf-services` needs for its own tests. It is signed
along with everything else.

### Trying it out first

A published version cannot be changed, so it is worth building everything before you tag:

```bash
mvn -Prelease -Dgpg.skip=true clean verify
```

This builds every file the release would upload, source and javadoc files included, but signs
nothing and uploads nothing.

## Version numbers

- Tags are `vX.Y.Z`, for example `v0.11.16`.
- Only the root `pom.xml` states a version of its own. Every module takes it from its parent and
  states only the parent version, written out in full. There is no version property, so the only
  supported way to change the version is `mvn versions:set`, which changes the root and every
  parent reference at once.
- While work is going on, the version is the last released version with the last number raised by
  one and `-SNAPSHOT` added.
- A release raises the last number, unless a change calls for a bigger step. If it does, answer the
  version question in the script with the version you want instead of taking the suggestion.

## If something goes wrong

### The script stopped before it changed anything

- **"The working tree has changed or untracked files"**: commit, stash or remove them first.
- **"No branch is checked out"**: you are on a detached HEAD. Check out `main`, or the branch you
  want to release from.
- **"... is not a version of the form X.Y.Z"**: the version must be three numbers separated by
  dots, such as `0.11.16`. No `v`, no `-SNAPSHOT`.
- **"The branch ... already exists"**: a branch of that name is already here or on `origin`. Remove
  it, or release a different version.
- **"The tag ... already exists"**: that version has been released. Release the next one.

In all of these the repository is exactly as it was.

### The build failed

Fix the problem on the release branch and commit it. Then set the version and build again by hand:

```bash
mvn versions:set -DnewVersion=X.Y.Z -DprocessAllModules=true -DgenerateBackupPoms=false
mvn clean install
```

Then carry on from step 9 above, or start the script again on that branch, which uses it as it is.

### The script stopped after the branch was pushed but before the tag

Either you answered no to the tag question, or you stopped the script. The branch is on `origin`
and holds the release version. There is no tag and nothing is published. To finish, from the
release commit:

```bash
git tag -a vX.Y.Z -m "Version X.Y.Z"
git push origin vX.Y.Z
```

Then do the next snapshot version by hand, as below.

### The script stopped after the tag was pushed

The release is published, or is being published. What is left is the next snapshot version and the
pull request. From the release branch:

```bash
mvn versions:set -DnewVersion=X.Y.Z-SNAPSHOT -DprocessAllModules=true -DgenerateBackupPoms=false
git add -- '**/pom.xml' pom.xml
git commit -m "choir: new version"
git push origin <branch>
```

Use the released version with the last number raised by one, so `0.11.17-SNAPSHOT` after releasing
`0.11.16`. Then open the pull request into `main` and merge it with "Create a merge commit".

### A workflow failed

- **The Docker release workflow failed on the version check.** The tag does not match the version
  in the POMs, or the version is still a snapshot. Nothing was published. Fix the version and
  release a new one rather than moving the tag.
- **The Maven Central workflow failed.** See [If the run fails](#if-the-run-fails) above.
