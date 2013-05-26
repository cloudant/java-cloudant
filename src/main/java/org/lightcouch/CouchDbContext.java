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
import static org.lightcouch.URIBuilder.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/**
 * Provides access to database APIs.
 * @author Ahmed Yehia
 */
public class CouchDbContext {

	private static final Log log = LogFactory.getLog(CouchDbContext.class);

	private CouchDbClient dbc;

	CouchDbContext(CouchDbClient dbc) {
		this.dbc = dbc;
		CouchDbProperties props = dbc.getConfig().getProperties();
		if (props.isCreateDbIfNotExist()) {
			createDB(props.getDbName());
		} else {
			serverVersion(); // pre warm up client
		}
	}

	/**
	 * Deletes a database.
	 * @param dbName The database name to delete
	 * @param confirm For double checking, the text "delete database" must be supplied.
	 */
	public void deleteDB(String dbName, String confirm) {
		assertNotEmpty(dbName, "Database name");
		if(!"delete database".equals(confirm))
			throw new IllegalArgumentException("Cannot delete database without confirmation!");
		dbc.delete(builder(dbc.getBaseUri()).path(dbName).build());
	}

	/**
	 * Creates a new Database, if it does not already exist.
	 * @param dbName The Database name
	 */
	public void createDB(String dbName) {
		assertNotEmpty(dbName, "Database name");
		HttpResponse headresp = null;
		HttpResponse putresp = null;
		URI uri = builder(dbc.getBaseUri()).path(dbName).build();
		try {
			headresp = dbc.head(uri);
		} catch (NoDocumentException e) { // db doesn't exist
			HttpPut put = new HttpPut(uri);
			putresp = dbc.executeRequest(put);
			log.info(String.format("Database: '%s' is created.", dbName));
		} finally {
			close(headresp);
			close(putresp);
		}
	}

	/**
	 * @return All server databases.
	 */
	public List<String> getAllDbs() {
		InputStream instream = null;
		try {
			Type typeOfList = new TypeToken<List<String>>() {}.getType();
			instream = dbc.get(builder(dbc.getBaseUri()).path("_all_dbs").build());
			Reader reader = new InputStreamReader(instream);
			return dbc.getGson().fromJson(reader, typeOfList);
		} finally {
			close(instream);
		}
	}

	/**
	 * Gets the info of the associated database instance with this client.
	 * @return {@link CouchDbInfo}
	 */
	public CouchDbInfo info() {
		return dbc.get(builder(dbc.getDBUri()).build(), CouchDbInfo.class);
	}

	/**
	 * @return CouchDB server version.
	 */
	public String serverVersion() {
		InputStream instream = null;
		try {
			instream = dbc.get(builder(dbc.getBaseUri()).build());
			Reader reader = new InputStreamReader(instream);
			return getElement(new JsonParser().parse(reader).getAsJsonObject(), "version");
		} finally {
			close(instream);
		}
	}

	/**
	 * Triggers a database compaction request.
	 */
	public void compact() {
		HttpResponse response = null;
		try {
			response = dbc.post(builder(dbc.getDBUri()).path("_compact").build(), "");
		} finally {
			close(response);
		}
	}

	/**
	 * Requests the database commits any recent changes to disk.
	 */
	public void ensureFullCommit() {
		HttpResponse response = null;
		try {
			response = dbc.post(builder(dbc.getDBUri()).path("_ensure_full_commit").build(), "");
		} finally {
			close(response);
		}
	}
	
	/**
	 * Request a database sends a list of UUIDs.
	 * @param count The count of UUIDs.
	 */
	public List<String> uuids(long count) {
		String uri = String.format("%s_uuids?count=%d", dbc.getBaseUri(), count);
		JsonObject json = dbc.findAny(JsonObject.class, uri);
		return dbc.getGson().fromJson(json.get("uuids").toString(), new TypeToken<List<String>>(){}.getType());
	}
}
