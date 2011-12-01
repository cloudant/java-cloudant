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

import static org.lightcouch.CouchDbUtil.*;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides configuration to client instance.
 * @author Ahmed Yehia
 */
class CouchDbConfig {
	private static final Log log = LogFactory.getLog(CouchDbConfig.class);
	private static final String DEFAULT_FILE = "couchdb.properties";
	
	// ------------------------------------------------------- Fields
	// ----------------------- required
	private String dbName;
	private boolean createDbIfNotExist;
	private String protocol;
	private String host;
	private int port;
	private String username;
	private String password;
	
	// ----------------------- optional 
	private int socketTimeout; 
	private int connectionTimeout;
	// add more

	public CouchDbConfig() {
		this(DEFAULT_FILE);
	}
	
	public CouchDbConfig(String configFileName) {
		readConfigFile(configFileName);
		setConfiguration(configFileName);
	}
	
	public CouchDbConfig(String dbName, boolean createDbIfNotExist, String protocol, String host, int port, String username, String password) {
		assertNotEmpty(dbName, "Database name");
		assertNotEmpty(protocol, "Protocol");
		assertNotEmpty(host, "Host address");
		assertNotEmpty(port, "Port Number");
		
		this.dbName = dbName;
		this.createDbIfNotExist = createDbIfNotExist;
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
	}
	
	private void setConfiguration(String file) {
		try {
			this.dbName = getProperty("couchdb.name", true, file);
			this.createDbIfNotExist = Boolean.parseBoolean(getProperty("couchdb.createdb.if-not-exist", true, file));
			this.protocol = getProperty("couchdb.protocol", true, file);
			this.host = getProperty("couchdb.host", true, file);
			this.port = Integer.parseInt(getProperty("couchdb.port", true, file));
			this.username = getProperty("couchdb.username", true, file);
			this.password = getProperty("couchdb.password", true, file);
			
			// get optional
			String prop = getProperty("couchdb.http.socket.timeout", false, file);
			this.socketTimeout = (prop != null) ? Integer.parseInt(prop) : 0;     // 0 is default 
			prop = getProperty("couchdb.http.socket.buffer-size", false, file);
			this.connectionTimeout = (prop != null) ? Integer.parseInt(prop) : 0; // 0 is default
		} catch (Exception e) {
			String msg = "Error reading properties from configuration file: " + file;
			log.error(msg);
			throw new IllegalStateException(msg, e);
		}
		properties = null;
	}

	// ------------------------------------------------------ Getters
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
	
	public void resetPassword() {
		this.password = "";
		this.password = null;
	}

	// -------------------------------------------------------- Property Parsing

	private Properties properties = new Properties();

	private void readConfigFile(String configFile) {
		try {
			InputStream inStream = getURL(configFile).openStream();
			properties.load(inStream);
		} catch (Exception e) {
			String msg = "Could not read configuration file from the classpath: " + configFile;
			log.error(msg);
			throw new IllegalStateException(msg, e);
		}
	}

	private String getProperty(String key, boolean isRequired, String file) {
		String property = properties.getProperty(key);
		if(property == null && isRequired) {
			String msg = String.format("A required property is missing. Key: %s, File: %s", key, file);
			log.error(msg);
			throw new IllegalStateException(msg);
		} else {
			return (property != null && property.length() != 0) ? property : null;
		}
	}
}
