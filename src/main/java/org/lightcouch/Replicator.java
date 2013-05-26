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
import java.util.Map;

import org.lightcouch.ReplicatorDocument.UserCtx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

	private String replicatorDB;
	private String userCtxName;
	private String[] userCtxRoles;

	private CouchDbClient dbc;
	private ReplicatorDocument replicatorDoc;
	private URI dbURI;
			
	public Replicator(CouchDbClient dbc) {
		this.dbc = dbc;
		replicatorDoc = new ReplicatorDocument();
		replicatorDB = "_replicator"; // default replicator db
		userCtxRoles = new String[0]; // default roles
		dbURI = builder(dbc.getBaseUri()).path(replicatorDB).path("/").build();
	}

	
	/**
	 * Adds a new document to the replicator database. 
	 * @return {@link Response}
	 */
	public Response save() {
		assertNotEmpty(replicatorDoc.getSource(), "Source");
		assertNotEmpty(replicatorDoc.getTarget(), "Target");
		if(userCtxName != null) {
			UserCtx ctx = replicatorDoc.new UserCtx();
			ctx.setName(userCtxName);
			ctx.setRoles(userCtxRoles);
			replicatorDoc.setUserCtx(ctx);
		}
		return dbc.put(dbURI, replicatorDoc, true);
	}
	
	/**
	 * Finds a document in the replicator database. 
	 * @return {@link ReplicatorDocument}
	 */
	public ReplicatorDocument find() {
		assertNotEmpty(replicatorDoc.getId(), "Doc id");
		URI uri = builder(dbURI).path(replicatorDoc.getId()).query("rev", replicatorDoc.getRevision()).build();
		return dbc.get(uri, ReplicatorDocument.class);
	}
	
	/**
	 * Finds all documents in the replicator database. 
	 */
	public List<ReplicatorDocument> findAll() {
		InputStream instream = null;
		try {  
			URI uri = builder(dbURI).path("_all_docs").query("include_docs", "true").build();
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
		assertNotEmpty(replicatorDoc.getId(), "Doc id");
		assertNotEmpty(replicatorDoc.getRevision(), "Doc rev");
		URI uri = builder(dbURI).path(replicatorDoc.getId()).query("rev", replicatorDoc.getRevision()).build();
		return dbc.delete(uri);
	} 
	
	// fields

	public Replicator source(String source) {
		replicatorDoc.setSource(source);
		return this;
	}

	public Replicator target(String target) {
		replicatorDoc.setTarget(target);
		return this;
	}

	public Replicator continuous(boolean continuous) {
		replicatorDoc.setContinuous(continuous);
		return this;
	}

	public Replicator filter(String filter) {
		replicatorDoc.setFilter(filter);
		return this;
	}

	public Replicator queryParams(String queryParams) {
		replicatorDoc.setQueryParams(dbc.getGson().fromJson(queryParams, JsonObject.class));
		return this;
	}
	
	public Replicator queryParams(Map<String, Object> queryParams) {
		replicatorDoc.setQueryParams(dbc.getGson().toJsonTree(queryParams).getAsJsonObject());
		return this;
	}

	public Replicator docIds(String... docIds) {
		replicatorDoc.setDocIds(docIds);
		return this;
	}

	public Replicator proxy(String proxy) {
		replicatorDoc.setProxy(proxy);
		return this;
	}

	public Replicator createTarget(Boolean createTarget) {
		replicatorDoc.setCreateTarget(createTarget);
		return this;
	}

	public Replicator replicatorDB(String replicatorDB) {
		this.replicatorDB = replicatorDB;
		dbURI = builder(dbc.getBaseUri()).path(replicatorDB).path("/").build();
		return this;
	}

	public Replicator replicatorDocId(String replicatorDocId) {
		replicatorDoc.setId(replicatorDocId);
		return this;
	}

	public Replicator replicatorDocRev(String replicatorDocRev) {
		replicatorDoc.setRevision(replicatorDocRev);
		return this;
	}

	public Replicator workerProcesses(int workerProcesses) {
		replicatorDoc.setWorkerProcesses(workerProcesses);
		return this;
	}

	public Replicator workerBatchSize(int workerBatchSize) {
		replicatorDoc.setWorkerBatchSize(workerBatchSize);
		return this;
	}

	public Replicator httpConnections(int httpConnections) {
		replicatorDoc.setHttpConnections(httpConnections);
		return this;
	}

	public Replicator connectionTimeout(long connectionTimeout) {
		replicatorDoc.setConnectionTimeout(connectionTimeout);
		return this;
	}

	public Replicator retriesPerRequest(int retriesPerRequest) {
		replicatorDoc.setRetriesPerRequest(retriesPerRequest);
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
	
	public Replicator sinceSeq(Integer sinceSeq) {
		replicatorDoc.setSinceSeq(sinceSeq);
		return this;
	}
}
