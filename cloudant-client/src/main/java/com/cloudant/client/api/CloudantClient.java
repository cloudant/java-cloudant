/*
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

package com.cloudant.client.api;

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.close;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponse;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponseList;

import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.Membership;
import com.cloudant.client.api.model.Task;
import com.cloudant.client.api.scheduler.SchedulerDocsResponse;
import com.cloudant.client.api.scheduler.SchedulerJobsResponse;
import com.cloudant.client.internal.URIBase;
import com.cloudant.client.internal.util.DeserializationTypes;
import com.cloudant.client.org.lightcouch.CouchDbClient;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.CouchDbProperties;
import com.cloudant.client.api.model.MetaInformation;
import com.cloudant.client.org.lightcouch.Replication;
import com.cloudant.client.org.lightcouch.Replicator;
import com.cloudant.http.HttpConnection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.EnumSet;
import java.util.List;

/**
 * Exposes the Cloudant client API
 * <P>
 * This class is the main object to use to gain access to the Cloudant APIs. Instances of
 * CloudantClient are created using a ClientBuilder. Once created a CloudantClient is immutable
 * and safe to access from multiple threads.
 *
 * </P>
 * <h1>Usage Examples:</h1>
 *
 * <h2>Create a new Cloudant client instance</h2>
 * <code>
 * CloudantClient client = ClientBuilder.account
 *
 * CloudantClient("mycloudantaccount","myusername",
 * "mypassword");
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
 * db.findByIndex(" \"selector\": {
 * \"Person_name\": \"Alec Guinness\" }", Movie.class)}
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
 * {@link Search db.search("views101/animals")}
 *
 * <h2>View APIs</h2>
 * {@link com.cloudant.client.api.views}
 *
 * <h2>Change Notifications</h2>
 * {@link Changes db.changes()}
 *
 * <h2>Design Documents</h2>
 * {@link DesignDocumentManager db.getDesignDocumentManager()}
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
     * @param props Properties file with account path, credentials, and connection options
     */
    CloudantClient(CouchDbProperties props, GsonBuilder gsonBuilder) {
        this.couchDbClient = new CouchDbClient(props);

        // set the gsonbuilder that includes additional cloudant deserializers
        couchDbClient.setGsonBuilder(gsonBuilder);
    }

    /**
     * Use the authorization feature to generate new API keys to access your data. An API key is
     * similar to a username/password pair for granting others access to your data.
     * <P>Example usage:
     * </P>
     * <pre>
     * {@code
     * ApiKey key = client.generateApiKey();
     * System.out.println(key);
     * }
     * </pre>
     * <P> Example output:
     * </P>
     * <pre>
     * {@code key: isdaingialkyciffestontsk password: XQiDHmwnkUu4tknHIjjs2P64}
     * </pre>
     *
     * @return the generated key and password
     * @see Database#setPermissions(String, EnumSet)
     */
    public ApiKey generateApiKey() {
        URI uri = new URIBase(getBaseUri()).path("_api").path("v2").path("api_keys").build();
        InputStream response = couchDbClient.post(uri, null);
        return getResponse(response, ApiKey.class, getGson());
    }

    /**
     * Get the list of active tasks from the server.
     *
     * @return List of tasks
     * @see <a href="https://console.bluemix.net/docs/services/Cloudant/api/active_tasks.html">
     *     Active tasks</a>
     */
    public List<Task> getActiveTasks() {
        InputStream response = null;
        URI uri = new URIBase(getBaseUri()).path("_active_tasks").build();
        try {
            response = couchDbClient.get(uri);
            return getResponseList(response, couchDbClient.getGson(), DeserializationTypes.TASKS);
        } finally {
            close(response);
        }
    }

    /**
     * Get the list of all nodes and the list of active nodes in the cluster.
     *
     * @return Membership object encapsulating lists of all nodes and the cluster nodes
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/advanced.html#-get-_membership-">
     * _membership</a>
     */
    public Membership getMembership() {
        URI uri = new URIBase(getBaseUri()).path("_membership").build();
        Membership membership = couchDbClient.get(uri,
                Membership.class);
        return membership;
    }

    /**
     * Get a database reference for the database with the specified name.
     * <P>
     * Note that if create is {@code false} and the database does not exist an instance will be
     * returned, but the first operation on that instance will throw a
     * {@link com.cloudant.client.org.lightcouch.NoDocumentException} because the database does not
     * exist.
     * </P>
     *
     * @param name   name of database to access
     * @param create flag indicating whether to create the database if it does not exist
     * @return Database object
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/database.html"
     * target="_blank">Databases</a>
     */
    public Database database(String name, boolean create) {
        return new Database(this, couchDbClient.database(name, create));
    }

    /**
     * Request to delete the database with the specified name.
     *
     * @param dbName the database name
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/database.html#deleting-a-database">
     * Databases - delete</a>
     */
    public void deleteDB(String dbName) {
        couchDbClient.deleteDB(dbName);
    }

    /**
     * Request to create a new database with the specified name.
     *
     * @param dbName the database name
     * @throws com.cloudant.client.org.lightcouch.PreconditionFailedException if a database with
     *                                                                        the same name
     *                                                                        already exists
     * @see <a target="_blank"
     * href="https://console.bluemix.net/docs/services/Cloudant/api/database.html#create">
     * Databases - create</a>
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
     * List all the databases on the server for the Cloudant account.
     *
     * @return List of the names of all the databases
     * @see <a target="_blank"
     * href="https://console.bluemix.net/docs/services/Cloudant/api/database.html#get-a-list-of-all-databases-in-the-account">
     * Databases - get list of databases</a>
     */
    public List<String> getAllDbs() {
        return couchDbClient.getAllDbs();
    }

    /**
     * Get the reported server version from the welcome message metadata.
     *
     * @return Cloudant server version.
     * @see <a target="_blank"
     * href="https://console.bluemix.net/docs/services/Cloudant/api/advanced.html#-get-">
     * GET meta information about the cluster</a>
     */
    public String serverVersion() {
        return couchDbClient.serverVersion();
    }

    /**
     * Get the welcome message metadata.
     *
     * @return Welcome message metadata.
     * @see <a target="_blank"
     * href="https://console.bluemix.net/docs/services/Cloudant/api/advanced.html#-get-">
     * GET meta information about the cluster</a>
     */
    public MetaInformation metaInformation() {
        return couchDbClient.metaInformation();
    }

    /**
     * Provides access to Cloudant <tt>replication</tt> APIs.
     *
     * @return Replication object for configuration and triggering
     * @see com.cloudant.client.api.Replication
     * @see <a target="_blank"
     * href="https://console.bluemix.net/docs/services/Cloudant/api/advanced_replication.html#the-_replicate-endpoint">
     * Replication - the _replicate endpoint</a>
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
     * @return Replicator object for interacting with the _replicator DB
     * @see com.cloudant.client.api.Replicator
     * @see <a target="_blank"
     * href="https://console.bluemix.net/docs/services/Cloudant/api/replication.html#the-_replicator-database">
     * Replication - the _replicator database</a>
     */
    public com.cloudant.client.api.Replicator replicator() {
        Replicator couchDbReplicator = couchDbClient.replicator();
        com.cloudant.client.api.Replicator replicator = new com.cloudant.client.api.Replicator
                (couchDbReplicator);
        return replicator;
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
        return couchDbClient.schedulerJobs();
    }

    /**
     * Lists replication documents. Includes information about all the documents, even in
     * completed and failed states. For each document it returns the document ID, the database,
     * the replication ID, source and target, and other information.
     *
     * @return All replication documents
     */
    public SchedulerDocsResponse schedulerDocs() {
        return couchDbClient.schedulerDocs();
    }

    /**
     * Get replication document state for a given replication document ID.
     *
     * @param docId The replication document ID
     * @return Replication document for {@code docId}
     */
    public SchedulerDocsResponse.Doc schedulerDoc(String docId) {
        return couchDbClient.schedulerDoc(docId);
    }

    /**
     * Executes a HTTP request. This method provides a mechanism to perform operations not
     * currently available in the client API.
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
     * <P>
     * Note that whilst the request can be constructed for any URL the connection information (e.g.
     * Cookie, BasicAuth etc) specified by this client will be added to the request before it is
     * executed.
     * </P>
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
     * Request a list of generated UUIDs from the Cloudant server.
     *
     * @param count the number of UUIDs
     * @return a List of UUID Strings
     * @see <a target="_blank"
     * href="https://console.bluemix.net/docs/services/Cloudant/api/advanced.html#-get-_uuids-">
     * _uuids</a>
     */
    public List<String> uuids(long count) {
        return couchDbClient.uuids(count);
    }

    /**
     * @return The Gson instance.
     */
    public Gson getGson() {
        return couchDbClient.getGson();
    }
}

