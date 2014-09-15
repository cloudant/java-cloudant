package com.cloudant;

import static org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.internal.CouchDbUtil.close;
import static org.lightcouch.internal.CouchDbUtil.createPost;
import static org.lightcouch.internal.CouchDbUtil.getAsString;
import static org.lightcouch.internal.CouchDbUtil.getResponse;
import static org.lightcouch.internal.CouchDbUtil.getResponseList;
import static org.lightcouch.internal.CouchDbUtil.getStream;
import static org.lightcouch.internal.URIBuilder.buildUri;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.lightcouch.Changes;
import org.lightcouch.CouchDatabase;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbDesign;
import org.lightcouch.CouchDbInfo;
import org.lightcouch.DocumentConflictException;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Params;
import org.lightcouch.Response;
import org.lightcouch.View;
import org.lightcouch.internal.GsonHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;



/**
 * Contains a Database Public API implementation.
 * @since 0.0.1
 * @author Mario Briggs
 */
public class Database {

	static final Log log = LogFactory.getLog(Database.class);
	private static Gson ownGSON;
	static {
		initGson();
	}
	private CouchDatabase db;
	private CloudantClient client;

	public enum Permissions {
		_admin,
		_reader,
		_writer
	}
	
	
	/**
	 * 
	 * @param client
	 * @param db
	 */
	Database(CloudantClient client, CouchDatabase db ) {
		super();
		this.client = client;
		this.db = db;
	}
	
	/**
	 * Set the permissions for this DB
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
		return client.get(buildUri(getDBUri()).path("_shards/").path(docId).build(), Shard.class);
	}


	/**
	 * Create a new index
	 * @param indexName optional name of the index (if not provided one will be generated) 
	 * @param designDocName optional name of the design doc in which the index will be created
	 * @param indexType optional, type of index (only "json" as of now)
	 * @param fields array of fields in the index
	 */
	 public void createIndex(String indexName, String designDocName, String indexType, IndexField[] fields) {
		String indexDefn = getIndexDefinition(indexName,designDocName,indexType,fields);
		createIndex(indexDefn);
	 }
	 
	 /**
	  * create a new Index
	  * @param @see <a href="http://docs.cloudant.com/api/cloudant-query.html#creating-a-new-index">indexDefinition</a> 
	  */
	 public void createIndex(String indexDefinition) {
		 assertNotEmpty(indexDefinition, "indexDefinition");
		 HttpResponse putresp = null;
		 URI uri = buildUri(getDBUri()).path("_index").build();
		 try {
			 putresp = client.executeRequest(createPost(uri,indexDefinition,"application/json"));
			 String result = getAsString(putresp,"result");
			 if  (result.equalsIgnoreCase("created")) {
				 log.info(String.format("Created Index: '%s'", indexDefinition));
			 }
			 else {
				 log.warn(String.format("Index already exists : '%s'", indexDefinition));
			 }
		 }
		 finally {
			 close(putresp);
		 }
	 }
	 
	  
	 /**
	  * Find documents using an index 
	  * @param selectorJson JSON object describing criteria used to select documents.
	  *        Is of the form "selector": { <your data here> } @see <a href="http://docs.cloudant.com/api/cloudant-query.html#cloudant-query-selectors">selector syntax</a>
	  * @param classOfT The class of Java objects to be returned
	  * @return List of classOfT objects
	  */
	 public <T> List<T> findByIndex(String selectorJson, Class<T> classOfT) {
		 return findByIndex(selectorJson, null, null, null, null , null, classOfT);
	 }
	 
	 /**
	  * Find documents using an index 
	  * @param selectorJson JSON object describing criteria used to select documents.
	  *        Is of the form "selector": { <your data here> }  @see <a href="http://docs.cloudant.com/api/cloudant-query.html#cloudant-query-selectors">selector syntax</a>
	  * @param limit optional, maximum number of results to be returned
	  * @param skip optional, skip first n results, where n is the specified value
	  * @param classOfT The class of Java objects to be returned
	  * @return List of classOfT objects
	  */
	 public <T> List<T> findByIndex(String selectorJson, Integer limit,
			 				Integer skip, Class<T> classOfT) {
		 return findByIndex(selectorJson, null, limit, skip, null , null, classOfT);
	}
	 
