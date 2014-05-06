/*
 * Copyright (C) 2011 Ahmed Yehia (ahmed.yehia.m@gmail.com)
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

import static java.lang.String.format;
import static org.lightcouch.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.CouchDbUtil.assertNull;
import static org.lightcouch.CouchDbUtil.close;
import static org.lightcouch.CouchDbUtil.generateUUID;
import static org.lightcouch.CouchDbUtil.getElement;
import static org.lightcouch.CouchDbUtil.streamToString;
import static org.lightcouch.URIBuilder.builder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
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
 * Base client class to be extended by a concrete subclass, responsible for establishing
 * a connection with the database and the definition of the basic HTTP request handling and validation. 
 * @see CouchDbClient
 * @see CouchDbClientAndroid
 * @author Ahmed Yehia
 */
public abstract class CouchDbClientBase {

	static final Log log = LogFactory.getLog(CouchDbClientBase.class);

	protected HttpClient httpClient;
	protected HttpHost host;
	private URI baseURI;
	private URI dbURI;
	private Gson gson; 
	private CouchDbConfig config;
	private CouchDbContext context;
	private CouchDbDesign design;
	
	protected CouchDbClientBase() {
		this(new CouchDbConfig());
	}
	
	protected CouchDbClientBase(CouchDbConfig config) {
		CouchDbProperties props = config.getProperties();
		this.httpClient = createHttpClient(props);
		this.gson = initGson(new GsonBuilder());
		this.host = new HttpHost(props.getHost(), props.getPort(), props.getProtocol());
		this.config = config;
		
		String path = props.getPath() != null ? props.getPath() : "";
        this.baseURI = builder().scheme(props.getProtocol()).host(props.getHost()).port(props.getPort()).path("/").path(path).build();
		this.dbURI   = builder(baseURI).path(props.getDbName()).path("/").build();
		
		this.context = new CouchDbContext(this); 
		this.design = new CouchDbDesign(this);
	}
	
	/**
	 * @return {@link HttpClient} instance for HTTP request execution.
	 */
	protected abstract HttpClient createHttpClient(CouchDbProperties properties);
	
	/**
	 * @return {@link HttpContext} instance for HTTP request execution.
	 */
	protected abstract HttpContext createContext();

	/**
	 * Shuts down the connection manager used by this client instance.
	 */
	protected abstract void shutdown();
	
	// Public API
	
	/**
	 * Provides access to the database APIs.
	 */
	public CouchDbContext context() {
		return context;
	}
	
	/**
	 * Provides access to the database design documents API.
	 */
	public CouchDbDesign design() {
		return design;
	}
	
	/**
	 * Provides access to the View APIs.
	 */
	public View view(String viewId) {
		return new View(this, viewId);
	}

	/**
	 * Provides access to the replication APIs.
	 */
	public Replication replication() {
		return new Replication(this);
	}
	
	/**
	 * Provides access to the replicator database APIs.
	 */
	public Replicator replicator() {
		return new Replicator(this);
	}
	
	/**
	 * Provides access to the Change Notifications API.
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
		return get(builder(getDBUri()).path(id).build(), classType);
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
		return get(builder(getDBUri()).path(id).query(params).build(), classType);
	}
	
	/**
	 * Finds an Object of the specified type.
	 * @param <T> Object type.
	 * @param classType The class of type T.
	 * @param id The document id to get.
	 * @param rev The document revision.
	 * @return An object of type T.
	 * @throws NoDocumentException If the document is not found in the database.
	 */
	public <T> T find(Class<T> classType, String id, String rev) {
		assertNotEmpty(classType, "Class");
		assertNotEmpty(id, "id");
		assertNotEmpty(id, "rev");
		URI uri = builder(getDBUri()).path(id).query("rev", rev).build();
		return get(uri, classType);
	}
	
	/**
	 * A General purpose find, that gives more control over the query.
	 * <p>Unlike other finders, this method expects a fully formated and encoded URI to be supplied.
	 * @param classType The class of type T.
	 * @param uri The URI.
	 * @return An object of type T.
	 */
	public <T> T findAny(Class<T> classType, String uri) {
		assertNotEmpty(classType, "Class");
		assertNotEmpty(uri, "uri");
		return get(URI.create(uri), classType);
	}
	
	/**
	 * <p>Finds a document and returns the result as an {@link InputStream}.</p>
	 * The stream should be properly closed after usage, as to avoid connection leaks.
	 * @param id The document id.
	 * @return The result of the request as an {@link InputStream}
	 * @throws NoDocumentException If the document is not found in the database.
	 * @see #find(String, String)
	 */
	public InputStream find(String id) {
		assertNotEmpty(id, "id");
		return get(builder(getDBUri()).path(id).build());
	}
	
