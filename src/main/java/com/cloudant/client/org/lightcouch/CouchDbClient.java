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
import static com.cloudant.client.org.lightcouch.internal.URIBuilder.buildUri;

import com.cloudant.client.org.lightcouch.internal.GsonHelper;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;
import com.cloudant.http.internal.DefaultHttpUrlConnectionFactory;
import com.cloudant.http.internal.ok.OkHttpClientHttpUrlConnectionFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.ConnectionPool;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
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

    private URI baseURI;

    private Gson gson;

    private List<HttpConnectionRequestInterceptor> requestInterceptors;
    private List<HttpConnectionResponseInterceptor> responseInterceptors;

    private HttpConnection.HttpUrlConnectionFactory factory =
            (OkHttpClientHttpUrlConnectionFactory.isOkUsable())
                    ? new OkHttpClientHttpUrlConnectionFactory()
                    : new DefaultHttpUrlConnectionFactory();

    CouchDbClient(CouchDbConfig config) {
        final CouchDbProperties props = config.getProperties();

        this.gson = GsonHelper.initGson(new GsonBuilder()).create();
        this.baseURI = buildUri().scheme(props.getProtocol()).host(props.getHost()).port(props
                .getPort()).path("/").build();

        //if OkHttp is available then use it for connection pooling, otherwise default to the
        //JVM built-in pooling for HttpUrlConnection
        if (OkHttpClientHttpUrlConnectionFactory.isOkUsable() && props.getMaxConnections() > 0) {
            OkHttpClientHttpUrlConnectionFactory factory = new
                    OkHttpClientHttpUrlConnectionFactory();
            //keep connections open for as long as possible, anything over 2.5 minutes will be
            //longer than the server
            ConnectionPool pool = new ConnectionPool(props.getMaxConnections(), TimeUnit.MINUTES
                    .toMillis(3));
            factory.getOkHttpClient().setConnectionPool(pool);
            this.factory = factory;
        } else {
            factory = new DefaultHttpUrlConnectionFactory();
        }

        //set the proxy if it has been configured
        if (props.getProxyURL() != null) {
            factory.setProxy(props.getProxyURL());
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
    }

    /**
     * @return The base URI.
     */
    public URI getBaseUri() {
        return baseURI;
    }


    /**
     * Get an instance of Database class to perform DB operations
     *
     * @param name   The name of the database
     * @param create Should the database be created if it doesn't exist
     * @throws Exception If the database doesn't exist and create is false, an exception is raised
     */
    //abstract CouchDatabaseBase database(String name, boolean create);

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
        delete(buildUri(getBaseUri()).path(dbName).build());
    }

    /**
     * Requests CouchDB creates a new database; if one doesn't exist.
     *
     * @param dbName The Database name
     */
    public void createDB(String dbName) {
        assertNotEmpty(dbName, "dbName");
        InputStream is = null;
        final URI uri = buildUri(getBaseUri()).path(dbName).build();
        try {
            is = get(uri);
        } catch (NoDocumentException e) { // db doesn't exist
            is = put(uri);
            log.info(String.format("Created Database: '%s'", dbName));
        } finally {
            close(is);
        }
    }

    /**
     * @return All Server databases.
     */
    public List<String> getAllDbs() {
        InputStream instream = null;
        try {
            Type typeOfList = new TypeToken<List<String>>() {
            }.getType();
            instream = get(buildUri(getBaseUri()).path("_all_dbs").build());
            Reader reader = new InputStreamReader(instream, "UTF-8");
            return getGson().fromJson(reader, typeOfList);
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
        InputStream instream = null;
        try {
            instream = get(buildUri(getBaseUri()).build());
            Reader reader = new InputStreamReader(instream, "UTF-8");
            return getAsString(new JsonParser().parse(reader).getAsJsonObject(), "version");
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
     * Request a database sends a list of UUIDs.
     *
     * @param count The count of UUIDs.
     */
    public List<String> uuids(long count) {
        final String uri = String.format("%s_uuids?count=%d", getBaseUri(), count);
        final JsonObject json = get(URI.create(uri), JsonObject.class);
        return getGson().fromJson(json.get("uuids").toString(), new TypeToken<List<String>>() {
        }.getType());
    }


    /**
     * Executes a HTTP request.
     * <p><b>Note</b>: The stream must be closed after use to release the connection.
     *
     * @param connection The HTTP request to execute.
     * @return Class type of object T (i.e. {@link Response}
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
     * Performs a HTTP GET request.
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
        return getResponse(response, classType, getGson());
    }

    /**
     * Performs a HTTP HEAD request.
     *
     * @return {@link Response}
     */
    InputStream head(URI uri) {
        HttpConnection connection = Http.HEAD(uri);
        return executeToInputStream(connection);
    }

    /**
     * Performs a HTTP PUT request, saves or updates a document.
     * This defaults to "application/json" content type.
     *
     * @return Input stream with response
     */
    InputStream put(URI uri) {
        return put(uri, "application/json");
    }

    /**
     * Performs a HTTP PUT request with content type, saves or updates a document.
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
            httpUri = buildUri(uri).pathToEncode(id).query("w",
                    writeQuorum).buildEncoded();
        } else {
            httpUri = buildUri(uri).pathToEncode(id).buildEncoded();
        }
        HttpConnection connection = Http.PUT(httpUri, "application/json");
        connection.setRequestBody(json.toString());

        return executeToResponse(connection);
    }

    /**
     * Performs a HTTP POST request with JSON request body.
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
            connection = connection.execute();
            int code = connection.getConnection().getResponseCode();
            String response = connection.getConnection().getResponseMessage();
            // everything ok? return the stream
            if (code / 100 == 2) { // success [200,299]
                return connection;
            } else {
                CouchDbException ex = new CouchDbException(response, code);
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
                }
                es = connection.getConnection().getErrorStream();
                //if there is an error stream try to deserialize into the typed exception
                if (es != null) {
                    Class<? extends CouchDbException> exceptionClass = ex.getClass();
                    try {
                        ex = getGson().fromJson(new InputStreamReader(es, "UTF-8"),
                                exceptionClass);
                    } catch (JsonParseException e) {
                        //suppress and just throw ex momentarily
                    }
                }
                //set the status code for the cases where we may not have already
                ex.setStatusCode(code);
                throw ex;
            }
        } catch (IOException ioe) {
            throw new CouchDbException("Error retrieving server response", ioe);
        } finally {
            close(es);
        }
    }

    /**
     * Execute the HttpConnection request and return the InputStream if there were no errors.
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
}
