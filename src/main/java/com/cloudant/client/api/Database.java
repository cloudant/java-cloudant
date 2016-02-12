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

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.close;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.createPost;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getAsString;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponse;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponseList;

import com.cloudant.client.api.model.DbInfo;
import com.cloudant.client.api.model.FindByIndexOptions;
import com.cloudant.client.api.model.Index;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Permissions;
import com.cloudant.client.api.model.Shard;
import com.cloudant.client.api.views.AllDocsRequestBuilder;
import com.cloudant.client.api.views.ViewRequestBuilder;
import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.client.internal.URIBase;
import com.cloudant.client.internal.views.AllDocsRequestBuilderImpl;
import com.cloudant.client.internal.views.AllDocsRequestResponse;
import com.cloudant.client.internal.views.ViewQueryParameters;
import com.cloudant.client.org.lightcouch.Changes;
import com.cloudant.client.org.lightcouch.CouchDatabase;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.client.org.lightcouch.Response;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Contains a Database Public API implementation.
 *
 * Methods may throw a {@link NoDocumentException} if the database does not exist.
 *
 * @author Mario Briggs
 * @since 0.0.1
 */
public class Database {

    static final Logger log = Logger.getLogger(Database.class.getCanonicalName());
    private CouchDatabase db;
    private CloudantClient client;
    private final URI apiV2DBSecurityURI;

    /**
     * Internal constructor for creating a new Database instance.
     *
     * @param client the CloudantClient instance to connect with
     * @param db     the CouchDatabase for executing operations
     */
    Database(CloudantClient client, CouchDatabase db) {
        super();
        this.client = client;
        this.db = db;
        apiV2DBSecurityURI = new URIBase(client.getBaseUri()).path("_api").path("v2").path("db")
                .path(db.getDbName()).path("_security").build();
    }

    /**
     * Constructor for subclasses that want to override Database methods. The supplied Database
     * instance is used to instantiate the super class. As such for non-overridden methods the
     * implementation of the extending class will be identical to the original Database instance.
     *
     * Usage example:
     * <pre>
     *     {@code
     *      public class ExtendedDatabase extends Database {
     *          public ExtendedDatabase(Database database) {
     *              super(database);
     *          }
     *      }
     *     }
     * </pre>
     *
     * @param db existing Database instance
     * @since 2.3.0
     */
    protected Database(Database db) {
        this(db.client, db.db);
    }

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
    public void setPermissions(String userNameorApikey, EnumSet<Permissions> permissions) {
        assertNotEmpty(userNameorApikey, "userNameorApikey");
        assertNotEmpty(permissions, "permissions");
        final JsonArray jsonPermissions = new JsonArray();
        for (Permissions s : permissions) {
            final JsonPrimitive permission = new JsonPrimitive(s.toString());
            jsonPermissions.add(permission);
        }

        // get existing permissions
        JsonObject perms = getPermissionsObject();

        // now set back
        JsonElement elem = perms.get("cloudant");
        if (elem == null) {
            perms.addProperty("_id", "_security");
            elem = new JsonObject();
            perms.add("cloudant", elem);
        }
        elem.getAsJsonObject().add(userNameorApikey, jsonPermissions);

        InputStream response = null;
        HttpConnection put = Http.PUT(apiV2DBSecurityURI, "application/json");
        put.setRequestBody(client.getGson().toJson(perms));
        try {
            response = client.couchDbClient.executeToInputStream(put);
            String ok = getAsString(response, "ok");
            if (!ok.equalsIgnoreCase("true")) {
                //raise exception
            }
        } finally {
            close(response);
        }
    }

