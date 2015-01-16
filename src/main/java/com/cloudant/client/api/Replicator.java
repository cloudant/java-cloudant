package com.cloudant.client.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cloudant.client.org.lightcouch.Replication;
import com.cloudant.client.org.lightcouch.ReplicatorDocument;
import com.cloudant.client.org.lightcouch.Response;
/**
 * This class provides access to the <tt>_replicator</tt> database introduced in CouchDB version 1.1.0
 * <p>A replication is triggered by persisting a document, and cancelled by removing the document that triggered the replication.
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * Response response = db.replicator()
 * 	.source("source-db")
 * 	.target("target-db")
 * 	.continuous(true)
 * 	.createTarget(true)
 * 	.replicatorDB("replicator-db-name") // optional, defaults to _replicator
 * 	.replicatorDocId("doc-id")          // optional, defaults to UUID 
 * 	.save(); // trigger replication
 * 
 * ReplicatorDocument replicatorDoc = db.replicator()
 * 	.replicatorDocId("doc-id")
 * 	.replicatorDocRev("doc-rev") // optional
 * 	.find();
 * 
 * {@code 
 * List<ReplicatorDocument> replicatorDocs = db.replicator().findAll();
 * }
 * 
 * Response response = db.replicator()
 * 	.replicatorDocId("doc-id")
 * 	.replicatorDocRev("doc-rev")
 * 	.remove(); // cancels a replication
 * </pre>
 * 
 * @see CloudantClient#replicator()
 * @see Replication 
 * @see ReplicatorDocument
 * @since 0.0.1
 * @author Ganesh K Choudhary
 *
 */
public class Replicator {
	private com.cloudant.client.org.lightcouch.Replicator replicator ;
	
	Replicator(com.cloudant.client.org.lightcouch.Replicator replicator){
		this.replicator = replicator ;
	}

	/**
	 * Adds a new document to the replicator database. 
	 * @return {@link Response}
	 */
	public com.cloudant.client.api.model.Response save() {
		Response couchDbResponse = replicator.save();
		com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model.Response(couchDbResponse);
		return response ;
	}

	/**
	 * Finds a document in the replicator database. 
	 * @return {@link ReplicatorDocument}
	 */
	public com.cloudant.client.api.model.ReplicatorDocument find() {
		ReplicatorDocument couchDbReplicatorDoc = replicator.find();
		com.cloudant.client.api.model.ReplicatorDocument replicatorDoc = new com.cloudant.client.api.model.ReplicatorDocument(couchDbReplicatorDoc);
		return replicatorDoc ;
	}

	/**
	 * Finds all documents in the replicator database. 
	 */
	public List<com.cloudant.client.api.model.ReplicatorDocument> findAll() {
		List<ReplicatorDocument> couchDbReplicatorDocList = replicator.findAll();
		List<com.cloudant.client.api.model.ReplicatorDocument> replicatorDocList = new ArrayList<com.cloudant.client.api.model.ReplicatorDocument>();
		for(ReplicatorDocument couchDbReplicatorDoc : couchDbReplicatorDocList){
			com.cloudant.client.api.model.ReplicatorDocument replicatorDoc = new com.cloudant.client.api.model.ReplicatorDocument(couchDbReplicatorDoc);
			replicatorDocList.add(replicatorDoc);
		}
		return replicatorDocList ;
	}

	/**
	 * Removes a document from the replicator database.  
	 * @return {@link Response}
	 */
	public com.cloudant.client.api.model.Response remove() {
		Response couchDbResponse = replicator.remove();
		com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model.Response(couchDbResponse);
		return response ;
	}

	/**
	 * @param source
	 * @return
	 */
	public Replicator source(String source) {
		this.replicator = replicator.source(source);
		return this ;
	}

	/**
	 * @param target
	 * @return
	 */
	public Replicator target(String target) {
		this.replicator = replicator.target(target);
		return this ;
	}

	/**
	 * @param continuous
	 * @return
	 */
	public Replicator continuous(boolean continuous) {
		this.replicator = replicator.continuous(continuous);
		return this ; 
	}

	/**
	 * @param filter
	 * @return
	 */
	public Replicator filter(String filter) {
		this.replicator = replicator.filter(filter);
		return this ;
	}

	/**
	 * @param queryParams
	 * @return
	 */
	public Replicator queryParams(String queryParams) {
		this.replicator = replicator.queryParams(queryParams);
		return this ;
	}

	/**
	 * @param queryParams
	 * @return
	 */
	public Replicator queryParams(Map<String, Object> queryParams) {
		this.replicator = replicator.queryParams(queryParams);
		return this ;
	}

	/**
	 * @param docIds
	 * @return
	 */
	public Replicator docIds(String... docIds) {
		this.replicator = replicator.docIds(docIds);
		return this ;
	}

	/**
	 * @param proxy
	 * @return
	 */
	public Replicator proxy(String proxy) {
		this.replicator = replicator.proxy(proxy);
		return this ;
	}

	/**
	 * @param createTarget
	 * @return
	 */
	public Replicator createTarget(Boolean createTarget) {
		this.replicator = replicator.createTarget(createTarget);
		return this ;
	}

	/**
	 * @param workerProcesses
	 * @return
	 */
	public Replicator workerProcesses(int workerProcesses) {
		this.replicator = replicator.workerProcesses(workerProcesses);
		return this;
	}

	/**
	 * @param connectionTimeout
	 * @return
	 */
	public Replicator connectionTimeout(long connectionTimeout) {
		this.replicator = replicator.connectionTimeout(connectionTimeout);
		return this ;
	}

	/**
	 * @param replicatorDB
	 * @return
	 */
	public Replicator replicatorDB(String replicatorDB) {
		this.replicator = replicator.replicatorDB(replicatorDB);
		return this ;
	}

	/**
	 * @param replicatorDocId
	 * @return
	 */
	public Replicator replicatorDocId(String replicatorDocId) {
		this.replicator = replicator.replicatorDocId(replicatorDocId);
		return this ;
	}

	/**
	 * @param replicatorDocRev
	 * @return
	 */
	public Replicator replicatorDocRev(String replicatorDocRev) {
		this.replicator = replicator.replicatorDocRev(replicatorDocRev);
		return this ;
	}

	/**
	 * @param workerBatchSize
	 * @return
	 */
	public Replicator workerBatchSize(int workerBatchSize) {
		this.replicator = replicator.workerBatchSize(workerBatchSize);
		return this ;
	}

	/**
	 * @param httpConnections
	 * @return
	 */
	public Replicator httpConnections(int httpConnections) {
		this.replicator = replicator.httpConnections(httpConnections);
		return this ;
	}

	/**
	 * @param retriesPerRequest
	 * @return
	 */
	public Replicator retriesPerRequest(int retriesPerRequest) {
		this.replicator = replicator.retriesPerRequest(retriesPerRequest);
		return this ;
	}

	/**
	 * @param userCtxRoles
	 * @return
	 */
	public Replicator userCtxRoles(String... userCtxRoles) {
		this.replicator = replicator.userCtxRoles(userCtxRoles);
		return this ;
	}

	/**
	 * @param sinceSeq
	 * @return
	 */
	public Replicator sinceSeq(Integer sinceSeq) {
		this.replicator = replicator.sinceSeq(sinceSeq);
		return this ;
	}

	/**
	 * @param userCtxName
	 * @return
	 */
	public Replicator userCtxName(String userCtxName) {
		this.replicator =  replicator.userCtxName(userCtxName);
		return this ;
	}
	
	
}
