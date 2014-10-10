# Cloudant Java Client

This is the official Cloudant library for Java

* [Installation and Usage](#installation-and-usage)
* [Getting Started](#getting-started)
* [API Reference](#api-reference)
* [Development](#development)
  * [Test Suite](#test-suite)
  * [Using in Other Projects](#using-in-other-projects)
  * [License](#license)

## Installation and Usage

Maven
~~~ xml

  <dependency>
  <groupId>com.cloudant.client.api</groupId>
  <artifactId>cloudant</artifactId> 
  <version>0.0.9</version>
</dependency>

~~~

Alternately download the dependencies  
  [cloudant.jar](todo)    
  [HttpClient 4.3.3](http://hc.apache.org/downloads.cgi)  
  [HttpCore 4.3.2](http://hc.apache.org/downloads.cgi)  
  [Commons Codec 1.](http://commons.apache.org/codec/download_codec.cgi)
  [Commons Logging 1.1.3](http://commons.apache.org/logging/download_logging.cgi)  
  [Gson 2.2.4](http://code.google.com/p/google-gson/downloads/list)  
  
### Getting Started

Now it's time to begin doing real work with Cloudant and Java

Initialize your Cloudant connection by constructing a *com.cloudant.client.api.CloudantClient* supplying the *account* to connect to along with *userName or Apikey* and  *password*

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
client.deleteDB("alice", "delete database");

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
- [Server Functions](#Server Functions)
	- [CloudantClient.createDB(name)](#com.cloudant.client.api.CloudantClient.createDB(name))
	- [CloudantClient.database(name, create)](#com.cloudant.client.api.CloudantClient.database(name,create))
	- [CloudantClient.deleteDB(name, confirmFlag)](com.cloudant.client.api.CloudantClient.deleteDB(name, confirmDelete))
	- [CloudantClient.getAllDbs()](#com.cloudant.client.api.CloudantClient.getAllDbs())
	- [CloudantClient.getMembership()](#com.cloudant.client.api.CloudantClient.getMembership())
	- [CloudantClient.getActiveTasks()](#com.cloudant.client.api.CloudantClient.getActiveTasks())	
	- [CloudantClient.replicator()](#com.cloudant.client.api.CloudantClient.replicator())
	- [CloudantClient.replication()](#com.cloudant.client.api.CloudantClient.replication() )
	- [CloudantClient.executeRequest()](#com.cloudant.client.api.CloudantClient.executeRequest())
	- [CloudantClient.uuids(number)](#com.cloudant.client.api.CloudantClient.uuids())  
	- [CloudantClient.getServerVersion()](#com.cloudant.client.api.CloudantClient.getServerVersion())
- [Database Functions](#Database Functions)
	- [Database.changes()](#com.cloudant.client.api.Database.changes())
	- [Database.getShard(documentId)](#com.cloudant.client.api.Database.getShard(documentId))
	- [Database.info()](#com.cloudant.client.api.Database.Database.info())
	- [Database.setPermissions()](#com.cloudant.client.api.Database.setPermissions())
- [Document Functions](#Document functions)
	- [Database.save(pojo)](#com.cloudant.client.api.Database.save(pojo))
	- [Database.save(map)](#com.cloudant.client.api.Database.save(map))
	- [Database.save(jsonObject)](#com.cloudant.client.api.Database.save(jsonObject))
	- [Database.find(class,doc-id)](#com.cloudant.client.api.Database.find(class,doc-id))
	- [Database.find(class,doc-id,rev-id)](#com.cloudant.client.api.Database.find(class,doc-id,rev-id))
	- [Database.contains(doc-id)](#com.cloudant.client.api.Database.contains(doc-id))
	- [Database.remove(object)](#com.cloudant.client.api.Database.remove(object))
	- [Database.remove(doc-id,rev-id)](#com.cloudant.client.api.Database.remove(doc-id,rev-id))
- [Bulk Documents](#Bulk Documents)
	- [Insert/Update docs ](#Insert/Update docs )
	- [Fetch multiple documents](#Fetch multiple documents)
- [Attachment Functions](#Attachment Functions)
	- [Inline attachment](#Inline attachment)
	- [Standalone Attachments](#Standalone Attachments)	
- [Design Document Functions](#Design Document Functions)
	- [query on a view](#query on a view)
	- [retrieving the design doc from server](#retrieving the design doc from server)
	- [synchronizing design doc ](#synchronizing design doc)
- [Cloudant Query](#Cloudant Query)
- [Cloudant Search](#Cloudant Search)
- [Cookie Authentication](#Cookie Authentication)
- [Advanced Configuration](#Advanced Configuration)
- [tests](#tests)

### Initialization

To use Cloudant, initialize your Cloudant connection by constructing a `CloudantClient` supplying the `account` to connect to along with `userName or Apikey` and  `password` (And see the [security note](#security-note) about placing your password into your source code.

~~~ java
String password = System.getProperty("cloudant_password");
CloudantClient client = new CloudantClient("mdb","mdb",password);

System.out.println("Connected to Cloudant");

  /*
   * The rest of my code goes here.
   */
})
~~~



## Authorization

This feature interfaces with the Cloudant [authorization API][auth].

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

### com.cloudant.client.api.CloudantClient.deleteDB(name, confirmDelete)

Destroy database named `name`.

~~~ java
client.deleteDB("alice","delete database");

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

`.setPermissions()` sets the permissions for the DB.
~~~ java
	ApiKey key = client.generateApiKey();
	EnumSet<Permissions> p = EnumSet.<Permissions>of( Permissions._writer, Permissions._reader);
	db.setPermissions(key.getKey(), p);
~~~

 
## Document functions

Once you run `com.cloudant.client.api.CloudantClient.database(name,create)`, use the returned object to work with documents in the database.

### com.cloudant.client.api.Database.save(pojo)

Insert `pojo` in the database. The parameter (an object) is the pojo. 

~~~ java
com.cloudant.client.api.Database db = dbClient.database("alice", true);
Foo foo = new Foo(); 
Response response = db.save(foo); 

~~~

### com.cloudant.client.api.Database.save(map)
Insert `map` in the database. The parameter (map) is the key value presentation of a document.

~~~ java
com.cloudant.client.api.Database db = dbClient.database("alice", true);
Map<String, Object> map = new HashMap<>();
map.put("_id", "test-doc-id-1");
map.put("title", "test-doc");
Response response =db.save(map);

~~~ 


### com.cloudant.client.api.Database.save(jsonObject)

~~~ java
com.cloudant.client.api.Database db = dbClient.database("alice", true);
JsonObject json = new JsonObject();
json.addProperty("_id", "test-doc-id-2");
json.add("json-array", new JsonArray());
Response response =db.save(json); 

~~~ 

### com.cloudant.client.api.Database.find(class,doc-id)

Retrieve the pojo from database by providing `doc_id`  .

~~~ java
com.cloudant.client.api.Database db = dbClient.database("alice", true);
Foo foo = db.find(Foo.class, "doc-id");

~~~

### com.cloudant.client.api.Database.find(class,doc-id,rev-id)
Retrieve the pojo from database by providing `doc_id`and `rev-id`  .

~~~ java
com.cloudant.client.api.Database db = dbClient.database("alice", true);
Foo foo = db.find(Foo.class, "doc-id", "rev-id");

~~~

### com.cloudant.client.api.Database.contains(doc-id)
returns true if the document exists with given `doc-id`

~~~ java

com.cloudant.client.api.Database db = dbClient.database("alice", true);
boolean found = db.contains("doc-id");
~~~

### com.cloudant.client.api.Database.remove(object)

The API removes the `object` from database. The object should contain `_id` and `_rev` .

~~~ java

com.cloudant.client.api.Database db = dbClient.database("alice", true);
Response response = db.remove(foo);
~~~

### com.cloudant.client.api.Database.remove(doc-id,rev-id)
~~~ java

com.cloudant.client.api.Database db = dbClient.database("alice", true);
Response response = db.remove("doc-id", "doc-rev");
~~~
## Bulk Documents

Bulk documents API performs two operations: Modify & Fetch for bulk documents. 

### Insert/Update docs 

~~~ java

List<Object> newDocs = new ArrayList<Object>();
newDocs.add(new Foo());
newDocs.add(new JsonObject());
boolean allOrNothing = true;
List<Response> responses = db.bulk(newDocs, allOrNothing);
~~~

### Fetch multiple documents

List all the docs in the database with optional query string additions `params`.

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

As with Nano, when working with a database (as opposed to the root server), call the `.database()` method.

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

List<Movie> movies = db.findByIndex("\"selector\": { \"Movie_year\": {\"$gt\": 1960}, \"Person_name\": \"Alec Guinness\" }",
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

Besides the account and password options, you can add an optional `com.cloudant.client.api.model.ConnectOptions` value, which will initialize Request (the underlying HTTP library) as you need it.

~~~ java
ConnectOptions connectOptions = new ConnectOptions()
                                                              . setSocketTimeout(50)
                                                              . setConnectionTimeout(50)
                                                              . setMaxConnections(100)
                                                              .setProxyHost("http://localhost")
                                                              .setProxyPort(8080);
 CloudantClient client = new CloudantClient("cloudant.com","test","password",  
                                                  connectOptions );
                                                  
~~~



## tests

to run (and configure) the test suite simply:

~~~ sh
cd Cloudant
npm install
npm test
~~~

after adding a new test you can run it individually (with verbose output) using:

~~~ sh
Cloudant_env=testing node tests/doc/list.js list_doc_params
~~~

where `list_doc_params` is the test name.

## Test Suite

We use npm to handle running the test suite. To run the comprehensive test suite, just run `npm test`. However, to run only the Cloudant-specific bits, we have a custom `test-cloudant` script.

    $ npm run test-cloudant

    > cloudant@5.10.1 test-cloudant /Users/jhs/src/cloudant/nodejs-cloudant
    > env NOCK=on sh tests/cloudant/run-tests.sh

    Test against mocked local database

      /tests/cloudant/auth.js

    ? 5/5 cloudant:generate_api_key took 196ms
    ? 3/3 cloudant:set_permissions took 7ms
    ? 8/8 summary took 224ms
    <...cut a bunch of test output...>

This runs against a local "mock" web server, called Nock. However the test suite can also run against a live Cloudant service. I have registered "nodejs.cloudant.com" for this purpose. To use it, run the `test-cloudant-live` script.

    $ npm run test-cloudant-live

    > cloudant@5.10.1 test-cloudant-live /Users/jhs/src/cloudant/nodejs-cloudant
    > sh tests/cloudant/run-tests.sh

    Test against mocked local database

      /tests/cloudant/auth.js

    ? 5/5 cloudant:generate_api_key took 192ms
    ? 3/3 cloudant:set_permissions took 7ms
    ? 8/8 summary took 221ms
    <...cut a bunch of test output...>

Unfortunately you need to know the password.

    $ npm run test-cloudant-live

    > cloudant@5.10.1 test-cloudant-live /Users/jhs/src/cloudant/nodejs-cloudant
    > sh tests/cloudant/run-tests.sh

    Test against remote Cloudant database
    No password configured for remote Cloudant database. Please run:

    npm config set cloudant_password "<your-password>"

    npm ERR! cloudant@5.10.1 test-cloudant-live: `sh tests/cloudant/run-tests.sh`
    <...cut npm error messages...>

Get the password from Jason somehow, and set it as an npm variable.

    # Note the leading space to keep this command out of the Bash history.
    $  npm config set cloudant_password "ask jason for the password" # <- Not the real password
    $ npm run test-cloudant-live
    <...cut successful test suite run...>

## License

Copyright 2014 Cloudant, an IBM company.

Licensed under the apache license, version 2.0 (the "license"); you may not use this file except in compliance with the license.  you may obtain a copy of the license at

    http://www.apache.org/licenses/LICENSE-2.0.html

Unless required by applicable law or agreed to in writing, software distributed under the license is distributed on an "as is" basis, without warranties or conditions of any kind, either express or implied. See the license for the specific language governing permissions and limitations under the license.

[nano]: https://github.com/dscape/nano
[query]: http://docs.cloudant.com/api/cloudant-query.html
[search]: http://docs.cloudant.com/api/search.html
[auth]: http://docs.cloudant.com/api/authz.html
[issues]: https://github.com/cloudant/nodejs-cloudant/issues
[follow]: https://github.com/iriscouch/follow
[request]:  https://github.com/mikeal/request
