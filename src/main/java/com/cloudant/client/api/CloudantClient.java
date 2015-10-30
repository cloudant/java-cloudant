/*
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

package com.cloudant.client.api;

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.close;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponse;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponseList;
import static com.cloudant.client.org.lightcouch.internal.URIBuilder.buildUri;

import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.ConnectOptions;
import com.cloudant.client.api.model.Index;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.Membership;
import com.cloudant.client.api.model.Permissions;
import com.cloudant.client.api.model.Shard;
import com.cloudant.client.api.model.Task;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.org.lightcouch.Changes;
import com.cloudant.client.org.lightcouch.CouchDbClient;
import com.cloudant.client.org.lightcouch.CouchDbDesign;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.CouchDbProperties;
import com.cloudant.client.org.lightcouch.Replication;
import com.cloudant.client.org.lightcouch.Replicator;
import com.cloudant.client.org.lightcouch.Response;
import com.cloudant.http.interceptors.CookieInterceptor;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.interceptors.ProxyAuthInterceptor;
import com.cloudant.http.interceptors.SSLCustomizerInterceptor;
import com.cloudant.http.interceptors.TimeoutCustomizationInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes the Cloudant client API
 * <p>This class is the main object to use to gain access to the Cloudant APIs.
 * <h1>Usage Examples:</h1>
 *
 * <h2>Create a new Cloudant client instance</h2>
 * <code>
 * CloudantClient client = new CloudantClient("mycloudantaccount","myusername","mypassword");
 * </code>
 *
 * <h2>Client use of the API</h2>
 * <ul>
 * <li>Server APIs accessed by the client directly e.g.:
 * <p>
 * {@link CloudantClient#getAllDbs() client.getAllDbs()}
 * </p>
 * </li>
 * <li>DB is accessed by getting the {@link Database} from the client e.g.:
 * <p>
 * <code>Database db = client.database("customers",false);</code>
 * </p>
 * </li>
 * <li>Document <code>CRUD</code> APIs accessed from the {@link Database} e.g.:
 * <p>
 * {@link Database#find(Class, String) db.find(Foo.class, "doc-id")}
 * </p>
 * </li>
 * </ul>
 *
 * <h2>Cloudant Query</h2>
 * <ul>
 * <li>Create an index
 * {@link Database#createIndex(String, String, String, IndexField[])} e.g.:
 * <p>
 * <code>
 * db.createIndex("Person_name", "Person_name_design_doc", "json", new IndexField[] { new IndexField
 * ("Person_name",SortOrder.asc)})
 * </code>
 * </p>
 * </li>
 * <li>Find using an index
 * {@link Database#findByIndex(String, Class)} e.g.:
 * <p>
 * <code>
 * db.findByIndex(" "selector": {
 * "Person_name": "Alec Guinness" }", Movie.class)}
 * </code>
 * </p>
 * </li>
 * <li>Delete an index
 * {@link Database#deleteIndex(String, String)} e.g.:
 * <p>
 * <code>
 * db.deleteIndex("Person_name", "Person_name_design_doc")
 * </code>
 * </p>
 * </li>
 * </ul>
 *
 * <h2>Cloudant Search</h2>
 * {@link Search db.search("views101/animals)}
 *
 * <h2>View APIs</h2>
 * {@link com.cloudant.client.api.views}
 *
 * <h2>Change Notifications</h2>
 * {@link Changes db.changes()}
 *
 * <h2>Design Documents</h2>
 * {@link CouchDbDesign db.design()}
 *
 * <h2>Replication</h2>
 * Replication {@link Replication account.replication()} and {@link Replicator account.replicator()}
 *
 * @author Mario Briggs
 * @since 0.0.1
 */
public class CloudantClient {

    CouchDbClient couchDbClient;