	/**
	 * <p>Finds a document given an id and revision, returns the result as {@link InputStream}.</p>
	 * The stream should be properly closed after usage, as to avoid connection leaks.
	 * @param id The document id.
	 * @param rev The document revision.
	 * @return The result of the request as an {@link InputStream}
	 * @throws NoDocumentException If the document is not found in the database.
	 */
	public InputStream find(String id, String rev) {
		assertNotEmpty(id, "id");
		assertNotEmpty(rev, "rev");
		return get(builder(getDBUri()).path(id).query("rev", rev).build());
	}
	
	/**
	 * Checks if the database contains a document given an id.
	 * @param id The document id.
	 * @return true If the document is found, false otherwise.
	 */
	public boolean contains(String id) { 
		assertNotEmpty(id, "id");
		HttpResponse response = null;
		try {
			response = head(builder(getDBUri()).path(id).build());
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
	 * Saves the given object in a batch request.
	 * @param object The object to save.
	 */
	public void batch(Object object) {
		assertNotEmpty(object, "object");
		HttpResponse response = null;
		try { 
			URI uri = builder(getDBUri()).query("batch", "ok").build();
			response = post(uri, getGson().toJson(object));
		} finally {
			close(response);
		}
	}
	
	/**
	 * Performs a Bulk Documents request.
	 * @param objects The {@link List} of objects.
	 * @param allOrNothing Indicated whether the request has all-or-nothing semantics.
	 * @return {@code List<Response>} Containing the resulted entries.
	 */
	public List<Response> bulk(List<?> objects, boolean allOrNothing) {
		assertNotEmpty(objects, "objects");
		HttpResponse response = null;
		try { 
			String allOrNothingVal = allOrNothing ? "\"all_or_nothing\": true, " : "";
			URI uri = builder(getDBUri()).path("_bulk_docs").build();
			String json = String.format("{%s%s%s}", allOrNothingVal, "\"docs\": ", getGson().toJson(objects));
			response = post(uri, json);
			return getResponseList(response);
		} finally {
			close(response);
		}
	}
	
	/**
	 * <p>Saves an attachment under a new document with a generated UUID as the document id.
	 * <p>To retrieve an attachment, see {@link #find(String)}.
	 * @param instream The {@link InputStream} holding the binary data.
	 * @param name The attachment name.
	 * @param contentType The attachment "Content-Type".
	 * @return {@link Response}
	 */
	public Response saveAttachment(InputStream instream, String name, String contentType) {
		assertNotEmpty(instream, "InputStream");
		assertNotEmpty(name, "name");
		assertNotEmpty(contentType, "ContentType");
		URI uri = builder(getDBUri()).path(generateUUID()).path("/").path(name).build();
		return put(uri, instream, contentType);
	}
	
	/**
	 * <p>Saves an attachment under an existing document given both a document id
	 * and revision, or under a new document given only the document id.
	 * <p>To retrieve an attachment, see {@link #find(String)}.
	 * @param instream The {@link InputStream} holding the binary data.
	 * @param name The attachment name.
	 * @param contentType The attachment "Content-Type".
	 * @param docId The document id to save the attachment under, or {@code null} to save under a new document.
	 * @param docRev The document revision to save the attachment under, or {@code null} when saving to a new document.
	 * @throws DocumentConflictException 
	 * @return {@link Response}
	 */
	public Response saveAttachment(InputStream instream, String name, String contentType, String docId, String docRev) {
		assertNotEmpty(instream, "InputStream");
		assertNotEmpty(name, "name");
		assertNotEmpty(contentType, "ContentType");
		assertNotEmpty(docId, "DocId");
		URI uri = builder(getDBUri()).path(docId).path("/").path(name).query("rev", docRev).build();
		return put(uri, instream, contentType);
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
	 * Removes an object from the database, the object must have the correct id and revision values.
	 * @param object The object to remove
	 * @throws NoDocumentException If the document could not be found in the database.
	 * @return {@link Response}
	 */
	public Response remove(Object object) {
		assertNotEmpty(object, "object");
		JsonObject jsonObject = getGson().toJsonTree(object).getAsJsonObject();
		String id = getElement(jsonObject, "_id");
		String rev = getElement(jsonObject, "_rev");
		return remove(id, rev);
	}
	
	/**
	 * Removes a document from the database, given both an id and revision values.
	 * @param id The document id
	 * @param rev The document revision
	 * @throws NoDocumentException If the document could not be found in the database.
	 * @return {@link Response}
	 */
	public Response remove(String id, String rev) {
		assertNotEmpty(id, "id");
		assertNotEmpty(rev, "rev");
		return delete(builder(getDBUri()).path(id).query("rev", rev).build());
	}
	
	/**
	 * Invokes an Update Handler.
	 * @param updateHandlerUri The Update Handler URI, in the format: <code>designDocId/updateFunction</code>
	 * @param docId The document id to update.
	 * @param query The query string parameters, e.g, field=field1&value=value1
	 * @return The output of the request.
	 */
	public String invokeUpdateHandler(String updateHandlerUri, String docId, String query) {
		assertNotEmpty(updateHandlerUri, "updateHandlerUri");
		assertNotEmpty(docId, "docId");
		String[] v = updateHandlerUri.split("/");
		String path = String.format("_design/%s/_update/%s/%s", v[0], v[1], docId);
		URI uri = builder(getDBUri()).path(path).query(query).build();
		HttpResponse response = executeRequest(new HttpPut(uri));
		return streamToString(getStream(response));
	}
	
	/**
	 * Synchronize all design documents on desk with the database.
	 * <p>Shorthand for {@link CouchDbDesign#synchronizeAllWithDb()}
	 * <p>This method might be used to sync design documents upon a client creation, eg. a Spring bean init-method.
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
	 * @return The database URI.
	 */
	public URI getDBUri() {
		return dbURI;
	}
	
	/**
	 * @return The base URI.
	 */
	public URI getBaseUri() {
		return baseURI;
	}
	
	/**
	 * @return The Gson instance.
	 */
	public Gson getGson() {
		return gson;
	}
	
	// End - Public API
	
	// Getters
	
	protected CouchDbConfig getConfig() {
		return config;
	}
	
	// HTTP Requests
	
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
		InputStream instream = null;
		try {
			instream = get(uri);
			return deserialize(instream, classType);
		} finally {
			close(instream);
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
			String id = getElement(json, "_id");
			String rev = getElement(json, "_rev");
			if(newEntity) { // save
				assertNull(rev, "revision");
				id = (id == null) ? generateUUID() : id;
			} else { // update
				assertNotEmpty(id, "id");
				assertNotEmpty(rev, "revision");
			}
			HttpPut put = new HttpPut(builder(uri).path(id).build());
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
	
	/**
	 * Executes a HTTP request.
	 * @param request The HTTP request to execute.
	 * @return {@link HttpResponse}
	 */
	protected HttpResponse executeRequest(HttpRequestBase request) {
		try {
			return  httpClient.execute(host, request, createContext());
		} catch (IOException e) {
			request.abort();
			throw new CouchDbException("Error executing request. ", e);
		} 
	}
	
	// Helpers
	
	/**
	 * Validates a HTTP response; on error cases logs status and throws relevant exceptions.
	 * @param response The HTTP response.
	 */
	protected void validate(HttpResponse response) throws IOException {
		int code = response.getStatusLine().getStatusCode();
		if(code == 200 || code == 201 || code == 202) { // success (ok | created | accepted)
			return;
		} 
		String msg = format("<< Status: %s (%s) ", code, response.getStatusLine().getReasonPhrase());
		switch (code) {
		case HttpStatus.SC_NOT_FOUND: {
			log.info(msg); 
			throw new NoDocumentException(msg);
		}
		case HttpStatus.SC_CONFLICT: {
			log.warn(msg);
			throw new DocumentConflictException(msg);
		}
		default: { // other errors: 400 | 401 | 500 etc.
			log.error(msg += EntityUtils.toString(response.getEntity()));
			throw new CouchDbException(msg);
		}
		}
	}
	
	/**
	 * @param response The {@link HttpResponse}
	 * @return {@link Response}
	 */
	Response getResponse(HttpResponse response) throws CouchDbException {
		return deserialize(getStream(response), Response.class);
	}
	
	/**
	 * @param response The {@link HttpResponse}
	 * @return {@link Response}
	 */
	List<Response> getResponseList(HttpResponse response) throws CouchDbException {
		InputStream instream = getStream(response);
		Reader reader = new InputStreamReader(instream);
		return getGson().fromJson(reader, new TypeToken<List<Response>>(){}.getType());
	}
	
	/**
	 * Sets a JSON String as a request entity.
	 * @param httpRequest The request to set entity.
	 * @param json The JSON String to set.
	 */
	protected void setEntity(HttpEntityEnclosingRequestBase httpRequest, String json) {
		try {
			StringEntity entity = new StringEntity(json, "UTF-8");
			entity.setContentType("application/json");
			httpRequest.setEntity(entity);
		} catch (UnsupportedEncodingException e) {
			log.error("Error setting request data. " + e.getMessage());
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * @return {@link InputStream} from a {@link HttpResponse}
	 */
	InputStream getStream(HttpResponse response) {
		try { 
			return response.getEntity().getContent();
		} catch (Exception e) {
			log.error("Error reading response. " + e.getMessage());
			throw new CouchDbException(e);
		}
	}
	
	<T> T deserialize(InputStream instream, Class<T> classType) {
		Reader reader = new InputStreamReader(instream);
		return getGson().fromJson(reader, classType);
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