    /**
     * @return /api/v2/db/$dbname/_security JSON data
     * @throws UnsupportedOperationException if called on a database that does not provide the
     *                                       Cloudant authorization API
     */
    private JsonObject getPermissionsObject() {
        try {
            return client.couchDbClient.get(apiV2DBSecurityURI, JsonObject.class);
        } catch (CouchDbException exception) {
            //currently we can't inspect the HttpResponse code
            //being in this catch block means it was not a 20x code
            //look for the "bad request" that implies the endpoint is not supported
            if (exception.getMessage().toLowerCase().contains("bad request")) {
                throw new UnsupportedOperationException("The methods getPermissions and " +
                        "setPermissions are not supported for this database, consider using the " +
                        "/db/_security endpoint.");
            } else {
                throw exception;
            }
        }
    }

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
    public Map<String, EnumSet<Permissions>> getPermissions() {
        JsonObject perms = getPermissionsObject();
        return client.getGson().getAdapter(new TypeToken<Map<String, EnumSet<Permissions>>>() {
        }).fromJsonTree(perms);
    }

    /**
     * Get info about the shards in the database.
     *
     * @return List of shards
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/advanced.html#get-/$db/_shards">_shards</a>
     */
    public List<Shard> getShards() {
        InputStream response = null;
        try {
            response = client.couchDbClient.get(new DatabaseURIHelper(db.getDBUri()).path("_shards")
                    .build());
            return getResponseList(response, client.getGson(),
                    new TypeToken<List<Shard>>() {
                    }.getType());
        } finally {
            close(response);
        }
    }

    /**
     * Get info about the shard a document belongs to.
     *
     * @param docId document ID
     * @return Shard info
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/advanced.html#get-/$db/_shards">_shards</a>
     */
    public Shard getShard(String docId) {
        assertNotEmpty(docId, "docId");
        return client.couchDbClient.get(new DatabaseURIHelper(db.getDBUri()).path("_shards")
                        .path(docId).build(),
                Shard.class);
    }

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
    public void createIndex(String indexName, String designDocName, String indexType,
                            IndexField[] fields) {
        JsonObject indexDefn = getIndexDefinition(indexName, designDocName, indexType, fields);
        createIndex(indexDefn.toString());
    }

    /**
     * Create a new index from a JSON string
     *
     * @param indexDefinition String representation of the index definition JSON
     * @see #createIndex(String, String, String, IndexField[])
     * @see <a target="_blank"
     * href="http://docs.cloudant.com/api/cloudant-query.html#creating-a-new-index">
     * index definition</a>
     */
    public void createIndex(String indexDefinition) {
        assertNotEmpty(indexDefinition, "indexDefinition");
        InputStream putresp = null;
        URI uri = new DatabaseURIHelper(db.getDBUri()).path("_index").build();
        try {
            putresp = client.couchDbClient.executeToInputStream(createPost(uri, indexDefinition,
                    "application/json"));
            String result = getAsString(putresp, "result");
            if (result.equalsIgnoreCase("created")) {
                log.info(String.format("Created Index: '%s'", indexDefinition));
            } else {
                log.warning(String.format("Index already exists : '%s'", indexDefinition));
            }
        } finally {
            close(putresp);
        }
    }

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
    public <T> List<T> findByIndex(String selectorJson, Class<T> classOfT) {
        return findByIndex(selectorJson, classOfT, new FindByIndexOptions());
    }

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
    public <T> List<T> findByIndex(String selectorJson, Class<T> classOfT, FindByIndexOptions
            options) {
        assertNotEmpty(selectorJson, "selectorJson");
        assertNotEmpty(options, "options");

        URI uri = new DatabaseURIHelper(db.getDBUri()).path("_find").build();
        JsonObject body = getFindByIndexBody(selectorJson, options);
        InputStream stream = null;
        try {
            stream = client.couchDbClient.executeToInputStream(createPost(uri, body.toString(),
                    "application/json"));
            Reader reader = new InputStreamReader(stream, "UTF-8");
            JsonArray jsonArray = new JsonParser().parse(reader)
                    .getAsJsonObject().getAsJsonArray("docs");
            List<T> list = new ArrayList<T>();
            for (JsonElement jsonElem : jsonArray) {
                JsonElement elem = jsonElem.getAsJsonObject();
                T t = client.getGson().fromJson(elem, classOfT);
                list.add(t);
            }
            return list;
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        } finally {
            close(stream);
        }
    }

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
    public List<Index> listIndices() {
        InputStream response = null;
        try {
            URI uri = new DatabaseURIHelper(db.getDBUri()).path("_index").build();
            response = client.couchDbClient.get(uri);
            return getResponseList(response, client.getGson(),
                    new TypeToken<List<Index>>() {
                    }.getType());
        } finally {
            close(response);
        }
    }