    /**
     * Constructs a new instance of this class and connects to the cloudant server with the
     * specified credentials
     *
     * @param account       For cloudant.com, the cloudant account to connect to. For Cloudant
     *                      local, the server URL
     * @param loginUsername The apiKey (if using an APIKey, else pass in the account for this
     *                      parameter also)
     * @param password      The Password credential
     */
    public CloudantClient(String account, String loginUsername, String password) {
        super();
        Map<String, String> h = parseAccount(account);

        doInit(h.get("scheme"),
                h.get("hostname"),
                new Integer(h.get("port")).intValue(),
                loginUsername,
                password,
                null //connectoptions
        );
    }

    /**
     * Constructs a new instance of this class and connects to the cloudant account with the
     * specified credentials
     *
     * @param account        For cloudant.com, the cloudant account to connect to. For Cloudant
     *                       local, the server URL
     * @param loginUsername  The apiKey (if using an APIKey, else pass in the account for this
     *                       parameter also)
     * @param password       The Password credential
     * @param connectOptions optional properties to connect e.g connectionTime,socketTimeout,etc
     */
    public CloudantClient(String account, String loginUsername, String password, ConnectOptions
            connectOptions) {
        super();
        Map<String, String> h = parseAccount(account);

        doInit(h.get("scheme"),
                h.get("hostname"),
                new Integer(h.get("port")).intValue(),
                loginUsername,
                password,
                connectOptions
        );
    }

    /**
     * Constructs a new instance of this class and connects to the cloudant account with the
     * specified credentials
     *
     * @param account For cloudant.com, the cloudant account to connect to. For Cloudant
     *                local, the server URL
     */
    public CloudantClient(String account) {
        super();
        Map<String, String> h = parseAccount(account);

        doInit(h.get("scheme"),
                h.get("hostname"),
                new Integer(h.get("port")).intValue(),
                null, null, null
        );
    }

    /**
     * Constructs a new instance of this class and connects to the cloudant account with the
     * specified credentials
     *
     * @param account        For cloudant.com, the cloudant account to connect to. For Cloudant
     *                       local, the server URL
     * @param account        The cloudant account to connect to
     * @param connectOptions optional properties to connect e.g connectionTime,socketTimeout,etc
     */
    public CloudantClient(String account, ConnectOptions connectOptions) {
        super();
        Map<String, String> h = parseAccount(account);

        doInit(h.get("scheme"),
                h.get("hostname"),
                new Integer(h.get("port")).intValue(),
                null, null, connectOptions);
    }

    /**
     * Generate an API key
     *
     * @return the generated key and password
     */
    public ApiKey generateApiKey() {
        URI uri = buildUri(getBaseUri()).path("_api/v2/api_keys").build();
        InputStream response = couchDbClient.post(uri, null);
        return getResponse(response, ApiKey.class, getGson());
    }

    /**
     * Get all active tasks
     *
     * @return List of tasks
     */
    public List<Task> getActiveTasks() {
        InputStream response = null;
        try {
            response = couchDbClient.get(buildUri(getBaseUri()).path("_active_tasks").build());
            return getResponseList(response, couchDbClient.getGson(), Task.class,
                    new TypeToken<List<Task>>() {
                    }.getType());
        } finally {
            close(response);
        }
    }

    /**
     * Get the list of nodes in a cluster
     *
     * @return cluster nodes and all nodes
     */
    public Membership getMembership() {
        Membership membership = couchDbClient.get(buildUri(getBaseUri()).path("_membership").build(),
                Membership.class);
        return membership;
    }


    /**
     * Get a database
     *
     * @param name   name of database to access
     * @param create flag indicating whether to create the database if it does not exist.
     * @return Database object
     */
    public Database database(String name, boolean create) {
        return new Database(this, couchDbClient.database(name, create));
    }


    /**
     * Request to  delete a database.
     *
     * @param dbName  The database name
     * @param confirm A confirmation string with the value: <tt>delete database</tt>
     * @deprecated use {@link CloudantClient#deleteDB(String)}
     */
    @Deprecated
    public void deleteDB(String dbName, String confirm) {
        couchDbClient.deleteDB(dbName, confirm);
    }

