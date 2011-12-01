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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.lightcouch.ReplicatorDocument.UserCtx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * <p>This class allows construction and sending of replication requests targeting a replicator database.
 * <p>The Replicator database, by default is called <tt>_replicator</tt> was introduced in CouchDB version 1.1.0
 * <p>The API supports triggering replication requests by adding a document to the replicator database, 
 * and cancelling a replication by removing the document that triggered the replication.
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * Response response = dbClient.replicator()
 * 	.source("source-db")
 * 	.target("target-db")
 * 	.continuous(true)
 * 	.createTarget(true)
 * 	.replicatorDB("replicator-db-name") // optionally specify database name, defaults to _replicator
 * 	.replicatorDocId("doc-id")          // optionally specify document id, defaults to a new UUID being assigned
 * 	.save(); 
 * 
 * ReplicatorDocument document = dbClient.replicator()
 * 	.replicatorDocId("doc-id")
 * 	.replicatorDocRev("doc-rev") // optional
 * 	.find();
 * 
 * {@code 
 * List<ReplicatorDocument> docs = dbClient.replicator().findAll();
 * }
 * 
 * Response response = dbClient.replicator()
 * 	.replicatorDocId("doc-id")
 * 	.replicatorDocRev("doc-rev")
 * 	.remove();
 * </pre>
 * 
 * @see Replication 
 * @author Ahmed Yehia
 *
 */
public class Replicator {

	private String source;
	private String target;
	private Boolean continuous;
	private String filter;
	private String queryParams; 
	private String[] docIds;      
	private String proxy;       
	private Boolean createTarget;
	
	private String replicatorDB;
	private String replicatorDocId;
	private String replicatorDocRev;
	
	private String userCtxName; // for delegated requests
	private String[] userCtxRoles;

	private CouchDbClient dbc;
			
	public Replicator(CouchDbClient dbc) {
		this.dbc = dbc;
		replicatorDB = "_replicator"; // default replicator db
		userCtxRoles = new String[0]; // default roles
	}

	// ------------------------------------------------------------- Field setters

	public Replicator source(String source) {
		this.source = source;
		return this;
	}
	
	public Replicator target(String target) {
		this.target = target;
		return this;
	}
	
	public Replicator continuous(Boolean continuous) {
		this.continuous = continuous;
		return this;
	}

	public Replicator filter(String filter) {
		this.filter = filter;
		return this;
	}

	public Replicator queryParams(String queryParams) {
		this.source = queryParams;
		return this;
	}

	public Replicator docIds(String... docIds) {
		this.docIds = docIds;
		return this;
	}

	public Replicator proxy(String proxy) {
		this.proxy = proxy;
		return this;
	}

	public Replicator createTarget(Boolean createTarget) {
		this.createTarget = createTarget;
		return this;
	}
	
	public Replicator replicatorDB(String replicatorDB) {
		this.replicatorDB = replicatorDB;
		return this;
	}
	
	public Replicator replicatorDocId(String replicatorDocId) {
		this.replicatorDocId = replicatorDocId;
		return this;
	}
	
	public Replicator replicatorDocRev(String replicatorDocRev) {
		this.replicatorDocRev = replicatorDocRev;
		return this;
	}
	
	public Replicator userCtxName(String userCtxName) {
		this.userCtxName = userCtxName;
		return this;
	}
	
	public Replicator userCtxRoles(String... userCtxRoles) {
		this.userCtxRoles = userCtxRoles;
		return this;
	}

	// --------------------------------------------------------------- Requests
	
	/**
	 * Adds a new document to the replicator database. 
	 * @return {@link Response}
	 */
	public Response save() {
		assertNotEmpty(source, "Source database");
		assertNotEmpty(target, "Target database");
		ReplicatorDocument rd = new ReplicatorDocument();
		rd.setId(replicatorDocId);
		rd.setSource(source);
		rd.setTarget(target);
		rd.setContinuous(continuous);
		rd.setFilter(filter);
		rd.setQueryParams(queryParams);
		rd.setDocIds(docIds);
		rd.setProxy(proxy);
		rd.setCreateTarget(createTarget);
		if(userCtxName != null) {
			UserCtx ctx = rd.new UserCtx();
			ctx.setName(userCtxName);
			ctx.setRoles(userCtxRoles);
			rd.setUserCtx(ctx);
		}
		URI uri = builder(dbc.getBaseUri()).path(replicatorDB).path("/").build();
		return dbc.put(uri, rd, true);
	}
	
	/**
	 * Finds a document in the replicator database. 
	 * @return {@link ReplicatorDocument}
	 */
	public ReplicatorDocument find() {
		assertNotEmpty(replicatorDocId, "Document Id");
		URI uri = builder(dbc.getBaseUri()).path(replicatorDB).path("/").path(replicatorDocId).query("rev", replicatorDocRev).build();
		return dbc.get(uri, ReplicatorDocument.class);
	}
	
	/**
	 * Finds all documents in the replicator database. 
	 */
	public List<ReplicatorDocument> findAll() {
		InputStream instream = null;
		try {  
			URI uri = builder(dbc.getBaseUri()).path(replicatorDB).path("/").path("_all_docs").query("include_docs", "true").build();
			Reader reader = new InputStreamReader(instream = dbc.get(uri));
			JsonArray jsonArray = new JsonParser().parse(reader)
					.getAsJsonObject().getAsJsonArray("rows");
			List<ReplicatorDocument> list = new ArrayList<ReplicatorDocument>();
			for (JsonElement jsonElem : jsonArray) {
				JsonElement elem = jsonElem.getAsJsonObject().get("doc");
				if(!getElement(elem.getAsJsonObject(), "_id").startsWith("_design")) { // skip design docs
					ReplicatorDocument rd = dbc.getGson().fromJson(elem, ReplicatorDocument.class);
					list.add(rd);
				}
			}
			return list;
		} finally {
			close(instream);
		}
	}
	
	/**
	 * Removes a document from the replicator database.  
	 * @return {@link Response}
	 */
	public Response remove() {
		assertNotEmpty(replicatorDocId, "Replicator Document Id");
		assertNotEmpty(replicatorDocRev, "Replicator Document revision");
		URI uri = builder(dbc.getBaseUri()).path(replicatorDB).path("/").path(replicatorDocId).query("rev", replicatorDocRev).build();
		return dbc.delete(uri);
	} 
}
