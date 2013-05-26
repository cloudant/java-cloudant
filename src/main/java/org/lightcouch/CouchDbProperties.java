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

/**
 * Represents configuration properties for connecting to CouchDB.
 * 
 * @author Ahmed Yehia
 * @author Daan van Berkel
 */
public class CouchDbProperties {

	// required
	private String dbName;
	private boolean createDbIfNotExist;
	private String protocol;
	private String host;
	private int port;
	private String username;
	private String password;

	// optional
	private int socketTimeout;
	private int connectionTimeout;
	private int maxConnections;
	private String proxyHost;
	private int proxyPort;

	public CouchDbProperties() {
		// default constructor
	}

	public CouchDbProperties(String dbName, boolean createDbIfNotExist, String protocol,
			String host, int port, String username, String password) {
		this.dbName = dbName;
		this.createDbIfNotExist = createDbIfNotExist;
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public String getDbName() {
		return dbName;
	}

	public boolean isCreateDbIfNotExist() {
		return createDbIfNotExist;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getHost() {
		return host;
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

	public CouchDbProperties setDbName(String dbName) {
		this.dbName = dbName;
		return this;
	}

	public CouchDbProperties setCreateDbIfNotExist(boolean createDbIfNotExist) {
		this.createDbIfNotExist = createDbIfNotExist;
		return this;
	}

	public CouchDbProperties setProtocol(String protocol) {
		this.protocol = protocol;
		return this;
	}

	public CouchDbProperties setHost(String host) {
		this.host = host;
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

	public void clearPassword() {
		setPassword("");
		setPassword(null);
	}
}
