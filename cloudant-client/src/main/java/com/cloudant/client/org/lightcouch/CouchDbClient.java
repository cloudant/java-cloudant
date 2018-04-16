/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

import com.cloudant.client.api.model.MetaInformation;
import com.cloudant.client.api.scheduler.SchedulerDocsResponse;
import com.cloudant.client.api.scheduler.SchedulerJobsResponse;
import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.client.internal.URIBase;
import com.cloudant.client.internal.util.DeserializationTypes;
import com.cloudant.client.org.lightcouch.internal.GsonHelper;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;
import com.cloudant.http.internal.interceptors.HttpConnectionInterceptorException;
import com.cloudant.http.internal.DefaultHttpUrlConnectionFactory;
import com.cloudant.http.internal.ok.OkHelper;
import com.cloudant.http.internal.ok.OkHttpClientHttpUrlConnectionFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.apache.commons.io.IOUtils;

import okhttp3.ConnectionPool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


/**
 * Contains a Client Public API implementation.
 *
 * @author Ahmed Yehia
 * @see CouchDbClient
 */

public class CouchDbClient {

    static final Logger log = Logger.getLogger(CouchDbClient.class.getCanonicalName());

    private Gson gson;

    private URI clientUri;

    private List<HttpConnectionRequestInterceptor> requestInterceptors;
    private List<HttpConnectionResponseInterceptor> responseInterceptors;

    private final HttpConnection.HttpUrlConnectionFactory factory;

