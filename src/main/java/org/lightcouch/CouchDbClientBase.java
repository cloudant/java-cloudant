/*
 * Copyright (C) 2011 lightcouch.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lightcouch;

import static org.lightcouch.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.CouchDbUtil.assertNull;
import static org.lightcouch.CouchDbUtil.close;
import static org.lightcouch.CouchDbUtil.generateUUID;
import static org.lightcouch.CouchDbUtil.getAsString;
import static org.lightcouch.CouchDbUtil.getStream;
import static org.lightcouch.CouchDbUtil.streamToString;
import static org.lightcouch.URIBuilder.buildUri;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

/**
 * Contains a client Public API implementation.
 * @see CouchDbClient
 * @see CouchDbClientAndroid
 * @author Ahmed Yehia
 */
public abstract class CouchDbClientBase {

	static final Log log = LogFactory.getLog(CouchDbClient.class);

	private URI baseURI;
	private URI dbURI;
	private Gson gson; 
	private CouchDbContext context;
	private CouchDbDesign design;
	final HttpClient httpClient;
	final HttpHost host;
	
	CouchDbClientBase() {
		this(new CouchDbConfig());
	}
	
	CouchDbClientBase(CouchDbConfig config) {
		final CouchDbProperties props = config.getProperties();
		this.httpClient = createHttpClient(props);
		this.gson = initGson(new GsonBuilder());
		this.host = new HttpHost(props.getHost(), props.getPort(), props.getProtocol());
		
		final String path = props.getPath() != null ? props.getPath() : "";
        this.baseURI = buildUri().scheme(props.getProtocol()).host(props.getHost()).port(props.getPort()).path("/").path(path).build();
		this.dbURI   = buildUri(baseURI).path(props.getDbName()).path("/").build();
		
		this.context = new CouchDbContext(this, props); 
		this.design = new CouchDbDesign(this);
	}
	
	// Client(s) provided implementation
	
	/**
	 * @return {@link HttpClient} instance for HTTP request execution.
	 */
	abstract HttpClient createHttpClient(CouchDbProperties properties);
	
	/**
	 * @return {@link HttpContext} instance for HTTP request execution.
	 */
	abstract HttpContext createContext();

	/**
	 * Shuts down the connection manager used by this client instance.
	 */
	abstract void shutdown();
	
	// Public API
	
	/**
	 * Provides access to DB server APIs.
	 * @see CouchDbContext
	 */
	public CouchDbContext context() {
		return context;
	}
	
	/**
	 * Provides access to CouchDB Design Documents.
	 * @see CouchDbDesign
	 */
	public CouchDbDesign design() {
		return design;
	}
	
	/**
	 * Provides access to CouchDB <tt>View</tt> APIs.
	 * @see View
	 */
	public View view(String viewId) {
		return new View(this, viewId);
	}

	/**
	 * Provides access to CouchDB <tt>replication</tt> APIs.
	 * @see Replication
	 */
	public Replication replication() {
		return new Replication(this);
	}
	
	/**
	 * Provides access to the <tt>replicator database</tt>.
	 * @see Replicator
	 */
	public Replicator replicator() {
		return new Replicator(this);
	}
	
	/**
	 * Provides access to <tt>Change Notifications</tt> API.
	 * @see Changes
	 */
	public Changes changes() {
		return new Changes(this);
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
		assertNotEmpty(classType, "Class");
		assertNotEmpty(id, "id");
		final URI uri = buildUri(getDBUri()).pathToEncode(id).buildEncoded();
		return get(uri, classType);
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
		assertNotEmpty(classType, "Class");
		assertNotEmpty(id, "id");
		final URI uri = buildUri(getDBUri()).pathToEncode(id).query(params).buildEncoded();
		return get(uri, classType);
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
		assertNotEmpty(classType, "Class");
		assertNotEmpty(id, "id");
		assertNotEmpty(id, "rev");
		final URI uri = buildUri(getDBUri()).pathToEncode(id).query("rev", rev).buildEncoded();
		return get(uri, classType);
	}
	
	/**
	 * This method finds any document given a URI.
	 * <p>The URI must be URI-encoded.
	 * @param classType The class of type T.
	 * @param uri The URI as string.
	 * @return An object of type T.
	 */
	public <T> T findAny(Class<T> classType, String uri) {
		assertNotEmpty(classType, "Class");
		assertNotEmpty(uri, "uri");
		return get(URI.create(uri), classType);
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
		assertNotEmpty(id, "id");
		return get(buildUri(getDBUri()).path(id).build());
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
		assertNotEmpty(id, "id");
		assertNotEmpty(rev, "rev");
		final URI uri = buildUri(getDBUri()).path(id).query("rev", rev).build();
		return get(uri);
	}
	
