package com.cloudant;

import static org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.internal.CouchDbUtil.close;
import static org.lightcouch.internal.CouchDbUtil.createPost;
import static org.lightcouch.internal.CouchDbUtil.getAsString;
import static org.lightcouch.internal.CouchDbUtil.getResponse;
import static org.lightcouch.internal.CouchDbUtil.getResponseList;
import static org.lightcouch.internal.URIBuilder.buildUri;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.EnumSet;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.lightcouch.Changes;
import org.lightcouch.CouchDatabase;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbDesign;
import org.lightcouch.CouchDbInfo;
import org.lightcouch.Params;
import org.lightcouch.Response;
import org.lightcouch.View;
import org.lightcouch.internal.GsonHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;



/**
 * Database  exposes Cloudant DB specific features.
 * This class would expose all the public methods of LightCouch's CouchDatabase that we want to expose
 * plus cloudant DB specific API's
 *
 */

public class Database {

	private static Gson ownGSON;
	static {
		initGson();
	}
	private CouchDatabase db;
	private CloudantAccount client;

	public enum Permissions {
		_admin,
		_reader,
		_writer
	}
	
	
	/**
	 * 
	 * This constructor is for the use-case where a customer is already using LightCouch 
	 * and now upgrades to cloudant. So he already has a CouchDatabase object in his code
	 *  and easily gets to Cloudant DB features without the need to change all his existing code to use
	 *  Database/CloudantClient class
	 * @param db
	 
	public Database(CouchDatabase db) {
		super();
		this.db = db;
		// TODO need to instaniate a client
	}
	*/
	
	Database(CloudantAccount client, CouchDatabase db ) {
		super();
		this.client = client;
		this.db = db;
	}
	
	/**
	 * Set the permissions for the DB
	 * @param userNameorApikey
	 * @param permissions
	 */
	public void setPermissions(String userNameorApikey,  EnumSet<Permissions> permissions) {
		assertNotEmpty(userNameorApikey,"userNameorApikey");
		assertNotEmpty(permissions,"permissions");
		HttpResponse response = null;
		CouchDbClient tmp = new CouchDbClient("https", "cloudant.com", 443, client.getLoginUsername(), client.getPassword());		
		URI uri = buildUri(tmp.getBaseUri()).path("/api/set_permissions").build();
		String body = getPermissionsBody(userNameorApikey, permissions);
				
		try {
			response = tmp.executeRequest(createPost(uri,body,"application/x-www-form-urlencoded"));
			String ok = getAsString(response,"ok");
			if ( !ok.equalsIgnoreCase("true")) {
				//raise exception
			}
		}
		finally {
			close(response);
		}
	
	}
	
	/**
	 * Get info about the shards in the database
	 * @return List of shards
	 */
	 public List<Shard> getShards() {
		HttpResponse response = null;
		HttpGet get = new HttpGet(buildUri(db.getDBUri()).path("/_shards").build());
		try {
			response = client.executeRequest(get);
			return getResponseList(response, ownGSON, Shard.class,
							new TypeToken<List<Shard>>(){}.getType());
		}
		finally {
			close(response);
		}
	}
	
	 /**
		 * Get info about the shard a document belongs to 
		 * @param String documentId
		 * @return Shard info
		 */
		 public Shard getShard(String docId) {
			assertNotEmpty(docId,"docId");
			HttpResponse response = null;
			HttpGet get = new HttpGet(buildUri(db.getDBUri()).path("_shards/").path(docId).build());
			try {
				response = client.executeRequest(get);
				return getResponse(response, Shard.class,ownGSON);
			}
			finally {
				close(response);
			}
		}
	

	/**
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#design()
	 */
	public CouchDbDesign design() {
		return db.design();
	}



	/**
	 * @param viewId
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#view(java.lang.String)
	 */
	public View view(String viewId) {
		return db.view(viewId);
	}



	/**
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#changes()
	 */
	public Changes changes() {
		return db.changes();
	}



	/**
	 * @param classType
	 * @param id
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#find(java.lang.Class, java.lang.String)
	 */
	public <T> T find(Class<T> classType, String id) {
		return db.find(classType, id);
	}



	/**
	 * @param classType
	 * @param id
	 * @param params
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#find(java.lang.Class, java.lang.String, org.lightcouch.Params)
	 */
	public <T> T find(Class<T> classType, String id, Params params) {
		return db.find(classType, id, params);
	}



	/**
	 * @param classType
	 * @param id
	 * @param rev
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#find(java.lang.Class, java.lang.String, java.lang.String)
	 */
	public <T> T find(Class<T> classType, String id, String rev) {
		return db.find(classType, id, rev);
	}



