![Logo](docs/images/sweden-connect.png)

# Contributing to the OpenId Federation Services

The OpenId Federation Services is released under the Apache 2.0 license. If you would like to contribute something, or simply want to hack on the code this document should help you get started.

## Using GitHub Issues

We use [GitHub issues](https://github.com/swedenconnect/openid-federation-services/issues) to track bugs and enhancements.

If you are reporting a bug, please assist to speed up problem diagnosis by providing as much information as possible. We will not act on bug reports that only is a screenshot of a web page or something similar.

For each bug report include the following:

- Add the "bug" label to the issue.
- Expected behaviour and the actual behaviour.
- Steps to reproduce: Explain how we can reproduce this error.
- Log entries. If possible with a detailed log level.
- HAR (HTTP Archive) files if possible. See [googles instruction](https://support.google.com/admanager/answer/10358597?hl=en) if you are unsure.

We are also glad to receive suggestions on enhancements. For these submissions do:

- Add the "enhancement" label to the issue.

- Describe the suggested feature and why you think it should be included in the code.

The OpenId Federation Services repository is intended to be generic OpenId Federation Services and we will not add features that
are not classed as "generic", for example, to provide a specific type of logging.

## Submitting Pull Requests

This project uses pull requests to suggest changes to the project. There are a few important things to keep in mind when submitting a pull request:

- Expect feedback and to make changes to your contribution.

- Follow the [Code Style](#code-style) for the project.

- Use Squash Commits ...
    - Use git rebase –interactive, git add –patch and other tools to "squash" multiple commits into atomic changes.

- For the commit message(s): Always start with the issue that this commit concerns, for example: "IS-76 Changed log level of received AutnnRequest messages to debug".

    - A PR containg commit messages with no text or default text such as "Updated Class.java" will be rejected with no exceptions!

- Unless it is a minor change:

    - It is best to discuss pull requests on an issue before doing work.

    - The pull request should be as small as possible and focus on a single unit of change.

    - Generally, this means do not introduce any new interfaces and as few classes as possible.

- Use signed commits see [Github](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits) documentation on how to sign your commits.

<a name="code-style"></a>
## Code Style

All developers contributing to this project should follow the [Spring Framework Code Style](https://github.com/spring-projects/spring-framework/wiki/Code-Style). Read it!

### Code Style Templates

- IntelliJ: [internal-docs/code-style/spring-intellij-code-style.xml](internal-docs/spring-intellij-code-style.xml)

**Note:** For IntelliJ also make sure to set the editor in "never join already wrapped lines"-mode. See this [article](https://intellij-support.jetbrains.com/hc/en-us/community/posts/360006393539-How-to-prevent-IntelliJ-from-changing-file-formatting-if-lines-meet-hard-wrap-constraints-).

### Apache v 2.0 License Header

- Intellij: [internal-docs/code-style/intellij-copyright.xml](internal-docs/code-style/intellij-copyright.xml)

Include the following header in all Java files:

```
/*
 * Copyright 2024-${year} Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

*Where ${year} should be the current year (unless it is 2024).*

Configure your IDE to do it automatically!

-----

Copyright &copy; 2024, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).