	/**
	 * Checks if a document exist in the database.
	 * @param id The document _id field.
	 * @return true If the document is found, false otherwise.
	 */
	public boolean contains(String id) { 
		assertNotEmpty(id, "id");
		HttpResponse response = null;
		try {
			response = head(buildUri(getDBUri()).path(id).build());
		} catch (NoDocumentException e) {
			return false;
		} finally {
			close(response);
		}
		return true;
	}
	
	/**
	 * Saves an object in the database.
	 * @param object The object to save
	 * @throws DocumentConflictException If a conflict is detected during the save.
	 * @return {@link Response}
	 */
	public Response save(Object object) {
		return put(getDBUri(), object, true);
	}
	
	/**
	 * Saves an object in the database using <tt>POST</tt> request.
	 * @param object The object to save
	 * @return {@link Response}
	 */
	public Response post(Object object) {
		assertNotEmpty(object, "object");
		HttpResponse response = null;
		try { 
			URI uri = buildUri(getDBUri()).build();
			response = post(uri, getGson().toJson(object));
			return getResponse(response);
		} finally {
			close(response);
		}
	}
	
	/**
	 * Saves a document with <tt>batch=ok</tt> query param.
	 * @param object The object to save.
	 */
	public void batch(Object object) {
		assertNotEmpty(object, "object");
		HttpResponse response = null;
		try { 
			URI uri = buildUri(getDBUri()).query("batch", "ok").build();
			response = post(uri, getGson().toJson(object));
		} finally {
			close(response);
		}
	}
	
	/**
	 * Updates an object in the database, the object must have the correct id and revision values.
	 * @param object The object to update
	 * @throws DocumentConflictException If a conflict is detected during the update.
	 * @return {@link Response}
	 */
	public Response update(Object object) {
		return put(getDBUri(), object, false);
	}
	
	/**
	 * Removes a document from the database. 
	 * <p>The object must have the correct _id and _rev values.
	 * @param object The document to remove as object.
	 * @throws NoDocumentException If the document is not found in the database.
	 * @return {@link Response}
	 */
	public Response remove(Object object) {
		assertNotEmpty(object, "object");
		JsonObject jsonObject = getGson().toJsonTree(object).getAsJsonObject();
		final String id = getAsString(jsonObject, "_id");
		final String rev = getAsString(jsonObject, "_rev");
		return remove(id, rev);
	}
	
	/**
	 * Removes a document from the database given both _id and _rev values.
	 * @param id The document _id field.
	 * @param rev The document _rev field.
	 * @throws NoDocumentException If the document is not found in the database.
	 * @return {@link Response}
	 */
	public Response remove(String id, String rev) {
		assertNotEmpty(id, "id");
		assertNotEmpty(rev, "rev");
		final URI uri = buildUri(getDBUri()).pathToEncode(id).query("rev", rev).buildEncoded();
		return delete(uri);
	}
	
	/**
	 * Performs a Bulk Documents request.
	 * @param objects The {@link List} of objects.
	 * @param allOrNothing Indicates whether the request has <tt>all-or-nothing</tt> semantics.
	 * @return {@code List<Response>} Containing the resulted entries.
	 */
	public List<Response> bulk(List<?> objects, boolean allOrNothing) {
		assertNotEmpty(objects, "objects");
		HttpResponse response = null;
		try { 
			String allOrNothingVal = allOrNothing ? "\"all_or_nothing\": true, " : "";
			URI uri = buildUri(getDBUri()).path("_bulk_docs").build();
			String json = String.format("{%s%s%s}", allOrNothingVal, "\"docs\": ", getGson().toJson(objects));
			response = post(uri, json);
			return getResponseList(response);
		} finally {
			close(response);
		}
	}
	
