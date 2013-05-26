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

import static org.lightcouch.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.CouchDbUtil.close;
import static org.lightcouch.CouchDbUtil.generateUUID;
import static org.lightcouch.CouchDbUtil.getElement;
import static org.lightcouch.CouchDbUtil.streamToString;
import static org.lightcouch.URIBuilder.builder;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * <p>Presents a client to a CouchDB database instance.
 * <p>This is the main class to use to gain access to the various APIs defined by this client.
 * 
 * <h3>Usage Example:</h3> 
 * <p>Instantiating an instance of this class requires configuration options to be supplied.
 * Properties files may be used for this purpose. See overloaded constructors for available options.
 * <p>A typical example for creating an instance is by preparing a properties file named 
 * <tt>couchdb.properties</tt> and placing it in your application classpath:
 * 
 * <pre>
 * couchdb.name=my-db
 * couchdb.createdb.if-not-exist=true
 * couchdb.protocol=http
 * couchdb.host=127.0.0.1
 * couchdb.port=5984
 * couchdb.username=
 * couchdb.password=
 * </pre>
 * 
 * <p>Then construct a new instance using the default constructor: 
 * <pre>
 * CouchDbClient dbClient = new CouchDbClient(); // looks for <tt>classpath:couchdb.properties</tt>
 * // access the API here
 * </pre>
 * <p>Multiple client instances could be created to handle multiple database instances simultaneously in a thread-safe manner, 
 * typically one client for each database. 
 * 
 * <p>A client instance provides access to various APIs, accessible under several locations or contexts.
 * <p>Document APIs are available directly under this instance:
 * <pre>
 *  Foo foo = dbClient.find(Foo.class, "some-id");
 * </pre>
 * 
 * <p>Design documents API under the context <tt>design()</tt> {@link CouchDbDesign} contains usage example.
 * 
 * <p>View APIs under the context <tt>view()</tt> {@link View} contains usage examples.
 * 
 * <p>Change Notifications API under the context <tt>changes()</tt> see {@link Changes} for usage example.
 * 
 * <p>Replication APIs under two contexts: <tt>replication()</tt> and <tt>replicator()</tt>, 
 * the latter supports the replicator database introduced with CouchDB v 1.1.0 
 * {@link Replication} and {@link Replicator} provide usage examples.
 * 
 * <p>Database APIs under the context <tt>context()</tt>
 * 
 * <p>After completing usage of this client, it might be useful to shutdown it's 
 * underlying connection manager to ensure proper release of resources: 
 * <tt>dbClient.shutdown()</tt>
 * 
 * @author Ahmed Yehia
 *
 */
public final class CouchDbClient extends CouchDbClientBase {

	// -------------------------------------------------------------------------- Constructors
	/**
	 * Constructs a new instance of this class, expects a configuration file named 
	 * <code>couchdb.properties</code> to be available in your application classpath.
	 */
	public CouchDbClient() {
		super();
	}
	
	/**
	 * Constructs a new instance of this class.
	 * @param configFileName The configuration file name.
	 */
	public CouchDbClient(String configFileName) {
		super(new CouchDbConfig(configFileName));
	}
	
	/**
	 * Constructs a new instance of this class.
	 * @param dbName The database name.
	 * @param createDbIfNotExist To create a new database if it does not already exist.
	 * @param protocol The protocol to use (i.e http or https)
	 * @param host The database host address
	 * @param port The database listening port
	 * @param username The Username credential
	 * @param password The Password credential
	 */
	public CouchDbClient(String dbName, boolean createDbIfNotExist, 
			String protocol, String host, int port, String username, String password) { 
		super(new CouchDbConfig(new CouchDbProperties(dbName, createDbIfNotExist, protocol, host, port, username, password)));
	}
	
	/**
	 * Constructs a new instance of this class.
	 * @param properties An object containing configuration properties.
	 * @see {@link CouchDbProperties}
	 */
	public CouchDbClient(CouchDbProperties properties) {
		super(new CouchDbConfig(properties));
	}
	
	private CouchDbContext context;
	private CouchDbDesign design;
	
	{ 
		context = new CouchDbContext(this); 
		design = new CouchDbDesign(this);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setGsonBuilder(GsonBuilder gsonBuilder) {
		super.setGsonBuilder(gsonBuilder);
	}
	
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
	 * Synchronize all design documents on desk with the database.
	 * <p>Shorthand for {@link CouchDbDesign#synchronizeAllWithDb()}
	 * <p>This method might be used to sync design documents upon a client creation, eg. a Spring bean init-method.
	 */
	public void syncDesignDocsWithDb() {
		design().synchronizeAllWithDb();
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
	 * {@inheritDoc}
	 */
	@Override
	public URI getDBUri() {
		return super.getDBUri();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public URI getBaseUri() {
		return super.getBaseUri();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Gson getGson() {
		return super.getGson();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdown() {
		super.shutdown();
	}
}
