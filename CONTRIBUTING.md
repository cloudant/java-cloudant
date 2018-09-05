# Contributing

## Issues

Please [read these guidelines](http://ibm.biz/cdt-issue-guide) before opening an issue.
If you still need to open an issue then we ask that you complete the template as
fully as possible.

## Pull requests

We welcome pull requests, but ask contributors to keep in mind the following:

* Only PRs with the template completed will be accepted
* We will not accept PRs for user specific functionality

### Developer Certificate of Origin

In order for us to accept pull-requests, the contributor must sign-off a
[Developer Certificate of Origin (DCO)](DCO1.1.txt). This clarifies the
intellectual property license granted with any contribution. It is for your
protection as a Contributor as well as the protection of IBM and its customers;
it does not change your rights to use your own Contributions for any other purpose.

Please read the agreement and acknowledge it by ticking the appropriate box in the PR
 text, for example:

- [x] Tick to sign-off your agreement to the Developer Certificate of Origin (DCO) 1.1

## General information

Cloudant-client is written in Java and uses gradle as its build tool.

### Projects
There are two sub-projects in java-cloudant each of which produces an artifact for publishing.

#### cloudant-client
The cloudant-client jar is the main java-cloudant artifact and includes the classes for interacting
with the Cloudant or CouchDB servers. It includes the functionality to build requests for specific
Cloudant operations and for changing between POJOs and Cloudant documents.

#### cloudant-http
A jar for basic HTTP operations such as forming a request for a URL and specifying the request
properties and body. It provides methods for executing the requests and getting the response content
in some different forms. It also includes interceptor interfaces and some implementations for
customizing requests and handling responses. This jar is used by both the cloudant-client and by the
[sync-android](https://github.com/cloudant/sync-android) project.

### Coding guidelines

Adopting the [Google Java Style](https://google-styleguide.googlecode.com/svn/trunk/javaguide.html)
with the following changes:

```
4.2
    Our block indent is +4 characters

4.4
    Our line length is 100 characters.

4.5.2
    Indent continuation of +4 characters fine, but I think
    IDEA defaults to 8, which is okay too.
```

#### Code Style

An IDEA code style matching these guidelines is included in the project,
in the `.idea` folder.

If you already have the project, to enable the code style follow these steps:

1. Go to _Preferences_ -> _Editor_ -> _Code Style_.
2. In the _Scheme_ dropdown, select _Project_.

IDEA will then use the style when reformatting, refactoring and so on.

## Requirements

- gradle
- Java 1.6

### Installing requirements

#### Java

Follow the instructions for your platform.

#### Gradle

The project uses the gradle wrapper to download the specified version of gradle.
The gradle wrapper is run by using the following command:

```bash
$ ./gradlew
```
Note: on windows the command to run is gradlew.bat rather than gradlew

## Building

The project should build out of the box with:

```bash
$ ./gradlew compileJava
```

## Testing

### Configuration

The tests can be configured before running, by default they use the local
CouchDB. To run tests with a remote CouchDB or Cloudant, you need set the
details of this CouchDB server, including access credentials:

```
systemProp.test.couch.username=yourUsername
systemProp.test.couch.password=yourPassword
systemProp.test.couch.host=couchdbHost # default localhost
systemProp.test.couch.port=couchdbPort # default 5984
systemProp.test.couch.http=[http|https] # default http
```
Alternatively, provide a URL containing all the above information:
```
systemProp.test.couch.url=https://example.couch.db
```
You can leave out the port (it will use the protocol's default) and username
and password (if the server does not require one)

Note: using the uri config option will override *all* the individual options such as
"test.couch.username"

Add these properties to a `gradle.properties` file, this can be either:

1. in the `GRADLE_USER_HOME` folder
2. in the same folder as `build.gradle`

Take care not to commit your credentials. Using the `GRADLE_USER_HOME` folder
will make it harder to do this accidentally!

### Execution
The default tests (that require a running DB) can be run with:

```bash
$ ./gradlew check
```

The tests are categorized based on the resources they need. In addition to the
default `check` and `test` tasks there are gradle tasks to run tests from these
selected categories:

1. Tests that do not need a database
```bash
$ ./gradlew noDBTest
```
1. For tests which run against Cloudant Local or the Cloudant Service
```bash
$ ./gradlew cloudantTest
```
1. For tests which run only against the Cloudant Service
```bash
$ ./gradlew cloudantServiceTest
```

Note: you will need a Cloudant account to run tests against the Cloudant service.
Additionally the ReplicatorTest cases require the `_replicator` DB to exist.
