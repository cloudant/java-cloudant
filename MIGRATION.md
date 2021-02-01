# Migrating to the `cloudant-java-sdk` library
This document is to assist in migrating from the `java-cloudant` (package: `com.cloudant.cloudant-client`) to the newly supported [`cloudant-java-sdk`](https://github.com/IBM/cloudant-java-sdk) (package: `com.ibm.cloud.cloudant`).

## Initializing the client connection
There are several ways to create a client connection in `cloudant-java-sdk`:
1. [Environment variables](https://github.com/IBM/cloudant-java-sdk#authentication-with-environment-variables)
2. [External configuration file](https://github.com/IBM/cloudant-java-sdk#authentication-with-external-configuration)
3. [Programmatically](https://github.com/IBM/cloudant-java-sdk#programmatic-authentication)

[See the README](https://github.com/IBM/cloudant-java-sdk#code-examples) for code examples on using environment variables.

## Other differences
1. Fetching the Database object first before performing additional operations is not required. For example, in the case of updating a document you would first call `getDocument` to fetch and then `putDocument` to update.
2. Model classes are used instead of POJOs.
3. Sending and receiving byte responses is available for some of our operations.  See [the Raw IO section](https://github.com/IBM/cloudant-java-sdk#raw-io) of `cloudant-java-sdk` README for more details.

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
