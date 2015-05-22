Contributing
=======

Cloudant-client is written in Java and uses maven as its build tool.


## Requirements

- maven
- Java 1.6
- cloudant.com account


## Installing requirements

### Java

Follow the instructions for your platform.

### Maven

Using brew

```bash
$ brew install maven
```

# Building the library

The project should build out of the box with:

```bash
$ mvn compile
```

### Running the tests

After configuring using cloudant-sample.properties and
cloudant-2.sample.properties as guides, run:

```bash
$ mvn test
```
