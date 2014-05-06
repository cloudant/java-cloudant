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

import java.io.IOException;
import java.net.URLDecoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

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

	@Override
	protected HttpClient createHttpClient(CouchDbProperties props) {
		DefaultHttpClient httpclient = null;
		try {
			SchemeSocketFactory ssf = null;
			if(props.getProtocol().equals("https")) {
				TrustManager trustManager = new X509TrustManager() {
					public void checkClientTrusted(X509Certificate[] chain,
							String authType) throws CertificateException {
					}
					public void checkServerTrusted(X509Certificate[] chain,
							String authType) throws CertificateException {
					}
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}
				};
				SSLContext sslcontext = SSLContext.getInstance("TLS");
				sslcontext.init(null, new TrustManager[] { trustManager }, null);
				ssf = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER); 
				SSLSocket socket = (SSLSocket) ssf.createSocket(null); 
				socket.setEnabledCipherSuites(new String[] { "SSL_RSA_WITH_RC4_128_MD5" });
			} else {
				ssf = PlainSocketFactory.getSocketFactory();
			}
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme(props.getProtocol(), props.getPort(), ssf));
			PoolingClientConnectionManager ccm = new PoolingClientConnectionManager(schemeRegistry);
			httpclient = new DefaultHttpClient(ccm);
			// Http params
			httpclient.getParams().setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "UTF-8");
			httpclient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, props.getSocketTimeout());
			httpclient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, props.getConnectionTimeout());
			int maxConnections = props.getMaxConnections();
			if(maxConnections != 0) {
				ccm.setMaxTotal(maxConnections);
				ccm.setDefaultMaxPerRoute(maxConnections);
			}
			if(props.getProxyHost() != null) {
				HttpHost proxy = new HttpHost(props.getProxyHost(), props.getProxyPort());
				httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
			}
			// basic authentication
			if(props.getUsername() != null && props.getPassword() != null) {
				httpclient.getCredentialsProvider().setCredentials(
						new AuthScope(props.getHost(), props.getPort()),
						new UsernamePasswordCredentials(props.getUsername(), props.getPassword()));
				props.clearPassword();
			}
			// request interceptor
			httpclient.addRequestInterceptor(new HttpRequestInterceptor() {
                public void process(
                        final HttpRequest request,
                        final HttpContext context) throws IOException {
                    if (log.isInfoEnabled()) {
                        RequestLine req = request.getRequestLine();
                        log.info(">> " + req.getMethod() + URLDecoder.decode(req.getUri(), "UTF-8"));
                    }
                }
            });
			// response interceptor
			httpclient.addResponseInterceptor(new HttpResponseInterceptor() {
                public void process(
                        final HttpResponse response,
                        final HttpContext context) throws IOException {
                	validate(response);
                	if(log.isInfoEnabled())
        				log.info("<< Status: " + response.getStatusLine().getStatusCode());
                }
            });
		} catch (Exception e) {
			log.error("Error Creating HTTP client. " + e.getMessage());
			throw new IllegalStateException(e);
		}
		return httpclient;
	}

	@Override
	protected HttpContext createContext() {
		AuthCache authCache = new BasicAuthCache();
		authCache.put(host, new BasicScheme());
		
		HttpContext context = new BasicHttpContext();
	    context.setAttribute(ClientContext.AUTH_CACHE, authCache);
		return context;
	}

	@Override
	public void shutdown() {
		HttpClientUtils.closeQuietly(this.httpClient);
	}
	
}