    /**
     * Delete an index
     *
     * @param indexName   name of the index
     * @param designDocId ID of the design doc
     */
    public void deleteIndex(String indexName, String designDocId) {
        assertNotEmpty(indexName, "indexName");
        assertNotEmpty(designDocId, "designDocId");
        URI uri = new DatabaseURIHelper(db.getDBUri()).path("_index").path(designDocId)
                .path("json").path(indexName).build();
        InputStream response = null;
        try {
            HttpConnection connection = Http.DELETE(uri);
            response = client.couchDbClient.executeToInputStream(connection);
            getResponse(response, Response.class, client.getGson());
        } finally {
            close(response);
        }
    }

    /**
     * Provides access to Cloudant <tt>Search</tt> APIs.
     *
     * @param searchIndexId the name of the index to search
     * @return Search object for searching the index
     * @see <a target="_blank" href="https://docs.cloudant.com/search.html">Search</a>
     */
    public Search search(String searchIndexId) {
        return new Search(client, this, searchIndexId);
    }

    /**
     * Get a manager that has convenience methods for managing design documents.
     *
     * @return a {@link DesignDocumentManager} for this database
     * @see DesignDocumentManager
     */
    public DesignDocumentManager getDesignDocumentManager() {
        return new DesignDocumentManager(this);
    }

    /**
     * @param designDoc containing the view
     * @param viewName  the view name
     * @return a builder to build view requests for the specified design document and view of
     * this database
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/creating_views.html#using-views">Using views</a>
     */
    public ViewRequestBuilder getViewRequestBuilder(String designDoc, String viewName) {
        return new ViewRequestBuilder(client, this, designDoc, viewName);
    }

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
    public AllDocsRequestBuilder getAllDocsRequestBuilder() {
        return new AllDocsRequestBuilderImpl(new ViewQueryParameters<String,
                AllDocsRequestResponse.Revision>(client, this, "", "", String.class,
                AllDocsRequestResponse.Revision.class) {
            protected DatabaseURIHelper getViewURIBuilder() {
                return new DatabaseURIHelper(db.getDBUri()).path("_all_docs");
            }
        });
    }

    /**
     * Provides access for interacting with the changes feed.
     * <P>
     * See the {@link com.cloudant.client.api.Changes} API for examples.
     * </P>
     *
     * @return a Changes object for using the changes feed
     * @see com.cloudant.client.api.Changes
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/database.html#get-changes">Databases - get
     * changes</a>
     */
    public com.cloudant.client.api.Changes changes() {
        Changes couchDbChanges = db.changes();
        com.cloudant.client.api.Changes changes = new com.cloudant.client.api.Changes
                (couchDbChanges);
        return changes;
    }

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
    public <T> T find(Class<T> classType, String id) {
        return db.find(classType, id);
    }

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
    public <T> T find(Class<T> classType, String id, Params params) {
        assertNotEmpty(params, "params");
        return db.find(classType, id, params.getInternalParams());
    }

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
    public <T> T find(Class<T> classType, String id, String rev) {
        return db.find(classType, id, rev);
    }

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
    public <T> T findAny(Class<T> classType, String uri) {
        return db.findAny(classType, uri);
    }

