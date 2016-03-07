/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright (c) 2015 IBM Corp. All rights reserved.
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

package com.cloudant.client.org.lightcouch;

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNull;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.close;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.generateUUID;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getAsString;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponse;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponseList;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.streamToString;

import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

/**
 * Contains a Database Public API implementation.
 *
 * @author Ahmed Yehia
 * @see CouchDatabase
 */
public abstract class CouchDatabaseBase {

    static final Logger log = Logger.getLogger(CouchDatabase.class.getCanonicalName());

    CouchDbClient couchDbClient;
    private String dbName;
    private URI dbUri;
    private URI clientUri;

    CouchDatabaseBase(CouchDbClient client, String name, boolean create) {
        assertNotEmpty(name, "name");
        this.dbName = name;
        this.couchDbClient = client;
        this.clientUri = couchDbClient.getBaseUri();
        this.dbUri = new DatabaseURIHelper(clientUri, name).getDatabaseUri();
        if (create) {
            create();
        }
    }

    /**
     * Provides access to <tt>Change Notifications</tt> API.
     *
     * @see Changes
     */
    public Changes changes() {
        return new Changes(this);
    }

    /**
     * Finds an Object of the specified type.
     *
     * @param <T>       Object type.
     * @param classType The class of type T.
     * @param id        The document id.
     * @return An object of type T.
     * @throws NoDocumentException If the document is not found in the database.
     */
    public <T> T find(Class<T> classType, String id) {
        assertNotEmpty(classType, "Class");
        assertNotEmpty(id, "id");
        final URI uri = new DatabaseURIHelper(dbUri).documentUri(id);
        return couchDbClient.get(uri, classType);
    }

    /**
     * Finds an Object of the specified type.
     *
     * @param <T>       Object type.
     * @param classType The class of type T.
     * @param id        The document id.
     * @param params    Extra parameters to append.
     * @return An object of type T.
     * @throws NoDocumentException If the document is not found in the database.
     */
    public <T> T find(Class<T> classType, String id, Params params) {
        assertNotEmpty(classType, "Class");
        assertNotEmpty(id, "id");
        final URI uri = new DatabaseURIHelper(dbUri).documentUri(id, params);
        return couchDbClient.get(uri, classType);
    }

    /**
     * Finds an Object of the specified type.
     *
     * @param <T>       Object type.
     * @param classType The class of type T.
     * @param id        The document _id field.
     * @param rev       The document _rev field.
     * @return An object of type T.
     * @throws NoDocumentException If the document is not found in the database.
     */
    public <T> T find(Class<T> classType, String id, String rev) {
        assertNotEmpty(classType, "Class");
        assertNotEmpty(id, "id");
        assertNotEmpty(id, "rev");
        final URI uri = new DatabaseURIHelper(dbUri).documentUri(id, "rev", rev);
        return couchDbClient.get(uri, classType);
    }

    /**
     * This method finds any document given a URI.
     * <p>The URI must be URI-encoded.
     *
     * @param classType The class of type T.
     * @param uri       The URI as string.
     * @return An object of type T.
     */
    public <T> T findAny(Class<T> classType, String uri) {
        assertNotEmpty(classType, "Class");
        assertNotEmpty(uri, "uri");
        return couchDbClient.get(URI.create(uri), classType);
    }

    /**
     * Finds a document and return the result as {@link InputStream}.
     * <p><b>Note</b>: The stream must be closed after use to release the connection.
     *
     * @param id The document _id field.
     * @return The result as {@link InputStream}
     * @throws NoDocumentException If the document is not found in the database.
     * @see #find(String, String)
     */
    public InputStream find(String id) {
        assertNotEmpty(id, "id");
        return couchDbClient.get(new DatabaseURIHelper(dbUri).documentUri(id));
    }

    /**
     * Finds a document given id and revision and returns the result as {@link InputStream}.
     * <p><b>Note</b>: The stream must be closed after use to release the connection.
     *
     * @param id  The document _id field.
     * @param rev The document _rev field.
     * @return The result as {@link InputStream}
     * @throws NoDocumentException If the document is not found in the database.
     */
    public InputStream find(String id, String rev) {
        assertNotEmpty(id, "id");
        assertNotEmpty(rev, "rev");
        final URI uri = new DatabaseURIHelper(dbUri).documentUri(id, "rev", rev);
        return couchDbClient.get(uri);
    }