	/**
	 * @param classType
	 * @param uri
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#findAny(java.lang.Class, java.lang.String)
	 */
	public <T> T findAny(Class<T> classType, String uri) {
		return db.findAny(classType, uri);
	}



	/**
	 * @param id
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#find(java.lang.String)
	 */
	public InputStream find(String id) {
		return db.find(id);
	}



	/**
	 * @param id
	 * @param rev
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#find(java.lang.String, java.lang.String)
	 */
	public InputStream find(String id, String rev) {
		return db.find(id, rev);
	}



	/**
	 * @param id
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#contains(java.lang.String)
	 */
	public boolean contains(String id) {
		return db.contains(id);
	}



	/**
	 * @param object
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#save(java.lang.Object)
	 */
	public Response save(Object object) {
		return db.save(object);
	}



	/**
	 * @param object
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#post(java.lang.Object)
	 */
	public Response post(Object object) {
		return db.post(object);
	}



	/**
	 * @param object
	 * @see org.lightcouch.CouchDatabaseBase#batch(java.lang.Object)
	 */
	public void batch(Object object) {
		db.batch(object);
	}



	/**
	 * @param object
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#update(java.lang.Object)
	 */
	public Response update(Object object) {
		return db.update(object);
	}



	/**
	 * @param object
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#remove(java.lang.Object)
	 */
	public Response remove(Object object) {
		return db.remove(object);
	}



	/**
	 * @param id
	 * @param rev
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#remove(java.lang.String, java.lang.String)
	 */
	public Response remove(String id, String rev) {
		return db.remove(id, rev);
	}



	/**
	 * @param objects
	 * @param allOrNothing
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#bulk(java.util.List, boolean)
	 */
	public List<Response> bulk(List<?> objects, boolean allOrNothing) {
		return db.bulk(objects, allOrNothing);
	}



	/**
	 * @param in
	 * @param name
	 * @param contentType
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#saveAttachment(java.io.InputStream, java.lang.String, java.lang.String)
	 */
	public Response saveAttachment(InputStream in, String name,
			String contentType) {
		return db.saveAttachment(in, name, contentType);
	}



	/**
	 * @param in
	 * @param name
	 * @param contentType
	 * @param docId
	 * @param docRev
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#saveAttachment(java.io.InputStream, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public Response saveAttachment(InputStream in, String name,
			String contentType, String docId, String docRev) {
		return db.saveAttachment(in, name, contentType, docId, docRev);
	}



	/**
	 * @param updateHandlerUri
	 * @param docId
	 * @param query
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#invokeUpdateHandler(java.lang.String, java.lang.String, java.lang.String)
	 */
	public String invokeUpdateHandler(String updateHandlerUri, String docId,
			String query) {
		return db.invokeUpdateHandler(updateHandlerUri, docId, query);
	}



	/**
	 * @param updateHandlerUri
	 * @param docId
	 * @param params
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#invokeUpdateHandler(java.lang.String, java.lang.String, org.lightcouch.Params)
	 */
	public String invokeUpdateHandler(String updateHandlerUri, String docId,
			Params params) {
		return db.invokeUpdateHandler(updateHandlerUri, docId, params);
	}



	/**
	 * 
	 * @see org.lightcouch.CouchDatabaseBase#syncDesignDocsWithDb()
	 */
	public void syncDesignDocsWithDb() {
		db.syncDesignDocsWithDb();
	}



	/**
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#getDBUri()
	 */
	public URI getDBUri() {
		return db.getDBUri();
	}



	/**
	 * @return
	 * @see org.lightcouch.CouchDatabaseBase#info()
	 */
	public CouchDbInfo info() {
		return db.info();
	}



	/**
	 * 
	 * @see org.lightcouch.CouchDatabaseBase#ensureFullCommit()
	 */
	public void ensureFullCommit() {
		db.ensureFullCommit();
	}



	private String getPermissionsBody(String userNameorApikey, EnumSet<Permissions> permissions ) {
		String body;
		try {
			body = "username=" + URLEncoder.encode(userNameorApikey,"UTF-8") +
						"&database=" + client.getAccountName() + "/" + URLEncoder.encode(db.getDbName(),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getLocalizedMessage()); // TODO fix this
		}
		for (Permissions s : permissions) {
			body += "&roles=" +  s.toString();
		}
		return body;
	}
	
	
	/**
	 * setup our own Deserializers
	 */
	private static void initGson() {
		GsonBuilder builder = GsonHelper.initGson(new GsonBuilder());
		builder.registerTypeAdapter(new TypeToken<List<Shard>>(){}.getType(), new ShardDeserializer());
		ownGSON = builder.create();
	}
	
	static Gson getGson() {
		return ownGSON;
	}
}
