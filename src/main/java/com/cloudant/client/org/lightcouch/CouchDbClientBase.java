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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Contains a Client Public API implementation.
 *
 * @author Ahmed Yehia
 * @see CouchDbClient
 */

public abstract class CouchDbClientBase {

    static final Log log = LogFactory.getLog(CouchDbClient.class);

    private URI baseURI;

    private Gson gson;

    private Map<String, String> customHeaders;

    private List<HttpConnectionRequestInterceptor> requestInterceptors;
    private List<HttpConnectionResponseInterceptor> responseInterceptors;

    CouchDbClientBase() {
        this(new CouchDbConfig());
    }

    CouchDbClientBase(CouchDbConfig config) {
        final CouchDbProperties props = config.getProperties();

        this.gson = GsonHelper.initGson(new GsonBuilder()).create();
        final String path = props.getPath() != null ? props.getPath() : "";
        //Add username and password if authentication info exists
        if (props.getUserInfo() != null && !props.getUserInfo().isEmpty()) {
            this.baseURI = buildUri().scheme(props.getProtocol()).userInfo(props.getUserInfo())
                    .host(props.getHost()).port(props.getPort())
                    .path("/").path(path).build();
        } else {
            this.baseURI = buildUri().scheme(props.getProtocol()).host(props.getHost()).port(props
                    .getPort()).path("/").path(path).build();
        }

        this.requestInterceptors = new ArrayList<HttpConnectionRequestInterceptor>();
        this.responseInterceptors = new ArrayList<HttpConnectionResponseInterceptor>();

        if(props.getRequestInterceptors() != null) {
            this.requestInterceptors.addAll(props.getRequestInterceptors());
        }

        if(props.getResponseInterceptors() != null) {
            this.responseInterceptors.addAll(props.getResponseInterceptors());
        }


        props.clearPassword();
    }

    // Client(s) provided implementation

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
    abstract CouchDatabaseBase database(String name, boolean create);


    /**
     * Requests CouchDB deletes a database.
     *
     * @param dbName  The database name
     * @param confirm A confirmation string with the value: <tt>delete database</tt>
     * @Deprecated Use {@link CouchDbClientBase#deleteDB(String)}
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
    public <T> T executeRequest(HttpConnection connection, Class<T> classType) {
        InputStream is = null;
        try {
            is = this.executeToInputStream(connection);
            if(classType.isInstance(InputStream.class)) {
                return (T)is;
            } else {
                return getResponse(is, classType, getGson());
            }
        } catch (CouchDbException e) {
            if (e.getStatusCode() == 409) {
                throw new DocumentConflictException(e.toString());
            } else {
                throw e;
            }
        } finally {
            closeQuietly(is);
        }
    }


    /**
     * Performs a HTTP DELETE request.
     *
     * @return {@link Response}
     */
    Response delete(URI uri) {
        HttpConnection connection = Http.DELETE(uri);
        return executeRequest(connection, Response.class);
    }

    /**
     * Performs a HTTP GET request.
     *
     * @return Input stream with response
     */
    public InputStream get(URI uri) {
        HttpConnection httpConnection = Http.GET(uri);
        return executeRequest(httpConnection);
    }

    /**
     * Performs a HTTP GET request.
     *
     * @return Class type of object T (i.e. {@link Response}
     */
    public <T> T get(URI uri, Class<T> classType) {
        HttpConnection connection = Http.GET(uri);
        if(classType.isInstance(HttpConnection.class)) {
            return (T)connection;
        } else {
            //return executeToJsonObject(connection, classType);
            return executeRequest(connection, classType);
        }

    }

    /**
     * Performs a HTTP HEAD request.
     *
     * @return {@link Response}
     */
    InputStream head(URI uri) {
        HttpConnection connection = Http.HEAD(uri);
        return executeRequest(connection);
    }

    /**
     * Performs a HTTP PUT request, saves or updates a document.
     *
     * @return Input stream with response
     */
    InputStream put(URI uri) {
        return put(uri, null);
    }

    /**
     * Performs a HTTP PUT request with content type, saves or updates a document.
     *
     * @return Input stream with response
     */
    InputStream put(URI uri, String contentType) {
        HttpConnection connection = null;
        if(contentType != null) {
            connection = Http.PUT(uri, contentType);
        } else {
            connection = Http.PUT(uri, "application/json");
        }

        return executeRequest(connection);
    }