    /**
     * Checks if a document exist in the database.
     *
     * @param id The document _id field.
     * @return true If the document is found, false otherwise.
     */
    public boolean contains(String id) {
        assertNotEmpty(id, "id");
        InputStream response = null;
        try {
            response = couchDbClient.head(new DatabaseURIHelper(dbUri).documentUri(id));
        } catch (NoDocumentException e) {
            return false;
        } finally {
            close(response);
        }
        return true;
    }

    /**
     * Saves an object in the database, using HTTP <tt>PUT</tt> request.
     * <p>If the object doesn't have an <code>_id</code> value, the code will assign a
     * <code>UUID</code> as the document id.
     *
     * @param object The object to save
     * @return {@link Response}
     * @throws DocumentConflictException If a conflict is detected during the save.
     */
    public Response save(Object object) {
        return couchDbClient.put(getDBUri(), object, true);
    }

    /**
     * Saves an object in the database using HTTP <tt>POST</tt> request.
     * <p>The database will be responsible for generating the document id.
     *
     * @param object The object to save
     * @return {@link Response}
     */
    public Response post(Object object) {
        assertNotEmpty(object, "object");
        InputStream response = null;
        try {
            response = couchDbClient.post(new DatabaseURIHelper(dbUri).getDatabaseUri(),
                    getGson().toJson(object));
            return getResponse(response, Response.class, getGson());
        } finally {
            close(response);
        }
    }

    /**
     * Updates an object in the database, the object must have the correct <code>_id</code> and
     * <code>_rev</code> values.
     *
     * @param object The object to update
     * @return {@link Response}
     * @throws DocumentConflictException If a conflict is detected during the update.
     */
    public Response update(Object object) {
        return couchDbClient.put(getDBUri(), object, false);
    }

    /**
     * Removes a document from the database.
     * <p>The object must have the correct <code>_id</code> and <code>_rev</code> values.
     *
     * @param object The document to remove as object.
     * @return {@link Response}
     * @throws NoDocumentException If the document is not found in the database.
     */
    public Response remove(Object object) {
        assertNotEmpty(object, "object");
        JsonObject jsonObject = getGson().toJsonTree(object).getAsJsonObject();
        final String id = getAsString(jsonObject, "_id");
        final String rev = getAsString(jsonObject, "_rev");
        return remove(id, rev);
    }

    /**
     * Removes a document from the database given both a document <code>_id</code> and
     * <code>_rev</code> values.
     *
     * @param id  The document _id field.
     * @param rev The document _rev field.
     * @return {@link Response}
     * @throws NoDocumentException If the document is not found in the database.
     */
    public Response remove(String id, String rev) {
        assertNotEmpty(id, "id");
        assertNotEmpty(rev, "rev");
        final URI uri = new DatabaseURIHelper(dbUri).documentUri(id, rev);
        return couchDbClient.delete(uri);
    }

    /**
     * Performs a Bulk Documents insert request.
     *
     * @param objects      The {@link List} of objects.
     * @param allOrNothing Indicates whether the request has <tt>all-or-nothing</tt> semantics.
     * @return {@code List<Response>} Containing the resulted entries.
     */
    public List<Response> bulk(List<?> objects, boolean allOrNothing) {
        assertNotEmpty(objects, "objects");
        InputStream responseStream = null;
        HttpConnection connection;
        try {
            final JsonObject jsonObject = new JsonObject();
            if(allOrNothing) {
                jsonObject.addProperty("all_or_nothing", true);
            }
            final URI uri = new DatabaseURIHelper(dbUri).bulkDocsUri();
            jsonObject.add("docs", getGson().toJsonTree(objects));
            connection = Http.POST(uri, "application/json");
            if (jsonObject.toString().length() != 0) {
                connection.setRequestBody(jsonObject.toString());
            }
            couchDbClient.execute(connection);
            responseStream = connection.responseAsInputStream();
            List<Response> bulkResponses = getResponseList(responseStream, getGson(),
                    new TypeToken<List<Response>>() {
                    }.getType());
            for(Response response : bulkResponses) {
                response.setStatusCode(connection.getConnection().getResponseCode());
                response.setReason(connection.getConnection().getResponseMessage());
            }
            return bulkResponses;
        }
        catch (IOException e) {
            throw new CouchDbException("Error retrieving response input stream.", e);
        } finally {
            close(responseStream);
        }
    }

