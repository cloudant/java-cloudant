/*
 * Copyright (C) 2011 lightcouch.org
 *
 * Modifications for this distribution by IBM Cloudant, Copyright (c) 2015 IBM Corp.
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

import static org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static org.lightcouch.internal.CouchDbUtil.assertNull;
import static org.lightcouch.internal.CouchDbUtil.close;
import static org.lightcouch.internal.CouchDbUtil.generateUUID;
import static org.lightcouch.internal.CouchDbUtil.getAsString;
import static org.lightcouch.internal.CouchDbUtil.getResponse;
import static org.lightcouch.internal.CouchDbUtil.getStream;
import static org.lightcouch.internal.URIBuilder.buildUri;

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
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie2;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.lightcouch.internal.CouchDbUtil;
import org.lightcouch.internal.GsonHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;


/**
 * Contains a Client Public API implementation.
 * @see CouchDbClient
 * @see CouchDbClientAndroid
 * @author Ahmed Yehia
 */

public abstract class CouchDbClientBase {
	
	static final Log log = LogFactory.getLog(CouchDbClient.class);
	
	private URI baseURI;
	final HttpClient httpClient;
	final HttpHost host;
	private Gson gson; 
	
	final CookieStore cookies = new BasicCookieStore();
	
	CouchDbClientBase() {
		this(new CouchDbConfig());
	}
	
	CouchDbClientBase(CouchDbConfig config) {
		
		final CouchDbProperties props = config.getProperties();
		if(props.getAuthCookie() != null){
			setCookie(props);
		}
		this.httpClient = createHttpClient(props);
		this.host = new HttpHost(props.getHost(), props.getPort(), props.getProtocol());
		this.gson = GsonHelper.initGson(new GsonBuilder()).create();
		final String path = props.getPath() != null ? props.getPath() : "";
        this.baseURI = buildUri().scheme(props.getProtocol()).host(props.getHost()).port(props.getPort()).path("/").path(path).build();
        
        // for the authentication using previous session's cookie . do not get the cookie
        if ( props.getAuthCookie() == null ) {
        	getCookie(props);
        }
        props.clearPassword();
    }

	
	private void setCookie(CouchDbProperties props){
		BasicClientCookie2  cookie = new BasicClientCookie2("AuthSession", props.getAuthCookie());
		cookie.setDomain(props.getHost());
		cookies.addCookie(cookie);
		
	}
	
	
	private void getCookie(final CouchDbProperties props) {
		if(props.getUsername() == null || props.getPassword() == null){
			return;
		}
		URI uri = buildUri(baseURI).path("_session").build();
        String body = "name=" +  props.getUsername() + "&password=" + props.getPassword();
              
        HttpResponse response = null;
        try {
        	response = executeRequest(CouchDbUtil.createPost(uri,body,"application/x-www-form-urlencoded"));
        	for (Header h : response.getHeaders("set-cookie")) {
             	if ( h.getName().equalsIgnoreCase("AuthSession")) {
             		cookies.addCookie(new BasicClientCookie2("AuthSession", h.getValue()));
             		break;
             	}
             }
        }
        finally {
        	close(response);
        }
       
        
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
		
		/**
		 * @return The cookie.
		 */
		public String getCookie(){
			List<Cookie> cookies2 = cookies.getCookies();
			for(Cookie cookie : cookies2){
				if(cookie.getName().equalsIgnoreCase("AuthSession")){
					return cookie.getValue();
				}
			}
			return null ;
		}
		
		

		/**
		 * Get an instance of Database class to perform DB operations
		 * @param name The name of the database
		 * @param create Should the database be created if it doesn't exist
		 * 
		 * @exception If the database doesn't exist and create is false, an exception is raised
		 */
		abstract CouchDatabaseBase database(String name, boolean create);

		
		/**
		 * Requests CouchDB deletes a database.
		 * @param dbName The database name
		 * @param confirm A confirmation string with the value: <tt>delete database</tt>
		 * @Deprecated Use {@link CouchDbClientBase#deleteDB(String)}
		 */
		@Deprecated
		public void deleteDB(String dbName, String confirm) {
			if(!"delete database".equals(confirm))
				throw new IllegalArgumentException("Invalid confirm!");
			deleteDB(dbName);
		}

		/**
		 * Requests CouchDB deletes a database.
		 * @param dbName The database name
		 */
	    public void deleteDB(String dbName){
			assertNotEmpty(dbName, "dbName");
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
				Reader reader = new InputStreamReader(instream, "UTF-8");
				return getGson().fromJson(reader, typeOfList);
			} catch (UnsupportedEncodingException e) {
				// This should never happen as every implementation of the java platform is required to support UTF-8.
				throw new RuntimeException(e);
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
				Reader reader = new InputStreamReader(instream, "UTF-8");
				return getAsString(new JsonParser().parse(reader).getAsJsonObject(), "version");
			} catch (UnsupportedEncodingException e) {
				// This should never happen as every implementation of the java platform is required to support UTF-8.
				throw new RuntimeException(e);
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
				return httpClient.execute(host, request,createContext());
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
				return getResponse(response,Response.class, getGson());
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
		public <T> T get(URI uri, Class<T> classType) {
			InputStream in = null;
			try {
				in = get(uri);
				return getGson().fromJson(new InputStreamReader(in, "UTF-8"), classType);
			} catch (UnsupportedEncodingException e) {
				// This should never happen as every implementation of the java platform is required to support UTF-8.
				throw new RuntimeException(e);
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
				return getResponse(response,Response.class,getGson());
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
				return getResponse(response, Response.class,getGson());
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
		public void setGsonBuilder(GsonBuilder gsonBuilder) {
			this.gson = GsonHelper.initGson(gsonBuilder).create();
		}
		
		
		/**
		 * @return The Gson instance.
		 */
		public Gson getGson() {
			return gson;
		}
		
		
		/**
		 * Executes a HTTP request and return a domain object
		 * @param request The HTTP request to execute.
		 * @param Class<T> class of domain object to return
		 * @return <T> T
		 */
		public <T> T executeRequest(HttpRequestBase request, Class<T> classType) {
			HttpResponse response = null;
			try {
				response =  executeRequest(request);
				return getResponse(response, classType, getGson());
			}
			finally {
				close(response);
			}
		}
}
