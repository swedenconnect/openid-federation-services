# Releasing

How to make a release of OpenID Federation Services.

This is for maintainers. Only maintainers can create release tags.

## How a release works

A release is made in three steps:

1. [`internal/release.sh`](release.sh) sets the release version on a release branch, builds it,
   commits it, pushes the branch, and puts the tag `vX.Y.Z` on the release commit. Pushing that tag
   is what starts publishing.
2. You merge the release branch into `main`, through a pull request.
3. [`internal/post-release-bump.sh`](post-release-bump.sh), run on an up to date `main`, opens the
   next snapshot version on a new branch, which you then merge into `main` too.

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

It does the release up to and including the tag, in one run:

1. Checks that the working tree is clean and that a branch is checked out.
2. Fetches the tags from `origin`, takes the newest `vX.Y.Z` tag and suggests the next version,
   which is the last number raised by one. You confirm it, or type another version. If there is no
   such tag yet, you type the first version. Tags with a suffix, such as `v1.0.0-RC1`, are ignored.
3. Works out which branch to use. See [Which branch the release is made on](#which-branch-the-release-is-made-on).
4. Checks that the version is of the form X.Y.Z, that the branch does not exist yet, here or on
   `origin`, and that the tag `vX.Y.Z` does not exist either. Everything that can stop the release
   is checked at this point, before anything is changed. If a check fails the script stops and the
   repository is exactly as it was.
5. Creates the release branch, if it is making one.
6. Sets the version in every `pom.xml`, with `mvn versions:set -DprocessAllModules=true`.
7. Builds and tests with `mvn clean install`.
8. Stops and asks you to write the release notes for this version in
   [`docs/release-notes.md`](../docs/release-notes.md), including the date. Do that now, then press
   Enter.
9. Commits the version and the release notes as `build: X.Y.Z release`, and pushes the
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
12. Tells you to open a pull request from the branch into `main`, and to run
    `internal/post-release-bump.sh` on `main` once it is merged.

Only three points wait for you: the version, the release notes, and the question before tagging.

Only the new tag is pushed, with `git push origin vX.Y.Z`. Nothing pushes all tags, so a tag you
happen to have locally cannot start a release workflow by accident.

## Run the post-release bump script

Once the release branch is merged into `main`, from the root of the repository:

```bash
git checkout main
git pull
./internal/post-release-bump.sh
```

1. Checks that the working tree is clean and that a branch is checked out. On `main` it also checks
   that `main` is not behind `origin/main`, so that the merged release is there.
2. Checks that the version in the POMs is a release version of the form X.Y.Z and not a snapshot.
3. Suggests the next snapshot version, the released version with the last number raised by one
   and `-SNAPSHOT` added. You confirm it, or type another version as X.Y.Z, and `-SNAPSHOT` is
   added.
4. Works out which branch to use. From `main` that is a new branch named `bump/X_Y_Z` after the
   coming version, so version 1.0.2 is opened on `bump/1_0_2`. It checks that the branch does not
   exist yet, here or on `origin`. If any check fails the script stops and the repository is
   exactly as it was.
5. Creates the bump branch, if it is making one.
6. Sets the version in every `pom.xml`.
7. Adds a section for the coming version at the top of `docs/release-notes.md`, with the date
   `_not yet released_`. If the file already has a section for that version it is left as it is.
8. Commits the changes as `build: bump version after X.Y.Z` and pushes the branch.

Then open a pull request from the bump branch into `main` and merge it. The bump branch holds no
tagged commit, so any of the merge buttons will do.

This is the recommended way. The script can also be run on the release branch before it is
merged, and then makes the bump on that branch, so one pull request carries both the release and
the bump. That pull request must then be merged with "Create a merge commit".

## Testing the scripts

The scripts have tests in [`internal/test-scripts/`](test-scripts):

```bash
./internal/test-scripts/release-test.sh
./internal/test-scripts/post-release-bump-test.sh
./internal/test-scripts/check-release-version-test.sh
```

The first two run the scripts in throwaway git repositories, each with its own `origin` and a
stand-in for `mvn` (see [`sandbox.sh`](test-scripts/sandbox.sh)), so they never push, tag or change
this repository. They cover how the version and the branch are picked, that every check stops a
script before anything is changed, and how the release notes section is added. The last one needs
Maven, as it also reads the POMs of this repository.

## Which branch the release is made on

- **From `main`**, the script makes a new branch named `release/X_Y_Z`, with underscores. Version
  1.0.0 is released on `release/1_0_0`.
- **From any other branch**, the release is made on that branch. No new branch is made.

## Merge the pull request with "Create a merge commit"

When you merge the release branch into `main`, use the "Create a merge commit" button.

The other two buttons write new commits. "Squash and merge" replaces the branch with one new
commit, and "Rebase and merge" copies the commits onto `main` as new ones. Either way the commit
the tag points at is not part of the history of `main`, so GitHub shows the tag as not being on
`main`. The code on `main` is the same in all three cases, and the tag keeps the released commit
available, so this is a recommendation and nothing enforces it.

## Rules for tags

1. **Start with `v`.** Version `1.0.0` is tagged `v1.0.0`.

2. **Use an annotated tag**, never a lightweight one, so the tag records who made it, when and why:

   ```bash
   git tag -a v1.0.0 -m "Version 1.0.0"
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

## What a tag starts

Three workflows react to a pushed `v*` tag. They run next to each other and do not depend on each
other. There is no approval step, publishing starts as soon as the tag is pushed.

- [`maven-central-deploy.yml`](../.github/workflows/maven-central-deploy.yml) publishes the library
  modules to Maven Central. See [Publishing to Maven Central](#publishing-to-maven-central).
- [`docker-release.yml`](../.github/workflows/docker-release.yml) checks that the tag matches the
  Maven version and that the version is not a snapshot, installs the modules, and builds and pushes
  the image of `oidf-services` with Jib, for `linux/amd64` and `linux/arm64`, to
  `ghcr.io/swedenconnect/openid-federation-services`, tagged with the version and with `latest`.
- [`github-release.yml`](../.github/workflows/github-release.yml) creates a GitHub release from the
  tag, titled with the version and marked as the latest, that points at `docs/release-notes.md` at
  that tag. Nothing is built or attached.

All builds use Java 25 from Temurin.

## Publishing to Maven Central

Pushing the tag starts the Maven Central workflow, which builds the tagged commit and publishes the
files as the `swedenconnect-bot` Central user. Nothing is published by hand.

### What the workflow does

1. Checks out the tag and sets up Java 25 from Temurin.
2. Checks the version, before anything is built, so that a mismatch publishes nothing. Every module
   that is published must be at the version the tag names, and that version must not be a snapshot.
3. Builds the whole project with `mvn -Prelease clean deploy`, tests included. The integration
   tests run as usual, so a failing test stops the release.
4. Signs every file and uploads them to Central.

The version check is [`internal/check-release-version.sh`](check-release-version.sh). You can run
it yourself before tagging:

```bash
./internal/check-release-version.sh v1.0.0
```

It reads the version of every `pom.xml` under version control, skips a module that sets
`maven.deploy.skip`, and lists every module that does not match instead of stopping at the first
one.

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

### Publishing by hand

Only if the workflow cannot be used. You need a Central portal token as a server with the id
`central` in your `~/.m2/settings.xml`, and a GPG key that `gpg` can find on your machine, with its
public half on a key server that Central checks. Run it from the tagged commit, so that what
reaches Central is built from exactly the source the tag points at:

```bash
git checkout v1.0.0
mvn -Prelease clean deploy
```

### What is published

| Published | Not published |
|-----------|----------------|
| `oidf-parent` | `oidf-services` |
| `oidf-modules` and `nimbus-extensions`, `common`, `resolver`, `trustanchor`, `trustmarkissuer` | |
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

- Tags are `vX.Y.Z`, for example `v1.0.0`.
- Only the root `pom.xml` states a version of its own. Every module takes it from its parent and
  states only the parent version, written out in full. There is no version property, so the only
  supported way to change the version is `mvn versions:set`, which changes the root and every
  parent reference at once.
- While work is going on, the version is the last released version with the last number raised by
  one and `-SNAPSHOT` added.
- A release raises the last number, unless a change calls for a bigger step. If it does, answer the
  version question in the script with the version you want instead of taking the suggestion.

## If something goes wrong

### A script stopped before it changed anything

- **"The working tree has changed or untracked files"**: commit, stash or remove them first.
- **"No branch is checked out"**: you are on a detached HEAD. Check out `main`, the branch you want
  to release from, or the release branch.
- **"... is not a version of the form X.Y.Z"**: the version must be three numbers separated by
  dots, such as `1.0.0`. No `v`, and for `release.sh` no `-SNAPSHOT`.
- **"The branch ... already exists"**: a branch of that name is already here or on `origin`. Remove
  it, or release a different version.
- **"The tag ... already exists"**: that version has been released. Release the next one.
- **"main is behind origin/main"**: run `git pull` on `main` first, so the merged release branch is
  there.
- **"The version in the POMs is already the snapshot ..."**: `post-release-bump.sh` has already
  been run, the release branch is not merged yet, or the release was never made. There is nothing
  to bump.
- **"The version in the POMs, ..., is not a released version of the form X.Y.Z"**: the POMs hold
  something other than a release version. Set the version by hand, as below.

In all of these the repository is exactly as it was.

### The build failed

Fix the problem on the release branch and commit it. Then set the version and build again by hand:

```bash
mvn versions:set -DnewVersion=X.Y.Z -DprocessAllModules=true -DgenerateBackupPoms=false
mvn clean install
```

Then carry on from step 8 of the release script above, or start the script again on that branch,
which uses it as it is.

### The release script stopped after the branch was pushed but before the tag

Either you answered no to the tag question, or you stopped the script. The branch is on `origin`
and holds the release version. There is no tag and nothing is published. To finish, from the
release commit:

```bash
git tag -a vX.Y.Z -m "Version X.Y.Z"
git push origin vX.Y.Z
```

Then merge the branch into `main` and run `internal/post-release-bump.sh` on `main`.

### The post-release bump script failed

If it stopped before it changed anything, see above. If it stopped later, for example because the
push failed, look at what is left with `git status` and `git log`. Either push the bump commit
yourself with `git push -u origin <branch>`, or start over: check out `main` again, delete the bump
branch with `git branch -D bump/X_Y_Z`, and run the script again.

### A workflow failed

- **The Docker release workflow failed on the version check.** The tag does not match the version
  in the POMs, or the version is still a snapshot. Nothing was published. Fix the version and
  release a new one rather than moving the tag.
- **The Docker release workflow failed later.** The image is not on ghcr.io, or only partly. It is
  independent of Maven Central, so the workflow can be run again on the same tag from the Actions
  page once the cause is fixed.
- **The GitHub release workflow failed.** Run it again, or create the release by hand from the tag
  on GitHub.
- **The Maven Central workflow failed.** What to do depends on how far the run got. In every case,
  release a new version instead of moving the tag:
  - **The version check failed.** Nothing was built and nothing was uploaded. The tag does not
    match the version in the POMs, or the version is still a snapshot. Fix the version and release a
    new version.
  - **The build or the tests failed.** Nothing was uploaded. Fix the problem on `main` and release
    a new version.
  - **The upload or Central's own checks failed.** An upload that fails those checks never goes
    live, so nothing was published. Look at the deployment in the Central portal, fix the cause,
    and release a new version. Running the workflow again on the same tag only works if the upload
    never reached Central at all.
  - **The files went live but are wrong.** They cannot be replaced or taken down. Release a new
    version.