	 /**
	  * Find documents using an index 
	  * @param selectorJson JSON object describing criteria used to select documents.
	  *        Is of the form "selector": { <your data here> } @see <a href="http://docs.cloudant.com/api/cloudant-query.html#cloudant-query-selectors">selector syntax</a>
	  * @param sortOrder optional sort field order
	  * @param limit optional, maximum number of results to be returned
	  * @param skip optional, skip first n results, where n is the specified value
	  * @param returnFields optional, specify which fields of an object should be returned. If it is omitted, the entire object is returned.
	  * @param classOfT The class of Java objects to be returned
	  * @return List of classOfT objects
	  */
	 public <T> List<T> findByIndex(String selectorJson, IndexField[] sortOrder,
						Integer limit, Integer skip, String[] returnFields, Class<T> classOfT) {
		 return findByIndex(selectorJson, sortOrder, limit, skip, returnFields , null, classOfT);
	 }
	 
	 
	 /**
	  * Find documents using an index 
	  * @param selectorJson JSON object describing criteria used to select documents.
	  *        Is of the form "selector": { <your data here> } @see <a href="http://docs.cloudant.com/api/cloudant-query.html#cloudant-query-selectors">selector syntax</a>
	  * @param sortOrder optional sort field order
	  * @param limit optional, maximum number of results to be returned
	  * @param skip optional, skip first n results, where n is the specified value
	  * @param returnFields optional, specify which fields of an object should be returned. If it is omitted, the entire object is returned.
	  * @param readQuorum optional, default = 1. Read quorum needed for the result
	  * @param classOfT The class of Java objects to be returned
	  * @return List of classOfT objects
	  */
	 public <T> List<T> findByIndex(String selectorJson, IndexField[] sortOrder,
				Integer limit, Integer skip, String[] returnFields,
				Integer readQuorum, Class<T> classOfT) {
		 assertNotEmpty(selectorJson, "selectorJson");
		 URI uri = buildUri(getDBUri()).path("_find").build();
		 String body = getFindByIndexBody(selectorJson, sortOrder,
	 								limit,  skip,  returnFields,  readQuorum);
		 InputStream stream = null; 
		 try {
			 stream = getStream(client.executeRequest(createPost(uri, body, "application/json")));
			 Reader reader = new InputStreamReader(stream);
			 JsonArray jsonArray = new JsonParser().parse(reader)
						.getAsJsonObject().getAsJsonArray("docs");
			List<T> list = new ArrayList<T>();
			for (JsonElement jsonElem : jsonArray) {
				JsonElement elem = jsonElem.getAsJsonObject();
				T t = ownGSON.fromJson(elem, classOfT);
				list.add(t);
			}
			return list;
		 }
		 finally {
			 close(stream);
		 }
		
	 }
	 
	 /**
	  * List all indices
	  * @return List of Index
	  */
	 public List<Index> listIndices() {
		 HttpResponse response = null;
		 try {
			 response = client.executeRequest(new HttpGet(buildUri(getDBUri()).path("_index/").build()));
			 return getResponseList(response, ownGSON, Index.class,
							new TypeToken<List<Index>>(){}.getType());
		 }
		 finally {
			 close(response);
		 }
	 }
	 
	 /**
	  * Delete an index
	  * @param indexName name of the index
	  * @param designDocId ID of the design doc
	  */
	 public void deleteIndex(String indexName, String designDocId) {
		 assertNotEmpty(indexName, "indexName");
		 assertNotEmpty(designDocId, "designDocId");
		 URI uri = buildUri(getDBUri()).path("_index/").path(designDocId).path("/json/").path(indexName).build();
		 HttpResponse response = null;
		try {
			response = client.executeRequest(new HttpDelete(uri)); 
			getResponse(response,Response.class, getGson());
		} finally {
			close(response);
		}
	 }
	 
	 /**
	  * Provides access to Cloudant <tt>Search</tt> APIs.
	  * @see Search
	  */
	 public Search search(String searchIndexId) {
		 return new Search(this, searchIndexId);
	 }
	 
	 /**
	  * Provides access to CouchDB Design Documents.
	  * @see CouchDbDesign
	  */
	public CouchDbDesign design() {
		return db.design();
	}



	/**
	 * Provides access to CouchDB <tt>View</tt> APIs.
	 * @see View
	 */
	public View view(String viewId) {
		return db.view(viewId);
	}



	/**
	 * Provides access to <tt>Change Notifications</tt> API.
	 * @see Changes
	 */
	public Changes changes() {
		return db.changes();
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
		return db.find(classType, id);
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
		return db.find(classType, id, params);
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
		return db.find(classType, id, rev);
	}



