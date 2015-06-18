# Cloudant Java Client
[![Build Status](https://travis-ci.org/cloudant/java-cloudant.svg?branch=master)](https://travis-ci.org/cloudant/java-cloudant)

This is the official Cloudant library for Java

* [Installation and Usage](#installation-and-usage)
* [Getting Started](#getting-started)
* [API Reference](#api-reference)
* [Development](#development)
  * [Test Suite](#test-suite)
  * [Using in Other Projects](#using-in-other-projects)
  * [License](#license)

## Installation and Usage

Maven:

~~~ xml

<dependency>
  <groupId>com.cloudant</groupId>
  <artifactId>cloudant-client</artifactId>
  <version>1.0.1</version>
</dependency>

~~~

Gradle:

```groovy
dependencies {
    compile group: 'com.cloudant', name: 'cloudant-client', version:'1.0.1'
}
```

Alternately download the dependencies  
  [cloudant.jar](http://search.maven.org/remotecontent?filepath=com/cloudant/cloudant-client/1.0.1/cloudant-client-1.0.1.jar)
  [HttpClient 4.3.3](http://hc.apache.org/downloads.cgi)  
  [HttpCore 4.3.2](http://hc.apache.org/downloads.cgi)  
  [Commons Codec 1.6](http://commons.apache.org/codec/download_codec.cgi)  
  [Commons Logging 1.1.3](http://commons.apache.org/logging/download_logging.cgi)  
  [Gson 2.2.4](http://code.google.com/p/google-gson/downloads/list)  

### Getting Started

Now it's time to begin doing real work with Cloudant and Java. For working code samples of any of the API's please go to our Test suite.

Initialize your Cloudant connection by constructing a *com.cloudant.client.api.CloudantClient* . If you are connecting the managed service on cloudant.com, supply the *account* to connect to, *api key* ( if using an API key, otherwise pass in the account for this parameter also) and *password*. If you are connecting to Cloudant Local supply its URL, the *userName or Apikey* and  *password*

Connecting to the managed service at cloudant.com example
~~~ java
String password = System.getProperty("cloudant_password");
CloudantClient client = new CloudantClient("mdb","mdb",password);

System.out.println("Connected to Cloudant");
System.out.println("Server Version: " + client.serverVersion());

List<String> databases = client.getAllDbs();
System.out.println("All my databases : ");
for ( String db : databases ) {
	System.out.println(db);
}
~~~

Connecting to Cloudant Local example
~~~ java
String password = System.getProperty("cloudant_password");
CloudantClient client = new CloudantClient("https://9.149.23.12","mdb",password);

System.out.println("Connected to Cloudant");
System.out.println("Server Version: " + client.serverVersion());

List<String> databases = client.getAllDbs();
System.out.println("All my databases : ");
for ( String db : databases ) {
	System.out.println(db);
}
~~~

Output:

    Connected to cloudant
    Server version = 1.0.2
    All my databases: example_db, jasons_stuff, scores

When you instaniate a `CloudantClient`, you are authenticating with cloudant using the [cookie authentication](http://guide.couchdb.org/editions/1/en/security.html#cookies) functionality

### Security Note

**DO NOT hard-code your password and commit it to Git**. Storing your password directly in your source code (even in old commits) is a serious security risk to your data. Whoever gains access to your software will now also have access read, write, and delete permission to your data. Think about GitHub security bugs, or contractors, or disgruntled employees, or lost laptops at a conference. If you check in your password, all of these situations become major liabilities. (Also, note that if you follow these instructions, the `export` command with your password will likely be in your `.bash_history` now, which is kind of bad. However, if you input a space before typing the command, it will not be stored in your history.)

Here is simple but complete example of working with data:

~~~ java

String password = System.getProperty("cloudant_password");
CloudantClient client = new CloudantClient("mdb","mdb",password);

// Clean up the database we created previously.
client.deleteDB("alice");

// Create a new database.
client.createDB("alice");

// specify the database we are going to use
Database db = client.database("alice", false);

// and insert a document in it
db.save(new Rabbit(true));
System.out.println("You have inserted the Rabbit");  
Rabbit r = db.find(Rabbit.class,"rabbit");
System.out.println(r);

   ...
public class Rabbit {
	private boolean crazy;
	private String _id = "rabbit";

	public Rabbit(boolean isCrazy) {
		crazy = isCrazy;
	}

	public String toString() {
		return " { id : " + _id + ", rev : " + _rev + ", crazy : " + crazy + "}";
	}
}
~~~

If you run this example, you will see:

    you have inserted the rabbit.
    { crazy: true,
      id: rabbit,
      rev: 1-6e4cb465d49c0368ac3946506d26335d
    }

## API Reference

- [Initialization](#initialization)
- [Authorization](#authorization)
- [Server Functions](#server-functions)
	- [CloudantClient.createDB(name)](#comcloudantclientapicloudantclientcreatedbname)
	- [CloudantClient.database(name, create)](#comcloudantclientapicloudantclientdatabasenamecreate)
	- [CloudantClient.deleteDB(name)](#comcloudantclientapicloudantclientdeletedbname)
	- [CloudantClient.getAllDbs()](#comcloudantclientapicloudantclientgetalldbs)
	- [CloudantClient.getMembership()](#comcloudantclientapicloudantclientgetmembership)
	- [CloudantClient.getActiveTasks()](#comcloudantclientapicloudantclientgetactivetasks)
	- [CloudantClient.replicator()](#comcloudantclientapicloudantclientreplicator)
	- [CloudantClient.replication()](#comcloudantclientapicloudantclientreplication )
	- [CloudantClient.executeRequest()](#comcloudantclientapicloudantclientexecuterequest)
	- [CloudantClient.uuids(number)](#comcloudantclientapicloudantclientuuids)  
	- [CloudantClient.getServerVersion()](#comcloudantclientapicloudantclientgetserverversion)
- [Database Functions](#database-functions)
	- [Database.changes()](#comcloudantclientapidatabasechanges)
	- [Database.getShard(documentId)](#comcloudantclientapidatabasegetsharddocumentid)
	- [Database.info()](#comcloudantclientapidatabasedatabaseinfo)
	- [Database.setPermissions()](#comcloudantclientapidatabasesetpermissions)
	- [Database.ensureFullCommit()](#comcloudantclientapidatabaseensurefullcommit)
- [Document Functions](#document-functions)
	- [Database.save(object)](#comcloudantclientapidatabasesaveobject)
	- [Database.save(object,writeQuorum)](#comcloudantclientapidatabasesaveobjectwritequorum)
	- [Database.post(object)](#comcloudantclientapidatabasepostobject)
	- [Database.post(object,writeQuorum)](#comcloudantclientapidatabasepostobjectwritequorum)
	- [Database.saveAttachment(inputStream,name,contentType)](#comcloudantclientapidatabasesaveattachmentinputstreamnamecontenttype)
	- [Database.saveAttachment(inputStream,name,contentType,docId,docRev)](#comcloudantclientapidatabasesaveattachmentinputstreamnamecontenttypedociddocrev)
	- [Database.batch(obect)](#comcloudantclientapidatabasebatchobject)
	- [Database.find(doc-id)](#comcloudantclientapidatabasefinddoc-id)
	- [Database.find(doc-id,rev)](#comcloudantclientapidatabasefinddoc-idrev)
	- [Database.find(class,doc-id)](#comcloudantclientapidatabasefindclassdoc-id)
	- [Database.find(class,doc-id,rev-id)](#comcloudantclientapidatabasefindclassdoc-idrev-id)
	- [Database.find(class,doc-id,params)](#comcloudantclientapidatabasefindclassdoc-idparams)
	- [Database.findAny(class,uri)](#comcloudantclientapidatabasefindanyclassuri)
	- [Database.contains(doc-id)](#comcloudantclientapidatabasecontainsdoc-id)
	- [Database.update(object)](#comcloudantclientapidatabaseupdateobject)
	- [Database.update(object,writeQuorum)](#comcloudantclientapidatabaseupdateobjectwritequorum)
	- [Database.remove(object)](#comcloudantclientapidatabaseremoveobject)
	- [Database.remove(doc-id,rev-id)](#comcloudantclientapidatabaseremovedoc-idrev-id)
	- [Database.invokeUpdateHandler(updateHandlerUri,docId,query)](#comcloudantclientapidatabaseinvokeupdatehandlerupdatehandleruridocidquery)
	- [Database.invokeUpdateHandler(updateHandlerUri,docId,params)](#comcloudantclientapidatabaseinvokeupdatehandlerupdatehandleruridocidparams)
- [Bulk Documents](#bulk-documents)
	- [Insert/Update docs ](#insertupdate-docs )
	- [Fetch All/multiple documents](#fetch-all/multiple-documents)
- [Attachment Functions](#attachment-functions)
	- [Inline attachment](#inline-attachment)
	- [Standalone Attachments](#standalone-attachments)
- [Design Document Functions](#design-document-functions)
	- [query on a view](#query-on-a-view)
	- [retrieving the design doc from server](#retrieving-the-design-doc-from-server)
	- [synchronizing design doc ](#synchronizing-design-doc)
- [Cloudant Query](#cloudant-query)
- [Cloudant Search](#cloudant-search)
- [Cookie Authentication](#cookie-authentication)
- [Advanced Configuration](#advanced-configuration)
- [tests](#tests)

### Initialization

When using the managed service at cloudant.com, initialize your Cloudant connection by constructing a `CloudantClient` supplying the `account` to connect to, `api key` ( if using an API key, otherwise pass in the account for this parameter also) and `password`  (And see the [security note](#security-note) about placing your password into your source code.

~~~ java
String password = System.getProperty("cloudant_password");
CloudantClient client = new CloudantClient("mdb","mdb",password);

System.out.println("Connected to Cloudant");

  /*
   * The rest of my code goes here.
   */
})
~~~

When using Cloudant Local, initialize your Cloudant connection by constructing a `CloudantClient` supplying the URL of the Cloudant Local along with `userName or Apikey` and  `password` (And see the [security note](#security-note) about placing your password into your source code.

~~~ java
String password = System.getProperty("cloudant_password");
CloudantClient client = new CloudantClient("https://9.123.45.64","mdb",password);

System.out.println("Connected to Cloudant");

  /*
   * The rest of my code goes here.
   */
})
~~~


## Authorization

This feature interfaces with the Cloudant authorization API

Use the authorization feature to generate new API keys to access your data. An API key is basically a username/password pair for granting others access to your data, without giving them the keys to the castle.

Generate an API key.

~~~ java
ApiKey key = client.generateApiKey();
System.out.println(key);

~~~

Output:

    key: isdaingialkyciffestontsk password: XQiDHmwnkUu4tknHIjjs2P64

Next, set access roles for this API key:

~~~ java
// Set read-only access for this key.
Database db = client.database("alice", false);
db.setPermissions(key.getKey(), EnumSet.<Permissions>of(Permissions._reader));
System.out.println(key.getKey() + " now has read-only access to alice")

~~~

## Server Functions

Once CloudantClient is initialized without errors, the returned object is representing your connection to the server. To work with databases, use these database functions. (To work with data *inside* the databases, see below.)

### com.cloudant.client.api.CloudantClient.createDB(name)

Create a Cloudant database with the given `name`.

~~~ java
client.createDB("alice");

~~~

### com.cloudant.client.api.CloudantClient.database(name,create)

Get a Database reference

~~~ java
Database db = client.database("alice", false);
System.out.println("Database Name:" + db.info().getDbName() );

~~~

### com.cloudant.client.api.CloudantClient.deleteDB(name)

Destroy database named `name`.

~~~ java
client.deleteDB("alice");

~~~

### com.cloudant.client.api.CloudantClient.getAllDbs()

List all the databases in Cloudant server.

~~~ java
List<String> databases = client.getAllDbs();
System.out.println("All my databases : ");
for ( String db : databases ) {
	System.out.println(db);
}

~~~

### com.cloudant.client.api.CloudantClient.getMembership()
`getMembership()` returns the list of nodes in a cluster

~~~ java
	Membership membership = client.getMembership();
~~~

### com.cloudant.client.api.CloudantClient.getActiveTasks()
`getActiveTasks()` returns all active tasks

~~~ java
	List<Task> tasks = client.getActiveTasks();
~~~

### com.cloudant.client.api.CloudantClient.replicator()

`replicator()` provides access to Cloudant `com.cloudant.client.api.Replicator` APIs

~~~ java
Replicator replicator = client.replicator()
~~~

### com.cloudant.client.api.CloudantClient.replication()

Replicates `source` to `target`. `target`
must exist, add `createTarget(true)` to create it prior to
replication.

~~~ java
ReplicationResult result = client.replication()
					.createTarget(true)
					.source(db1.getDBUri().toString())
					.target(db2.getDBUri().toString())
					.trigger();
List<ReplicationHistory> histories = result.getHistories();

~~~

### com.cloudant.client.api.CloudantClient.executeRequest()

This API enables extending Cloudant internal API by allowing a user-defined raw HTTP request to execute against a cloudant client.
~~~ java

HttpHead head = new HttpHead(dbClient.getDBUri() + "doc-id");
HttpResponse response = dbClient.executeRequest(head);
String revision = response.getFirstHeader("ETAG").getValue();
HttpClientUtils.closeQuietly(response);

~~~

### com.cloudant.client.api.CloudantClient.uuids()
`uuids()` request cloudant to send a list of UUIDs.

~~~ java
	List<String> uuids = client.uuids(count);
~~~

### com.cloudant.client.api.CloudantClient.getServerVersion()
`getServerVersion()` returns Cloudant Server version.

~~~ java
	String serverVersion = client.serverVersion();
~~~

## Database Functions

### com.cloudant.client.api.Database.changes()

`com.cloudant.client.api.Database.changes().getChanges()` asks for the changes feed on the specified database. `includeDocs(true)` and `limit(1)` sets additional properties to the query string.

~~~ java
ChangesResult changes = db.changes()
				.includeDocs(true)
				.limit(1)
				.getChanges();

List<ChangesResult.Row> rows = changes.getResults();

for (Row row : rows) {
	List<ChangesResult.Row.Rev> revs = row.getChanges();
	String docId = row.getId();
	JsonObject doc = row.getDoc();
}

~~~

`com.cloudant.client.api.Database.changes().continuousChanges()` asks for the continuous changes feed on the specified database. `since(since)`, `includeDocs(true)` and `limit(1)` sets additional properties to the query string.

~~~ java
CouchDbInfo dbInfo = db.info();
String since = dbInfo.getUpdateSeq();
Changes changes = db.changes()
				.includeDocs(true)
				.since(since)
				.heartBeat(30000)
				.continuousChanges();

while (changes.hasNext()) {
	ChangesResult.Row feed = changes.next();
	final JsonObject feedObject = feed.getDoc();
	final String docId = feed.getId();
	changes.stop();
}
~~~

### com.cloudant.client.api.Database.getShard(documentId)
`getShard(documentId)` gets info about the shard this document belongs to .

~~~ java
	Shard s = db.getShard("snipe");
~~~

`getShards()`get info about the shards in the database.

~~~ java
	List<Shard> shards = db.getShards();
~~~

### com.cloudant.client.api.Database.Database.info()

`.info()` returns the DB info for this db.

~~~ java
	DbInfo dbInfo = db.info();
~~~

### com.cloudant.client.api.Database.setPermissions()

`.setPermissions()` sets the permissions for a user/apiKey on the DB.
~~~ java
ApiKey key = client.generateApiKey();
EnumSet<Permissions> p = EnumSet.<Permissions>of( Permissions._writer, Permissions._reader);
db.setPermissions(key.getKey(), p);
~~~

### com.cloudant.client.api.Database.ensureFullCommit()
Requests the database commits any recent changes to disk

~~~ java
db.ensureFullCommit();
~~~

## Document Functions

Once you run `com.cloudant.client.api.CloudantClient.database(name,create)`, use the returned object to work with documents in the database.

### com.cloudant.client.api.Database.save(object)
Saves an object in the database, using HTTP PUT request.If the object doesn't have an `_id` value, we will assign a `UUID` as the document id.

~~~ java
Database db = dbClient.database("alice", true);
JsonObject json = new JsonObject();
json.addProperty("_id", "test-doc-id-2");
json.add("json-array", new JsonArray());
Response response =db.save(json);

~~~

Insert `pojo` in the database. The parameter `object` can be a pojo.

~~~ java
Database db = dbClient.database("alice", true);
Foo foo = new Foo();
Response response = db.save(foo);

~~~

Insert `map` in the database. The parameter `object`  can be a `map` having key value presentation of a document.

~~~ java
Database db = dbClient.database("alice", true);
Map<String, Object> map = new HashMap<>();
map.put("_id", "test-doc-id-1");
map.put("title", "test-doc");
Response response =db.save(map);

~~~


### com.cloudant.client.api.Database.save(object,writeQuorum)
Saves an object in the database, using HTTP PUT request, with specified write quorum .If the object doesn't have an `_id` value, we will assign a `UUID` as the document id.
~~~ java
Database db = dbClient.database("alice", true);
Response response = db.save(new Animal("human"), 2);

~~~

### com.cloudant.client.api.Database.post(object)
Saves an object in the database using HTTP POST request.The database will be responsible for generating the document id.

~~~ java
Database db = dbClient.database("alice", true);
Response response = db.post(new Foo());

~~~

### com.cloudant.client.api.Database.post(object,writeQuorum)
Saves an object in the database using HTTP POST request with specificied write quorum.The database will be responsible for generating the document id.

~~~ java
Database db = dbClient.database("alice", true);
db.post(new Animal("test"), 2);
Animal h = db.find(Animal.class, "test",
				new com.cloudant.client.api.model.Params().readQuorum(3));
~~~

### com.cloudant.client.api.Database.saveAttachment(inputStream,name,contentType)
 Saves an attachment to a new document with a generated UUID as the document id.

~~~ java
byte[] bytesToDB = "binary data".getBytes();
ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain");
~~~


### com.cloudant.client.api.Database.saveAttachment(inputStream,name,contentType,docId,docRev)
Saves an attachment to an existing document given both a document id and revision, or save to a new document given only the id, and rev as null.

~~~ java
byte[] bytesToDB = "binary data".getBytes();
ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain","abcd12345",null);
~~~

### com.cloudant.client.api.Database.batch(object)
Saves a document with batch. You can write documents to the database at a higher rate by using the batch option. This collects document writes together in memory (on a user-by-user basis) before they are committed to disk. This increases the risk of the documents not being stored in the event of a failure, since the documents are not written to disk immediately.

~~~ java
Database db = dbClient.database("alice", true);
db.batch(new Foo());

~~~

### com.cloudant.client.api.Database.find(doc-id)
Finds a document based on the provided `doc-id` and return the result as `InputStream`. Make sure you close the stream when done, else you could lock up the client easily

~~~ java
Database db = dbClient.database("alice", true);
Response response = db.save(new Foo());
InputStream inputStream = db.find(response.getId());

// do stuff and finally dont forget to close the stream
inputStream.close();

~~~

### com.cloudant.client.api.Database.find(doc-id,rev)
Finds a document based on the provided `doc-id` and `rev`,return the result as `InputStream`. Make sure you close the stream when done, else you could lock up the client easily

~~~ java
Database db = dbClient.database("alice", true);
Response response = db.save(new Foo());
InputStream inputStream = db.find(response.getId(),response.getRev());

// do stuff and finally dont forget to close the stream
inputStream.close();
~~~
### com.cloudant.client.api.Database.find(class,doc-id)

Retrieve the pojo from database by providing `doc_id`  .

~~~ java
Database db = dbClient.database("alice", true);
Foo foo = db.find(Foo.class, "doc-id");

~~~

### com.cloudant.client.api.Database.find(class,doc-id,rev-id)
Retrieve the pojo from database by providing `doc_id`and `rev-id`  .

~~~ java
Database db = dbClient.database("alice", true);
Foo foo = db.find(Foo.class, "doc-id", "rev-id");

~~~

### com.cloudant.client.api.Database.find(class,doc-id,params)
Finds an Object of the specified type by providing `doc_id`.Extra query parameters can be specified via `params` argument

~~~ java
Database db = dbClient.database("alice", true);
Response response = db.save(new Foo());
Foo foo = db.find(Foo.class, response.getId(), new Params().revsInfo());

~~~

### com.cloudant.client.api.Database.findAny(class,uri)
This method finds any document given a URI.The URI must be URI-encoded.

~~~ java
Database db = dbClient.database("alice", true);
Foo foo = db.findAny(Foo.class,
				"https://mdb.cloudant.com/alice/03c6a4619b9e42d68db0e592757747fe");
~~~

### com.cloudant.client.api.Database.contains(doc-id)
returns true if the document exists with given `doc-id`

~~~ java

Database db = dbClient.database("alice", true);
boolean found = db.contains("doc-id");
~~~

### com.cloudant.client.api.Database.update(object)
Updates an object in the database, the object must have the correct `_id` and `_rev` values.

~~~ java
Database db = dbClient.database("alice", true);
String idWithSlash = "a/" + generateUUID();
Response response = db.save(new Bar(idWithSlash));

Bar bar = db.find(Bar.class, response.getId());
Response responseUpdate = db.update(bar);
~~~

### com.cloudant.client.api.Database.update(object,writeQuorum)
Updates an object in the database, the object must have the correct `_id` and `_rev` values. The second argument is the write quorum for the update.

~~~ java
db.save(new Animal("human"), 2);
Animal h = db.find(Animal.class, "human",
				new com.cloudant.client.api.model.Params().readQuorum(2));
db.update(h.setClass("inhuman"), 2);

~~~


### com.cloudant.client.api.Database.remove(object)

The API removes the `object` from database. The object should contain `_id` and `_rev` .

~~~ java

Database db = dbClient.database("alice", true);
Response response = db.remove(foo);
~~~

### com.cloudant.client.api.Database.remove(doc-id,rev-id)
~~~ java

Database db = dbClient.database("alice", true);
Response response = db.remove("doc-id", "doc-rev");
~~~

### com.cloudant.client.api.Database.invokeUpdateHandler(updateHandlerUri,docId,query)
Invokes an Update Handler.Use this method in particular when the docId contain special characters such as slashes (/). The `updateHandlerUri` should be in the format: `designDoc/update1`.
~~~ java
final String oldValue = "foo";
final String newValue = "foo bar";
Response response = db.save(new Foo(null, oldValue));
String query = "field=title&value=" + newValue;
String output = db.invokeUpdateHandler("example/example_update", response.getId(), query);
~~~

### com.cloudant.client.api.Database.invokeUpdateHandler(updateHandlerUri,docId,params)
This method can be used if the query is generated using `Params` API.

~~~ java
final String oldValue = "foo";
final String newValue = "foo bar";
Response response = db.save(new Foo(null, oldValue));
Params params = new Params()
				.addParam("field", "title")
				.addParam("value", newValue);
String output = db.invokeUpdateHandler("example/example_update", response.getId(), params);
~~~

## Bulk Documents

Bulk documents API performs two operations: Modify & Fetch for bulk documents.

### Insert/Update docs

~~~ java

List<Object> newDocs = new ArrayList<Object>();
newDocs.add(new Foo());
newDocs.add(new JsonObject());
List<Response> responses = db.bulk(newDocs);
~~~

### Fetch all/multiple documents

List all the docs in the database with optional query string additions `params`.

~~~ java

List<Foo> docs = dbClient.view("_all_docs")
					  .includeDocs(true)
					  .query(Foo.class);

~~~

List multiple documents specified by docID's in the database .
~~~ java

List<String> keys = Arrays.asList(new String[]{"doc-id-1", "doc-id-2"});
List<Foo> docs = dbClient.view("_all_docs")
					  .includeDocs(true)
					  .keys(keys)
					  .query(Foo.class);

~~~


## Attachment Functions

### Inline attachment
 com.cloudant.client.api.model.Attachment represents an inline attachment enclosed in a document.

The base64 data of an attachment may be encoded utilizing the included dependency on Apache Codec Base64.encodeBase64String(bytes).

Model classes that extend com.cloudant.client.api.model.Document inherit the support for inline attachments.
~~~ java

Attachment attachment = new Attachment();
attachment.setData(Base64.encodeBase64String("attachment test string".getBytes()));
attachment.setContentType("text/plain");
Foo foo = new Foo(); // Foo extends Document
foo.addAttachment("attachment.txt", attachment);
db.save(foo);
~~~

To retrieve the base64 data of an attachment, include a parameter to the find()

~~~ java

Foo foo = db.find(Foo.class, "doc-id", new Params().attachments());
String attachmentData = foo.getAttachments().get("attachment.txt").getData();
~~~

### Standalone Attachments
Standalone attachments could be saved to an existing, or to a new document. The attachment data is provided as `InputStream`

~~~ java

byte[] bytesToDB = "binary attachment data".getBytes();
ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain");
InputStream in = db.find( "doc_id/foo.txt");
~~~

## Design Document Functions

These functions are for working with views and design documents, including querying the database using map-reduce views, [Cloudant Search](#cloudant-search), and [Cloudant Query](#cloudant-query).

### query on a view

Call a view of the specified design to get the list of documents.

~~~ java

List<Foo> foos = db.view("example/foo")
			        .includeDocs(true)
				.query(Foo.class);
~~~
 If you're looking to filter the view results by key(s), pass multiple keys as the argument of key() function

~~~ java

List<Foo> foos = db.view("example/foo")
				.includeDocs(true)
				.key("key-1","key-2")
				.query(Foo.class);
~~~

If you're looking to filter the view results by a range of keys, call startKey(key) and endKey(key) method

~~~ java

List<Foo> foos = db.view("example/foo")
				.startKey("key-1")
				.endKey("key-2")
				.includeDocs(true)
				.query(Foo.class);

~~~

To get the primitive value  call the scalar methods e.g queryForInt() or queryForLong()

~~~ java

int count = dbClient.view("example/by_tag").key("cloudantdb").queryForInt();
~~~
### retrieving the design doc from server
call getFromDb(design-doc) to retrieve the server copy .
~~~ java

DesignDocument designDoc = db.design().getFromDb("_design/example");
~~~
### synchronizing design doc

call synchronizeWithDb(design-doc) method to synchronize the server copy with local one.

~~~ java

DesignDocument designDoc = db.design().getFromDesk("example");
db.design().synchronizeWithDb(designDoc);
~~~

All the design documents can be synchronized in a single attempt

~~~ java

db.syncDesignDocsWithDb();
~~~



Take a look at the [CouchDB wiki](http://wiki.apache.org/couchdb/Formatting_with_Show_and_List#Showing_Documents) for possible query paramaters and more information on show functions.



## Cloudant Query

This feature interfaces with Cloudant's query functionality. See the [Cloudant Query documentation][query] for details.

when working with a database (as opposed to the root server), call the `.database()` method.

~~~ java

Database db = dbClient.database("movies-demo", false);
~~~

To see all the indexes in a database, call the database `.listIndices()` method .

~~~ java

List<Index> indices = db.listIndices();
~~~

To create an index, use  `.createIndex()` method

~~~ java

db.createIndex("Person_name", "Person_name", null,
				new IndexField[]{
					new IndexField("Person_name",SortOrder.asc),
					new IndexField("Movie_year",SortOrder.asc)});
db.createIndex("Movie_year", "Movie_year", null,
				new IndexField[]{
				       new IndexField("Movie_year",SortOrder.asc)});
~~~



To query using the index, use the `.findByIndex()` method.

~~~ java

List<Movie> movies = db.findByIndex("{
                     \"Movie_year\": {\"$gt\": 1960}, \"Person_name\": \"Alec Guinness\"
                     }",
					Movie.class,
					new FindByIndexOptions()
						.sort(new IndexField("Movie_year", SortOrder.desc))
						.fields("Movie_name").fields("Movie_year"));
~~~


## Cloudant Search

This feature interfaces with Cloudant's search functionality. See the [Cloudant Search documentation][search] for details.

First, when working with a database (as opposed to the root server), call the `.database()` method.

~~~ java

Database db = dbClient.database("movies-demo", false);
~~~

To create a Cloudant Search index, create a design document and call `synchronizeWithDb()` method.

~~~ java

DesignDocument designDoc = db.design().getFromDesk("views101");
db.design().synchronizeWithDb(designDoc);
~~~

To query this index, create instance of `com.cloudant.client.api.Search`  by calling the database `.search()` method. The argument of `.search()` method is the design document name. The other criterion can be set by calling different methods on `search` object.

~~~ java

Search search = db.search("views101/animals");
SearchResult<Animal> rslt= search.limit(10)
				                       .includeDocs(true)
                                 			.counts(new String[] {"class","diet"})
				                        .querySearchResult("l*", Animal.class);
~~~

## Cookie Authentication

Cloudant supports making requests using Cloudant's [cookie authentication](http://guide.couchdb.org/editions/1/en/security.html#cookies) functionality. there's a [step-by-step guide here](http://codetwizzle.com/articles/couchdb-cookie-authentication-nodejs-couchdb/), but essentially you just:

~~~ java

 CloudantClient client = new CloudantClient("cloudant.account",
							 "cloudant.username",
							 "cloudant.password"));
String cookie = client.getCookie() ;
  ~~~

To reuse a cookie:

~~~ java

// Make a new connection with the cookie.
CloudantClient cookieBasedClient = new
                          CloudantClient("cloudant.account", cookie);

~~~


### Advanced Configuration

Besides the account and password options, you can add an optional `com.cloudant.client.api.model.ConnectOptions` value, which will initialize HttpClient (the underlying HTTP library) as you need it.

~~~ java
ConnectOptions connectOptions = new ConnectOptions()
                                        .setSocketTimeout(50)
                                        .setConnectionTimeout(50)
                                        .setMaxConnections(100)
                                        .setProxyHost("http://localhost")
                                        .setProxyPort(8080)
                                        .disableSSLAuthentication(true);
 CloudantClient client = new CloudantClient("cloudant.com","test","password",  
                                                  connectOptions );

~~~

java-cloudant internally uses the Gson library to serialize/deserialize JSON to/from Java objects. You can register your custom de-serializers by providing the CloudantClient instance by with your own GsonBuilder instance

~~~java
GsonBuilder builder = new GsonBuilder();
builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss");

CloudantClient account = new CloudantClient(cloudantaccount,userName,password);
account.setGsonBuilder(builder);
~~~


## tests

The test suite needs access to cloudant account(s) to run.
To run the test suite first edit the cloudant properties.
Copy the files `src/test/resources/cloudant-sample.properties` and
`src/test/resources/cloudant-2-sample.properties` to
`src/test/resources/cloudant.properties` and
`src/test/resources/cloudant-2.properties`, and then provide values for the following properties:

~~~ java
cloudant.account=myCloudantAccount
cloudant.username=testuser
cloudant.password=testpassword
~~~

Once all the required properties are provided in the properties file
run the `com.cloudant.test.main.CloudantTestSuite` test class.

## License

Copyright 2014-2015 Cloudant, an IBM company.

Licensed under the apache license, version 2.0 (the "license"); you may not use this file except in compliance with the license.  you may obtain a copy of the license at

    http://www.apache.org/licenses/LICENSE-2.0.html

Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "as is" basis, without warranties or conditions of any kind, either express or implied. See the license for the specific language governing permissions and limitations under the license.

[query]: http://docs.cloudant.com/api/cloudant-query.html
[search]: http://docs.cloudant.com/api/search.html
[auth]: http://docs.cloudant.com/api/authz.html
[issues]: https://github.com/cloudant/java-cloudant /issues
[follow]: https://github.com/iriscouch/follow
[request]:  https://github.com/mikeal/request
