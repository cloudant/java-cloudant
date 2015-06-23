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

import javax.net.ssl.SSLSocketFactory;

/**
 * Represents configuration properties for connecting to CouchDB.
 * 
 * @author Ahmed Yehia
 * @author Daan van Berkel
 */
public class CouchDbProperties {

	// required
	private String protocol;
	private String host;
	private String path;
	private int port;
	private String username;
	private String password;
	
	private String authCookie;

	// optional
	private int socketTimeout;
	private int connectionTimeout;
	private int maxConnections;
	private String proxyHost;
	private int proxyPort;
	private boolean disableSSLAuthentication;
	private SSLSocketFactory secureSSLSocketFactory;

	public CouchDbProperties() {
		// default constructor
	}

	public CouchDbProperties(String protocol, String host, int port,
			String authCookie) {
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.authCookie = authCookie;
	}
	
	public CouchDbProperties(String protocol,
			String host, int port, String username, String password) {
		
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getHost() {
		return host;
	}
	
	public String getPath() {
		return path;
	}

	public int getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public CouchDbProperties setProtocol(String protocol) {
		this.protocol = protocol;
		return this;
	}

	public CouchDbProperties setHost(String host) {
		this.host = host;
		return this;
	}

	public CouchDbProperties setPath(String path) {
        	this.path = path;
        	return this;
    	}

	public CouchDbProperties setPort(int port) {
		this.port = port;
		return this;
	}

	public CouchDbProperties setUsername(String username) {
		this.username = username;
		return this;
	}

	public CouchDbProperties setPassword(String password) {
		this.password = password;
		return this;
	}

	public CouchDbProperties setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
		return this;
	}

	public CouchDbProperties setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
		return this;
	}

	public CouchDbProperties setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
		return this;
	}

	public CouchDbProperties setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
		return this;
	}

	public CouchDbProperties setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
		return this;
	}

	public String getAuthCookie() {
		return authCookie;
	}

	public void setAuthCookie(String authCookie) {
		this.authCookie = authCookie;
	}

	public void clearPassword() {
		setPassword("");
		setPassword(null);
	}

	/**
	 * Enables/disables hostname verification, certificate chain validation,
	 * and the use of the optional {@link #getSecureSSLSocketFactory()}.
	 * @param disabled set to true to disable or false to enable.
	 * @return the updated {@link CouchDbProperties} object.
	 * @see #isSSLAuthenticationDisabled
	 */
	public CouchDbProperties disableSSLAuthentication(boolean disabled) {
		this.disableSSLAuthentication = disabled;
		return this;
	}

	/**
	 * @return true if hostname verification, certificate chain validation,
	 * and the use of the optional {@link #getSecureSSLSocketFactory()} are
	 * disabled, or false otherwise.
	 * @see #disableSSLAuthentication(boolean)
	 */
	public boolean isSSLAuthenticationDisabled() {
		return disableSSLAuthentication;
	}

	/**
	 * Returns the SSLSocketFactory that gets used when connecting to
	 * CouchDB over a <code>https</code> URL, when SSL authentication is
	 * enabled.
	 * @return An SSLSocketFactory, or <code>null</code>, which stands for
	 *         the default SSLSocketFactory of the JRE.
	 * @see #setSecureSSLSocketFactory(javax.net.ssl.SSLSocketFactory)
	 */
	public SSLSocketFactory getSecureSSLSocketFactory() {
		return secureSSLSocketFactory;
	}

	/**
	 * Specifies the SSLSocketFactory to use when connecting to CouchDB
	 * over a <code>https</code> URL, when SSL authentication is enabled.
	 * @param factory An SSLSocketFactory, or <code>null</code> for the
	 *                default SSLSocketFactory of the JRE.
	 * @see #getSecureSSLSocketFactory()
	 */
	public CouchDbProperties setSecureSSLSocketFactory(SSLSocketFactory factory) {
		this.secureSSLSocketFactory = factory;
		return this;
	}

}