    /**
     * Request to  delete a database.
     *
     * @param dbName The database name
     */
    public void deleteDB(String dbName) {
        couchDbClient.deleteDB(dbName);
    }


    /**
     * Request to create a new database; if one doesn't exist.
     *
     * @param dbName The Database name
     */
    public void createDB(String dbName) {
        couchDbClient.createDB(dbName);
    }


    /**
     * @return The base URI.
     */
    public URI getBaseUri() {
        return couchDbClient.getBaseUri();
    }


    /**
     * @return All Server databases.
     */
    public List<String> getAllDbs() {
        return couchDbClient.getAllDbs();
    }


    /**
     * @return Cloudant Server version.
     */
    public String serverVersion() {
        return couchDbClient.serverVersion();
    }


    /**
     * Provides access to Cloudant <tt>replication</tt> APIs.
     *
     * @see Replication
     */
    public com.cloudant.client.api.Replication replication() {
        Replication couchDbReplication = couchDbClient.replication();
        com.cloudant.client.api.Replication replication = new com.cloudant.client.api.Replication
                (couchDbReplication);
        return replication;
    }


    /**
     * Provides access to Cloudant <tt>replication</tt> APIs.
     *
     * @see Replication
     */
    public com.cloudant.client.api.Replicator replicator() {
        Replicator couchDbReplicator = couchDbClient.replicator();
        com.cloudant.client.api.Replicator replicator = new com.cloudant.client.api.Replicator
                (couchDbReplicator);
        return replicator;
    }


    /**
     * Executes a HTTP request. This method provides a mechanism to extend the API
     * <p><b>Note</b>: Streams obtained from the HttpConnection must be closed after use to release
     * the connection.
     * </p>
     * <pre>
     * {@code
     * HttpConnection response = account.executeRequest(Http.GET(new URL(account.getBaseUri() +
     *         "/aNewAPI")));
     * if (response.getConnection().getResponseCode() == HttpURLConnection.HTTP_OK) {
     *     InputStream stream = response.responseAsInputStream();
     *     //process stream
     * }
     * }
     * </pre>
     *
     * @param request The HTTP request to execute, obtained from {@link com.cloudant.http.Http}.
     * @return {@link HttpConnection} that has been executed
     * @throws CouchDbException for error HTTP status codes or if there is an {@link IOException}
     */
    public HttpConnection executeRequest(HttpConnection request) {
            return couchDbClient.execute(request);
    }

    /**
     * Shuts down the connection manager used by this client instance.
     */
    public void shutdown() {
        couchDbClient.shutdown();
    }

    /**
     * Request cloudant to send a list of UUIDs.
     *
     * @param count The count of UUIDs.
     */
    public List<String> uuids(long count) {
        return couchDbClient.uuids(count);
    }


    /**
     * Sets a {@link GsonBuilder} to create {@link Gson} instance.
     * <p>Useful for registering custom serializers/deserializers, such as Datetime formats.
     *
     * @deprecated this setter will be removed and customizing the GsonBuilder will be a
     * CloudantClient initialization option in future
     */
    public void setGsonBuilder(GsonBuilder gsonBuilder) {
        //register additional cloudant deserializers and then let lightcouch init too
        gsonBuilder.registerTypeAdapter(new TypeToken<List<Shard>>() {
        }.getType(), new ShardDeserializer())
                .registerTypeAdapter(new TypeToken<List<Index>>() {
                }.getType(), new IndexDeserializer())
                .registerTypeAdapter(new TypeToken<Map<String, EnumSet<Permissions>>>() {
                        }.getType(),
                        new SecurityDeserializer())
                .registerTypeAdapter(Key.ComplexKey.class, new Key.ComplexKeyDeserializer());
        couchDbClient.setGsonBuilder(gsonBuilder);
    }


