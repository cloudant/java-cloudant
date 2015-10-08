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
import com.cloudant.client.org.lightcouch.CouchDbProperties;
import com.cloudant.client.org.lightcouch.Replication;
import com.cloudant.client.org.lightcouch.Replicator;
import com.cloudant.client.org.lightcouch.Response;
import com.cloudant.http.CookieInterceptor;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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

    private CouchDbClient client;

    private String accountName;
    private String loginUsername;
    private String password;


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
        this.loginUsername = loginUsername;
        this.password = password;

        CookieInterceptor interceptor = null;
        if(!loginUsername.isEmpty() && !password.isEmpty()) {
            interceptor = new CookieInterceptor(
                    loginUsername,
                    password);
        }

        doInit(h.get("scheme"),
                h.get("hostname"),
                new Integer(h.get("port")).intValue(),
                loginUsername,
                password,
                null, //connectoptions
                null, //authcookie
                interceptor //interceptor
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
        this.loginUsername = loginUsername;
        this.password = password;

        CookieInterceptor interceptor = new CookieInterceptor(
                loginUsername,
                password);

        doInit(h.get("scheme"),
                h.get("hostname"),
                new Integer(h.get("port")).intValue(),
                loginUsername,
                password,
                connectOptions,
                null, //authcookie
                interceptor //interceptor
        );
    }

    /**
     * Constructs a new instance of this class and connects to the cloudant account with the
     * specified credentials
     *
     * @param account    For cloudant.com, the cloudant account to connect to. For Cloudant
     *                   local, the server URL
     * @param authCookie The cookie obtained from last login
     */
    public CloudantClient(String account, String authCookie) {
        super();
        Map<String, String> h = parseAccount(account);
        assertNotEmpty(authCookie, "AuthCookie");

        doInit(h.get("scheme"),
                h.get("hostname"),
                new Integer(h.get("port")).intValue(),
                null, null, null,
                authCookie,
                null //interceptor
        );
    }

    /**
     * Constructs a new instance of this class and connects to the cloudant account with the
     * specified credentials
     *
     * @param account        For cloudant.com, the cloudant account to connect to. For Cloudant
     *                       local, the server URL
     * @param account        The cloudant account to connect to
     * @param authCookie     The cookie obtained from last login
     * @param connectOptions optional properties to connect e.g connectionTime,socketTimeout,etc
     */
    public CloudantClient(String account, String authCookie, ConnectOptions connectOptions) {
        super();
        Map<String, String> h = parseAccount(account);
        assertNotEmpty(authCookie, "AuthCookie");

        doInit(h.get("scheme"),
                h.get("hostname"),
                new Integer(h.get("port")).intValue(),
                null, null, connectOptions, authCookie, null);
    }

    /**
     * Generate an API key
     *
     * @return the generated key and password
     */
    public ApiKey generateApiKey() {
        URI uri = buildUri(getBaseUri()).path("_api/v2/api_keys").build();
        InputStream response =  client.post(uri, null);
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
            response = client.get(buildUri(getBaseUri()).path("_active_tasks").build());
            return getResponseList(response, client.getGson(), Task.class,
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
        Membership membership = client.get(buildUri(getBaseUri()).path("_membership").build(),
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
        return new Database(this, client.database(name, create));
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
        client.deleteDB(dbName, confirm);
    }

    /**
     * Request to  delete a database.
     *
     * @param dbName The database name
     */
    public void deleteDB(String dbName) {
        client.deleteDB(dbName);
    }


    /**
     * Request to create a new database; if one doesn't exist.
     *
     * @param dbName The Database name
     */
    public void createDB(String dbName) {
        client.createDB(dbName);
    }


    /**
     * @return The base URI.
     */
    public URI getBaseUri() {
        return client.getBaseUri();
    }


    /**
     * @return All Server databases.
     */
    public List<String> getAllDbs() {
        return client.getAllDbs();
    }


    /**
     * @return Cloudant Server version.
     */
    public String serverVersion() {
        return client.serverVersion();
    }


    /**
     * Provides access to Cloudant <tt>replication</tt> APIs.
     *
     * @see Replication
     */
    public com.cloudant.client.api.Replication replication() {
        Replication couchDbReplication = client.replication();
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
        Replicator couchDbReplicator = client.replicator();
        com.cloudant.client.api.Replicator replicator = new com.cloudant.client.api.Replicator
                (couchDbReplicator);
        return replicator;
    }


    /**
     * Executes a HTTP request. This method provides a mechanism to extend the API
     * <p><b>Note</b>: The response must be closed after use to release the connection.
     *
     * @param request The HTTP request to execute.
     * @return {@link InputStream} with response from {@link HttpConnection}
     */
    public InputStream executeRequest(HttpConnection request) {
        return client.executeToInputStream(request);
    }

    /**
     * Shuts down the connection manager used by this client instance.
     */
    public void shutdown() {
        client.shutdown();
    }

    /**
     * Request cloudant to send a list of UUIDs.
     *
     * @param count The count of UUIDs.
     */
    public List<String> uuids(long count) {
        return client.uuids(count);
    }


    /**
     * Sets a {@link GsonBuilder} to create {@link Gson} instance.
     * <p>Useful for registering custom serializers/deserializers, such as Datetime formats.
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
        client.setGsonBuilder(gsonBuilder);
    }


    /**
     * @return The Gson instance.
     */
    public Gson getGson() {
        return client.getGson();
    }


    // Helper methods

    String getLoginUsername() {
        return loginUsername;
    }

    String getPassword() {
        return password;
    }


    /**
     * @return the accountName
     */
    String getAccountName() {
        return accountName;
    }

    /**
     * Performs a HTTP GET request.
     *
     * @return An object of type T
     */
    <T> T get(URI uri, Class<T> classType) {
        return client.get(uri, classType);
    }

    /**
     * Performs a HTTP GET request.
     *
     * @return InputStream with response
     */
    InputStream get(URI uri) {
        return client.get(uri);
    }


    Response put(URI uri, Object object, boolean newEntity, int writeQuorum) {
        return client.put(uri, object, newEntity, writeQuorum);
    }

    private Map<String, String> parseAccount(String account) {
        assertNotEmpty(account, "accountName");
        this.accountName = account;
        Map<String, String> h = new HashMap<String, String>();
        if (account.startsWith("http://") || account.startsWith("https://")) {
            // user is specifying a uri
            try {
                URI uri = new URI(account);
                uri.getPort();
                h.put("scheme", uri.getScheme());
                h.put("hostname", uri.getHost());
                h.put("port", new Integer(uri.getPort()).toString());
                /*if(uri.getUserInfo() != null && !uri.getUserInfo().isEmpty()) {
                    h.put("username", uri.getUserInfo().split(":")[0]);
                    h.put("password", uri.getUserInfo().split(":")[1]);
                }*/
            } catch (URISyntaxException e) {

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
     * @param authCookie
     */
    private void doInit(String scheme, String hostname, int port,
                        String loginUsername, String password, ConnectOptions connectOptions,
                        String authCookie, CookieInterceptor interceptor) {

        CouchDbProperties props;
        if (authCookie == null) {
            props = new CouchDbProperties(scheme, hostname, port,
                    loginUsername, password);

        } else {
            props = new CouchDbProperties(scheme, hostname, port,
                    authCookie);
        }

        //Set request and response interceptors
        if(interceptor != null) {
            List<HttpConnectionRequestInterceptor> cookieRequestInterceptor =
                    new ArrayList<HttpConnectionRequestInterceptor>();
            cookieRequestInterceptor.add(interceptor);

            List<HttpConnectionResponseInterceptor> cookieResponseInterceptor =
                    new ArrayList<HttpConnectionResponseInterceptor>();
            cookieResponseInterceptor.add(interceptor);

            props.setRequestInterceptors(cookieRequestInterceptor);
            props.setResponseInterceptors(cookieResponseInterceptor);
        }

        if (connectOptions != null) {
            props.setConnectionTimeout(connectOptions.getConnectionTimeout());
            props.setSocketTimeout(connectOptions.getSocketTimeout());
            props.setMaxConnections(connectOptions.getMaxConnections());

            props.setProxyHost(connectOptions.getProxyHost());
            props.setProxyPort(connectOptions.getProxyPort());
            props.disableSSLAuthentication(connectOptions.isSSLAuthenticationDisabled());
            props.setAuthenticatedModeSSLSocketFactory(connectOptions
                    .getAuthenticatedModeSSLSocketFactory());
        }
        this.client = new CouchDbClient(props);

        // set a custom gsonbuilder that includes additional cloudant deserializers
        setGsonBuilder(new GsonBuilder());
    }
}

