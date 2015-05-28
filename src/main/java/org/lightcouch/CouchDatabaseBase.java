/*
 * Copyright (C) 2011 lightcouch.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lightcouch;

import static org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.internal.CouchDbUtil.close;
import static org.lightcouch.internal.CouchDbUtil.generateUUID;
import static org.lightcouch.internal.CouchDbUtil.getAsString;
import static org.lightcouch.internal.CouchDbUtil.getResponse;
import static org.lightcouch.internal.CouchDbUtil.getResponseList;
import static org.lightcouch.internal.CouchDbUtil.getStream;
import static org.lightcouch.internal.CouchDbUtil.streamToString;
import static org.lightcouch.internal.URIBuilder.buildUri;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * Contains a Database Public API implementation.
 * @see CouchDatabase
 * @author Ahmed Yehia
 */
public abstract class CouchDatabaseBase {

    static final Log log = LogFactory.getLog(CouchDatabase.class);

    CouchDbClientBase client;
    private URI dbURI;

    private CouchDbDesign design;
    private String dbName;


    CouchDatabaseBase(CouchDbClientBase client, String name, boolean create) {
        assertNotEmpty(name, "name");
        this.dbName = name;
        this.client = client;
        this.dbURI   = buildUri(client.getBaseUri()).path(name).path("/").build();
        connect(create);
        this.design = new CouchDbDesign(this);
    }


    /**
     * Provides access to CouchDB Design Documents.
     * @see CouchDbDesign
     */
    public CouchDbDesign design() {
        return design;
    }

    /**
     * Provides access to CouchDB <tt>View</tt> APIs.
     * @see View
     */
    public View view(String viewId) {
        return new View(this, viewId);
    }


    /**
     * Provides access to <tt>Change Notifications</tt> API.
     * @see Changes
     */
    public Changes changes() {
        return new Changes(this);
    }

    /**
     * Finds an Object of the specified type.
     * @param <T> Object type.
     * @param classType The class of type T.
     * @param id The document id.
     * @return An object of type T.
     * @throws NoDocumentException If the document is not found in the database.
     */
    public <T> T find(Class<T> classType, String id) {
        assertNotEmpty(classType, "Class");
        assertNotEmpty(id, "id");
        final URI uri = buildUri(getDBUri()).pathToEncode(id).buildEncoded();
        return client.get(uri, classType);
    }

    /**
     * Finds an Object of the specified type.
     * @param <T> Object type.
     * @param classType The class of type T.
     * @param id The document id.
     * @param params Extra parameters to append.
     * @return An object of type T.
     * @throws NoDocumentException If the document is not found in the database.
     */
    public <T> T find(Class<T> classType, String id, Params params) {
        assertNotEmpty(classType, "Class");
        assertNotEmpty(id, "id");
        final URI uri = buildUri(getDBUri()).pathToEncode(id).query(params).buildEncoded();
        return client.get(uri, classType);
    }

    /**
     * Finds an Object of the specified type.
     * @param <T> Object type.
     * @param classType The class of type T.
     * @param id The document _id field.
     * @param rev The document _rev field.
     * @return An object of type T.
     * @throws NoDocumentException If the document is not found in the database.
     */
    public <T> T find(Class<T> classType, String id, String rev) {
        assertNotEmpty(classType, "Class");
        assertNotEmpty(id, "id");
        assertNotEmpty(id, "rev");
        final URI uri = buildUri(getDBUri()).pathToEncode(id).query("rev", rev).buildEncoded();
        return client.get(uri, classType);
    }

    /**
     * This method finds any document given a URI.
     * <p>The URI must be URI-encoded.
     * @param classType The class of type T.
     * @param uri The URI as string.
     * @return An object of type T.
     */
    public <T> T findAny(Class<T> classType, String uri) {
        assertNotEmpty(classType, "Class");
        assertNotEmpty(uri, "uri");
        return client.get(URI.create(uri), classType);
    }

    /**
     * Finds a document and return the result as {@link InputStream}.
     * <p><b>Note</b>: The stream must be closed after use to release the connection.
     * @param id The document _id field.
     * @return The result as {@link InputStream}
     * @throws NoDocumentException If the document is not found in the database.
     * @see #find(String, String)
     */
    public InputStream find(String id) {
        assertNotEmpty(id, "id");
        return client.get(buildUri(getDBUri()).path(id).build());
    }

    /**
     * Finds a document given id and revision and returns the result as {@link InputStream}.
     * <p><b>Note</b>: The stream must be closed after use to release the connection.
     * @param id The document _id field.
     * @param rev The document _rev field.
     * @return The result as {@link InputStream}
     * @throws NoDocumentException If the document is not found in the database.
     */
    public InputStream find(String id, String rev) {
        assertNotEmpty(id, "id");
        assertNotEmpty(rev, "rev");
        final URI uri = buildUri(getDBUri()).path(id).query("rev", rev).build();
        return client.get(uri);
    }

