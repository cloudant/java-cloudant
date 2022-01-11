# Migrating to the `cloudant-java-sdk` library
This document is to assist in migrating from the `java-cloudant` (coordinates: `com.cloudant:cloudant-client`) to the newly supported [`cloudant-java-sdk`](https://github.com/IBM/cloudant-java-sdk) (coordinates: `com.ibm.cloud:cloudant`).

## Initializing the client connection
There are several ways to create a client connection in `cloudant-java-sdk`:
1. [Environment variables](https://github.com/IBM/cloudant-java-sdk#authentication-with-environment-variables)
2. [External configuration file](https://github.com/IBM/cloudant-java-sdk#authentication-with-external-configuration)
3. [Programmatically](https://github.com/IBM/cloudant-java-sdk#programmatic-authentication)

[See the README](https://github.com/IBM/cloudant-java-sdk#code-examples) for code examples on using environment variables.

## Other differences
1.  In `cloudant-java-sdk` all operations are performed from the scope of the client instance and
  not associated with any sub-scope like `Database`. There is no need to instantiate
  a `Database` object to interact with documents - the database name is included as part of
  document operations. For example, in the case of updating a document you would first call
  `getDocument` to fetch and then `putDocument` to update, there is no need to `getDatabase`.
1. In `cloudant-java-sdk` a user-supplied POJO is not required to represent a document.
  The default document representation is the `Document` model class. It is still possible
  to use POJOs in preference instead of the `Document` model, see examples in the section
  [POJO usage in the `cloudant-java-sdk` library](#pojo-usage-in-the-new-library).
1. Sending and receiving byte responses is available for operations that accept user-defined documents or return user-defined documents, document projections or map/reduce data. See [the Raw IO section](https://github.com/IBM/cloudant-java-sdk#raw-io) of `cloudant-java-sdk` README or the [Bypass the document model and use the `asStream` methods section](#3-bypass-the-document-model-and-use-the-asstream-methods) for more details.
1. There is no built-in pagination support for views. Examples coming soon.
1. Replay interceptors are replaced by the [automatic retries](https://github.com/IBM/ibm-cloud-sdk-common/#automatic-retries) feature for failed requests.
1. Error handling is not transferable from `java-cloudant` to `cloudant-java-sdk`. For more information go to the [Error handling section](https://cloud.ibm.com/apidocs/cloudant?code=java#error-handling) in our API docs.
1. Custom HTTP client configurations in `java-cloudant` can be set differently in
   `cloudant-java-sdk`. For more information go to the
   [Configuring the HTTP client section](https://github.com/IBM/ibm-cloud-sdk-common/#configuring-the-http-client)
   in the IBM Cloud SDK Common README.

### Troubleshooting
1. Authentication errors occur during service instantiation. For example, the code `Cloudant
   service = Cloudant.newInstance("EXAMPLE");` will fail with `Authentication information was
   not properly configured.` if required environment variables prefixed with `EXAMPLE` are not set.
1. Server errors occur when running a request against the service. We suggest to
   check server errors with
   [`getServerInformation`](https://cloud.ibm.com/apidocs/cloudant?code=java#getserverinformation)
   which is the new alternative of `metaInformation()`.

### POJO usage in the new library

Since the new library supports models, this guide will demonstrate three ways to migrate your POJOs from `java-cloudant`.

Let's start with a simple POJO:

```java
public class Pojo {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
```
The code block below uses the POJO in a simple update implementation - reading the
document into an object, performing an update and then writing the updated document in
the database.

```java
//... set up the service client and the database
Pojo p = db.find(Pojo.class, "example_id");
System.out.println(p); // the value of the Pojo's toString method

p.setName("new_name");
System.out.println(p.getName()); // will be new_name

Response response = db.update(p); // the same object is used for the update, it will set the name parameter to new_name
```

#### 1. Use the `Document` model class, storing user properties in the `Map`

```java
//set up the service client
GetDocumentOptions documentOptions =
        new GetDocumentOptions.Builder()
                .db("example_db")
                .docId("example_id")
                .build();

Document doc = service.getDocument(documentOptions)
        .execute()
        .getResult();

System.out.println(doc); // will be a JSON

// Set the JSON properties to the Pojo
Pojo p = new Pojo();
p.setName((String) doc.getProperties().get("name"));
System.out.println(p); // will be a Pojo with the same name as the JSON

p.setName("new_name");
System.out.println(p.getName()); // new_name

doc.put("name", p.getName()); // add your modifications to the Document object

PutDocumentOptions putDocumentOptions =
        new PutDocumentOptions.Builder()
                .db("example_db")
                .docId("example_id")
                .document(doc)
                .build();

DocumentResult response =
        service.putDocument(putDocumentOptions)
                .execute()
                .getResult();
```

#### 2. Convert the `Document` model into a POJO (and vice versa)

```java
//set up the service client
GetDocumentOptions documentOptions =
        new GetDocumentOptions.Builder()
                .db("example_db")
                .docId("example_id")
                .build();

Document doc = service.getDocument(documentOptions)
        .execute()
        .getResult();

System.out.println(doc); // will be a JSON

// Serialize the JSON to Pojo
Pojo p = YourJsonSerializer.fromJson(doc.toString(), Pojo.class);
System.out.println(p); // the value of the Pojo's toString method

p.setName("new_name");
System.out.println(p.getName()); // will be new_name

// Deserialize the Pojo back to the Document model
doc.setProperties(YourJsonSerializer.fromJson(YourJsonSerializer.toJson(p), Map.class));

PutDocumentOptions putDocumentOptions =
                new PutDocumentOptions.Builder()
                        .db("example_db")
                        .docId("example_id")
                        .document(doc)
                        .build();

DocumentResult response =
        service.putDocument(putDocumentOptions)
                .execute()
                .getResult();
```

#### 3. Bypass the `Document` model and use the `AsStream` methods

```java
//set up the service client
GetDocumentOptions documentOptions =
        new GetDocumentOptions.Builder()
                .db("example_db")
                .docId("example_id")
                .build();

Pojo p = new Pojo()
try(InputStream is = service.getDocumentAsStream(documentOptions).execute().getResult()){
    p = YourSeriliazer.fromJson(is, Old.Pojo.class);
    System.out.println(p); // the value of the Pojo's toString method
} catch (RuntimeException re){
    // ...
}
p.setName("new_name");
System.out.println(p.getName()); // will be new_name

try (InputStream is = new ByteArrayInputStream(YourSeriliazer.toJson(p).getBytes())) {
    PutDocumentOptions putDocumentOptions =
            new PutDocumentOptions.Builder()
                    .db("example_db")
                    .docId("example_id")
                    .body(is)
                    .build();

    DocumentResult response =
            service.putDocument(putDocumentOptions)
                    .execute()
                    .getResult();
} catch (IOException e) {
    // ...
}
```

## Request mapping
Here's a list of the top 5 most frequently used `java-cloudant` operations and the `cloudant-java-sdk` equivalent API operation documentation link:

| `java-cloudant` method | `cloudant-java-sdk` API method documentation link |
|-----------------------------|---------------------------------|
|`db.find()`|[`getDocument`, `getDocumentAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#getdocument)|
|`db.getViewRequestBuilder().newRequest().build()`|[`postView`, `postViewAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postview)|
|`db.query()`|[`postFind`, `postFindAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postfind)|
|`db.contains()`|[`headDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#headdocument)|
|`db.update()`|[`putDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#putdocument)|

[A table](#reference-table) with the whole list of operations is provided at the end of this guide.

The `cloudant-java-sdk` library is generated from a more complete API spec and provides a significant number of operations that do not exist in `java-cloudant`. See [the IBM Cloud API Documentation](https://cloud.ibm.com/apidocs/cloudant) to review request parameter and body options, code examples, and additional details for every endpoint.

## Known Issues
There's an [outline of known issues](https://github.com/IBM/cloudant-java-sdk/blob/master/KNOWN_ISSUES.md) in the `cloudant-java-sdk` repository.

## Reference table
The table below contains a list of `java-cloudant` functions and the `cloudant-java-sdk` equivalent API operation documentation link.  The `cloudant-java-sdk` operation documentation link will contain the new function in a code sample e.g. `getServerInformation` link will contain a code example with `getServerInformation()`.

**Note:** There are many API operations included in the new `cloudant-java-sdk` that are not available in the `java-cloudant` library. The [API documentation](https://cloud.ibm.com/apidocs/cloudant?code=java) contains the full list of operations.

|java-cloudant operation | cloudant-java-sdk method reference |
|-------------------------|--------------------------------------|
|`metaInformation()`|[`getServerInformation`](https://cloud.ibm.com/apidocs/cloudant?code=java#getserverinformation)|
|`getActiveTasks()`|[`getActiveTasks`](https://cloud.ibm.com/apidocs/cloudant?code=java#getactivetasks)|
|`getAllDbs()`|[`getAllDbs`](https://cloud.ibm.com/apidocs/cloudant?code=java#getalldbs)|
|`getMembership()`|[`getMembershipInformation`](https://cloud.ibm.com/apidocs/cloudant?code=java#getmembershipinformation)|
|`replicator().remove()`|[`deleteReplicationDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#deletereplicationdocument)|
|`replicator().find()`|[`getReplicationDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#getreplicationdocument)|
|`replication().trigger()`/`replicator().save()`|[`putReplicationDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#putreplicationdocument)|
|`schedulerDocs()`|[`getSchedulerDocs`](https://cloud.ibm.com/apidocs/cloudant?code=java#getschedulerdocs)|
|`schedulerDoc()`|[`getSchedulerDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#getschedulerdocument)|
|`schedulerJobs()`|[`getSchedulerJobs`](https://cloud.ibm.com/apidocs/cloudant?code=java#getschedulerjobs)|
|`uuids()`|[`getUUids`](https://cloud.ibm.com/apidocs/cloudant?code=java#getuuids)|
|`deleteDB()`|[`deleteDatabase`](https://cloud.ibm.com/apidocs/cloudant?code=java#deletedatabase)|
|`db.info()`|[`getDatabaseInformation`](https://cloud.ibm.com/apidocs/cloudant?code=java#getdatabaseinformation)|
|`db.save()/db.post()`|[`postDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#postdocument)|
|`createDB()/database() with create=true/createPartitionedDb(dbName)`|[`putDatabase`](https://cloud.ibm.com/apidocs/cloudant?code=java#putdatabase)|
|`db.getAllDocsRequestBuilder().build()`|[`postAllDocs`, `postAllDocsAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postalldocs)|
|`db.bulk()`|[`postBulkDocs`](https://cloud.ibm.com/apidocs/cloudant?code=java#postbulkdocs)|
|`db.changes()`|[`postChanges`, `postChangesAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postchanges-changes)|
|`db.getDesignDocumentManager().remove()`|[`deleteDesignDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#deletedesigndocument)|
|`db.getDesignDocumentManager().get()`|[`getDesignDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#getdesigndocument)|
|`db.getDesignDocumentManager().put()`|[`putDesignDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#putdesigndocument)|
|`db.search()`|[`postSearch`, `postSearchAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postsearch)|
|`db.getViewRequestBuilder().newRequest().build()`|[`postView`, `postViewAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postview)|
|`db.getDesignDocumentManager().list() with a filter`|[`postDesignDocs`](https://cloud.ibm.com/apidocs/cloudant?code=java#postdesigndocs)|
|`db.query()`|[`postFind`, `postFindAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postfind)|
|`db.listIndexes()`|[`getIndexesInformation`](https://cloud.ibm.com/apidocs/cloudant?code=java#getindexesinformation)|
|`db.createIndex()`|[`postIndex`](https://cloud.ibm.com/apidocs/cloudant?code=java#postindex)|
|`db.deleteIndex()`|[`deleteIndex`](https://cloud.ibm.com/apidocs/cloudant?code=java#deleteindex)|
|`db.remove() with an _id with _local path`|[`deleteLocalDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#deletelocaldocument)|
|`db.find() with _local path`|[`getLocalDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#getlocaldocument)|
|`db.save() with _local path`|[`putLocalDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#putlocaldocument)|
|`db.partitionInfo()`|[`getPartitionInformation`](https://cloud.ibm.com/apidocs/cloudant?code=java#getpartitioninformation)|
|`db.getAllDocsRequestBuilder().partition().build()`|[`postPartitionAllDocs`, `postPartitionAllDocsAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postpartitionalldocs)|
|`db.search()`|[`postPartitionSearch`, `postPartitionSearchAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postpartitionsearch)|
|`db.getViewRequestBuilder().newRequest().partition().build()`|[`postPartitionView`, `postPartitionViewAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postpartitionview)|
|`db.query() using partitionKey method arg`|[`postPartitionFind`, `postPartitionFindAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#postpartitionfind-partitioned-databases)|
|`db.getPermissions()`|[`getSecurity`](https://cloud.ibm.com/apidocs/cloudant?code=java#getsecurity)|
|`db.setPermissions(userNameorApikey, permissions)`|[`putSecurity`](https://cloud.ibm.com/apidocs/cloudant?code=java#putsecurity)|
|`db.getShards()`|[`getShardsInformation`](https://cloud.ibm.com/apidocs/cloudant?code=java#getshardsinformation)|
|`db.getShard()`|[`getDocumentShardsInfo`](https://cloud.ibm.com/apidocs/cloudant?code=java#getdocumentshardsinfo)|
|`db.remove()`|[`deleteDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#deletedocument)|
|`db.find()`|[`getDocument`, `getDocumentAsStream`](https://cloud.ibm.com/apidocs/cloudant?code=java#getdocument)|
|`db.contains()`|[`headDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#headdocument)|
|`db.update()`|[`putDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#putdocument)|
|`db.removeAttachment()`|[`deleteAttachment`](https://cloud.ibm.com/apidocs/cloudant?code=java#deleteattachment)|
|`db.getAttachment()`|[`getAttachment`](https://cloud.ibm.com/apidocs/cloudant?code=java#getattachment)|
|`db.saveAttachment()`|[`putAttachment`](https://cloud.ibm.com/apidocs/cloudant?code=java#putattachment)|
|`generateApiKey()`|[`postApiKeys`](https://cloud.ibm.com/apidocs/cloudant?code=java#postapikeys)|
|`db.setPermissions()`|[`putCloudantSecurityConfiguration`](https://cloud.ibm.com/apidocs/cloudant?code=java#putcloudantsecurity)|
