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

import java.io.IOException;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

/**
 * Presents a <i>client</i> to CouchDB database server; targeted to run on Android platform.
 * @see CouchDbClient
 * @since 0.1.0
 * @author Ahmed Yehia
 */
@SuppressWarnings("deprecation")
public class CouchDbClientAndroid extends CouchDbClientBase {
	
	/**
	 * @see CouchDbClient#CouchDbClient()
	 */
	public CouchDbClientAndroid() {
		super();
	}
	
	/**
	 * @see CouchDbClient#CouchDbClient(String)
	 */
	public CouchDbClientAndroid(String configFileName) {
		super(new CouchDbConfig(configFileName));
	}
	
	/**
	 *@see CouchDbClient#CouchDbClient(String, boolean, String, String, int, String, String)
	 */
	public CouchDbClientAndroid(String dbName, boolean createDbIfNotExist, 
			String protocol, String host, int port, String username, String password) { 		
		super(new CouchDbConfig(new CouchDbProperties(dbName, createDbIfNotExist, protocol, host, port, username, password)));
	}
	
	/**
	 * @see CouchDbClient#CouchDbClient(CouchDbProperties)
	 */
	public CouchDbClientAndroid(CouchDbProperties properties) {
		super(new CouchDbConfig(properties));
	}
	
	/**
	 * @return {@link DefaultHttpClient} instance.
	 */
	@Override
	HttpClient createHttpClient(CouchDbProperties props) {
		DefaultHttpClient httpclient = null;
		try {
			final SchemeRegistry schemeRegistry = createRegistry(props);
			// Http params
			final HttpParams params = new BasicHttpParams();
			params.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, "UTF-8");
			params.setParameter(CoreConnectionPNames.SO_TIMEOUT, props.getSocketTimeout());
			params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, props.getConnectionTimeout());
			final ThreadSafeClientConnManager ccm = new ThreadSafeClientConnManager(params,schemeRegistry);
			httpclient = new DefaultHttpClient(ccm, params);
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
			registerInterceptors(httpclient);
		} catch (Exception e) {
			throw new IllegalStateException("Error Creating HTTP client. ", e);
		}
		return httpclient;
	}
	
	@Override
	HttpContext createContext() {	
		HttpContext context = new BasicHttpContext();
		BasicScheme basicAuth = new BasicScheme();
		context.setAttribute("preemptive-auth", basicAuth);
		((AbstractHttpClient) httpClient).addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
		return context;
	}

	@Override
	public void shutdown() {
		this.httpClient.getConnectionManager().shutdown();
	}

	private SchemeRegistry createRegistry(CouchDbProperties properties) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
		SchemeRegistry registry = new SchemeRegistry();
		if("https".equals(properties.getProtocol())) {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
	        trustStore.load(null, null);
	        SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
	        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			registry.register(new Scheme(properties.getProtocol(), sf, properties.getPort()));
		} else {
			registry.register(new Scheme(properties.getProtocol(), PlainSocketFactory.getSocketFactory(), properties.getPort()));
		}
		return registry;
	}
	
	private void registerInterceptors(DefaultHttpClient httpclient) {
		httpclient.addRequestInterceptor(new HttpRequestInterceptor() {
		    public void process(
		            final HttpRequest request,
		            final HttpContext context) throws IOException {
		        if (log.isInfoEnabled()) {
					RequestLine req = request.getRequestLine();
					log.info("> " + req.getMethod() + URLDecoder.decode(req.getUri(), "UTF-8"));
		        }
		    }
		});
		httpclient.addResponseInterceptor(new HttpResponseInterceptor() {
		    public void process(
		            final HttpResponse response,
		            final HttpContext context) throws IOException {
		    	if(log.isInfoEnabled()) {
					log.info("< Status: " + response.getStatusLine().getStatusCode());
		    	}
		    	validate(response);
		    }
		});
	}
	
	private static class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}
	
	private static class PreemptiveAuthInterceptor implements HttpRequestInterceptor {

	    public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
	        AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

	        // If no auth scheme avaialble yet, try to initialize it preemptively
	        if (authState.getAuthScheme() == null) {
	            AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
	            CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
	            HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
	            if (authScheme != null) {
	            	authState.setAuthScheme(authScheme);
	                Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
	                if (creds != null) {
	                	authState.setCredentials(creds);
	                }
	            }
	        }

	    }

	}
}