    /**
     * Checks if a document exist in the database.
     * @param id The document _id field.
     * @return true If the document is found, false otherwise.
     */
    public boolean contains(String id) {
        assertNotEmpty(id, "id");
        HttpResponse response = null;
        try {
            response = client.head(buildUri(getDBUri()).path(id).build());
        } catch (NoDocumentException e) {
            return false;
        } finally {
            close(response);
        }
        return true;
    }

    /**
     * Saves an object in the database, using HTTP <tt>PUT</tt> request.
     * <p>If the object doesn't have an <code>_id</code> value, the code will assign a <code>UUID</code> as the document id.
     * @param object The object to save
     * @throws DocumentConflictException If a conflict is detected during the save.
     * @return {@link Response}
     */
    public Response save(Object object) {
        return client.put(getDBUri(), object, true);
    }

    /**
     * Saves an object in the database using HTTP <tt>POST</tt> request.
     * <p>The database will be responsible for generating the document id.
     * @param object The object to save
     * @return {@link Response}
     */
    public Response post(Object object) {
        assertNotEmpty(object, "object");
        HttpResponse response = null;
        try {
            URI uri = buildUri(getDBUri()).build();
            response = client.post(uri, getGson().toJson(object));
            return getResponse(response,Response.class, client.getGson());
        } finally {
            close(response);
        }
    }

    /**
     * Saves a document with <tt>batch=ok</tt> query param.
     * @param object The object to save.
     */
    public void batch(Object object) {
        assertNotEmpty(object, "object");
        HttpResponse response = null;
        try {
            URI uri = buildUri(getDBUri()).query("batch", "ok").build();
            response = client.post(uri, getGson().toJson(object));
        } finally {
            close(response);
        }
    }

    /**
     * Updates an object in the database, the object must have the correct <code>_id</code> and <code>_rev</code> values.
     * @param object The object to update
     * @throws DocumentConflictException If a conflict is detected during the update.
     * @return {@link Response}
     */
    public Response update(Object object) {
        return client.put(getDBUri(), object, false);
    }

    /**
     * Removes a document from the database.
     * <p>The object must have the correct <code>_id</code> and <code>_rev</code> values.
     * @param object The document to remove as object.
     * @throws NoDocumentException If the document is not found in the database.
     * @return {@link Response}
     */
    public Response remove(Object object) {
        assertNotEmpty(object, "object");
        JsonObject jsonObject = getGson().toJsonTree(object).getAsJsonObject();
        final String id = getAsString(jsonObject, "_id");
        final String rev = getAsString(jsonObject, "_rev");
        return remove(id, rev);
    }

    /**
     * Removes a document from the database given both a document <code>_id</code> and <code>_rev</code> values.
     * @param id The document _id field.
     * @param rev The document _rev field.
     * @throws NoDocumentException If the document is not found in the database.
     * @return {@link Response}
     */
    public Response remove(String id, String rev) {
        assertNotEmpty(id, "id");
        assertNotEmpty(rev, "rev");
        final URI uri = buildUri(getDBUri()).pathToEncode(id).query("rev", rev).buildEncoded();
        return client.delete(uri);
    }

    /**
     * Performs a Bulk Documents insert request.
     * @param objects The {@link List} of objects.
     * @param allOrNothing Indicates whether the request has <tt>all-or-nothing</tt> semantics.
     * @return {@code List<Response>} Containing the resulted entries.
     */
    public List<Response> bulk(List<?> objects, boolean allOrNothing) {
        assertNotEmpty(objects, "objects");
        HttpResponse response = null;
        try {
            final String allOrNothingVal = allOrNothing ? "\"all_or_nothing\": true, " : "";
            final URI uri = buildUri(getDBUri()).path("_bulk_docs").build();
            final String json = String.format("{%s%s%s}", allOrNothingVal, "\"docs\": ", getGson().toJson(objects));
            response = client.post(uri, json);
            return getResponseList(response, client.getGson(), Response.class,
                        new TypeToken<List<Response>>(){}.getType());
        } finally {
            close(response);
        }
    }

    /**
     * Saves an attachment to a new document with a generated <tt>UUID</tt> as the document id.
     * <p>To retrieve an attachment, see {@link #find(String)}.
     * @param in The {@link InputStream} holding the binary data.
     * @param name The attachment name.
     * @param contentType The attachment "Content-Type".
     * @return {@link Response}
     */
    public Response saveAttachment(InputStream in, String name, String contentType) {
        assertNotEmpty(in, "in");
        assertNotEmpty(name, "name");
        assertNotEmpty(contentType, "ContentType");
        final URI uri = buildUri(getDBUri()).path(generateUUID()).path("/").path(name).build();
        return client.put(uri, in, contentType);
    }