    /**
     * @return The Gson instance.
     */
    public Gson getGson() {
        return couchDbClient.getGson();
    }


    // Helper methods

    /**
     * Performs a HTTP GET request.
     *
     * @return An object of type T
     */
    <T> T get(URI uri, Class<T> classType) {
        return couchDbClient.get(uri, classType);
    }

    /**
     * Performs a HTTP GET request.
     *
     * @return InputStream with response
     */
    InputStream get(URI uri) {
        return couchDbClient.get(uri);
    }


    Response put(URI uri, Object object, boolean newEntity, int writeQuorum) {
        return couchDbClient.put(uri, object, newEntity, writeQuorum);
    }

    private Map<String, String> parseAccount(String account) {
        assertNotEmpty(account, "accountName");
        Map<String, String> h = new HashMap<String, String>();
        if (account.startsWith("http://") || account.startsWith("https://")) {
            // user is specifying a uri
            try {
                URI uri = new URI(account);
                int port = uri.getPort();
                if (port < 0) {
                    port = uri.toURL().getDefaultPort();
                }
                h.put("scheme", uri.getScheme());
                h.put("hostname", uri.getHost());
                h.put("port", new Integer(port).toString());
            } catch (URISyntaxException e) {
                throw new CouchDbException(e);
            } catch (MalformedURLException e2) {
                throw new CouchDbException(e2);
            }
        } else {
            h.put("scheme", "https");
            h.put("hostname", account + ".cloudant.com");
            h.put("port", "443");
        }
        return h;
    }

    /**
     * Initializes the internal lightCouch client
     *
     * @param scheme
     * @param hostname
     * @param port
     * @param loginUsername
     * @param password
     * @param connectOptions
     */
    private void doInit(String scheme, String hostname, int port,
                        String loginUsername, String password, ConnectOptions connectOptions) {

        CouchDbProperties props = new CouchDbProperties(scheme, hostname, port);

        if (loginUsername != null && password != null) {
            //make interceptor if both username and password are not null

            //Create cookie interceptor and set in HttpConnection interceptors
            CookieInterceptor cookieInterceptor = new CookieInterceptor(loginUsername, password);

            props.addRequestInterceptors(cookieInterceptor);
            props.addResponseInterceptors(cookieInterceptor);
        } else {
            //If username or password is null, throw an exception
            if (loginUsername != null || password != null) {
                //Username and password both have to contain values
                throw new CouchDbException("Either a username and password must be provided, or " +
                        "both values must be null. Please check the credentials and try again.");
            }
        }

        if (connectOptions != null) {
            props.addRequestInterceptors(new TimeoutCustomizationInterceptor(connectOptions
                    .getConnectionTimeout(), connectOptions.getReadTimeout()));
            props.setMaxConnections(connectOptions.getMaxConnections());
            props.setProxyURL(connectOptions.getProxyURL());
            if (connectOptions.getProxyUser() != null) {
                //if there was proxy auth information create an interceptor for it
                props.addRequestInterceptors(new ProxyAuthInterceptor(connectOptions.getProxyUser
                        (), connectOptions.getProxyPassword()));
            }
            if (connectOptions.isSSLAuthenticationDisabled()) {
                props.addRequestInterceptors(SSLCustomizerInterceptor
                        .SSL_AUTH_DISABLED_INTERCEPTOR);
            } else {
                if (connectOptions.getAuthenticatedModeSSLSocketFactory() != null) {
                    props.addRequestInterceptors(new SSLCustomizerInterceptor(connectOptions
                            .getAuthenticatedModeSSLSocketFactory()));
                }
            }
        }
        this.couchDbClient = new CouchDbClient(props);

        // set a custom gsonbuilder that includes additional cloudant deserializers
        setGsonBuilder(new GsonBuilder());
    }
}