    /**
     * Saves an attachment to a new document with a generated <tt>UUID</tt> as the document id.
     * <p>To retrieve an attachment, see {@link #find(String)}.
     *
     * @param in          The {@link InputStream} holding the binary data.
     * @param name        The attachment name.
     * @param contentType The attachment "Content-Type".
     * @return {@link Response}
     */
    public Response saveAttachment(InputStream in, String name, String contentType) {
        return saveAttachment(in, name, contentType, null, null);
    }

    /**
     * Saves an attachment to an existing document given both a document id
     * and revision, or save to a new document given only the id, and rev as {@code null}.
     * <p>To retrieve an attachment, see {@link #find(String)}.
     *
     * @param in          The {@link InputStream} holding the binary data.
     * @param name        The attachment name.
     * @param contentType The attachment "Content-Type".
     * @param docId       The document id to save the attachment under, or {@code null} to save
     *                    under a new document.
     * @param docRev      The document revision to save the attachment under, or {@code null}
     *                    when saving to a new document.
     * @return {@link Response}
     * @throws DocumentConflictException
     */
    public Response saveAttachment(InputStream in, String name, String contentType, String docId,
                                   String docRev) {
        assertNotEmpty(in, "in");
        assertNotEmpty(name, "name");
        assertNotEmpty(contentType, "ContentType");
        if (docId == null) {
            docId = generateUUID();
            // A new doc is being created; there should be no revision specified.
            assertNull(docRev, "docRev");
        } else {
            // The id has been specified, ensure it is not empty
            assertNotEmpty(docId, "docId");
            if (docRev != null) {
                // Existing doc with the specified ID, ensure rev is not empty
                assertNotEmpty(docRev, "docRev");
            }
        }
        final URI uri = new DatabaseURIHelper(dbUri).attachmentUri(docId, docRev, name);
        return couchDbClient.put(uri, in, contentType);
    }

    /**
     * Invokes an Update Handler.
     * <pre>
     * Params params = new Params()
     * 	.addParam("field", "foo")
     * 	.addParam("value", "bar");
     * String output = dbClient.invokeUpdateHandler("designDoc/update1", "docId", params);
     * </pre>
     *
     * @param updateHandlerUri The Update Handler URI, in the format: <code>designDoc/update1</code>
     * @param docId            The document id to update.
     * @param params           The query parameters as {@link Params}.
     * @return The output of the request.
     */
    public String invokeUpdateHandler(String updateHandlerUri, String docId, Params params) {
        assertNotEmpty(updateHandlerUri, "uri");
        final String[] v = updateHandlerUri.split("/");
        final InputStream response;
        final URI uri;
        DatabaseURIHelper uriHelper = new DatabaseURIHelper(dbUri).path("_design").path(v[0])
                .path("_update").path(v[1]).query(params);
        if (docId != null && !docId.isEmpty()) {
            //Create PUT request using doc Id
            uri = uriHelper.path(docId).build();
            response = couchDbClient.put(uri);
        } else {
            //If no doc Id, create POST request
            uri = uriHelper.build();
            response = couchDbClient.post(uri, null);
        }
        return streamToString(response);
    }

    /**
     * @return The database URI.
     */
    public URI getDBUri() {
        return new DatabaseURIHelper(dbUri).getDatabaseUri();
    }


    // End - Public API


    /**
     * @return {@link CouchDbInfo} Containing the DB info.
     */
    public CouchDbInfo info() {
        return couchDbClient.get(new DatabaseURIHelper(dbUri).getDatabaseUri(), CouchDbInfo.class);
    }

    /**
     * Triggers a database <i>compact</i> request.
     */
    public void compact() {
        InputStream response = null;
        try {
            response = couchDbClient.post(new DatabaseURIHelper(dbUri).path("_compact").build(),
                    "");
        } finally {
            close(response);
        }
    }

    /**
     * Requests the database commits any recent changes to disk.
     */
    public void ensureFullCommit() {
        InputStream response = null;
        try {
            response = couchDbClient.post(new DatabaseURIHelper(dbUri).path("_ensure_full_commit")
                    .build(), "");
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


    private void create() {
        InputStream putresp = null;
        try {
            putresp = couchDbClient.put(new DatabaseURIHelper(clientUri, dbName).build());
            log.info(String.format("Created Database: '%s'", dbName));
        } catch (PreconditionFailedException e) {
            // The PreconditionFailedException is thrown if the database already existed.
            // To preserve the behaviour of the previous version, suppress this type of exception.
            // All other CouchDbExceptions will be thrown.
        } finally {
            close(putresp);
        }
    }

    // helper
    private Gson getGson() {
        return couchDbClient.getGson();
    }
}
