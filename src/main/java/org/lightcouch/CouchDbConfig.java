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
	
	private Properties properties = new Properties();
	private String configFile;
	private CouchDbProperties dbProperties;

	public CouchDbConfig() {
		this(DEFAULT_FILE);
	}
	
	public CouchDbConfig(String configFile) {
		this.configFile = configFile;
		try {
			InputStream instream = CouchDbConfig.class.getClassLoader().getResourceAsStream(configFile);
			properties.load(instream);
		} catch (Exception e) {
			String msg = "Could not read configuration file from the classpath: " + configFile;
			log.error(msg);
			throw new IllegalStateException(msg, e);
		}
		readProperties();
	}
	
	public CouchDbConfig(CouchDbProperties dbProperties) {
		assertNotEmpty(dbProperties, "Properties");
		assertNotEmpty(dbProperties.getDbName(), "Database");
		assertNotEmpty(dbProperties.getProtocol(), "Protocol");
		assertNotEmpty(dbProperties.getHost(), "Host");
		assertNotEmpty(dbProperties.getPort(), "Port");
		this.dbProperties = dbProperties;
	}
	
	private void readProperties() {
		try {
			// required
			dbProperties = new CouchDbProperties();
			dbProperties.setDbName(getProperty("couchdb.name", true));
			boolean create = Boolean.parseBoolean(getProperty("couchdb.createdb.if-not-exist", true));
			dbProperties.setCreateDbIfNotExist(create);
			dbProperties.setProtocol(getProperty("couchdb.protocol", true));
			dbProperties.setHost(getProperty("couchdb.host", true));
			int port = Integer.parseInt(getProperty("couchdb.port", true));
			dbProperties.setPort(port);
			dbProperties.setUsername(getProperty("couchdb.username", true));
			dbProperties.setPassword(getProperty("couchdb.password", true));
			
			// optional
			dbProperties.setSocketTimeout(getPropertyAsInt("couchdb.http.socket.timeout", false));
			dbProperties.setConnectionTimeout(getPropertyAsInt("couchdb.http.connection.timeout", false));
			dbProperties.setMaxConnections(getPropertyAsInt("couchdb.max.connections", false));
			dbProperties.setProxyHost(getProperty("couchdb.proxy.host", false));
			dbProperties.setProxyPort(getPropertyAsInt("couchdb.proxy.port", false));
			
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		properties = null;
	}
	
	public CouchDbProperties getProperties() {
		return dbProperties;
	}

	private String getProperty(String key, boolean isRequired) {
		String property = properties.getProperty(key);
		if(property == null && isRequired) {
			String msg = String.format("A required property is missing. Key: %s, File: %s", key, configFile);
			log.error(msg);
			throw new IllegalStateException(msg);
		} else {
			return (property != null && property.length() != 0) ? property.trim() : null;
		}
	}
	
	private int getPropertyAsInt(String key, boolean isRequired) {
		String prop = getProperty(key, isRequired);
		return (prop != null) ? Integer.parseInt(prop) : 0;
	}
}
