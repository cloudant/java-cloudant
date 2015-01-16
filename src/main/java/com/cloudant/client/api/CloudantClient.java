package com.cloudant.client.api;

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.close;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.createPost;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getResponseList;
import static com.cloudant.client.org.lightcouch.internal.URIBuilder.buildUri;

import java.net.URI;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.ConnectOptions;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.Membership;
import com.cloudant.client.api.model.Task;
import com.cloudant.client.org.lightcouch.Changes;
import com.cloudant.client.org.lightcouch.CouchDbClient;
import com.cloudant.client.org.lightcouch.CouchDbDesign;
import com.cloudant.client.org.lightcouch.CouchDbProperties;
import com.cloudant.client.org.lightcouch.Replication;
import com.cloudant.client.org.lightcouch.Replicator;
import com.cloudant.client.org.lightcouch.Response;
import com.cloudant.client.org.lightcouch.View;
import com.cloudant.client.org.lightcouch.internal.CouchDbUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * Exposes the Cloudant client API
 * <p>This class is the main object to use to gain access to the Cloudant APIs.
 * <h3>Usage Example:</h3> 
 * <p>Create a new Cloudant client instance:
 * <pre>
 * CloudantClient client = new CloudantClient("mycloudantaccount","myusername","mypassword");
 * </pre>
 * 
 * <p>Start using the API by the client:
 * 
 * <p>Server APIs is accessed by the client directly eg.: {@link CloudantClient#getAllDbs() client.getAllDbs()}
 * <p>DB is accessed by getting to the Database from the client 
 * <pre>
 * Database db = client.database("customers",false);
 * </pre>
 * <p>Documents <code>CRUD</code> APIs is accessed from the Database eg.: {@link Database#find(Class, String) db.find(Foo.class, "doc-id")}
 * <p>Cloudant Query 
 * <p>		{@link Database#createIndex(String, String, String, IndexField[]) db.createIndex("Person_name", "Person_name", "json",
				new IndexField[] { new IndexField("Person_name",SortOrder.asc)}) }
 * <p>      {@link Database#findByIndex(String, Class) db.findByIndex(" "selector": { "Person_name": "Alec Guinness" }", Movie.class)}
 * <p>      {@link Database#deleteIndex(String, String) db.deleteIndex("Person_name", "Person_name") }
 * 
 * <p>Cloudant Search {@link Search db.search("views101/animals)}
 * <p>View APIs {@link View db.view()} 
 * <p>Change Notifications {@link Changes db.changes()}
 * <p>Design documents {@link CouchDbDesign db.design()}
 *  
 * <p>Replication {@link Replication account.replication()} and {@link Replicator account.replicator()} 
 * 
 * 
 * <p>At the end of a client usage; it's useful to call: {@link #shutdown()} to ensure proper release of resources.
 * 
 * @since 0.0.1
 * @author Mario Briggs
 *
 */
public class CloudantClient {
	
	private CouchDbClient client;
	
	private String accountName;
	private String loginUsername;
	private String password;

	/**
	 * Constructs a new instance of this class. The caller has to provide a properly configured LightCouch client.
	 * 
	 * @param client
	 */
	public CloudantClient(CouchDbClient client) {
		super();
		this.client = client;
	}
		
	/**
	 * Constructs a new instance of this class and connects to the cloudant account with the specified credentials
	 * @param account The cloudant account to connect to
	 * @param loginUsername The Username credential
	 * @param password The Password credential
	 */
	public CloudantClient(String account, String loginUsername, String password) {
		super();
		assertNotEmpty(account,"accountName");
		assertNotEmpty(loginUsername,"loginUsername");
		assertNotEmpty(password,"password");
		
		this.accountName = account;
		this.loginUsername = loginUsername;
		this.password = password;
		this.client = new CouchDbClient("https", account + ".cloudant.com", 443, loginUsername, password);
	}
	
	/**
	 * Constructs a new instance of this class and connects to the cloudant account with the specified credentials
	 * @param account The cloudant account to connect to
	 * @param loginUsername The Username credential
	 * @param password The Password credential
	 * @param connectOptions optional properties to connect e.g connectionTime,socketTimeout,etc 
	 */
	public CloudantClient(String account, String loginUsername, String password,ConnectOptions connectOptions){
		super();
		assertNotEmpty(account,"accountName");
		assertNotEmpty(loginUsername,"loginUsername");
		assertNotEmpty(password,"password");
		
		this.accountName = account;
		this.loginUsername = loginUsername;
		this.password = password;
		
		CouchDbProperties props = new CouchDbProperties("https",account+".cloudant.com",443,loginUsername,password);
		if(connectOptions != null){
			props.setConnectionTimeout(connectOptions.getConnectionTimeout());
			props.setSocketTimeout(connectOptions.getSocketTimeout());
			props.setMaxConnections(connectOptions.getMaxConnections());
			
			props.setProxyHost(connectOptions.getProxyHost());
			props.setProxyPort(connectOptions.getProxyPort());
		}
		this.client = new CouchDbClient(props);
		
	}
	
	/**
	 * Constructs a new instance of this class and connects to the cloudant account with the specified credentials
	 * @param account The cloudant account to connect to
	 * @param authCookie The cookie obtained from last login
	 */
	public CloudantClient(String account, String authCookie){
		super();
		assertNotEmpty(account,"accountName");
		assertNotEmpty(authCookie, "AuthCookie");
		
		this.accountName = account ;
		this.client = new CouchDbClient("https",account +".cloudant.com",443,authCookie);
	}
	
	/**
	 * Constructs a new instance of this class and connects to the cloudant account with the specified credentials
	 * @param account The cloudant account to connect to
	 * @param authCookie The cookie obtained from last login
	 * @param connectOptions optional properties to connect e.g connectionTime,socketTimeout,etc 
	 */
	public CloudantClient(String account, String authCookie,ConnectOptions connectOptions){
		super();
		assertNotEmpty(account,"accountName");
		assertNotEmpty(authCookie, "AuthCookie");
		
		CouchDbProperties props = new CouchDbProperties("https",account+".cloudant.com",443,authCookie);
		if(connectOptions != null){
			props.setConnectionTimeout(connectOptions.getConnectionTimeout());
			props.setSocketTimeout(connectOptions.getSocketTimeout());
			props.setMaxConnections(connectOptions.getMaxConnections());
			
			props.setProxyHost(connectOptions.getProxyHost());
			props.setProxyPort(connectOptions.getProxyPort());
		}		
		this.client = new CouchDbClient(props);
	}
		
	/**
	 * Generate an API key
	 * @return the generated key and password
	 */
	public ApiKey generateApiKey() {
		URI uri = buildUri(getBaseUri()).path("_api/v2/api_keys").build();
		return client.executeRequest(createPost(uri,"",""), ApiKey.class);		
	}

	/**
	 * Get all active tasks
	 * @return List of tasks
	 */
	public List<Task> getActiveTasks() {
		HttpResponse response = null;
		HttpGet get = new HttpGet(buildUri(getBaseUri()).path("/_active_tasks").build());
		try {
			response = executeRequest(get);
			return getResponseList(response, Database.getGson(), Task.class,
							new TypeToken<List<Task>>(){}.getType());
		}
		finally {
			close(response);
		}
	}
	
	/**
	 * Get the cookieStore
	 * @return cookieStore
	 */
	public String getCookie(){
		return client.getCookie();
	}
	
	/**
	 * Get the list of nodes in a cluster
	 * @return cluster nodes and all nodes
	 */
	public Membership getMembership() {
		Membership membership = client.get(buildUri(getBaseUri()).path("/_membership").build(), Membership.class);
		return membership ;		
	}
	
	
	/**
	 * Get a database
	 * @param name name of database to access
	 * @param create flag indicating whether to create the database if doesnt exist.
	 * @return Database object
	 */
	public Database database(String name, boolean create) {
		return new Database(this,client.database(name, create));
	}


	/**
	 * Request to  delete a database.
	 * @param dbName The database name
	 * @param confirm A confirmation string with the value: <tt>delete database</tt>
	 */
	public void deleteDB(String dbName, String confirm) {
		client.deleteDB(dbName, confirm);
	}


	/**
	 * Request to create a new database; if one doesn't exist.
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
	 * @see Replication
	 */
	public com.cloudant.client.api.Replication replication() {
		Replication couchDbReplication = client.replication();
		com.cloudant.client.api.Replication replication = new com.cloudant.client.api.Replication(couchDbReplication);
		return replication;
	}


	/**
	 * Provides access to Cloudant <tt>replication</tt> APIs.
	 * @see Replication
	 */
	public com.cloudant.client.api.Replicator replicator() {
		Replicator couchDbReplicator = client.replicator();
		com.cloudant.client.api.Replicator replicator = new com.cloudant.client.api.Replicator(couchDbReplicator);
		return replicator ;
	}


	/**
	 * Executes a HTTP request. This method provides a mechanism to extend the API
	 * <p><b>Note</b>: The response must be closed after use to release the connection.
	 * @param request The HTTP request to execute.
	 * @return {@link HttpResponse}
	 */
	public HttpResponse executeRequest(HttpRequestBase request) {

		HttpResponse response = client.executeRequest(request);
		return response;

	}

	

	/**
	 * Shuts down the connection manager used by this client instance.
	 */
	public void shutdown() {
		client.shutdown();
	}


	
	/**
	 * Request cloudant to send a list of UUIDs.
	 * @param count The count of UUIDs.
	 */
	public List<String> uuids(long count) {
		return client.uuids(count);
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
	 * @return An object of type T
	 */
	<T> T get(URI uri, Class<T> classType) {
		return client.get(uri, classType);
	}



	Response put(URI uri, Object object, boolean newEntity, int writeQuorum, Gson gson) {
			assertNotEmpty(object, "object");
			HttpResponse response = null;
			try {  
				final JsonObject json = gson.toJsonTree(object).getAsJsonObject();
				String id = CouchDbUtil.getAsString(json, "_id");
				String rev = CouchDbUtil.getAsString(json, "_rev");
				if(newEntity) { // save
					CouchDbUtil.assertNull(rev, "rev");
					id = (id == null) ? CouchDbUtil.generateUUID() : id;
				} else { // update
					assertNotEmpty(id, "id");
					assertNotEmpty(rev, "rev");
				}
				final HttpPut put = new HttpPut(buildUri(uri).pathToEncode(id).query("w", writeQuorum).buildEncoded());
				CouchDbUtil.setEntity(put, json.toString(),"application/json");
				response = client.executeRequest(put); 
				return CouchDbUtil.getResponse(response,Response.class,gson);
			} finally {
				close(response);
			}
		}
}
	