    CouchDbClient(CouchDbConfig config) {
        final CouchDbProperties props = config.getProperties();

        try {
            this.clientUri = props.getCouchDbURL().toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Error converting account URL to URI.", e);
        }

        this.gson = GsonHelper.initGson(new GsonBuilder()).create();

        // If OkHttp is available then use it for connection pooling, otherwise default to the
        // JVM built-in pooling for HttpUrlConnection
        if (OkHelper.isOkUsable()) {
            log.config("Using OkHttp");
            OkHttpClientHttpUrlConnectionFactory factory = new
                    OkHttpClientHttpUrlConnectionFactory();
            final int maxConns = props.getMaxConnections();
            if (maxConns > 0) {
                log.config("Setting max connections to " + maxConns);
                //keep connections open for as long as possible, anything over 2.5 minutes will be
                //longer than the server so we'll use a 3 minute timeout
                ConnectionPool pool = new ConnectionPool(maxConns, 3l, TimeUnit
                        .MINUTES);
                factory.getOkHttpClientBuilder().connectionPool(pool);
            }
            this.factory = factory;
        } else {
            log.config("Using built-in HttpUrlConnection");
            this.factory = new DefaultHttpUrlConnectionFactory();
        }

        //set the proxy if it has been configured
        if (props.getProxyURL() != null) {
            factory.setProxy(props.getProxyURL());
            if (props.getProxyAuthentication() != null) {
                factory.setProxyAuthentication(props.getProxyAuthentication());
            }
        }

        this.requestInterceptors = new ArrayList<HttpConnectionRequestInterceptor>();
        this.responseInterceptors = new ArrayList<HttpConnectionResponseInterceptor>();

        if (props.getRequestInterceptors() != null) {
            this.requestInterceptors.addAll(props.getRequestInterceptors());
        }

        if (props.getResponseInterceptors() != null) {
            this.responseInterceptors.addAll(props.getResponseInterceptors());
        }
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param properties An object containing configuration properties.
     * @see {@link CouchDbProperties}
     */
    public CouchDbClient(CouchDbProperties properties) {
        this(new CouchDbConfig(properties));
    }

    /**
     * Shuts down and releases resources used by this couchDbClient instance.
     * Note: Apache's httpclient was replaced by HttpUrlConnection.
     * Connection manager is no longer used.
     */
    public void shutdown() {
        // Delete the cookie _session if there is one
        Response response = executeToResponse(Http.DELETE(new URIBase(clientUri).path("_session")
                .build()));
        if (!response.isOk()) {
            log.warning("Error deleting session on client shutdown.");
        }
        // The execute method handles non-2xx response codes by throwing a CouchDbException.

        factory.shutdown();
    }

    /**
     * @return The base URI.
     */
    public URI getBaseUri() {
        return new URIBase(clientUri).getUri();
    }

    /**
     * Get an instance of Database class to perform DB operations
     *
     * @param name   name of database to access
     * @param create flag indicating whether to create the database if it doesn't exist.
     * @return CouchDatabase object
     */
    public CouchDatabase database(String name, boolean create) {
        return new CouchDatabase(this, name, create);
    }

    /**
     * Requests CouchDB deletes a database.
     *
     * @param dbName  The database name
     * @param confirm A confirmation string with the value: <tt>delete database</tt>
     * @Deprecated Use {@link CouchDbClient#deleteDB(String)}
     */
    @Deprecated
    public void deleteDB(String dbName, String confirm) {
        if (!"delete database".equals(confirm)) {
            throw new IllegalArgumentException("Invalid confirm!");
        }
        deleteDB(dbName);
    }

    /**
     * Requests CouchDB deletes a database.
     *
     * @param dbName The database name
     */
    public void deleteDB(String dbName) {
        assertNotEmpty(dbName, "dbName");
        delete(new DatabaseURIHelper(getBaseUri(), dbName).getDatabaseUri());

    }

    /**
     * Requests CouchDB creates a new database; if one doesn't exist.
     *
     * @param dbName The Database name
     */
    public void createDB(String dbName) {
        assertNotEmpty(dbName, "dbName");
        final URI uri = new DatabaseURIHelper(getBaseUri(), dbName).getDatabaseUri();
        executeToResponse(Http.PUT(uri, "application/json"));
        log.info(String.format("Created Database: '%s'", dbName));
    }

    /**
     * @return All Server databases.
     */
    public List<String> getAllDbs() {
        InputStream instream = null;
        try {
            instream = get(new URIBase(clientUri).path("_all_dbs").build());
            Reader reader = new InputStreamReader(instream, "UTF-8");
            return getGson().fromJson(reader, DeserializationTypes.STRINGS);
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        } finally {
            close(instream);
        }
    }

    /**
     * @return DB Server version.
     */
    public String serverVersion() {
        return metaInformation().getVersion();
    }

    /**
     * @return DB Server metadata.
     */
    public MetaInformation metaInformation() {
        InputStream instream = null;
        try {
            instream = get(getBaseUri());
            Reader reader = new InputStreamReader(instream, "UTF-8");
            return getGson().fromJson(reader, MetaInformation.class);
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        } finally {
            close(instream);
        }
    }


    /**
     * Provides access to CouchDB <tt>replication</tt> APIs.
     *
     * @see Replication
     */
    public Replication replication() {
        return new Replication(this);
    }

    /**
     * Provides access to the <tt>replicator database</tt>.
     *
     * @see Replicator
     */
    public Replicator replicator() {
        return new Replicator(this);
    }


    /**
     * Lists replication jobs. Includes replications created via /_replicate endpoint as well as
     * those created from replication documents. Does not include replications which have
     * completed or have failed to start because replication documents were malformed. Each job
     * description will include source and target information, replication id, a history of
     * recent event, and a few other things.
     *
     * @return All current replication jobs
     */
    public SchedulerJobsResponse schedulerJobs() {
        return this.get(new DatabaseURIHelper(getBaseUri()).path("_scheduler").path("jobs").build(),
                SchedulerJobsResponse.class);
    }

    /**
     * Lists replication documents. Includes information about all the documents, even in
     * completed and failed states. For each document it returns the document ID, the database,
     * the replication ID, source and target, and other information.
     *
     * @return All replication documents
     */
    public SchedulerDocsResponse schedulerDocs() {
        return this.get(new DatabaseURIHelper(getBaseUri()).path("_scheduler").path("docs").build(),
                SchedulerDocsResponse.class);
    }

    /**
     * Get replication document state for a given replication document ID.
     *
     * @param docId The replication document ID
     * @return Replication document for {@code docId}
     */
    public SchedulerDocsResponse.Doc schedulerDoc(String docId) {
        assertNotEmpty(docId, "docId");
        return this.get(new DatabaseURIHelper(getBaseUri()).
                        path("_scheduler").path("docs").path("_replicator").path(docId).build(),
                SchedulerDocsResponse.Doc.class);
    }

    /**
     * Request a database sends a list of UUIDs.
     *
     * @param count The count of UUIDs.
     */
    public List<String> uuids(long count) {
        final URI uri = new URIBase(clientUri).path("_uuids").query("count", count).build();
        final JsonObject json = get(uri, JsonObject.class);
        return getGson().fromJson(json.get("uuids").toString(), DeserializationTypes.STRINGS);
    }

    /**
     * Executes a HTTP request and parses the JSON response into a Response instance.
     *
     * @param connection The HTTP request to execute.
     * @return Response object of the deserialized JSON response
     */
    public Response executeToResponse(HttpConnection connection) {
        InputStream is = null;
        try {
            is = this.executeToInputStream(connection);
            Response response = getResponse(is, Response.class, getGson());
            response.setStatusCode(connection.getConnection().getResponseCode());
            response.setReason(connection.getConnection().getResponseMessage());
            return response;
        } catch (IOException e) {
            throw new CouchDbException("Error retrieving response code or message.", e);
        } finally {
            close(is);
        }
    }

    /**
     * Performs a HTTP DELETE request.
     *
     * @return {@link Response}
     */
    Response delete(URI uri) {
        HttpConnection connection = Http.DELETE(uri);
        return executeToResponse(connection);
    }

    /**
     * <p>Performs a HTTP GET request.</p>
     * <p>The stream <b>must</b> be closed after use.</p>
     *
     * @return Input stream with response
     */
    public InputStream get(URI uri) {
        HttpConnection httpConnection = Http.GET(uri);
        return executeToInputStream(httpConnection);
    }

    /**
     * Performs a HTTP GET request.
     *
     * @return Class type of object T (i.e. {@link Response}
     */
    public <T> T get(URI uri, Class<T> classType) {
        HttpConnection connection = Http.GET(uri);
        InputStream response = executeToInputStream(connection);
        try {
            return getResponse(response, classType, getGson());
        } finally {
            close(response);
        }
    }

    /**
     * <p>Performs a HTTP HEAD request.</p>
     * <p>The stream <b>must</b> be closed after use.</p>
     *
     * @return {@link Response}
     */
    InputStream head(URI uri) {
        HttpConnection connection = Http.HEAD(uri);
        return executeToInputStream(connection);
    }

    /**
     * <p>Performs a HTTP PUT request, saves or updates a document.
     * This defaults to "application/json" content type.</p>
     * <p>The stream <b>must</b> be closed after use.</p>
     *
     * @return Input stream with response
     */
    InputStream put(URI uri) {
        return put(uri, "application/json");
    }

    /**
     * <p>Performs a HTTP PUT request with content type, saves or updates a document.</p>
     * <p>The stream <b>must</b> be closed after use.</p>
     *
     * @return Input stream with response
     */
    InputStream put(URI uri, String contentType) {
        HttpConnection connection = Http.PUT(uri, contentType);
        return executeToInputStream(connection);
    }

    /**
     * Performs a HTTP PUT request, saves an attachment.
     *
     * @return {@link Response}
     */
    Response put(URI uri, InputStream instream, String contentType) {
        HttpConnection connection = Http.PUT(uri, contentType);

        connection.setRequestBody(instream);

        return executeToResponse(connection);
    }

    /**
     * Performs a HTTP PUT request, saves or updates a document.
     *
     * @param object    Object for updating request
     * @param newEntity If true, saves a new document. Else, updates an existing one.
     * @return {@link Response}
     */
    public Response put(URI uri, Object object, boolean newEntity) {
        return put(uri, object, newEntity, -1);
    }

    /**
     * Performs a HTTP PUT request, saves or updates a document.
     *
     * @param object    Object for updating request
     * @param newEntity If true, saves a new document. Else, updates an existing one.
     * @return {@link Response}
     */
    public Response put(URI uri, Object object, boolean newEntity, int writeQuorum) {
        assertNotEmpty(object, "object");
        final JsonObject json = getGson().toJsonTree(object).getAsJsonObject();
        String id = getAsString(json, "_id");
        String rev = getAsString(json, "_rev");
        if (newEntity) { // save
            assertNull(rev, "rev");
            id = (id == null) ? generateUUID() : id;
        } else { // update
            assertNotEmpty(id, "id");
            assertNotEmpty(rev, "rev");
        }
        URI httpUri = null;
        if (writeQuorum > -1) {
            httpUri = new DatabaseURIHelper(uri).documentUri(id, "w", writeQuorum);
        } else {
            httpUri = new DatabaseURIHelper(uri).documentUri(id);
        }
        HttpConnection connection = Http.PUT(httpUri, "application/json");
        connection.setRequestBody(json.toString());

        return executeToResponse(connection);
    }

    /**
     * <p>Performs a HTTP POST request with JSON request body.</p>
     * <p>The stream <b>must</b> be closed after use.</p>
     *
     * @return Input stream with response
     */
    public InputStream post(URI uri, String json) {
        HttpConnection connection = Http.POST(uri,
                "application/json");
        if (json != null && !json.isEmpty()) {
            connection.setRequestBody(json);
        }
        return executeToInputStream(connection);
    }

    // Helpers

    /**
     * Sets a {@link GsonBuilder} to create {@link Gson} instance.
     * <p>Useful for registering custom serializers/deserializers, such as JodaTime classes.
     */
    public void setGsonBuilder(GsonBuilder gsonBuilder) {
        this.gson = GsonHelper.initGson(gsonBuilder).create();
    }


    /**
     * @return The Gson instance.
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Execute a HTTP request and handle common error cases.
     *
     * @param connection the HttpConnection request to execute
     * @return the executed HttpConnection
     * @throws CouchDbException for HTTP error codes or if an IOException was thrown
     */
    public HttpConnection execute(HttpConnection connection) {

        //set our HttpUrlFactory on the connection
        connection.connectionFactory = factory;

        // all CouchClient requests want to receive application/json responses
        connection.requestProperties.put("Accept", "application/json");
        connection.responseInterceptors.addAll(this.responseInterceptors);
        connection.requestInterceptors.addAll(this.requestInterceptors);
        InputStream es = null; // error stream - response from server for a 500 etc

        // first try to execute our request and get the input stream with the server's response
        // we want to catch IOException because HttpUrlConnection throws these for non-success
        // responses (eg 404 throws a FileNotFoundException) but we need to map to our own
        // specific exceptions
        try {
            try {
                connection = connection.execute();
            } catch (HttpConnectionInterceptorException e) {
                CouchDbException exception = new CouchDbException(connection.getConnection()
                        .getResponseMessage(), connection.getConnection().getResponseCode());
                if (e.deserialize) {
                    try {
                        JsonObject errorResponse = new Gson().fromJson(e.error, JsonObject
                                .class);
                        exception.error = getAsString(errorResponse, "error");
                        exception.reason = getAsString(errorResponse, "reason");
                    } catch (JsonParseException jpe) {
                        exception.error = e.error;
                    }
                } else {
                    exception.error = e.error;
                    exception.reason = e.reason;
                }
                throw exception;
            }
            int code = connection.getConnection().getResponseCode();
            String response = connection.getConnection().getResponseMessage();
            // everything ok? return the stream
            if (code / 100 == 2) { // success [200,299]
                return connection;
            } else {
                final CouchDbException ex;
                switch (code) {
                    case HttpURLConnection.HTTP_NOT_FOUND: //404
                        ex = new NoDocumentException(response);
                        break;
                    case HttpURLConnection.HTTP_CONFLICT: //409
                        ex = new DocumentConflictException(response);
                        break;
                    case HttpURLConnection.HTTP_PRECON_FAILED: //412
                        ex = new PreconditionFailedException(response);
                        break;
                    case 429:
                        // If a Replay429Interceptor is present it will check for 429 and retry at
                        // intervals. If the retries do not succeed or no 429 replay was configured
                        // we end up here and throw a TooManyRequestsException.
                        ex = new TooManyRequestsException(response);
                        break;
                    default:
                        ex = new CouchDbException(response, code);
                        break;
                }
                es = connection.getConnection().getErrorStream();
                //if there is an error stream try to deserialize into the typed exception
                if (es != null) {
                    try {
                        //read the error stream into memory
                        byte[] errorResponse = IOUtils.toByteArray(es);

                        Class<? extends CouchDbException> exceptionClass = ex.getClass();
                        //treat the error as JSON and try to deserialize
                        try {
                            // Register an InstanceCreator that returns the existing exception so
                            // we can just populate the fields, but not ignore the constructor.
                            // Uses a new Gson so we don't accidentally recycle an exception.
                            Gson g = new GsonBuilder().registerTypeAdapter(exceptionClass, new
                                    CouchDbExceptionInstanceCreator(ex)).create();
                            // Now populate the exception with the error/reason other info from JSON
                            g.fromJson(new InputStreamReader(new ByteArrayInputStream
                                    (errorResponse),
                                    "UTF-8"), exceptionClass);
                        } catch (JsonParseException e) {
                            // The error stream was not JSON so just set the string content as the
                            // error field on ex before we throw it
                            ex.error = new String(errorResponse, "UTF-8");
                        }
                    } finally {
                        close(es);
                    }
                }
                ex.setUrl(connection.url.toString());
                throw ex;
            }
        } catch (IOException ioe) {
            CouchDbException ex = new CouchDbException("Error retrieving server response", ioe);
            ex.setUrl(connection.url.toString());
            throw ex;
        }
    }

    /**
     * <p>Execute the HttpConnection request and return the InputStream if there were no errors.</p>
     * <p>The stream <b>must</b> be closed after use.</p>
     *
     * @param connection the request HttpConnection
     * @return InputStream from the HttpConnection response
     * @throws CouchDbException for HTTP error codes or if there was an IOException
     */
    public InputStream executeToInputStream(HttpConnection connection) throws CouchDbException {
        try {
            return execute(connection).responseAsInputStream();
        } catch (IOException ioe) {
            throw new CouchDbException("Error retrieving server response", ioe);
        }
    }

    private static final class CouchDbExceptionInstanceCreator implements
            InstanceCreator<CouchDbException> {

        private final CouchDbException ex;

        CouchDbExceptionInstanceCreator(CouchDbException ex) {
            this.ex = ex;
        }

        @Override
        public CouchDbException createInstance(Type type) {
            return ex;
        }
    }
}