    /**
     * Performs a HTTP PUT request, saves an attachment.
     *
     * @return {@link Response}
     */
    Response put(URI uri, InputStream instream, String contentType) {
        HttpConnection connection;
        try {
            connection = Http.PUT(uri, contentType);

            StringWriter writer = new StringWriter();
            IOUtils.copy(instream, writer, "UTF-8");
            connection.setRequestBody(writer.toString());
            writer.close();

            return executeRequest(connection, Response.class);
        } catch (IOException e) {
            //Error happened while copying stream to string
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Performs a HTTP PUT request, saves or updates a document.
     *
     * @return {@link Response}
     */
    Response put(URI uri, Object object, boolean newEntity) {
        assertNotEmpty(object, "object");
        try {
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
            HttpConnection connection = Http.PUT(buildUri(uri).pathToEncode(id).buildEncoded(),
                    "application/json");
            connection.setRequestBody(json.toString());

            return executeRequest(connection, Response.class);
        } catch (CouchDbException e) {
            if (e.getStatusCode() == 409) {
                throw new DocumentConflictException(e.toString());
            } else {
                throw e;
            }
        }
    }

    /**
     * Performs a HTTP POST request with JSON request body.
     *
     * @return Input stream with response
     */
    InputStream post(URI uri, String json) {
        HttpConnection connection = Http.POST(uri,
                "application/json");
        if(json != null && !json.isEmpty()) {
            connection.setRequestBody(json);
        }
        return executeRequest(connection);
    }

    /**
     * Performs a HTTP POST request.
     *
     * @return Input stream with response
     */
    InputStream post(URI uri) {
        return post(uri, null);
    }


    // Helpers


    /**
     * Sets a JSON String as a request entity.
     *
     * @param connection The request to set entity.
     * @param json        The JSON String to set.
     */
    public HttpConnection setEntity(HttpConnection connection, String json) {
        connection.requestProperties.put("Content-Type", "application/json");
        connection.setRequestBody(json);
        return connection;
    }

    /**
     * Validates a HTTP response; on error cases logs status and throws relevant exceptions.
     *
     * @param response The HTTP response.
     */
    void validate(HttpConnection response) throws IOException {
        final int code = response.getConnection().getResponseCode();
        if (code == 200 || code == 201 || code == 202) { // success (ok | created | accepted)
            return;
        }
        String reason = response.getConnection().getResponseMessage();
        switch (code) {
            case HttpURLConnection.HTTP_NOT_FOUND: {
                throw new NoDocumentException(reason);
            }
            case HttpURLConnection.HTTP_CONFLICT: {
                throw new DocumentConflictException(reason);
            }
            case HttpURLConnection.HTTP_PRECON_FAILED: {
                throw new PreconditionFailedException(reason);
            }
            default: { // other errors: 400 | 401 | 500 etc.
                throw new CouchDbException(reason);
            }
        }
    }

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
     * Executes a HTTP request and return a domain object
     *
     * @param connection  The HTTP connection to execute.
     * @return <T> T json object
     */
    public InputStream executeRequest(HttpConnection connection) {
        return executeToInputStream(connection);
    }

    // - if 2xx then return stream
    // - map 404 to NoResourceException
    // - if there's a couch error returned as json, un-marshall and throw
    // - anything else, just throw the IOException back, use the cause part of the exception?

    // it needs to catch eg FileNotFoundException and rethrow to emulate the previous exception handling behaviour
    private InputStream executeToInputStream(HttpConnection connection) throws CouchDbException {

        // all CouchClient requests want to receive application/json responses
        connection.requestProperties.put("Accept", "application/json");
        this.addCustomHeaders(connection);
        connection.responseInterceptors.addAll(this.responseInterceptors);
        connection.requestInterceptors.addAll(this.requestInterceptors);
        InputStream is = null; // input stream - response from server on success
        InputStream es = null; // error stream - response from server for a 500 etc
        int code = -1;
        Throwable cause = null;

        // first try to execute our request and get the input stream with the server's response
        // we want to catch IOException because HttpUrlConnection throws these for non-success
        // responses (eg 404 throws a FileNotFoundException) but we need to map to our own
        // specific exceptions
        try {
            is = connection.execute().responseAsInputStream();
        } catch (IOException ioe) {
            cause = ioe;
        }

        try {
            JsonObject errorJsonMessage = null;
            if(cause != null && connection.getConnection().getErrorStream() != null) {
                String errorString = IOUtils.toString(connection.getConnection()
                        .getErrorStream(), "UTF-8");
                errorJsonMessage = gson.fromJson(errorString, JsonObject.class);
            }

            //User errorStream to get error message if database does not exist or already exists
            String response = "";
            if(errorJsonMessage != null && errorJsonMessage.has("reason")) {
                response = errorJsonMessage.toString();
            } else {
                response = connection.getConnection().getResponseMessage();
            }
            code = connection.getConnection().getResponseCode();
            // everything ok? return the stream
            if (code / 100 == 2) { // success [200,299]
                return is;
            } else if (code == 404) {
                throw new NoDocumentException(response, cause);
            } else if(code == 412) {
                //Database already created
                return is;
            } else {
                es = connection.getConnection().getErrorStream();

                CouchDbException ex = getGson().fromJson(new InputStreamReader(es),
                        CouchDbException.class);
                if (ex == null && errorJsonMessage != null) {
                    //Try again with error json message
                    try {
                        ex = getGson().fromJson(errorJsonMessage,
                                CouchDbException.class);
                        ex.setStatusCode(code);
                    } catch (NullPointerException e) {
                        //Create new Exception from scratch
                        ex = new CouchDbException(response, code);
                    }

                }
                throw ex;
            }
        } catch (IOException ioe) {
            throw new CouchDbException("Error retrieving server response", ioe, code);
        } finally {
            if (es != null) {
                try {
                    es.close();
                } catch (IOException ioe) {
                    ;
                }
            }
        }
    }

    /**
     * Adds headers from CouchConfig to {@code connection}.
     * @param connection connection to add headers to.
     */
    public void addCustomHeaders(HttpConnection connection) {
        if (this.customHeaders != null) {
            connection.requestProperties.putAll(this.customHeaders);
        }
    }

    private void closeQuietly(InputStream is) {
        try {
            if (is != null) {
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
