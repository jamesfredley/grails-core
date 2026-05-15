# Grails Installation Guide

Grails is a powerful Groovy-based web application framework for the JVM built on top of Spring Boot that has many plugins to further extend its functionality. The full documentation for how to install Grails via different mechanisms and use it can be found in the [Grails documentation](https://grails.apache.org/docs/latest). This document specifically covers the basic building and installation of Grails from a source distribution.

## How to Use the Source Distribution

Grails is ultimately a set of libraries and Gradle build plugins that are used to create a Grails application. Getting started with Grails requires knowing which libraries and which Gradle plugins to use. Since these dependencies vary based on the application type, Grails also ships with a set of CLI commands to assist with application generation.

The source distribution can be used to build and publish the code used by Grails applications. It can also be used to build the CLI commands that assist with application generation for that published code.

## Requirements and Tooling Setup for Building

Grails requires a Java Development Kit (JDK) and [Gradle](https://www.gradle.org) to build. Only specific versions are supported. A configuration file, `.sdkmanrc`, for [SDKMAN!](https://sdkman.io) exists in the source root to make it easier to set up the preferred tooling. This file contains the recommended versions for building Grails.

If SDKMAN! is installed, the tooling can be selected by running the following command at the source root:

```bash
sdk env install
```

Otherwise, Gradle must be bootstrapped so that the correct version is used. The recommended way to do this is to use the Gradle Wrapper, which can be set up with files included in the source distribution. The wrapper will automatically download the correct version of Gradle specified in the `gradle/wrapper/gradle-wrapper.properties` file. To set up the wrapper, run the following commands:

```bash
cd gradle-bootstrap
gradle bootstrap
```

For the remaining commands in this document, use `gradle` if tooling is installed with SDKMAN! or `./gradlew` if the Gradle Wrapper is used.

## Requirements for Testing

Grails has a comprehensive test suite that includes unit, integration, and functional tests. The functional tests use [Testcontainers](https://java.testcontainers.org/), which requires a container runtime. If a container runtime is not available, the tests can be skipped by using the `-PskipTests` argument. Some container runtimes require more configuration than others. See the [Testcontainers configuration documentation](https://java.testcontainers.org/features/configuration/) for how to customize the container runtime used by Testcontainers.

## Project Structure

The source of Grails is a multi-project Gradle build that uses composite multi-project builds via Gradle's `includeBuild` feature. The main project is `grails-core`, which contains the core framework code and all libraries that an end user would use in their application. The `grails-gradle` project contains code meant to run on the Gradle build classpath. `grails-forge` contains tooling related to application generation.

## Building a Release Version

The source distribution will always be a release version of Grails. This means the version number will not contain the suffix `SNAPSHOT`, and the release requirements will apply by default. One of those requirements is JAR file signing.

In order for Forge to always correctly pull the `grails-core` project, a local publishing strategy is used: the JAR files are published to a directory inside of the build directory of `grails-core`. This publishing means building the JAR files will always trigger the signing process. To successfully build the source distribution, either signing must be disabled or signing must be correctly configured.

## Disabling Signing

To disable signing, set the environment variable `GRAILS_PUBLISH_RELEASE` to `false`. This can be done by running the following command in the terminal:

```bash
export GRAILS_PUBLISH_RELEASE=false
```

## Enabling Signing

Grails makes use of the Grails Publish Gradle plugin to handle publishing artifacts. This plugin supports signing either via native tooling or via java libraries.

To configure the native tooling, GPG must be installed with a key imported. Then the following must be specified:

```bash
export SIGNING_KEY=<your-gpg-key-id>
export SIGNING_PASSPHRASE=<your-gpg-key-passphrase>
```

To configure the Java tooling, the following must be specified:

```bash
export SIGNING_KEY=<your-gpg-key-id>
export SIGNING_KEYRING=<path-to-your-gpg-keyring-file>
export SIGNING_PASSPHRASE=<your-gpg-key-passphrase>
```

Please note: if no GPG Key passphrase is set, then simply leave the environment variable `SIGNING_PASSPHRASE` unset.

## Building grails-core

To build the libraries a Grails application would use, run the following command at the source root:

```bash
./gradlew build -PskipTests
```

This will build the project and skip any tests.

## Building grails-gradle

To build the libraries for the Gradle to use, run the following command under the `grails-gradle` directory:

```bash
./gradlew build -PskipTests
```

## Building grails-forge

To build the Grails Forge, which is used for application generation, run the following command under the `grails-forge` directory:

```bash
./gradlew build -PskipTests
```

## Using Built Libraries

To use the built libraries in a Grails application, libraries must be published to a Maven repository. Since the source distribution is meant for offline use, this document covers how to publish them locally. To build the full project, run the following commands at the source root:

```bash
cd grails-gradle && ./gradlew build publishToMavenLocal -PskipTests && cd ..
./gradlew build publishToMavenLocal -PskipTests
cd grails-forge && ./gradlew build publishToMavenLocal -PskipTests
```

## Creating an Application

By default, the Grails CLI uses standard Maven repositories to retrieve upstream Grails libraries. However, it's possible to override this behavior by setting the environment variable `GRAILS_REPO_URL` to an alternative location. When using the libraries generated by a source distribution, set it to your local Maven repository, which is typically `~/.m2/repository`. This can be done by running the following command in the terminal:

```bash
export GRAILS_REPO_URL=$HOME/.m2/repository
```

This variable also supports multiple repositories by using the separator `;`. For example:

```bash
export GRAILS_REPO_URL="$HOME/.m2/repository;https://repo1.maven.org/maven2/"
```

The CLI distributions are located under the `grails-forge/grails-cli` project. The build directory contains the CLI distribution, which can be used to create a Grails application. Unzip it to any location, for the purposes of this document, we will use `grails-forge/grails-cli/build/distributions` as the working directory.

```bash
cd grails-forge/grails-cli/build/distributions
unzip *.zip
```

The `bin` directory of the extracted CLI distribution contains the `grails` command, which can be used to create a Grails application. To create a basic application, run the following command from the `bin` directory of the extracted CLI distribution:

```bash
./grails -t forge create-app DemoApplication
```

Run the application by changing into the created directory and running the following command:

```bash
cd DemoApplication
./gradlew bootRun
```

Then visit it in your browser at `http://localhost:8080`. The application should display a welcome page.
