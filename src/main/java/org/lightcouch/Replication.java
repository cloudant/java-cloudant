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
import static org.lightcouch.URIBuilder.builder;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;

import com.google.gson.JsonObject;

/**
 * <p>This class allows construction and sending of replication requests.
 * <p>Replication is triggered by sending a POST request to _replicate. 
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * ReplicationResult result = dbClient.replication()
 * 	.source("source-db")
 * 	.target("target-db")
 * 	.createTarget(true)
 * 	.trigger();
 * </pre>
 * 
 * @see Replicator
 * @author Ahmed Yehia
 *
 */
public class Replication {

	private static final Log log = LogFactory.getLog(Replication.class);

	private String source;
	private String target;
	private Boolean cancel;
	private Boolean continuous;
	private String filter;
	private String queryParams; 
	private String[] docIds;      
	private String proxy;       
	private Boolean createTarget;

	private CouchDbClient dbc;
			
	public Replication(CouchDbClient dbc) {
		this.dbc = dbc;
	}

	// ------------------------------------------------------------- Field setters

	public Replication source(String source) {
		this.source = source;
		return this;
	}
	
	public Replication target(String target) {
		this.target = target;
		return this;
	}
	
	public Replication continuous(Boolean continuous) {
		this.continuous = continuous;
		return this;
	}

	public Replication filter(String filter) {
		this.filter = filter;
		return this;
	}

	public Replication queryParams(String queryParams) {
		this.source = queryParams;
		return this;
	}

	public Replication docIds(String... docIds) {
		this.docIds = docIds;
		return this;
	}

	public Replication proxy(String proxy) {
		this.proxy = proxy;
		return this;
	}

	public Replication cancel(Boolean cancel) {
		this.cancel = cancel;
		return this;
	}

	public Replication createTarget(Boolean createTarget) {
		this.createTarget = createTarget;
		return this;
	}

	// --------------------------------------------------------------- Requests
	
	/**
	 * Triggers a replication request. 
	 */
	public ReplicationResult trigger() {
		assertNotEmpty(source, "Source database");
		assertNotEmpty(target, "Target database");
		HttpResponse response = null;
		try {
			URI uri = builder(dbc.getBaseUri()).path("_replicate").build();
			response = dbc.post(uri, getReplicationDoc().toString());
			Reader reader = new InputStreamReader(response.getEntity().getContent());
			return dbc.getGson().fromJson(reader, ReplicationResult.class); 
		} catch (Exception e) {
			log.error("Error performing replication. " + e.getMessage());
			throw new CouchDbException(e);
		} finally {
			close(response);
		}
	}

	private JsonObject getReplicationDoc() {
		JsonObject json = new JsonObject();
		addProperty(json, "source", source);
		addProperty(json, "target", target);
		addProperty(json, "cancel", cancel);
		addProperty(json, "continuous", continuous);
		addProperty(json, "filter", filter);
		addProperty(json, "query_params", queryParams);
		if(docIds != null) {
			json.add("doc_ids", dbc.getGson().toJsonTree(docIds, String[].class));
		}
		addProperty(json, "proxy", proxy);
		addProperty(json, "create_target", createTarget);
		return json;
	}
	
	private void addProperty(JsonObject json, String name, Object value) {
		if(value != null) {
			if(value instanceof Boolean)
				json.addProperty(name, (Boolean)value);
			else if (value instanceof String)
				json.addProperty(name, (String)value);
		}
	}
}
