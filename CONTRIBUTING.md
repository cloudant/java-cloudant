Contributing
=======

Cloudant-client is written in Java and uses maven as its build tool.


## Requirements

- gradle
- Java 1.6



## Installing requirements

### Java

Follow the instructions for your platform.

### Gradle

The project uses the gradle wrapper to download  specified version of gradle.
The gradle wrapper is run by using the following command:

```bash
$ ./gradlew
```
Note: on windows the command to run is gradlew.bat rather than gradlew

## Coding guidelines

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

### Code Style

An IDEA code style matching these guidelines is included in the project,
in the `.idea` folder.

If you already have the project, to enable the code style follow these steps:

1. Go to _Preferences_ -> _Editor_ -> _Code Style_.
2. In the _Scheme_ dropdown, select _Project_.

IDEA will then use the style when reformatting, refactoring and so on.

# Building the library

The project should build out of the box with:

```bash
$ ./gradlew compileJava
```

### Running the tests

The tests can be configured before running, by default they use the local
CouchDB. To run tests with a remote CouchDB or Cloudant, you need set the
details of this CouchDB server, including access credentials: add the following to
`gradle.properties` in the same folder as `build.gradle`:
```
systemProp.test.couch.username=yourUsername
systemProp.test.couch.password=yourPassword
systemProp.test.couch.host=couchdbHost # default localhost
systemProp.test.couch.port=couchdbPort # default 5984
systemProp.test.couch.http=[http|https] # default http
```
Alternatively, provide a URL containing all the above information:
```
systemProp.test.couch.uri=https://example.couch.db
```
You can leave out the port (it will use the protocol's default) and username
and password (if the server does not require one)


Note: using the uri config option will override *all* the individual options such as
"test.couch.username"

The tests can be run with:

```bash
$ ./gradlew check
```
Some tests require the use of certain flavours of CouchDB, there are two targets
to run these additional tests.

For tests which run against Cloudant Local or the Cloudant Service
```bash
$ ./gradlew cloudantTest
```
For tests which run only against the Cloudant Service
```bash
$ ./gradlew cloudantServiceTest
```

Note: you will need a Cloudant account to run tests against the Cloudant service.