    /**
     * Finds the document with the specified document ID and returns it as an {@link InputStream}.
     * <p><b>Note</b>: The stream must be closed after use to release the connection.</p>
     *
     * @param id the document _id field
     * @return the result as {@link InputStream}
     * @throws NoDocumentException If the document is not found in the database.
     * @see #find(String, String)
     */
    public InputStream find(String id) {
        return db.find(id);
    }

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
    public InputStream find(String id, String rev) {
        return db.find(id, rev);
    }

    /**
     * Checks if a document exists in the database.
     *
     * @param id the document _id field
     * @return {@code true} if the document is found, {@code false} otherwise
     */
    public boolean contains(String id) {
        return db.contains(id);
    }

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
     * <P>
     * Note that the Java object is not modified by the save operation and so will not include the
     * revision generated by the database server. You can obtain the server revision for this write
     * from the response, for example:
     * </P>
     * <pre>
     *     {@code
     *     Foo foo = new Foo();
     *     Response response = db.save(foo);
     *     foo.setRevision(response.getRevision());
     *     }
     * </pre>
     *
     * @param object the object to save
     * @return {@link com.cloudant.client.api.model.Response}
     * @throws DocumentConflictException If a conflict is detected during the save.
     */
    public com.cloudant.client.api.model.Response save(Object object) {
        Response couchDbResponse = db.save(object);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    /**
     * Saves a document in the database similarly to {@link Database#save(Object)} but using a
     * specific write quorum.
     *
     * @param object      the object to save
     * @param writeQuorum the write quorum
     * @return {@link com.cloudant.client.api.model.Response}
     * @throws DocumentConflictException If a conflict is detected during the save.
     * @see Database#save(Object)
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/document.html#quorum---writing-and-reading-data">
     * Documents - quorum</a>
     */
    public com.cloudant.client.api.model.Response save(Object object, int writeQuorum) {
        Response couchDbResponse = client.couchDbClient.put(getDBUri(), object, true, writeQuorum);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

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
     * <P>
     * Note that the Java object is not modified by the create operation and so will not include the
     * revision generated by the database server. You can obtain the server revision for this write
     * from the response, for example:
     * </P>
     * <pre>
     *     {@code
     *     Foo foo = new Foo();
     *     Response response = db.post(foo);
     *     foo.setRevision(response.getRevision());
     *     }
     * </pre>
     *
     * @param object The object to save
     * @return {@link com.cloudant.client.api.model.Response}
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/document.html#documentCreate">Documents - create</a>
     */
    public com.cloudant.client.api.model.Response post(Object object) {
        Response couchDbResponse = db.post(object);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    /**
     * Creates a document in the database similarly to {@link Database#post(Object)} but using a
     * specific write quorum.
     *
     * @param object      The object to save
     * @param writeQuorum the write Quorum
     * @return {@link com.cloudant.client.api.model.Response}
     * @see Database#post(Object)
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/document.html#quorum---writing-and-reading-data">
     * Documents - quorum</a>
     */
    public com.cloudant.client.api.model.Response post(Object object, int writeQuorum) {
        assertNotEmpty(object, "object");
        InputStream response = null;
        try {
            URI uri = new DatabaseURIHelper(db.getDBUri()).query("w", writeQuorum).build();
            response = client.couchDbClient.executeToInputStream(createPost(uri, client.getGson()
                            .toJson(object),
                    "application/json"));
            Response couchDbResponse = getResponse(response, Response.class, client.getGson());
            com.cloudant.client.api.model.Response cloudantResponse = new com.cloudant.client.api
                    .model.Response(couchDbResponse);
            return cloudantResponse;
        } finally {
            close(response);
        }
    }

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
     * <P>
     * Note that the Java object is not modified by the update operation and so will not include the
     * revision generated by the database server. You can obtain the server revision for this write
     * from the response, for example:
     * </P>
     * <pre>
     *     {@code
     *     Foo foo = new Foo();
     *     Response response = db.update(foo);
     *     foo.setRevision(response.getRevision());
     *     }
     * </pre>
     *
     * @param object the object to update
     * @return {@link com.cloudant.client.api.model.Response}
     * @throws DocumentConflictException if a conflict is detected during the update.
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#update">Documents -
     * update</a>
     */
    public com.cloudant.client.api.model.Response update(Object object) {
        Response couchDbResponse = db.update(object);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    /**
     * Updates an object in the database similarly to {@link #update(Object)}, but specifying the
     * write quorum.
     *
     * @param object      the object to update
     * @param writeQuorum the write quorum
     * @return {@link com.cloudant.client.api.model.Response}
     * @throws DocumentConflictException if a conflict is detected during the update.
     * @see Database#update(Object)
     * @see <a target="_blank"
     * href="https://docs.cloudant.com/document.html#quorum---writing-and-reading-data">
     * Documents - quorum</a>
     */
    public com.cloudant.client.api.model.Response update(Object object, int writeQuorum) {
        Response couchDbResponse = client.couchDbClient.put(getDBUri(), object, false, writeQuorum);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

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
     * @return {@link com.cloudant.client.api.model.Response}
     * @throws NoDocumentException If the document is not found in the database.
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#delete">Documents -
     * delete</a>
     */
    public com.cloudant.client.api.model.Response remove(Object object) {
        Response couchDbResponse = db.remove(object);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

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
     * @return {@link com.cloudant.client.api.model.Response}
     * @throws NoDocumentException If the document is not found in the database.
     * @see <a target="_blank" href="https://docs.cloudant.com/document.html#delete">Documents -
     * delete</a>
     */
    public com.cloudant.client.api.model.Response remove(String id, String rev) {
        Response couchDbResponse = db.remove(id, rev);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

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
    public List<com.cloudant.client.api.model.Response> bulk(List<?> objects) {
        List<Response> couchDbResponseList = db.bulk(objects, false);
        List<com.cloudant.client.api.model.Response> cloudantResponseList = new ArrayList<com
                .cloudant.client.api.model.Response>();
        for (Response couchDbResponse : couchDbResponseList) {
            com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                    .Response(couchDbResponse);
            cloudantResponseList.add(response);
        }
        return cloudantResponseList;
    }

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
     * @return {@link com.cloudant.client.api.model.Response}
     * @see <a target="_blank" href="https://docs.cloudant.com/attachments.html">Attachments</a>
     */
    public com.cloudant.client.api.model.Response saveAttachment(InputStream in, String name,
                                                                 String contentType) {
        Response couchDbResponse = db.saveAttachment(in, name, contentType);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

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
     * @param docId       The document ID to save the attachment under, or {@code null} to save
     *                    under a new document with a generated ID.
     * @param docRev      The document revision to save the attachment under, or {@code null}
     *                    when saving to a new document.
     * @return {@link Response}
     * @throws DocumentConflictException if the attachment cannot be saved because of a conflict
     * @see <a target="_blank" href="https://docs.cloudant.com/attachments.html">Attachments</a>
     */
    public com.cloudant.client.api.model.Response saveAttachment(InputStream in, String name,
                                                                 String contentType, String
                                                                         docId, String docRev) {
        Response couchDbResponse = db.saveAttachment(in, name, contentType, docId, docRev);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

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
    public String invokeUpdateHandler(String updateHandlerUri, String docId,
                                      Params params) {
        assertNotEmpty(params, "params");
        return db.invokeUpdateHandler(updateHandlerUri, docId, params.getInternalParams());
    }

    /**
     * @return The database URI.
     */
    public URI getDBUri() {
        return db.getDBUri();
    }

    /**
     * Get information about this database.
     *
     * @return DbInfo encapsulating the database info
     * @see <a target="_blank" href="https://docs.cloudant.com/database.html#read">Databases -
     * read</a>
     */
    public DbInfo info() {
        return client.couchDbClient.get(new DatabaseURIHelper(db.getDBUri()).getDatabaseUri(),
                DbInfo.class);
    }

    /**
     * Requests the database commits any recent changes to disk.
     *
     * @see <a
     * href="http://docs.couchdb.org/en/1.6.1/api/database/compact.html#db-ensure-full-commit">
     * CouchDB _ensure_full_commit
     * </a>
     */
    public void ensureFullCommit() {
        db.ensureFullCommit();
    }

    // private helper methods

    /**
     * Form a create index json from parameters
     */
    private JsonObject getIndexDefinition(String indexName, String designDocName,
                                          String indexType, IndexField[] fields) {
        assertNotEmpty(fields, "index fields");
        JsonObject indexObject = new JsonObject();
        if (!(indexName == null || indexName.isEmpty())) {
            indexObject.addProperty("name", indexName);
        }
        if (!(designDocName == null || designDocName.isEmpty())) {
            indexObject.addProperty("ddoc", designDocName);
        }
        if (!(indexType == null || indexType.isEmpty())) {

            indexObject.addProperty("type", indexType);
        }

        JsonArray fieldsArray = new JsonArray();
        for (int i = 0; i < fields.length; i++) {
            JsonObject fieldObject = new JsonObject();
            fieldObject.addProperty(fields[i].getName(), fields[i].getOrder().toString());
            fieldsArray.add(fieldObject);
        }
        JsonObject arrayOfFields = new JsonObject();
        arrayOfFields.add("fields", fieldsArray);
        indexObject.add("index", arrayOfFields);

        return indexObject;
    }

    private JsonObject getFindByIndexBody(String selectorJson,
                                          FindByIndexOptions options) {

        JsonArray fieldsArray = new JsonArray();
        if (options.getFields().size() > 0) {
            for (String field : options.getFields()) {
                JsonPrimitive jsonField = client.getGson().fromJson(field,
                        JsonPrimitive.class);
                fieldsArray.add(jsonField);
            }
        }

        JsonArray sortArray = new JsonArray();
        if (options.getSort().size() > 0) {

            for (IndexField sort : options.getSort()) {
                JsonObject sortObject = new JsonObject();
                sortObject.addProperty(sort.getName(), sort.getOrder().toString());
                sortArray.add(sortObject);
            }
        }

        JsonObject indexObject = new JsonObject();

        //parse and find if valid json issue #28
        JsonObject selectorObject = null;
        boolean isObject = true;
        try {
            selectorObject = getGson().fromJson(selectorJson, JsonObject.class);
        } catch (JsonParseException e) {
            isObject = false;
        }

        if (!isObject) {
            if (selectorJson.startsWith("\"selector\"")) {
                selectorJson = selectorJson.substring(selectorJson.indexOf(":") + 1,
                        selectorJson.length()).trim();
                selectorObject = getGson().fromJson(selectorJson, JsonObject.class);
            } else {
                throw new JsonParseException("selectorJson should be valid json or like " +
                        "\"selector\": {...} ");
            }
        }

        if (selectorObject.has("selector")) {
            indexObject.add("selector", selectorObject.get("selector"));
        } else {
            indexObject.add("selector", selectorObject);
        }

        if (fieldsArray.size() > 0) {
            indexObject.add("fields", fieldsArray);
        }
        if (sortArray.size() > 0) {
            indexObject.add("sort", sortArray);
        }
        if (options.getLimit() != null) {
            indexObject.addProperty("limit", options.getLimit());
        }
        if (options.getSkip() != null) {
            indexObject.addProperty("skip", options.getSkip());
        }
        if (options.getReadQuorum() != null) {
            indexObject.addProperty("r", options.getReadQuorum());
        }
        if (options.getUseIndex() != null) {
            indexObject.add("use_index", getGson().fromJson(options.getUseIndex(),
                    JsonArray.class));
        }

        return indexObject;
    }

    Gson getGson() {
        return client.getGson();
    }
}


