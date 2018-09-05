# Cloudant Java Client
[![Build Status](https://travis-ci.org/cloudant/java-cloudant.svg?branch=master)](https://travis-ci.org/cloudant/java-cloudant)
[![Maven Central](https://img.shields.io/maven-central/v/com.cloudant/cloudant-client.svg)](http://search.maven.org/#search|ga|1|g:"com.cloudant"%20AND%20a:"cloudant-client")
[![Javadocs](http://www.javadoc.io/badge/com.cloudant/cloudant-client.svg)](http://www.javadoc.io/doc/com.cloudant/cloudant-client)

This is the official Cloudant library for Java.

* [Installation and Usage](#installation-and-usage)
* [Getting Started](#getting-started)
* [API Reference (javadoc)](http://www.javadoc.io/doc/com.cloudant/cloudant-client/)
* [Related Documentation](#related-documentation)
* [Development](#development)
    * [Contributing](CONTRIBUTING.md)
    * [Test Suite](CONTRIBUTING.md#testing)
    * [Using in Other Projects](#using-in-other-projects)
    * [License](#license)
    * [Issues](#issues)
* [Caching, Encryption, and Compression](#caching-encryption-and-compression)

## Installation and Usage

Gradle:
```groovy
dependencies {
    compile group: 'com.cloudant', name: 'cloudant-client', version: '2.13.1'
}
```

Gradle with [optional `okhttp-urlconnection` dependency](#optional-okhttp-dependency):
```groovy
dependencies {
    compile group: 'com.cloudant', name: 'cloudant-client', version: '2.13.1'
    compile group: 'com.squareup.okhttp3', name: 'okhttp-urlconnection', version: '3.8.1'
}
```

Maven:
~~~ xml
<dependency>
  <groupId>com.cloudant</groupId>
  <artifactId>cloudant-client</artifactId>
  <version>2.13.1</version>
</dependency>
~~~

Maven with [optional `okhttp-urlconnection` dependency](#optional-okhttp-dependency):

~~~ xml
<dependency>
  <groupId>com.cloudant</groupId>
  <artifactId>cloudant-client</artifactId>
  <version>2.13.1</version>
</dependency>

<dependency>
  <groupId>com.squareup.okhttp3</groupId>
  <artifactId>okhttp-urlconnection</artifactId>
  <version>3.8.1</version>
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

// Note: there are some convenience methods for IBM Bluemix
//
// To use the URL and credentials provided in VCAP metadata:
// ClientBuilder.bluemix(String vcapMetadata)
//              .build();
//
// To use an IAM API key:
// ClientBuilder.url("examplebluemixaccount.cloudant.com")
//              .iamApiKey("exampleApiKey")
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

There is significantly more documentation, including additional code samples and explanations, in
the [javadoc](http://www.javadoc.io/doc/com.cloudant/cloudant-client/).
The first page you land on when following the javadoc link includes a table of
contents with further links to help guide you to the documentation you need.
To find the additional information be sure to scroll down past the auto-generated summary tables and
do not overlook the package overviews.

## Related documentation
* [API reference (javadoc)](http://www.javadoc.io/doc/com.cloudant/cloudant-client/)
* [Cloudant docs](https://console.bluemix.net/docs/services/Cloudant/cloudant.html#overview)
* [Cloudant Learning Center](https://developer.ibm.com/clouddataservices/cloudant-learning-center/)

## Development

For information about contributing, building, and running tests see the [CONTRIBUTING.md](CONTRIBUTING.md).

### Using in Other Projects

The preferred approach for using java-cloudant in other projects is to use the Gradle or Maven dependency as described above.

### License

Copyright © 2014, 2016 IBM Corp. All rights reserved.

Licensed under the apache license, version 2.0 (the "license"); you may not use this file except in compliance with the license.  you may obtain a copy of the license at

    http://www.apache.org/licenses/LICENSE-2.0.html

Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "as is" basis, without warranties or conditions of any kind, either express or implied. See the license for the specific language governing permissions and limitations under the license.

### Issues

Before opening a new issue please consider the following:
* Only the latest release is supported. If at all possible please try to reproduce the issue using
the latest version.
* Please check the [existing issues](https://github.com/cloudant/java-cloudant/issues)
to see if the problem has already been reported. Note that the default search
includes only open issues, but it may already have been closed.
* Cloudant customers should contact Cloudant support for urgent issues.
* When opening a new issue [here in github](../../issues) please complete the template fully.

## Caching, Encryption, and Compression

Caching data at the client, when it is appropriate for the application, can often improve performance considerably. In some cases, it may also be desirable to encrypt or compress data at the client.
There is no built-in support for caching, encryption or compression at the client in java-cloudant. Other Java libraries that are [not officially supported by Cloudant](https://console.bluemix.net/docs/services/Cloudant/libraries/index.html#client-libraries), but can provide these capabilities are:

* [java-cloudant-cache](https://github.com/cloudant-labs/java-cloudant-cache) can be used to provide caching integrated with the API of java-cloudant.

* [storage-client-library](https://github.com/aruniyengar/storage-client-library) can be used to provide client-side encryption and compression.

