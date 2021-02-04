# Migrating to the `cloudant-java-sdk` library
This document is to assist in migrating from the `java-cloudant` (coordinates: `com.cloudant:cloudant-client`) to the newly supported [`cloudant-java-sdk`](https://github.com/IBM/cloudant-java-sdk) (coordinates: `com.ibm.cloud:cloudant`).

## Initializing the client connection
There are several ways to create a client connection in `cloudant-java-sdk`:
1. [Environment variables](https://github.com/IBM/cloudant-java-sdk#authentication-with-environment-variables)
2. [External configuration file](https://github.com/IBM/cloudant-java-sdk#authentication-with-external-configuration)
3. [Programmatically](https://github.com/IBM/cloudant-java-sdk#programmatic-authentication)

[See the README](https://github.com/IBM/cloudant-java-sdk#code-examples) for code examples on using environment variables.

## Other differences
1. Fetching the Database object first before performing additional operations is not required. For example, in the case of updating a document you would first call `getDocument` to fetch and then `putDocument` to update.
1. Model classes are used instead of POJOs. See examples in the [POJO usage in the `cloudant-java-sdk` library](#pojo-usage-in-the-new-library).
1. Sending and receiving byte responses is available for operations that accept user-defined documents or return user-defined documents, document projections or map/reduce data. See [the Raw IO section](https://github.com/IBM/cloudant-java-sdk#raw-io) of `cloudant-java-sdk` README for more details.
1. There is no pagination support for views. Examples coming soon.

### POJO usage in the new library

The new library is using models instead of POJOs, but it is still possible to migrate your
code with the new library.

Suppose that you have the following POJO and a simple read and write implementation with the `com.cloudant:cloudant-client` library you have three possibilities for the migration.

```java
public class POJO {
    private String _id;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return _id + ": " + name;
    }
}
```

```java
//... set up the service client and the database
POJO p = db.find(POJO.class, "example_id");
System.out.println(p); // the value of the POJO's toString method

p.setName("newName");
System.out.println(p.getName()); // will be newName

Response response = db.update(p); // the same object is used for the update, it will set the name parameter to newName
```

#### 1. Use the `Document` model class, storing user properties in the `Map`

```java
//... set up the service client
GetDocumentOptions documentOptions =
                new GetDocumentOptions.Builder()
                        .db("example_db")
                        .docId("example_id")
                        .build();

Document doc = service.getDocument(documentOptions)
                .execute()
                .getResult();

System.out.println(doc); // will be a JSON

// Serialize the JSON to a Map
Map m = YourJsonSerializer.fromJson(doc.toString(), Map.class);
System.out.println(m); // will be a Map with the same key value pairs as the JSON

m.put("name", "newName");
System.out.println(m.get("name")); // newName

doc.setProperties(m); // add your modifications to the Document object

PutDocumentOptions putDocumentOptions =
        new PutDocumentOptions.Builder()
                .db("products")
                .docId("small-appliances:1000042")
                .document(doc)
                .build();

DocumentResult response = service.putDocument(putDocumentOptions).execute()
                                .getResult();
```

#### 2. Convert the `Document` model into a POJO (and vice versa)

```java
//... set up the service client
GetDocumentOptions documentOptions =
                new GetDocumentOptions.Builder()
                        .db("example_db")
                        .docId("example_id")
                        .build();

Document doc = service.getDocument(documentOptions)
        .execute()
        .getResult();

System.out.println(doc); // will be a JSON

// Serialize the JSON to POJO
POJO p = YourJsonSerializer.fromJson(doc.toString(), POJO.class);
System.out.println(p); // the value of the POJO's toString method

p.setName("newName");
System.out.println(p.getName()); // will be newName

// Deserialize the POJO back to the Document model
doc.setProperties(YourJsonSerializer.fromJson(YourJsonSerializer.toJson(p), Map.class));

PutDocumentOptions putDocumentOptions =
                new PutDocumentOptions.Builder()
                        .db("example_db")
                        .docId("example_id")
                        .document(doc)
                        .build();

DocumentResult response = service.putDocument(putDocumentOptions).execute()
                        .getResult();
```

#### 3. Bypass the `Document` model and use the `AsStream` methods

```java
//... set up the service client
GetDocumentOptions documentOptions =
                new GetDocumentOptions.Builder()
                        .db("example_db")
                        .docId("example_id")
                        .build();

POJO p = new POJO()
try(InputStream is = service.getDocumentAsStream(documentOptions).execute().getResult()){
    InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
    p = YourSeriliazer.fromJson(isr, Old.POJO.class);
    System.out.println(p); // the value of the POJO's toString method
} catch (RuntimeException re){
    // ...
}
p.setName("newName");
System.out.println(p.getName()); // will be newName

try (InputStream is = new ByteArrayInputStream(YourSeriliazer.toJson(p).getBytes())) {
    PutDocumentOptions putDocumentOptions =
            new PutDocumentOptions.Builder()
                    .db("example_db")
                    .docId("example_id")
                    .body(is)
                    .build();
    DocumentResult response = service.putDocument(putDocumentOptions).execute()
            .getResult();
} catch (IOException e) {
    // ...
}
```

## Request mapping
Here's a list of the top 5 most frequently used `java-cloudant` operations and the `cloudant-java-sdk` equivalent API operation documentation link:

| `java-cloudant` method | `cloudant-java-sdk` API method documentation link |
|-----------------------------|---------------------------------|
|`db.find()`|[getDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#getdocument), getDocumentAsStream|
|`db.getViewRequestBuilder().newRequest().build()`|[postView](https://cloud.ibm.com/apidocs/cloudant?code=java#postview), postViewAsStream|
|`db.query()`|[postFind](https://cloud.ibm.com/apidocs/cloudant?code=java#postfind), postFindAsStream|
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
|`metaInformation()`|[getServerInformation](https://cloud.ibm.com/apidocs/cloudant?code=java#getserverinformation)|
|`getActiveTasks()`|[getActiveTasks](https://cloud.ibm.com/apidocs/cloudant?code=java#getactivetasks)|
|`getAllDbs()`|[getAllDbs](https://cloud.ibm.com/apidocs/cloudant?code=java#getalldbs)|
|`getMembership()`|[getMembershipInformation](https://cloud.ibm.com/apidocs/cloudant?code=java#getmembershipinformation)|
|`replication().trigger()`|[postReplicate](https://cloud.ibm.com/apidocs/cloudant?code=java#postreplicate)|
|`db.remove() with the replication document _id`|[deleteReplicationDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#deletereplicationdocument)|
|`db.find() with the replication document _id`|[getReplicationDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#getreplicationdocument)|
|`schedulerDocs()`|[getSchedulerDocs](https://cloud.ibm.com/apidocs/cloudant?code=java#getschedulerdocs)|
|`schedulerDoc()`|[getSchedulerDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#getschedulerdocument)|
|`schedulerJobs()`|[getSchedulerJobs](https://cloud.ibm.com/apidocs/cloudant?code=java#getschedulerjobs)|
|`uuids()`|[getUUids](https://cloud.ibm.com/apidocs/cloudant?code=java#getuuids)|
|`deleteDB()`|[deleteDatabase](https://cloud.ibm.com/apidocs/cloudant?code=java#deletedatabase)|
|`db.info()`|[getDatabaseInformation](https://cloud.ibm.com/apidocs/cloudant?code=java#getdatabaseinformation)|
|`db.save()/db.post()`|[postDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#postdocument)|
|`createDB()/database() with create=true/createPartitionedDb(dbName)`|[putDatabase](https://cloud.ibm.com/apidocs/cloudant?code=java#putdatabase)|
|`db.getAllDocsRequestBuilder().build()`|[postAllDocs, postAllDocsAsStream](https://cloud.ibm.com/apidocs/cloudant?code=java#postalldocs)|
|`db.bulk()`|[postBulkDocs](https://cloud.ibm.com/apidocs/cloudant?code=java#postbulkdocs)|
|`db.changes()`|[postChanges, postChangesAsStream](https://cloud.ibm.com/apidocs/cloudant?code=java#postchanges)|
|`db.getDesignDocumentManager().remove()`|[deleteDesignDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#deletedesigndocument)|
|`db.getDesignDocumentManager().get()`|[getDesignDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#getdesigndocument)|
|`db.getDesignDocumentManager().put()`|[putDesignDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#putdesigndocument)|
|`db.search()`|[postSearch](https://cloud.ibm.com/apidocs/cloudant?code=java#postsearch), postSearchAsStream|
|`db.getViewRequestBuilder().newRequest().build()`|[postView](https://cloud.ibm.com/apidocs/cloudant?code=java#postview), postViewAsStream|
|`db.getDesignDocumentManager().list() with a filter`|[postDesignDocs](https://cloud.ibm.com/apidocs/cloudant?code=java#postdesigndocs)|
|`db.query()`|[postFind](https://cloud.ibm.com/apidocs/cloudant?code=java#postfind), postFindAsStream|
|`db.listIndexes()`|[getIndexesInformation](https://cloud.ibm.com/apidocs/cloudant?code=java#getindexesinformation)|
|`db.createIndex()`|[postIndex](https://cloud.ibm.com/apidocs/cloudant?code=java#postindex)|
|`db.deleteIndex()`|[deleteIndex](https://cloud.ibm.com/apidocs/cloudant?code=java#deleteindex)|
|`db.remove() with an _id with _local path`|[deleteLocalDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#deletelocaldocument)|
|`db.find() with _local path`|[getLocalDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#getlocaldocument)|
|`db.save() with _local path`|[putLocalDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#putlocaldocument)|
|`db.partitionInfo()`|[getPartitionInformation](https://cloud.ibm.com/apidocs/cloudant?code=java#getpartitioninformation)|
|`db.getAllDocsRequestBuilder().partition().build()`|[postPartitionAllDocs](https://cloud.ibm.com/apidocs/cloudant?code=java#postpartitionalldocs), postPartitionAllDocsAsStream|
|`db.search()`|[postPartitionSearch](https://cloud.ibm.com/apidocs/cloudant?code=java#postpartitionsearch), postPartitionSearchAsStream|
|`db.getViewRequestBuilder().newRequest().partition().build()`|[postPartitionView](https://cloud.ibm.com/apidocs/cloudant?code=java#postpartitionview), postPartitionViewAsStream|
|`db.query() using partitionKey method arg`|[postPartitionFind](https://cloud.ibm.com/apidocs/cloudant?code=java#postpartitionfind), postPartitionFindAsStream|
|`db.getPermissions()`|[getSecurity](https://cloud.ibm.com/apidocs/cloudant?code=java#getsecurity)|
|`db.setPermissions(userNameorApikey, permissions)`|[putSecurity](https://cloud.ibm.com/apidocs/cloudant?code=java#putsecurity)|
|`db.getShards()`|[getShardsInformation](https://cloud.ibm.com/apidocs/cloudant?code=java#getshardsinformation)|
|`db.getShard()`|[getDocumentShardsInfo](https://cloud.ibm.com/apidocs/cloudant?code=java#getdocumentshardsinfo)|
|db.remove()|[deleteDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#deletedocument)|
|`db.find()`|[getDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#getdocument), getDocumentAsStream|
|`db.contains()`|[headDocument](https://cloud.ibm.com/apidocs/cloudant?code=java#headdocument)|
|`db.update()`|[`putDocument`](https://cloud.ibm.com/apidocs/cloudant?code=java#putdocument)|
|`db.removeAttachment()`|[deleteAttachment](https://cloud.ibm.com/apidocs/cloudant?code=java#deleteattachment)|
|`db.getAttachment()`|[getAttachment](https://cloud.ibm.com/apidocs/cloudant?code=java#getattachment)|
|`db.saveAttachment()`|[putAttachment](https://cloud.ibm.com/apidocs/cloudant?code=java#putattachment)|
|`generateApiKey()`|[postApiKeys](https://cloud.ibm.com/apidocs/cloudant?code=java#postapikeys)|
|`db.setPermissions()`|[putCloudantSecurityConfiguration](https://cloud.ibm.com/apidocs/cloudant?code=java#putcloudantsecurity)|
