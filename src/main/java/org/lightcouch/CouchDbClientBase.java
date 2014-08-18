package org.lightcouch;

import static org.lightcouch.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.CouchDbUtil.assertNull;
import static org.lightcouch.CouchDbUtil.close;
import static org.lightcouch.CouchDbUtil.generateUUID;
import static org.lightcouch.CouchDbUtil.getAsString;
import static org.lightcouch.CouchDbUtil.getStream;
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
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public abstract class CouchDbClientBase {
	
	static final Log log = LogFactory.getLog(CouchDbClient.class);
	
	private URI baseURI;
	final HttpClient httpClient;
	final HttpHost host;
	private Gson gson; 
	
	CouchDbClientBase() {
		this(new CouchDbConfig());
	}
	
	CouchDbClientBase(CouchDbConfig config) {
		
		final CouchDbProperties props = config.getProperties();
		this.httpClient = createHttpClient(props);
		this.host = new HttpHost(props.getHost(), props.getPort(), props.getProtocol());
		this.gson = initGson(new GsonBuilder());
		final String path = props.getPath() != null ? props.getPath() : "";
        this.baseURI = buildUri().scheme(props.getProtocol()).host(props.getHost()).port(props.getPort()).path("/").path(path).build();
		
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
		
		/**
		 * @return The base URI.
		 */
		public URI getBaseUri() {
			return baseURI;
		}
		

		abstract CouchDatabaseBase database(String name, boolean create);

		
		/**
		 * Requests CouchDB deletes a database.
		 * @param dbName The database name
		 * @param confirm A confirmation string with the value: <tt>delete database</tt>
		 */
		public void deleteDB(String dbName, String confirm) {
			assertNotEmpty(dbName, "dbName");
			if(!"delete database".equals(confirm))
				throw new IllegalArgumentException("Invalid confirm!");
			delete(buildUri(getBaseUri()).path(dbName).build());
			
			
		}

		/**
		 * Requests CouchDB creates a new database; if one doesn't exist.
		 * @param dbName The Database name
		 */
		public void createDB(String dbName) {
			assertNotEmpty(dbName, "dbName");
			InputStream getresp = null;
			HttpResponse putresp = null;
			final URI uri = buildUri(getBaseUri()).path(dbName).build();
			try {
				getresp = get(uri);
			} catch (NoDocumentException e) { // db doesn't exist
				final HttpPut put = new HttpPut(uri);
				putresp = executeRequest(put);
				log.info(String.format("Created Database: '%s'", dbName));
			} finally {
				close(getresp);
				close(putresp);
			}
		}

		/**
		 * @return All Server databases.
		 */
		public List<String> getAllDbs() {
			InputStream instream = null;
			try {
				Type typeOfList = new TypeToken<List<String>>() {}.getType();
				instream = get(buildUri(getBaseUri()).path("_all_dbs").build());
				Reader reader = new InputStreamReader(instream);
				return getGson().fromJson(reader, typeOfList);
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
				Reader reader = new InputStreamReader(instream);
				return getAsString(new JsonParser().parse(reader).getAsJsonObject(), "version");
			} finally {
				close(instream);
			}
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
		 * Request a database sends a list of UUIDs.
		 * @param count The count of UUIDs.
		 */
		public List<String> uuids(long count) {
			final String uri = String.format("%s_uuids?count=%d", getBaseUri(), count);
			final JsonObject json = get( URI.create(uri),JsonObject.class);
			return getGson().fromJson(json.get("uuids").toString(), new TypeToken<List<String>>(){}.getType());
		}

		
		/**
		 * Executes a HTTP request.
		 * <p><b>Note</b>: The response must be closed after use to release the connection.
		 * @param request The HTTP request to execute.
		 * @return {@link HttpResponse}
		 */
		public HttpResponse executeRequest(HttpRequestBase request) {
			try {
				return httpClient.execute(host, request, createContext());
			} catch (IOException e) {
				request.abort();
				throw new CouchDbException("Error executing request. ", e);
			} 
			
			
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
				final JsonObject json = getGson().toJsonTree(object).getAsJsonObject();
				String id = getAsString(json, "_id");
				String rev = getAsString(json, "_rev");
				if(newEntity) { // save
					assertNull(rev, "rev");
					id = (id == null) ? generateUUID() : id;
				} else { // update
					assertNotEmpty(id, "id");
					assertNotEmpty(rev, "rev");
				}
				final HttpPut put = new HttpPut(buildUri(uri).pathToEncode(id).buildEncoded());
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
				final HttpPut httpPut = new HttpPut(uri);
				final InputStreamEntity entity = new InputStreamEntity(instream, -1);
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
		
		
		
		// Helpers
		
		
		
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
		 * Validates a HTTP response; on error cases logs status and throws relevant exceptions.
		 * @param response The HTTP response.
		 */
		void validate(HttpResponse response) throws IOException {
			final int code = response.getStatusLine().getStatusCode();
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
		 * Sets a {@link GsonBuilder} to create {@link Gson} instance.
		 * <p>Useful for registering custom serializers/deserializers, such as JodaTime classes.
		 */
		void setGsonBuilder(GsonBuilder gsonBuilder) {
			this.gson = initGson(gsonBuilder);
		}
		
		
		/**
		 * @return The Gson instance.
		 */
		Gson getGson() {
			return gson;
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
		 * @param response The {@link HttpResponse}
		 * @return {@link Response}
		 */
		Response getResponse(HttpResponse response) throws CouchDbException {
			InputStreamReader reader = new InputStreamReader(getStream(response));
			return getGson().fromJson(reader, Response.class);
		}
}