	/**
	 * This method finds any document given a URI.
	 * <p>The URI must be URI-encoded.
	 * @param classType The class of type T.
	 * @param uri The URI as string.
	 * @return An object of type T.
	 */
	public <T> T findAny(Class<T> classType, String uri) {
		return db.findAny(classType, uri);
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
		return db.find(id);
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
		return db.find(id, rev);
	}



	/**
	 * Checks if a document exist in the database.
	 * @param id The document _id field.
	 * @return true If the document is found, false otherwise.
	 */
	public boolean contains(String id) {
		return db.contains(id);
	}


	/**
	 * Saves an object in the database, using HTTP <tt>PUT</tt> request.
	 * <p>If the object doesn't have an <code>_id</code> value, the code will assign a <code>UUID</code> as the document id.
	 * @param object The object to save
	 * @throws DocumentConflictException If a conflict is detected during the save.
	 * @return {@link Response}
	 */
	public Response save(Object object) {
		return db.save(object);
	}



	/**
	 * Saves an object in the database using HTTP <tt>POST</tt> request.
	 * <p>The database will be responsible for generating the document id.
	 * @param object The object to save
	 * @return {@link Response}
	 */
	public Response post(Object object) {
		return db.post(object);
	}



	/**
	 * Saves a document with <tt>batch=ok</tt> query param.
	 * @param object The object to save.
	 */
	public void batch(Object object) {
		db.batch(object);
	}



	/**
	 * Updates an object in the database, the object must have the correct <code>_id</code> and <code>_rev</code> values.
	 * @param object The object to update
	 * @throws DocumentConflictException If a conflict is detected during the update.
	 * @return {@link Response}
	 */
	public Response update(Object object) {
		return db.update(object);
	}



	/**
	 * Removes a document from the database. 
	 * <p>The object must have the correct <code>_id</code> and <code>_rev</code> values.
	 * @param object The document to remove as object.
	 * @throws NoDocumentException If the document is not found in the database.
	 * @return {@link Response}
	 */
	public Response remove(Object object) {
		return db.remove(object);
	}



	/**
	 * Removes a document from the database given both a document <code>_id</code> and <code>_rev</code> values.
	 * @param id The document _id field.
	 * @param rev The document _rev field.
	 * @throws NoDocumentException If the document is not found in the database.
	 * @return {@link Response}
	 */
	public Response remove(String id, String rev) {
		return db.remove(id, rev);
	}



	/**
	 * Performs a Bulk Documents insert request.
	 * @param objects The {@link List} of objects.
	 * @return {@code List<Response>} Containing the resulted entries.
	 */
	public List<Response> bulk(List<?> objects) {
		return db.bulk(objects, false);
	}



	/**
	 * Saves an attachment to a new document with a generated <tt>UUID</tt> as the document id.
	 * <p>To retrieve an attachment, see {@link #find(String)}.
	 * @param instream The {@link InputStream} holding the binary data.
	 * @param name The attachment name.
	 * @param contentType The attachment "Content-Type".
	 * @return {@link Response}
	 */
	public Response saveAttachment(InputStream in, String name,
			String contentType) {
		return db.saveAttachment(in, name, contentType);
	}