	/**
	 * Saves an attachment to a new document with a generated <tt>UUID</tt> as the document id.
	 * <p>To retrieve an attachment, see {@link #find(String)}.
	 * @param instream The {@link InputStream} holding the binary data.
	 * @param name The attachment name.
	 * @param contentType The attachment "Content-Type".
	 * @return {@link Response}
	 */
	public Response saveAttachment(InputStream in, String name, String contentType) {
		assertNotEmpty(in, "in");
		assertNotEmpty(name, "name");
		assertNotEmpty(contentType, "ContentType");
		final URI uri = buildUri(getDBUri()).path(generateUUID()).path("/").path(name).build();
		return put(uri, in, contentType);
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
	public Response saveAttachment(InputStream in, String name, String contentType, String docId, String docRev) {
		assertNotEmpty(in, "in");
		assertNotEmpty(name, "name");
		assertNotEmpty(contentType, "ContentType");
		assertNotEmpty(docId, "docId");
		final URI uri = buildUri(getDBUri()).path(docId).path("/").path(name).query("rev", docRev).build();
		return put(uri, in, contentType);
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
	public String invokeUpdateHandler(String updateHandlerUri, String docId, String query) {
		assertNotEmpty(updateHandlerUri, "uri");
		assertNotEmpty(docId, "docId");
		final String[] v = updateHandlerUri.split("/");
		final String path = String.format("_design/%s/_update/%s/", v[0], v[1]);
		final URI uri = buildUri(getDBUri()).path(path).path(docId).query(query).build();
		final HttpResponse response = executeRequest(new HttpPut(uri));
		return streamToString(getStream(response));
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
	public String invokeUpdateHandler(String updateHandlerUri, String docId, Params params) {
		assertNotEmpty(updateHandlerUri, "uri");
		assertNotEmpty(docId, "docId");
		final String[] v = updateHandlerUri.split("/");
		final String path = String.format("_design/%s/_update/%s/", v[0], v[1]);
		final URI uri = buildUri(getDBUri()).path(path).pathToEncode(docId).query(params).buildEncoded();
		final HttpResponse response = executeRequest(new HttpPut(uri));
		return streamToString(getStream(response));
	}
	
	/**
	 * Executes a HTTP request.
	 * <p><b>Note</b>: The response must be closed after use to release the connection.
	 * @param request The HTTP request to execute.
	 * @return {@link HttpResponse}
	 */
	public HttpResponse executeRequest(HttpRequestBase request) {
		try {
			return  httpClient.execute(host, request, createContext());
		} catch (IOException e) {
			request.abort();
			throw new CouchDbException("Error executing request. ", e);
		} 
	}
	
	/**
	 * Synchronize all design documents with the database.
	 */
	public void syncDesignDocsWithDb() {
		design().synchronizeAllWithDb();
	}
	
	/**
	 * Sets a {@link GsonBuilder} to create {@link Gson} instance.
	 * <p>Useful for registering custom serializers/deserializers, such as JodaTime classes.
	 */
	public void setGsonBuilder(GsonBuilder gsonBuilder) {
		this.gson = initGson(gsonBuilder);
	}
	
	/**
	 * @return The base URI.
	 */
	public URI getBaseUri() {
		return baseURI;
	}
	
	/**
	 * @return The database URI.
	 */
	public URI getDBUri() {
		return dbURI;
	}
    
	/**
	 * @return The Gson instance.
	 */
	public Gson getGson() {
		return gson;
	}
	
	// End - Public API
	
	/**
	 * Performs a HTTP GET request. 
	 * @return {@link InputStream} 
	 */
	InputStream get(HttpGet httpGet) {
		HttpResponse response = executeRequest(httpGet); 
		return getStream(response);
	}
	
	/**
	 * Performs a HTTP GET request. 
	 * @return {@link InputStream} 
	 */
	InputStream get(URI uri) {
		HttpGet get = new HttpGet(uri);
		get.addHeader("Accept", "application/json");
		return get(get);
	}
	
	/**
	 * Performs a HTTP GET request. 
	 * @return An object of type T
	 */
	<T> T get(URI uri, Class<T> classType) {
		InputStream in = null;
		try {
			in = get(uri);
			return getGson().fromJson(new InputStreamReader(in), classType);
		} finally {
			close(in);
		}
	}
	
	/**
	 * Performs a HTTP HEAD request. 
	 * @return {@link HttpResponse}
	 */
	HttpResponse head(URI uri) {
		return executeRequest(new HttpHead(uri));
	}
	
	/**
	 * Performs a HTTP PUT request, saves or updates a document.
	 * @return {@link Response}
	 */
	Response put(URI uri, Object object, boolean newEntity) {
		assertNotEmpty(object, "object");
		HttpResponse response = null;
		try {  
			JsonObject json = getGson().toJsonTree(object).getAsJsonObject();
			String id = getAsString(json, "_id");
			String rev = getAsString(json, "_rev");
			if(newEntity) { // save
				assertNull(rev, "rev");
				id = (id == null) ? generateUUID() : id;
			} else { // update
				assertNotEmpty(id, "id");
				assertNotEmpty(rev, "rev");
			}
			HttpPut put = new HttpPut(buildUri(uri).pathToEncode(id).buildEncoded());
			setEntity(put, json.toString());
			response = executeRequest(put); 
			return getResponse(response);
		} finally {
			close(response);
		}
	}
	
	/**
	 * Performs a HTTP PUT request, saves an attachment.
	 * @return {@link Response}
	 */
	Response put(URI uri, InputStream instream, String contentType) {
		HttpResponse response = null;
		try {
			HttpPut httpPut = new HttpPut(uri);
			InputStreamEntity entity = new InputStreamEntity(instream, -1);
			entity.setContentType(contentType);
			httpPut.setEntity(entity);
			response = executeRequest(httpPut);
			return getResponse(response);
		} finally {
			close(response);
		}
	}
	
	/**
	 * Performs a HTTP POST request.
	 * @return {@link HttpResponse}
	 */
	HttpResponse post(URI uri, String json) {
		HttpPost post = new HttpPost(uri);
		setEntity(post, json);
		return executeRequest(post);
	}
	
	/**
	 * Performs a HTTP DELETE request.
	 * @return {@link Response}
	 */
	Response delete(URI uri) {
		HttpResponse response = null;
		try {
			HttpDelete delete = new HttpDelete(uri);
			response = executeRequest(delete); 
			return getResponse(response);
		} finally {
			close(response);
		}
	}
	
	// Helpers
	
	/**
	 * Validates a HTTP response; on error cases logs status and throws relevant exceptions.
	 * @param response The HTTP response.
	 */
	void validate(HttpResponse response) throws IOException {
		int code = response.getStatusLine().getStatusCode();
		if(code == 200 || code == 201 || code == 202) { // success (ok | created | accepted)
			return;
		} 
		String reason = response.getStatusLine().getReasonPhrase();
		switch (code) {
		case HttpStatus.SC_NOT_FOUND: {
			throw new NoDocumentException(reason);
		}
		case HttpStatus.SC_CONFLICT: {
			throw new DocumentConflictException(reason);
		}
		default: { // other errors: 400 | 401 | 500 etc.
			throw new CouchDbException(reason += EntityUtils.toString(response.getEntity()));
		}
		}
	}
	
	/**
	 * @param response The {@link HttpResponse}
	 * @return {@link Response}
	 */
	private Response getResponse(HttpResponse response) throws CouchDbException {
		InputStreamReader reader = new InputStreamReader(getStream(response));
		return getGson().fromJson(reader, Response.class);
	}
	
	/**
	 * @param response The {@link HttpResponse}
	 * @return {@link Response}
	 */
	private List<Response> getResponseList(HttpResponse response) throws CouchDbException {
		InputStream instream = getStream(response);
		Reader reader = new InputStreamReader(instream);
		return getGson().fromJson(reader, new TypeToken<List<Response>>(){}.getType());
	}
	
	/**
	 * Sets a JSON String as a request entity.
	 * @param httpRequest The request to set entity.
	 * @param json The JSON String to set.
	 */
	private void setEntity(HttpEntityEnclosingRequestBase httpRequest, String json) {
		StringEntity entity = new StringEntity(json, "UTF-8");
		entity.setContentType("application/json");
		httpRequest.setEntity(entity);
	}
	
	/**
	 * Builds {@link Gson} and registers any required serializer/deserializer.
	 * @return {@link Gson} instance
	 */
	private Gson initGson(GsonBuilder gsonBuilder) {
		gsonBuilder.registerTypeAdapter(JsonObject.class, new JsonDeserializer<JsonObject>() {
			public JsonObject deserialize(JsonElement json,
					Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				return json.getAsJsonObject();
			}
		});
		gsonBuilder.registerTypeAdapter(JsonObject.class, new JsonSerializer<JsonObject>() {
			public JsonElement serialize(JsonObject src, Type typeOfSrc,
					JsonSerializationContext context) {
				return src.getAsJsonObject();
			}
			
		});
		return gsonBuilder.create();
	}
}