    /**
     * Saves an attachment to an existing document given both a document id
     * and revision, or save to a new document given only the id, and rev as {@code null}.
     * <p>To retrieve an attachment, see {@link #find(String)}.
     * @param in The {@link InputStream} holding the binary data.
     * @param name The attachment name.
     * @param contentType The attachment "Content-Type".
     * @param docId The document id to save the attachment under, or {@code null} to save under a new document.
     * @param docRev The document revision to save the attachment under, or {@code null} when saving to a new document.
     * @throws DocumentConflictException
     * @return {@link Response}
     */
    public Response saveAttachment(InputStream in, String name, String contentType, String docId, String docRev) {
        assertNotEmpty(in, "in");
        assertNotEmpty(name, "name");
        assertNotEmpty(contentType, "ContentType");
        assertNotEmpty(docId, "docId");
        final URI uri = buildUri(getDBUri()).path(docId).path("/").path(name).query("rev", docRev).build();
        return client.put(uri, in, contentType);
    }

    /**
     * Invokes an Update Handler.
     * <pre>
     * String query = "field=foo&value=bar";
     * String output = dbClient.invokeUpdateHandler("designDoc/update1", "docId", query);
     * </pre>
     * @param updateHandlerUri The Update Handler URI, in the format: <code>designDoc/update1</code>
     * @param docId The document id to update.
     * @param query The query string parameters, e.g, <code>field=field1&value=value1</code>
     * @return The output of the request.
     * @deprecated Use {@link #invokeUpdateHandler(String, String, Params)} instead.
     */
    @Deprecated
    public String invokeUpdateHandler(String updateHandlerUri, String docId, String query) {
        assertNotEmpty(updateHandlerUri, "uri");
        assertNotEmpty(docId, "docId");
        final String[] v = updateHandlerUri.split("/");
        final String path = String.format("_design/%s/_update/%s/", v[0], v[1]);
        final URI uri = buildUri(getDBUri()).path(path).path(docId).query(query).build();
        final HttpResponse response = client.executeRequest(new HttpPut(uri));
        return streamToString(getStream(response));
    }

    /**
     * Invokes an Update Handler.
     * <pre>
     * Params params = new Params()
     *	.addParam("field", "foo")
     *	.addParam("value", "bar");
     * String output = dbClient.invokeUpdateHandler("designDoc/update1", "docId", params);
     * </pre>
     * @param updateHandlerUri The Update Handler URI, in the format: <code>designDoc/update1</code>
     * @param docId The document id to update.
     * @param params The query parameters as {@link Params}.
     * @return The output of the request.
     */
    public String invokeUpdateHandler(String updateHandlerUri, String docId, Params params) {
        assertNotEmpty(updateHandlerUri, "uri");
        assertNotEmpty(docId, "docId");
        final String[] v = updateHandlerUri.split("/");
        final String path = String.format("_design/%s/_update/%s/", v[0], v[1]);
        final URI uri = buildUri(getDBUri()).path(path).pathToEncode(docId).query(params).buildEncoded();
        final HttpResponse response = client.executeRequest(new HttpPut(uri));
        return streamToString(getStream(response));
    }



    /**
     * Synchronize all design documents with the database.
     */
    public void syncDesignDocsWithDb() {
        design().synchronizeAllWithDb();
    }




    /**
     * @return The database URI.
     */
    public URI getDBUri() {
        return dbURI;
    }
    


    // End - Public API




    /**
     * @return {@link CouchDbInfo} Containing the DB info.
     */
    public CouchDbInfo info() {
        return client.get(buildUri(getDBUri()).build(), CouchDbInfo.class);
    }

    /**
     * Triggers a database <i>compact</i> request.
     */
    public void compact() {
        HttpResponse response = null;
        try {
            response = client.post(buildUri(getDBUri()).path("_compact").build(), "");
        } finally {
            close(response);
        }
    }

    /**
     * Requests the database commits any recent changes to disk.
     */
    public void ensureFullCommit() {
        HttpResponse response = null;
        try {
            response = client.post(buildUri(getDBUri()).path("_ensure_full_commit").build(), "");
        } finally {
            close(response);
        }
    }


    /**
     * @return the dbName
     */
    public String getDbName() {
        return dbName;
    }


    private void connect(boolean create) {

        InputStream getresp = null;
        HttpResponse putresp = null;

        try {
            getresp = get(dbURI);
        } catch (NoDocumentException e) { // db doesn't exist
            if (!create) {
                throw e;
            }
            final HttpPut put = new HttpPut(dbURI);
            putresp = client.executeRequest(put);
            log.info(String.format("Created Database: '%s'", dbName));
        } finally {
            close(getresp);
            close(putresp);
        }
    }

    // Helpers
    Gson getGson() {
        return client.getGson();
    }


    InputStream get(HttpGet httpGet) {
        return client.get(httpGet);
    }


    <T> T get(URI uri, Class<T> classType) {
        return client.get(uri, classType);
    }


    InputStream get(URI uri) {
        return client.get(uri);
    }

    HttpResponse post(URI uri, String json) {
        return client.post(uri, json);
    }
}
