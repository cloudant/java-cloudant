/*
 * Copyright (c) 2016 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.client.api;

import com.cloudant.client.api.model.DbInfo;
import com.cloudant.client.api.model.FindByIndexOptions;
import com.cloudant.client.api.model.Index;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Permissions;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.model.Shard;
import com.cloudant.client.api.views.AllDocsRequestBuilder;
import com.cloudant.client.api.views.ViewRequestBuilder;
import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;

import java.io.InputStream;
import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public interface Database {
    /**
     * Set permissions for a user/apiKey on this database.
     * <p>
     * Note this method is only applicable to databases that support the
     * <a target="_blank" href="http://docs.cloudant.com/authorization.html">
     * Cloudant authorization API
     * </a> such as Cloudant DBaaS. For unsupported databases consider using the /db/_security
     * endpoint.
     * </p>
     * <p>Example usage to set read-only access for a new key on the "example" database:</p>
     * <pre>
     * {@code
     * // generate an API key
     * ApiKey key = client.generateApiKey();
     *
     * // get the "example" database
     * Database db = client.database("example", false);
     *
     * // set read-only permissions
     * db.setPermissions(key.getKey(), EnumSet.<Permissions>of(Permissions._reader));
     * }
     * </pre>
     *
     * @param userNameorApikey the user or key to apply permissions to
     * @param permissions      permissions to grant
     * @throws UnsupportedOperationException if called on a database that does not provide the
     *                                       Cloudant authorization API
     * @see CloudantClient#generateApiKey()
     * @see <a target="_blank" href="http://docs.cloudant.com/authorization.html#roles">Roles</a>
     * @see <a target="_blank"
     * href="http://docs.cloudant.com/authorization.html#modifying-permissions">Modifying
     * permissions</a>
     */
    void setPermissions(String userNameorApikey, EnumSet<Permissions> permissions);

    /**
     * Returns the Permissions of this database.
     * <p>
     * Note this method is only applicable to databases that support the
     * <a target="_blank" href="http://docs.cloudant.com/authorization.html">
     * Cloudant authorization API
     * </a> such as Cloudant DBaaS. For unsupported databases consider using the /db/_security
     * endpoint.
     * </p>
     *
     * @return the map of userNames to their Permissions
     * @throws UnsupportedOperationException if called on a database that does not provide the
     *                                       Cloudant authorization API
     * @see <a target="_blank" href="http://docs.cloudant.com/authorization.html#roles">Roles</a>
     * @see <a target="_blank"
     * href="http://docs.cloudant.com/authorization.html#viewing-permissions">Viewing
     * permissions</a>
     */
    Map<String, EnumSet<Permissions>> getPermissions();

    /**
     * Get info about the shards in the database.
     *
     * @return List of shards
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/advanced.html#get-/$db/_shards">_shards</a>
     */
    List<Shard> getShards();

    /**
     * Get info about the shard a document belongs to.
     *
     * @param docId document ID
     * @return Shard info
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/advanced.html#get-/$db/_shards">_shards</a>
     */
    Shard getShard(String docId);

    /**
     * Create a new index
     * <P>
     * Example usage creating an index that sorts ascending on name, then by year.
     * </P>
     * <pre>
     * {@code
     * db.createIndex("Person_name", "Person_name", null, new IndexField[]{
     *       new IndexField("Person_name",SortOrder.asc),
     *       new IndexField("Movie_year",SortOrder.asc)});
     * }
     * </pre>
     * <P>
     * Example usage creating an index that sorts ascending by year.
     * </P>
     * <pre>
     * {@code
     * db.createIndex("Movie_year", "Movie_year", null, new IndexField[]{
     *      new IndexField("Movie_year",SortOrder.asc)});
     * }
     * </pre>
     *
     * @param indexName     optional name of the index (if not provided one will be generated)
     * @param designDocName optional name of the design doc in which the index will be created
     * @param indexType     optional, type of index (only "json" as of now)
     * @param fields        array of fields in the index
     */
    void createIndex(String indexName, String designDocName, String indexType, IndexField[] fields);

    /**
     * Create a new index from a JSON string
     *
     * @param indexDefinition String representation of the index definition JSON
     * @see #createIndex(String, String, String, IndexField[])
     * @see <a target="_blank"
     * href="http://docs.cloudant.com/api/cloudant-query.html#creating-a-new-index">
     * index definition</a>
     */
    void createIndex(String indexDefinition);

    /**
     * Find documents using an index
     *
     * @param selectorJson String representation of a JSON object describing criteria used to
     *                     select documents. For example:
     *                     {@code "{ \"selector\": {<your data here>} }"}.
     * @param classOfT     The class of Java objects to be returned
     * @param <T>          the type of the Java object to be returned
     * @return List of classOfT objects
     * @see #findByIndex(String, Class, FindByIndexOptions)
     * @see <a target="_blank"
     * href="http://docs.cloudant.com/api/cloudant-query.html#cloudant-query-selectors">
     * selector syntax</a>
     */
    <T> List<T> findByIndex(String selectorJson, Class<T> classOfT);

    /**
     * Find documents using an index
     * <P>
     * Example usage to return the name and year of movies starring
     * Alec Guinness since 1960 with the results sorted by year descending:
     * </P>
     * <pre>
     * {@code
     * List <Movie> movies = db.findByIndex("\"selector\": {
     * \"Movie_year\": {\"$gt\": 1960}, \"Person_name\": \"Alec Guinness\"
     * }",
     * Movie.class,
     * new FindByIndexOptions()
     * .sort(new IndexField("Movie_year", SortOrder.desc))
     * .fields("Movie_name").fields("Movie_year"));
     * }
     * </pre>
     *
     * @param selectorJson String representation of a JSON object describing criteria used to
     *                     select documents. For example:
     *                     {@code "{ \"selector\": {<your data here>} }"}.
     * @param options      {@link FindByIndexOptions query Index options}
     * @param classOfT     The class of Java objects to be returned
     * @param <T>          the type of the Java object to be returned
     * @return List of classOfT objects
     * @see <a target="_blank"
     * href="http://docs.cloudant.com/api/cloudant-query.html#cloudant-query-selectors">
     * selector syntax</a>
     */
    <T> List<T> findByIndex(String selectorJson, Class<T> classOfT, FindByIndexOptions options);

    /**
     * List all indices
     * <P>Example usage:</P>
     * <pre>
     * {@code
     * List <Index> indices = db.listIndices();
     * }
     * </pre>
     *
     * @return List of Index objects
     */
    List<Index> listIndices();

    /**
     * Delete an index
     *
     * @param indexName   name of the index
     * @param designDocId ID of the design doc
     */
    void deleteIndex(String indexName, String designDocId);

    /**
     * Provides access to Cloudant <tt>Search</tt> APIs.
     *
     * @param searchIndexId the name of the index to search
     * @return Search object for searching the index
     * @see <a target="_blank" href="https://docs.cloudant.com/search.html">Search</a>
     */
    Search search(String searchIndexId);

    /**
     * Get a manager that has convenience methods for managing design documents.
     *
     * @return a {@link DesignDocumentManager} for this database
     * @see DesignDocumentManager
     */
    DesignDocumentManager getDesignDocumentManager();

    /**
     * @param designDoc containing the view
     * @param viewName  the view name
     * @return a builder to build view requests for the specified design document and view of
     * this database
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/creating_views.html#using-views">Using views</a>
     */
    ViewRequestBuilder getViewRequestBuilder(String designDoc, String viewName);

    /**
     * Build a request for the _all_docs endpoint.
     * <P>
     * Example usage:
     * </P>
     * <pre>
     * {@code
     *  getAllDocsRequestBuilder().build().getResponse();
     * }
     * </pre>
     *
     * @return a request builder for the _all_docs endpoint of this database
     */
    AllDocsRequestBuilder getAllDocsRequestBuilder();

    /**
     * Provides access for interacting with the changes feed.
     * <P>
     * See the {@link Changes} API for examples.
     * </P>
     *
     * @return a Changes object for using the changes feed
     * @see Changes
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/database.html#get-changes">Databases - get
     * changes</a>
     */
    Changes changes();

    /**
     * Retrieve the document with the specified ID from the database and deserialize to an
     * instance of the POJO of type T.
     *
     * @param <T>       object type
     * @param classType the class of type T
     * @param id        the document id
     * @return an object of type T
     * @throws NoDocumentException if the document is not found in the database
     * @see #find(Class, String, String)
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#read">Documents -
     * read</a>
     */
    <T> T find(Class<T> classType, String id);

    /**
     * Retrieve the document with the specified ID from the database and deserialize to an
     * instance of the POJO of type T. Uses the additional parameters specified when making the
     * {@code GET} request.
     * <P>Example usage to get inline attachments:</P>
     * <pre>
     * {@code
     * Foo foo = db.find(Foo.class, "exampleId", new Params().attachments());
     * String attachmentData = foo.getAttachments().get("attachment.txt").getData();
     * }
     * </pre>
     *
     * @param <T>       object type
     * @param classType the class of type T
     * @param id        the document id
     * @param params    extra parameters to append
     * @return An object of type T
     * @throws NoDocumentException if the document is not found in the database.
     * @see Params
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#read">Documents -
     * read</a>
     */
    <T> T find(Class<T> classType, String id, Params params);

    /**
     * Retrieve the document with the specified ID at the specified revision from the database
     * and deserialize to an instance of the POJO of type T.
     * <P>Example usage:</P>
     * <pre>
     * {@code
     *     Foo foo = db.find(Foo.class, "exampleId", "1-12345exampleRev");
     * }
     * </pre>
     *
     * @param <T>       object type
     * @param classType the class of type T
     * @param id        the document _id field
     * @param rev       the document _rev field
     * @return an object of type T
     * @throws NoDocumentException if the document is not found in the database.
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#read">Documents -
     * read</a>
     */
    <T> T find(Class<T> classType, String id, String rev);

    /**
     * This method finds any document given a URI.
     * <p>The URI must be URI-encoded.</p>
     * <P>
     * Example usage retrieving the Foo POJO with document ID "exampleId" from the database
     * "exampleDb" in the "example" Cloudant account.
     * </P>
     * <pre>
     * {@code
     * Foo foo = db.findAny(Foo.class, "https://example.cloudant.com/exampleDb/exampleId");
     * }
     * </pre>
     *
     * @param classType the class of type T
     * @param uri       the URI as string
     * @param <T>       the type of Java object to return
     * @return an object of type T
     */
    <T> T findAny(Class<T> classType, String uri);

    /**
     * Finds the document with the specified document ID and returns it as an {@link InputStream}.
     * <p><b>Note</b>: The stream must be closed after use to release the connection.</p>
     *
     * @param id the document _id field
     * @return the result as {@link InputStream}
     * @throws NoDocumentException If the document is not found in the database.
     * @see #find(String, String)
     */
    InputStream find(String id);

    /**
     * Finds the document with the specified document ID and revision and returns it as {@link
     * InputStream}.
     * <p><b>Note</b>: The stream must be closed after use to release the connection.</p>
     * <P>Example usage:</P>
     * <pre>
     * {@code
     * InputStream inputStream = null;
     * try{
     *     inputStream = db.find("exampleId", "1-12345exampleRev");
     *     //do stuff with the stream
     * } finally {
     *     //close the input stream
     *     inputStream.close();
     * }
     * }
     * </pre>
     *
     * @param id  the document _id field
     * @param rev the document _rev field
     * @return the result as {@link InputStream}
     * @throws NoDocumentException if the document is not found in the database at the specified
     *                             revision
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#read">Documents -
     * read</a>
     */
    InputStream find(String id, String rev);

    /**
     * Checks if a document exists in the database.
     *
     * @param id the document _id field
     * @return {@code true} if the document is found, {@code false} otherwise
     */
    boolean contains(String id);

    /**
     * Saves a document in the database.
     * <p>If the serialized object's JSON does not contain an {@code _id} field, then a UUID will
     * be generated for the document ID.
     * </p>
     * <P>
     * Example of inserting a JsonObject into the database:
     * </P>
     * <pre>
     * {@code
     * JsonObject json = new JsonObject();
     * json.addProperty("_id", "test-doc-id-2");
     * json.add("json-array", new JsonArray());
     * Response response = db.save(json);
     * }
     * </pre>
     * <P>
     * Example of inserting a POJO into the database:
     * </P>
     * <pre>
     * {@code
     * Foo foo = new Foo();
     * Response response = db.save(foo);
     * }
     * </pre>
     * <P>
     * Example of inserting a Map into the database:
     * </P>
     * <pre>
     * {@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("_id", "test-doc-id-1");
     * map.put("title", "test-doc");
     * Response response = db.save(map);
     * }
     * </pre>
     *
     * @param object the object to save
     * @return {@link Response}
     * @throws DocumentConflictException If a conflict is detected during the save.
     */
    Response save(Object object);

    /**
     * Saves a document in the database similarly to {@link Database#save(Object)} but using a
     * specific write quorum.
     *
     * @param object      the object to save
     * @param writeQuorum the write quorum
     * @return {@link Response}
     * @throws DocumentConflictException If a conflict is detected during the save.
     * @see Database#save(Object)
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/document.html#quorum---writing-and-reading-data">
     * Documents - quorum</a>
     */
    Response save(Object object, int writeQuorum);

    /**
     * Creates a document in the database using a HTTP {@code POST} request.
     * <p>If the serialized object's JSON does not contain an {@code _id} field, then the server
     * will generate a document ID.</p>
     * <P>
     * Example of creating a document in the database for a POJO:
     * </P>
     * <pre>
     * {@code
     * Foo foo = new Foo();
     * Response response = db.post(foo);
     * }
     * </pre>
     *
     * @param object The object to save
     * @return {@link Response}
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/document.html#documentCreate">Documents - create</a>
     */
    Response post(Object object);

    /**
     * Creates a document in the database similarly to {@link Database#post(Object)} but using a
     * specific write quorum.
     *
     * @param object      The object to save
     * @param writeQuorum the write Quorum
     * @return {@link Response}
     * @see Database#post(Object)
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/document.html#quorum---writing-and-reading-data">
     * Documents - quorum</a>
     */
    Response post(Object object, int writeQuorum);

    /**
     * Updates an object in the database, the object must have the correct {@code _id} and
     * {@code _rev} values.
     * <P>Example usage:</P>
     * <pre>
     * {@code
     * //get a Bar object from the database
     * Bar bar = db.find(Bar.class, "exampleId");
     * //change something about bar
     * bar.setSomeProperty(true);
     * //now update the remote Bar
     * Response responseUpdate = db.update(bar);
     * }
     * </pre>
     *
     * @param object the object to update
     * @return {@link Response}
     * @throws DocumentConflictException if a conflict is detected during the update.
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#update">Documents -
     * update</a>
     */
    Response update(Object object);

    /**
     * Updates an object in the database similarly to {@link #update(Object)}, but specifying the
     * write quorum.
     *
     * @param object      the object to update
     * @param writeQuorum the write quorum
     * @return {@link Response}
     * @throws DocumentConflictException if a conflict is detected during the update.
     * @see Database#update(Object)
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/document.html#quorum---writing-and-reading-data">
     * Documents - quorum</a>
     */
    Response update(Object object, int writeQuorum);

    /**
     * Removes a document from the database.
     * <p>The object must have the correct {@code _id} and {@code _rev} values.</p>
     * <P>Example usage:</P>
     * <pre>
     * {@code
     * //get a Bar object from the database
     * Bar bar = db.find(Bar.class, "exampleId");
     * //now remove the remote Bar
     * Response response = db.remove(bar);
     * }
     * </pre>
     *
     * @param object the document to remove as an object
     * @return {@link Response}
     * @throws NoDocumentException If the document is not found in the database.
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#delete">Documents -
     * delete</a>
     */
    Response remove(Object object);

    /**
     * Removes the document from the database with the specified {@code _id} and {@code _rev}
     * values.
     * <P>Example usage:</P>
     * <pre>
     * {@code
     * Response response = db.remove("exampleId", "1-12345exampleRev");
     * }
     * </pre>
     *
     * @param id  the document _id field
     * @param rev the document _rev field
     * @return {@link Response}
     * @throws NoDocumentException If the document is not found in the database.
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#delete">Documents -
     * delete</a>
     */
    Response remove(String id, String rev);

    /**
     * Uses the {@code _bulk_docs} endpoint to insert multiple documents into the database in a
     * single HTTP request.
     * <P>Example usage:</P>
     * <pre>
     * {@code
     * //create a list
     * List<Object> newDocs = new ArrayList<Object>();
     * //add some objects to the list
     * newDocs.add(new Foo());
     * newDocs.add(new JsonObject());
     * //use the bulk insert
     * List<Response> responses = db.bulk(newDocs);
     * }
     * </pre>
     *
     * @param objects the {@link List} of objects
     * @return {@code List<Response>} one per object
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#bulk-operations">
     * Documents - bulk operations</a>
     */
    List<Response> bulk(List<?> objects);

    /**
     * Creates an attachment from the specified InputStream and a new document with a generated
     * document ID.
     * <P>Example usage:</P>
     * <pre>
     * {@code
     * byte[] bytesToDB = "binary data".getBytes();
     * ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
     * Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain");
     * }
     * </pre>
     * <p>To retrieve an attachment, see {@link #find(Class, String, Params)}</p>
     *
     * @param in          The {@link InputStream} providing the binary data.
     * @param name        The attachment name.
     * @param contentType The attachment "Content-Type".
     * @return {@link Response}
     * @see <a target="_blank" href="https://docs.cloudant.com/attachments.html">Attachments</a>
     */
    Response saveAttachment(InputStream in, String name, String contentType);

    /**
     * Creates or updates an attachment on the given document ID and revision.
     * <P>
     * If {@code docId} and {@code docRev} are {@code null} a new document will be created.
     * </P>
     * <P>
     * Example usage:
     * </P>
     * <pre>
     * {@code
     * byte[] bytesToDB = "binary data".getBytes();
     * ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
     * Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain","exampleId","3-rev");
     * }
     * </pre>
     * Saves an attachment to an existing document given both a document id
     * and revision, or save to a new document given only the id, and rev as {@code null}.
     * <p>To retrieve an attachment, see {@link #find(Class, String, Params)}</p>
     *
     * @param in          The {@link InputStream} providing the binary data.
     * @param name        The attachment name.
     * @param contentType The attachment "Content-Type".
     * @param docId       The document id to save the attachment under, or {@code null} to save
     *                    under a new document.
     * @param docRev      The document revision to save the attachment under, or {@code null}
     *                    when saving to a new document.
     * @return {@link Response}
     * @throws DocumentConflictException if the attachment cannot be saved because of a conflict
     * @see <a target="_blank" href="https://docs.cloudant.com/attachments.html">Attachments</a>
     */
    Response saveAttachment(InputStream in, String name, String contentType, String docId, String
            docRev);

    /**
     * Invokes an Update Handler.
     * <P>Example usage:</P>
     * <pre>
     * {@code
     * final String newValue = "foo bar";
     * Params params = new Params()
     *         .addParam("field", "title")
     *         .addParam("value", newValue);
     * String output = db.invokeUpdateHandler("example/example_update", "exampleId", params);
     *
     * }
     * </pre>
     * <pre>
     * Params params = new Params()
     * 	.addParam("field", "foo")
     * 	.addParam("value", "bar");
     * String output = dbClient.invokeUpdateHandler("designDoc/update1", "docId", params);
     * </pre>
     *
     * @param updateHandlerUri The Update Handler URI, in the format: <code>designDoc/update1</code>
     * @param docId            The document id to update.
     *                         If no id is provided, then a document will be created.
     * @param params           The query parameters as {@link Params}.
     * @return The output of the request.
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/design_documents.html#update-handlers">
     * Design documents - update handlers</a>
     */
    String invokeUpdateHandler(String updateHandlerUri, String docId, Params params);

    /**
     * @return The database URI.
     */
    URI getDBUri();

    /**
     * Get information about this database.
     *
     * @return DbInfo encapsulating the database info
     * @see <a target="_blank" href="https://docs.cloudant.com/database.html#read">Databases -
     * read</a>
     */
    DbInfo info();

    /**
     * Requests the database commits any recent changes to disk.
     *
     * @see <a
     * href="http://docs.couchdb.org/en/1.6.1/api/database/compact.html#db-ensure-full-commit">
     * CouchDB _ensure_full_commit
     * </a>
     */
    void ensureFullCommit();

}