	/**
	 * Saves an attachment to an existing document given both a document id
	 * and revision, or save to a new document given only the id, and rev as {@code null}.
	 * <p>To retrieve an attachment, see {@link #find(String)}.
	 * @param instream The {@link InputStream} holding the binary data.
	 * @param name The attachment name.
	 * @param contentType The attachment "Content-Type".
	 * @param docId The document id to save the attachment under, or {@code null} to save under a new document.
	 * @param docRev The document revision to save the attachment under, or {@code null} when saving to a new document.
	 * @throws DocumentConflictException 
	 * @return {@link Response}
	 */
	public Response saveAttachment(InputStream in, String name,
			String contentType, String docId, String docRev) {
		return db.saveAttachment(in, name, contentType, docId, docRev);
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
	 */
	public String invokeUpdateHandler(String updateHandlerUri, String docId,
			String query) {
		return db.invokeUpdateHandler(updateHandlerUri, docId, query);
	}



	/**
	 * Invokes an Update Handler.
	 * <p>Use this method in particular when the docId contain special characters such as slashes (/).
	 * <pre>
	 * Params params = new Params()
	 *	.addParam("field", "foo")
	 *	.addParam("value", "bar"); 
	 * String output = dbClient.invokeUpdateHandler("designDoc/update1", "docId", params);
	 * </pre>
	 * @param updateHandlerUri The Update Handler URI, in the format: <code>designDoc/update1</code>
	 * @param docId The document id to update.
	 * @param query The query parameters as {@link Params}.
	 * @return The output of the request.
	 */
	public String invokeUpdateHandler(String updateHandlerUri, String docId,
			Params params) {
		return db.invokeUpdateHandler(updateHandlerUri, docId, params);
	}



	/**
	 * Synchronize all design documents with the database.
	 */
	public void syncDesignDocsWithDb() {
		db.syncDesignDocsWithDb();
	}



	/**
	 * @return The database URI.
	 */
	public URI getDBUri() {
		return db.getDBUri();
	}



	/**
	 * @return {@link CouchDbInfo} Containing the DB info.
	 */
	public CouchDbInfo info() {
		return db.info();
	}



	/**
	 * Requests the database commits any recent changes to disk.
	 */
	public void ensureFullCommit() {
		db.ensureFullCommit();
	}


	// private helper methods

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
	 * Form a create index json from parameters
	 */
	private String getIndexDefinition(String indexName, String designDocName,
				String indexType, IndexField[] fields) {
		assertNotEmpty(fields, "index fields");
		boolean addComma = false;
		String json = "{";
		if ( !(indexName == null || indexName.isEmpty()) ) {
			json += "\"name\": \"" + indexName + "\"";
			addComma = true;
		}
		if ( !(designDocName == null || designDocName.isEmpty()) ) {
			if (addComma) {
				json += ",";
			}
			json += "\"ddoc\": \"" + designDocName + "\"";
			addComma = true;
		}
		if ( !(indexType == null || indexType.isEmpty()) ) {
			if (addComma) {
				json += ",";
			}
			json += "\"type\": \"" + indexType + "\"";
			addComma = true;
		}
		
		if (addComma) {
			json += ",";
		}
		json += "\"index\": { \"fields\": [";
		for (int i = 0 ; i < fields.length; i++) {
			json += "{\"" + fields[i].getName() + "\": " +  "\"" + fields[i].getOrder() + "\"}";
			if ( i+1 < fields.length) {
				json += ",";
			}
		}
		
		return json + "] }}";
	}
	 
	/**
	 * 
	 * @param selectorJson
	 * @param sortOrder
	 * @param limit
	 * @param skip
	 * @param returnFields
	 * @param readQuorum
	 * @return
	 */
	private String getFindByIndexBody(String selectorJson,
			IndexField[] sortOrder, Integer limit, Integer skip,
			String[] returnFields, Integer readQuorum) {
		
		StringBuilder rf = null;
		if ( !(returnFields == null || returnFields.length == 0) ) {
			rf = new StringBuilder("\"fields\": [");
			int i = 0;
			for ( String s : returnFields ) {
				if (i > 0 ) {
					rf.append(",");
				}
				rf.append("\"").append(s).append("\"");
				i++;
			}
			rf.append("]");
		}
		
		StringBuilder so = null;
		if ( !(sortOrder == null || sortOrder.length == 0) ) {
			so = new StringBuilder("\"sort\": [");
			int i = 0;
			for ( IndexField idxfld : sortOrder ) {
				if (i > 0 ) {
					so.append(",");
				}
				so.append("{\"")
					   .append(idxfld.getName())
					   .append("\": \"")
					   .append(idxfld.getOrder())
					   .append("\"}");
			}
			so.append("]");
		}
		
		StringBuilder finalbody = new StringBuilder("{" +selectorJson);
		if ( rf != null ) {
			finalbody.append(",")
					 .append(rf.toString());
		}
		if ( so != null ) {
			finalbody.append(",")
					 .append(so.toString());
		}
		if ( limit != null ) {
			finalbody.append(",")
					 .append("\"limit\": ")
					 .append(limit);
		}
		if ( skip != null ) {
			finalbody.append(",")
					 .append("\"skip\": ")
					 .append(skip);
		}
		if ( readQuorum != null ) {
			finalbody.append(",")
					 .append("\"r\": ")
					 .append(readQuorum);
		}
		finalbody.append("}");
		
		return finalbody.toString();
	}
	
	/**
	 * setup our own Deserializers
	 */
	private static void initGson() {
		GsonBuilder builder = GsonHelper.initGson(new GsonBuilder());
		builder.registerTypeAdapter(new TypeToken<List<Shard>>(){}.getType(), new ShardDeserializer())
			   .registerTypeAdapter(new TypeToken<List<Index>>(){}.getType(), new IndexDeserializer());
		ownGSON = builder.create();
	}
	
	static Gson getGson() {
		return ownGSON;
	}
	
	
	HttpResponse executeRequest(HttpRequestBase request) {
		return client.executeRequest(request);
	}
}
