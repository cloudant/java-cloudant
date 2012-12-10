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

	public CouchDbProperties(String dbName, boolean createDbIfNotExist,
			String protocol, String host, int port, String username,
			String password) {
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

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public void setCreateDbIfNotExist(boolean createDbIfNotExist) {
		this.createDbIfNotExist = createDbIfNotExist;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}
	
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public void clearPassword() {
		setPassword("");
		setPassword(null);
	}
}
