/*
 * Copyright © 2016, 2018 IBM Corp. All rights reserved.
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
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotNull;
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
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.api.model.Shard;
import com.cloudant.client.api.query.Indexes;
import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.views.AllDocsRequestBuilder;
import com.cloudant.client.api.views.ViewRequestBuilder;
import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.client.internal.URIBase;
import com.cloudant.client.internal.query.Helpers;
import com.cloudant.client.internal.util.DeserializationTypes;
import com.cloudant.client.internal.views.AllDocsRequestBuilderImpl;
import com.cloudant.client.internal.views.AllDocsRequestResponse;
import com.cloudant.client.internal.views.ViewQueryParameters;
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
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
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
     * <a target="_blank"
     * href="https://console.bluemix.net/docs/services/Cloudant/api/authorization.html#authorization">
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/authorization.html#roles"
     * target="_blank">Roles</a>
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/authorization.html#modifying-permissions"
     * target="_blank">Modifying permissions</a>
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

        HttpConnection put = Http.PUT(apiV2DBSecurityURI, "application/json");
        put.setRequestBody(client.getGson().toJson(perms));
        // CouchDbExceptions will be thrown for non-2XX cases
        client.couchDbClient.executeToResponse(put);
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
            if (exception.getMessage().toLowerCase(Locale.ENGLISH).contains("bad request")) {
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
     * <a target="_blank"
     * href="https://console.bluemix.net/docs/services/Cloudant/api/authorization.html#authorization">
     * Cloudant authorization API
     * </a> such as Cloudant DBaaS. For unsupported databases consider using the /db/_security
     * endpoint.
     * </p>
     *
     * @return the map of userNames to their Permissions
     * @throws UnsupportedOperationException if called on a database that does not provide the
     *                                       Cloudant authorization API
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/authorization.html#roles"
     * target="_blank">Roles</a>
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/authorization.html#viewing-permissions"
     * target="_blank">Viewing permissions</a>
     */
    public Map<String, EnumSet<Permissions>> getPermissions() {
        JsonObject perms = getPermissionsObject();
        return client.getGson().getAdapter(DeserializationTypes.PERMISSIONS_MAP_TOKEN).fromJsonTree
                (perms);
    }

    /**
     * Get info about the shards in the database.
     *
     * @return List of shards
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/advanced.html#-get-database-_shards-"
     * target="_blank">_shards</a>
     */
    public List<Shard> getShards() {
        InputStream response = null;
        try {
            response = client.couchDbClient.get(new DatabaseURIHelper(db.getDBUri()).path("_shards")
                    .build());
            return getResponseList(response, client.getGson(), DeserializationTypes.SHARDS);
        } finally {
            close(response);
        }
    }

    /**
     * Get info about the shard a document belongs to.
     *
     * @param docId document ID
     * @return Shard info
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/advanced.html#-get-database-_shards-"
     * target="_blank">_shards</a>
     */
    public Shard getShard(String docId) {
        assertNotEmpty(docId, "docId");
        return client.couchDbClient.get(new DatabaseURIHelper(db.getDBUri()).path("_shards")
                        .path(docId).build(),
                Shard.class);
    }

    /**
     * Create a new JSON index
     * <P>
     * Example usage creating an index that sorts ascending on name, then by year:
     * </P>
     * <pre>
     * {@code
     * db.createIndex("Person_name", "Person_name", null, new IndexField[]{
     *       new IndexField("Person_name",SortOrder.asc),
     *       new IndexField("Movie_year",SortOrder.asc)});
     * }
     * </pre>
     * <P>
     * Example usage creating an index that sorts ascending by year:
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
     * @param indexType     optional, type of index (only "json" for this method)
     * @param fields        array of fields in the index
     * @see Database#createIndex(String)
     */
    @Deprecated
    public void createIndex(String indexName, String designDocName, String indexType,
                            IndexField[] fields) {
        if (indexType == null || "json".equalsIgnoreCase(indexType)) {
            JsonIndex.Builder b = JsonIndex.builder().name(indexName).designDocument(designDocName);
            for (IndexField f : fields) {
                switch (f.getOrder()) {
                    case desc:
                        b.desc(f.getName());
                        break;
                    case asc:
                    default:
                        b.asc(f.getName());
                        break;
                }
            }
            createIndex(b.definition());
        } else {
            throw new CouchDbException("Unsupported index type " + indexType);
        }
    }

    /**
     * <p>
     * Create a new index from a string of JSON representing the index definition
     * </p>
     * <p>
     * Helpers are available to construct the index definition string for JSON and text indexes.
     * </p>
     * <p>
     * Example usage creating a JSON index with a generated name for the field named "Movie_year"
     * with ascending sort order:
     * </p>
     * <pre>
     * {@code
     * db.createIndex(JsonIndex.builder().asc("Movie_year").definition());
     * }
     * </pre>
     * <p>
     * Example usage creating a partial JSON index named "movies-after-2010-json" which will
     * index all movies with "Movie_year" greater than 2010, returning the field "Movie_year" in
     * descending order:
     * </p>
     * <pre>
     * {@code
     * Selector selector = gt("Movie_year", 2010);
     * String indexDefinition = JsonIndex.builder().
     *     name("movies-after-2010-json").
     *     desc("Movie_year").
     *     partialFilterSelector(selector).
     *     definition();
     * db.createIndex(indexDefinition);
     * }
     * </pre>
     * <p>
     * Example usage creating a text index with a generated name for the string field named
     * "Movie_title":
     * </p>
     * <pre>
     * {@code
     * db.createIndex(TextIndex.builder().string("Movie_title").definition());
     * }
     * </pre>
     * <p>
     * Example usage creating a partial text index named "movies-after-2010-text" for the string field
     * named "Movie_title" which will index all movies titles for movies with "Movie_year" greater
     * than 2010:
     * </p>
     * <pre>
     * {@code
     * Selector selector = gt("Movie_year", 2010);
     * String indexDefinition = TextIndex.builder().
     *     string("Movie_title").
     *     name("movies-after-2010-text").
     *     partialFilterSelector(selector).
     *     definition();
     * db.createIndex(indexDefinition);
     * }
     * </pre>
     *
     * @param indexDefinition String representation of the index definition JSON
     * @see com.cloudant.client.api.query.JsonIndex.Builder
     * @see com.cloudant.client.api.query.TextIndex.Builder
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/cloudant_query.html#creating-an-index"
     * target="_blank">index definition</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/cloudant_query.html#selector-syntax"
     * target="_blank">selector syntax</a>
     * @deprecated Use {@link #query(String, Class)} instead
     */
    @Deprecated
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/cloudant_query.html#selector-syntax"
     * target="_blank">selector syntax</a>
     * @deprecated Use {@link #query(String, Class)} instead
     */
    @Deprecated
    public <T> List<T> findByIndex(String selectorJson, Class<T> classOfT, FindByIndexOptions
            options) {
        JsonObject selector = Helpers.getSelectorFromString(selectorJson);
        assertNotEmpty(options, "options");
        JsonObject body = getFindByIndexBody(selector, options);
        return query(body.toString(), classOfT).getDocs();
    }

    /**
     * <p>
     *     Query documents using an index and a query selector.
     * </p>
     * <p>
     *     Note: the most convenient way to generate query selectors is using a
     *     {@link com.cloudant.client.api.query.QueryBuilder}.
     * </p>
     * <p>
     *     Example usage to return the name and year of movies starring Alec Guinness since 1960
     *     with the results sorted by year descending:
     * </p>
     * <pre>
     * {@code
     * QueryResult<Movie> movies = db.query(new QueryBuilder(and(
     *   gt("Movie_year", 1960),
     *   eq("Person_name", "Alec Guinness"))).
     *   sort(Sort.desc("Movie_year")).
     *   fields("Movie_name", "Movie_year").
     *   build(), Movie.class);
     * }
     * </pre>
     * @param query    String representation of a JSON object describing criteria used to
     *                 select documents.
     * @param classOfT The class of Java objects to be returned in the {@code docs} field of result.
     * @param <T>      The type of the Java object to be returned in the {@code docs} field of result.
     * @return         A {@link QueryResult} object, containing the documents matching the query
     *                 in the {@code docs} field.
     * @see com.cloudant.client.api.query.QueryBuilder
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/cloudant_query.html#selector-syntax"
     * target="_blank">selector syntax</a>
     */
    public <T> QueryResult<T> query(String query, final Class<T> classOfT) {
        URI uri = new DatabaseURIHelper(db.getDBUri()).path("_find").build();
        InputStream stream = null;
        try {
            stream = client.couchDbClient.executeToInputStream(createPost(uri, query,
                    "application/json"));
            Reader reader = new InputStreamReader(stream, "UTF-8");
            Type type = TypeToken.getParameterized(QueryResult.class, classOfT).getType();
            QueryResult<T> result = client.getGson().fromJson(reader, type);
            return result;
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
     * @see Database#listIndexes()
     */
    @Deprecated
    public List<Index> listIndices() {
        InputStream response = null;
        try {
            URI uri = new DatabaseURIHelper(db.getDBUri()).path("_index").build();
            response = client.couchDbClient.get(uri);
            return getResponseList(response, client.getGson(), DeserializationTypes.INDICES);
        } finally {
            close(response);
        }
    }

    /**
     * List the indexes in the database. The returned object allows for listing indexes by type.
     *
     * @return indexes object with methods for getting indexes of a particular type
     */
    public Indexes listIndexes() {
        URI uri = new DatabaseURIHelper(db.getDBUri()).path("_index").build();
        return client.couchDbClient.get(uri, Indexes.class);
    }

    /**
     * Delete a JSON index
     *
     * @param indexName   name of the index
     * @param designDocId ID of the design doc
     */
    public void deleteIndex(String indexName, String designDocId) {
        deleteIndex(indexName, designDocId, "json");
    }

    /**
     * Delete an index with the specified name and type in the given design document.
     *
     * @param indexName   name of the index
     * @param designDocId ID of the design doc (the _design prefix will be added if not present)
     * @param type        type of the index, valid values or "text" or "json"
     */
    public void deleteIndex(String indexName, String designDocId, String type) {
        assertNotEmpty(indexName, "indexName");
        assertNotEmpty(designDocId, "designDocId");
        assertNotNull(type, "type");
        if (!designDocId.startsWith("_design")) {
            designDocId = "_design/" + designDocId;
        }
        URI uri = new DatabaseURIHelper(db.getDBUri()).path("_index").path(designDocId)
                .path(type).path(indexName).build();
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
     * <p>Example usage:</p>
     * <pre>
     * {@code
     *  // Search query using design document _id '_design/views101' and search index 'animals'
     *  List<Bird> birds = db.search("views101/animals")
     * 	.limit(10)
     * 	.includeDocs(true)
     * 	.query("class:bird", Bird.class);
     * 	}
     * </pre>
     *
     * @param searchIndexId the design document with the name of the index to search
     * @return Search object for searching the index
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/search.html#search"
     * target="_blank">Search</a>
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
        return new DesignDocumentManager(client, this);
    }

    /**
     * @param designDoc containing the view
     * @param viewName  the view name
     * @return a builder to build view requests for the specified design document and view of
     * this database
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/using_views.html#using-views"
     * target="_blank">Using views</a>
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
                AllDocsRequestResponse.AllDocsValue>(client, this, "", "", String.class,
                AllDocsRequestResponse.AllDocsValue.class) {
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/database.html#get-changes"
     * target="_blank">Databases - get changes</a>
     */
    public Changes changes() {
        return new Changes(client, this);
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#read"
     * target="_blank">Documents - read</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#read"
     * target="_blank">Documents - read</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#read"
     * target="_blank">Documents - read</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#read"
     * target="_blank">Documents - read</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#quorum-writing-and-reading-data"
     * target="_blank">Documents - quorum</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#create"
     * target="_blank">Documents - create</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#quorum-writing-and-reading-data"
     * target="_blank">Documents - quorum</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#update"
     * target="_blank">Documents - update</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#quorum-writing-and-reading-data"
     * target="_blank">Documents - quorum</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#delete"
     * target="_blank">Documents - delete</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#delete"
     * target="_blank">Documents - delete</a>
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
     * <p>
     * Note that the value returned by {@code getStatusCode()} on each {@code Response} object is
     * the overall status returned from {@code bulk_docs} and will therefore be the same for all
     * {@code Response} objects.
     * </p>
     * <p>
     * The returned list of {@code Response}s should be examined to ensure that all of the documents submitted
     * in the original request were successfully added to the database.
     * </p>
     * <p>
     * When a document (or document revision) is not correctly committed to the database because of
     * an error, you must check the value of {@code getError()} to determine error type and course
     * of action. See
     * <a href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#bulk-document-validation-and-conflict-errors" target="_blank">
     * Bulk Document Validation and Conflict Errors</a> for more information.
     * </p>
     *
     * @param objects the {@link List} of objects
     * @return {@code List<Response>} one per object
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/document.html#bulk-operations"
     * target="_blank">Documents - bulk operations</a>
     */
    public List<com.cloudant.client.api.model.Response> bulk(List<?> objects) {
        List<Response> couchDbResponseList = db.bulk(objects, false);
        List<com.cloudant.client.api.model.Response> cloudantResponseList = new ArrayList<com
                .cloudant.client.api.model.Response>(couchDbResponseList.size());
        for (Response couchDbResponse : couchDbResponseList) {
            com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                    .Response(couchDbResponse);
            cloudantResponseList.add(response);
        }
        return cloudantResponseList;
    }

    /**
     * Reads an attachment from the database.
     *
     * The stream must be closed after usage, otherwise http connection leaks will occur.
     *
     * @param docId the document id
     * @param attachmentName the attachment name
     * @return the attachment in the form of an {@code InputStream}.
     */
    public InputStream getAttachment(String docId, String attachmentName) {
        return getAttachment(docId, attachmentName, null);
    }

    /**
     * Reads an attachment from the database.
     *
     * The stream must be closed after usage, otherwise http connection leaks will occur.
     *
     * @param docId the document id
     * @param attachmentName the attachment name
     * @param revId the document revision id or {@code null}
     * @return the attachment in the form of an {@code InputStream}.
     */
    public InputStream getAttachment(String docId, String attachmentName, String revId) {
        return db.getAttachment(docId, attachmentName, revId);
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/attachments.html#attachments"
     * target="_blank">Attachments</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/attachments.html#attachments"
     * target="_blank">Attachments</a>
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
     * Removes an attachment from the specified document.
     * <p>The object must have the correct {@code _id} and {@code _rev} values.</p>
     * <P>Example usage:</P>
     * <pre>
     * {@code
     * //get a Bar object from the database
     * Bar bar = db.find(Bar.class, "exampleId");
     * String attachmentName = "example.jpg";
     * //now remove the remote Bar attachment
     * Response response = db.removeAttachment(bar, attachmentName);
     * }
     * </pre>
     *
     * @param object the document to remove as an object
     * @param attachmentName the attachment name to remove
     * @return {@link com.cloudant.client.api.model.Response}
     * @throws NoDocumentException If the document is not found in the database.
     * @throws DocumentConflictException If a conflict is detected during the removal.
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/attachments.html#delete"
     * target="_blank">Documents - delete</a>
     */
    public com.cloudant.client.api.model.Response removeAttachment(Object object, String attachmentName) {
        Response couchDbResponse = db.removeAttachment(object, attachmentName);
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    /**
     * Removes the attachment from a document the specified {@code _id} and {@code _rev} and {@code attachmentName}
     * values.
     * <P>Example usage:</P>
     * <pre>
     * {@code
     * Response response = db.removeAttachment("exampleId", "1-12345exampleRev", "example.jpg");
     * }
     * </pre>
     *
     * @param id  the document _id field
     * @param rev the document _rev field
     * @param attachmentName the attachment name
     * @return {@link com.cloudant.client.api.model.Response}
     * @throws NoDocumentException If the document is not found in the database.
     * @throws DocumentConflictException if the attachment cannot be removed because of a conflict
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/attachments.html#delete"
     * target="_blank">Documents - delete</a>
     */
    public com.cloudant.client.api.model.Response removeAttachment(String id, String rev, String attachmentName) {
        Response couchDbResponse = db.removeAttachment(id, rev, attachmentName);
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/design_documents.html#update-handlers"
     * target="_blank">Design documents - update handlers</a>
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
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/database.html#getting-database-details"
     * target="_blank">Databases - read</a>
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
     *
     * @param selector sanitized selector JSON object (excluding "selector" key)
     * @param options find by index options
     * @return query object to POST
     */
    private JsonObject getFindByIndexBody(JsonObject selector,
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
        indexObject.add("selector", selector);


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
                    JsonElement.class));
        }

        return indexObject;
    }

    Gson getGson() {
        return client.getGson();
    }
}


