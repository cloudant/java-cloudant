# Cloudant Java Client
[![Build Status](https://travis-ci.org/cloudant/java-cloudant.svg?branch=master)](https://travis-ci.org/cloudant/java-cloudant)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.cloudant/cloudant-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.cloudant/cloudant-client)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.cloudant/cloudant-client/badge.svg)](http://www.javadoc.io/doc/com.cloudant/cloudant-client)

This is the official Cloudant library for Java.

* [Installation and Usage](#installation-and-usage)
* [Getting Started](#getting-started)
* [API Reference (javadoc)](http://www.javadoc.io/doc/com.cloudant/cloudant-client/)
* [Related Documentation](#related-documentation)
* [Development](#development)
    * [Contributing](CONTRIBUTING.md)
    * [Test Suite](CONTRIBUTING.md#running-the-tests)
    * [Using in Other Projects](#using-in-other-projects)
    * [License](#license)
    * [Issues](#issues)

## Installation and Usage

Gradle:
```groovy
dependencies {
    compile group: 'com.cloudant', name: 'cloudant-client', version: '2.3.0'
}
```

Gradle with [optional `okhttp-urlconnection` dependency](#optional-okhttp-dependency):
```groovy
dependencies {
    compile group: 'com.cloudant', name: 'cloudant-client', version: '2.3.0'
    compile group: 'com.squareup.okhttp', name: 'okhttp-urlconnection', version: '2.7.5'
}
```

Maven:
~~~ xml
<dependency>
  <groupId>com.cloudant</groupId>
  <artifactId>cloudant-client</artifactId>
  <version>2.3.0</version>
</dependency>
~~~

Maven with [optional `okhttp-urlconnection` dependency](#optional-okhttp-dependency):

~~~ xml
<dependency>
  <groupId>com.cloudant</groupId>
  <artifactId>cloudant-client</artifactId>
  <version>2.3.0</version>
</dependency>

<dependency>
  <groupId>com.squareup.okhttp</groupId>
  <artifactId>okhttp-urlconnection</artifactId>
  <version>2.7.5</version>
</dependency>
~~~

##### Optional OkHttp dependency

HTTP requests to the database are made using `java.net.HttpURLConnection`. Adding the optional dependency for the `okhttp-urlconnection` changes the
`HttpURLConnection` from the default JVM implementation to the OkHttp implementation. Note that to use OkHttp requires a minimum of Java 1.7.

The main use case that is supported by this optional dependency is configuration of connection pools on a per `CloudantClient` basis
([see the javadoc](http://www.javadoc.io/doc/com.cloudant/cloudant-client/) for ClientBuilder.maxConnections). If the OkHttp dependency is
available at runtime it will be used automatically. Not using OkHttp will result in a smaller application size.

## Getting Started

This section contains a simple example of creating a `com.cloudant.client.api.CloudantClient` instance and interacting with Cloudant.

For further examples and more advanced use cases [see the javadoc](http://www.javadoc.io/doc/com.cloudant/cloudant-client/) for the version you are using.
The source code for the tests in this github project also contains many examples of API usage.

~~~ java
// Create a new CloudantClient instance for account endpoint example.cloudant.com
CloudantClient client = ClientBuilder.account("example")
                                     .username("exampleUser")
                                     .password("examplePassword")
                                     .build();

// Note: for Cloudant Local or Apache CouchDB use:
// ClientBuilder.url(new URL("yourCloudantLocalAddress.example"))
//              .username("exampleUser")
//              .password("examplePassword")
//              .build();

// Show the server version
System.out.println("Server Version: " + client.serverVersion());

// Get a List of all the databases this Cloudant account
List<String> databases = client.getAllDbs();
System.out.println("All my databases : ");
for ( String db : databases ) {
	System.out.println(db);
}

// Working with data

// Delete a database we created previously.
client.deleteDB("example_db");

// Create a new database.
client.createDB("example_db");

// Get a Database instance to interact with, but don't create it if it doesn't already exist
Database db = client.database("example_db", false);

// A Java type that can be serialized to JSON
public class ExampleDocument {
  private String _id = "example_id";
  private String _rev = null;
  private boolean isExample;

  public ExampleDocument(boolean isExample) {
    this.isExample = isExample;
  }

  public String toString() {
    return "{ id: " + _id + ",\nrev: " + _rev + ",\nisExample: " + isExample + "\n}";
  }
}

// Create an ExampleDocument and save it in the database
db.save(new ExampleDocument(true));
System.out.println("You have inserted the document");

// Get an ExampleDocument out of the database and deserialize the JSON into a Java type
ExampleDocument doc = db.find(ExampleDocument.class,"example_id");
System.out.println(doc);
~~~

Output:
```
Server version = 1.0.2
All my databases: example_db, stuff, scores
You have inserted the document.
{ id: example_id,
  rev: 1-6e4cb465d49c0368ac3946506d26335d,
  isExample: true
}
```

## Related documentation
* [API reference (javadoc)](http://www.javadoc.io/doc/com.cloudant/cloudant-client/)
* [Cloudant docs](http://docs.cloudant.com/)
* [Cloudant for developers](https://cloudant.com/for-developers/)

## Development

For information about contributing, building, and running tests see the [CONTRIBUTING.md](CONTRIBUTING.md).

### Using in Other Projects

The preferred approach for using java-cloudant in other projects is to use the Gradle or Maven dependency as described above.

### License

Copyright 2014-2015 Cloudant, an IBM company.

Licensed under the apache license, version 2.0 (the "license"); you may not use this file except in compliance with the license.  you may obtain a copy of the license at

    http://www.apache.org/licenses/LICENSE-2.0.html

Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "as is" basis, without warranties or conditions of any kind, either express or implied. See the license for the specific language governing permissions and limitations under the license.

### Issues

If you are a Cloudant customer please contact Cloudant support for help with any issues.

It is also possible to open issues [here in github](../../issues).
