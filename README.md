# Build Version Maven Plugin

[![Build Status](https://github.com/evolvedbinary/buildversion-maven-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/evolvedbinary/buildversion-maven-plugin/actions/workflows/ci.yml)
[![Java 8](https://img.shields.io/badge/java-8-blue.svg)](https://adoptopenjdk.net/)
[![License](https://img.shields.io/badge/license-EPL%201.0-blue.svg)](https://www.eclipse.org/legal/epl/epl-v10.html)

**NOTE** This is a port from Clojure to Java of the original code from: https://github.com/code54/buildversion-plugin

This is a maven plugin that extracts current build information from git
projects, including: the latest commit hash, timestamp, most recent tag, number
of commits since most recent tag. It also implements a "follow first parent"
flavor of `git describe` (see "About git-describe" below for details).

Similar in intent to
[buildnumber-maven-plugin](http://mojo.codehaus.org/buildnumber-maven-plugin/),
this plugin sets Maven project properties intended to be used in later phases of
the Maven lifecycle. You may use this to include build version information on
property files, manifests and generated sources.


## Usage

Simply add `buildversion-plugin` to your pom, executing the `set-properties` goal. Example:

```xml
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>com.evolvedbinary.maven.plugins</groupId>
        <artifactId>buildversion-maven-plugin</artifactId>
        <version>1.1.0-SNAPSHOT</version>
        <executions>
          <execution>
            <goals><goal>set-properties</goal></goals>
          </execution>
        </executions>
      </plugin>
  ...
```

By default, the plugin runs on Maven's `initialize` phase. Any plugin running after that phase will see the following properties:

* `build-tag`: Closest repository tag (in commit history following "first parents"). NOTE: Only tags starting with `v` are considered, and the `v` is stripped. Example: `1.2.0-SNAPSHOT` (tag on git: `v1.2.0-SNAPSHOT`).
* `build-tag-delta`: Number of commits since the closest tag until HEAD. Example: `2`
* `build-commit`: Full hash of current commit (HEAD). Example: `c154712b8cea9da812c52f269578a458911f24cc`
* `build-commit-abbrev`: Abbreviated hash of current commit (HEAD). Example: `c154712`
* `build-version`: Descriptive version of current build. If a tag points to HEAD (that is, build-tag-delta is "0") then `build-version` equals `build-tag`; otherwise, it'll include: the closest tag, tag delta, and abbreviated commit hash. Examples: `1.2.0-SNAPSHOT` (tag points to HEAD),  `1.2.0-SNAPSHOT-2-c154712` (HEAD is two commits ahead of tag)
* `build-tstamp`: A date and time stamp of the current commit (HEAD). The pattern is configurable. Example: `20120407001823`.


## Configuration Parameters

<table>
  <tr>
    <th>Property</th>
    <th>Default</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>gitCmd</td>
    <td>git</td>
    <td>Name for 'git' executable to use. You may specify an absolute pathname or a command in you "PATH".</td>
  </tr>
  <tr>
    <td>tstampFormat</td>
    <td>yyyyMMddHHmmss</td>
    <td>Specify a custom format for the `build-tstamp` property. Use the pattern syntax defined by Java's <a href="http://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a></td>
  </tr>
  <tr>
    <td>customProperties</td>
    <td>-</td>
    <td>Optional. A Groovy snippet of code you may specify in order to modify or create new properties. The code must evaluate to a Map. The name of the keys in the Map become Maven properties on the project. For convenience, for each `build-*` property you have a local variable with the same name already defined for you. See below for an example.</td>
  </tr>
</table>


Example:

```xml
  <plugin>
    <groupId>com.code54.mojo</groupId>
    <artifactId>buildversion-plugin</artifactId>
    <version>1.0.3</version>
    <executions>
      <execution>
        <goals><goal>set-properties</goal></goals>
        <configuration>
          <!-- use only the day for the timestamp -->
          <tstampFormat>yyyy-MM-dd</tstampFormat>

          <!-- Define a new project property 'build-tag-lowercase', based on 'build-tag'
               Note how 'build-tag' is available to the script as a local variable. -->
          <customProperties>
            { :build-tag-lowercase (clojure.string/lower-case build-tag) }
          </customProperties>
        </configuration>
      </execution>
    </executions>
  </plugin>
```

# About git-describe

Before writing this plugin, I used to rely on a simple script which called `git
describe` to obtain a descriptive version number including most recent tag and
commits since such a tag.

Unfortunately, the logic behind `git describe` searches for the closest tag back
in history *following all parent commits on merges*. This means it may select
tags you originally put *on another branch*. So, if you are working on a
development branch and merge back a fix made on a release branch, calling `git
describe` on the development branch may shield a description that includes a tag
you placed on the release branch.

Until `git describe` accepts a `--first-parent` argument to prevent this
problem, this plugin implements its own logic, which basically relies on `git
log --first-parent` to traverse history on the current "line of development".

Reference:

 * [Another explanation](http://www.xerxesb.com/2010/git-describe-and-the-tale-of-the-wrong-commits/) of this same issue with `git describe`.
 * [GIT mailing list discussion](http://kerneltrap.org/mailarchive/git/2010/9/21/40071/thread) about `git describe`'s logic and lack of `--first-parent`.
 * Here's a [working patch to add `--first-parent` to `git describe`](https://github.com/git/git/tree/mrb/describe-first-parent)

## FAQ

 * Why don't you just use `buildnumber-maven-plugin`?
 Because it does not provide information about tagging and number of commits
 since tag. See "About git-describe" above for details.
