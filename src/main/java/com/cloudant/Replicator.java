package com.cloudant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lightcouch.CouchDbClientBase;
import org.lightcouch.ReplicatorDocument;
import org.lightcouch.Response;

public class Replicator {
	private org.lightcouch.Replicator replicator ;
	
	public Replicator(CouchDbClientBase client) {
		this.replicator = new org.lightcouch.Replicator(client);
	}
	
	Replicator(org.lightcouch.Replicator replicator){
		this.replicator = replicator ;
	}

	/**
	 * @return
	 * @see org.lightcouch.Replicator#save()
	 */
	public com.cloudant.Response save() {
		Response couchDbResponse = replicator.save();
		com.cloudant.Response response = new com.cloudant.Response(couchDbResponse);
		return response ;
	}

	/**
	 * @return
	 * @see org.lightcouch.Replicator#find()
	 */
	public com.cloudant.ReplicatorDocument find() {
		ReplicatorDocument couchDbReplicatorDoc = replicator.find();
		com.cloudant.ReplicatorDocument replicatorDoc = new com.cloudant.ReplicatorDocument(couchDbReplicatorDoc);
		return replicatorDoc ;
	}

	/**
	 * @return
	 * @see org.lightcouch.Replicator#findAll()
	 */
	public List<com.cloudant.ReplicatorDocument> findAll() {
		List<ReplicatorDocument> couchDbReplicatorDocList = replicator.findAll();
		List<com.cloudant.ReplicatorDocument> replicatorDocList = new ArrayList<>();
		for(ReplicatorDocument couchDbReplicatorDoc : couchDbReplicatorDocList){
			com.cloudant.ReplicatorDocument replicatorDoc = new com.cloudant.ReplicatorDocument(couchDbReplicatorDoc);
			replicatorDocList.add(replicatorDoc);
		}
		return replicatorDocList ;
	}

	/**
	 * @return
	 * @see org.lightcouch.Replicator#remove()
	 */
	public com.cloudant.Response remove() {
		Response couchDbResponse = replicator.remove();
		com.cloudant.Response response = new com.cloudant.Response(couchDbResponse);
		return response ;
	}

	/**
	 * @param source
	 * @return
	 * @see org.lightcouch.Replicator#source(java.lang.String)
	 */
	public Replicator source(String source) {
		this.replicator = replicator.source(source);
		return this ;
	}

	/**
	 * @param target
	 * @return
	 * @see org.lightcouch.Replicator#target(java.lang.String)
	 */
	public Replicator target(String target) {
		this.replicator = replicator.target(target);
		return this ;
	}

	/**
	 * @param continuous
	 * @return
	 * @see org.lightcouch.Replicator#continuous(boolean)
	 */
	public Replicator continuous(boolean continuous) {
		this.replicator = replicator.continuous(continuous);
		return this ; 
	}

	/**
	 * @param filter
	 * @return
	 * @see org.lightcouch.Replicator#filter(java.lang.String)
	 */
	public Replicator filter(String filter) {
		this.replicator = replicator.filter(filter);
		return this ;
	}

	/**
	 * @param queryParams
	 * @return
	 * @see org.lightcouch.Replicator#queryParams(java.lang.String)
	 */
	public Replicator queryParams(String queryParams) {
		this.replicator = replicator.queryParams(queryParams);
		return this ;
	}

	/**
	 * @param queryParams
	 * @return
	 * @see org.lightcouch.Replicator#queryParams(java.util.Map)
	 */
	public Replicator queryParams(Map<String, Object> queryParams) {
		this.replicator = replicator.queryParams(queryParams);
		return this ;
	}

	/**
	 * @param docIds
	 * @return
	 * @see org.lightcouch.Replicator#docIds(java.lang.String[])
	 */
	public Replicator docIds(String... docIds) {
		this.replicator = replicator.docIds(docIds);
		return this ;
	}

	/**
	 * @param proxy
	 * @return
	 * @see org.lightcouch.Replicator#proxy(java.lang.String)
	 */
	public Replicator proxy(String proxy) {
		this.replicator = replicator.proxy(proxy);
		return this ;
	}

	/**
	 * @param createTarget
	 * @return
	 * @see org.lightcouch.Replicator#createTarget(java.lang.Boolean)
	 */
	public Replicator createTarget(Boolean createTarget) {
		this.replicator = replicator.createTarget(createTarget);
		return this ;
	}

	/**
	 * @param workerProcesses
	 * @return
	 * @see org.lightcouch.Replicator#workerProcesses(int)
	 */
	public Replicator workerProcesses(int workerProcesses) {
		this.replicator = replicator.workerProcesses(workerProcesses);
		return this;
	}

	/**
	 * @param connectionTimeout
	 * @return
	 * @see org.lightcouch.Replicator#connectionTimeout(long)
	 */
	public Replicator connectionTimeout(long connectionTimeout) {
		this.replicator = replicator.connectionTimeout(connectionTimeout);
		return this ;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return replicator.equals(obj);
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return replicator.hashCode();
	}

	/**
	 * @param replicatorDB
	 * @return
	 * @see org.lightcouch.Replicator#replicatorDB(java.lang.String)
	 */
	public Replicator replicatorDB(String replicatorDB) {
		this.replicator = replicator.replicatorDB(replicatorDB);
		return this ;
	}

	/**
	 * @param replicatorDocId
	 * @return
	 * @see org.lightcouch.Replicator#replicatorDocId(java.lang.String)
	 */
	public Replicator replicatorDocId(String replicatorDocId) {
		this.replicator = replicator.replicatorDocId(replicatorDocId);
		return this ;
	}

	/**
	 * @param replicatorDocRev
	 * @return
	 * @see org.lightcouch.Replicator#replicatorDocRev(java.lang.String)
	 */
	public Replicator replicatorDocRev(String replicatorDocRev) {
		this.replicator = replicator.replicatorDocRev(replicatorDocRev);
		return this ;
	}

	/**
	 * @param workerBatchSize
	 * @return
	 * @see org.lightcouch.Replicator#workerBatchSize(int)
	 */
	public Replicator workerBatchSize(int workerBatchSize) {
		this.replicator = replicator.workerBatchSize(workerBatchSize);
		return this ;
	}

	/**
	 * @param httpConnections
	 * @return
	 * @see org.lightcouch.Replicator#httpConnections(int)
	 */
	public Replicator httpConnections(int httpConnections) {
		this.replicator = replicator.httpConnections(httpConnections);
		return this ;
	}

	/**
	 * @param retriesPerRequest
	 * @return
	 * @see org.lightcouch.Replicator#retriesPerRequest(int)
	 */
	public Replicator retriesPerRequest(int retriesPerRequest) {
		this.replicator = replicator.retriesPerRequest(retriesPerRequest);
		return this ;
	}

	/**
	 * @param userCtxRoles
	 * @return
	 * @see org.lightcouch.Replicator#userCtxRoles(java.lang.String[])
	 */
	public Replicator userCtxRoles(String... userCtxRoles) {
		this.replicator = replicator.userCtxRoles(userCtxRoles);
		return this ;
	}

	/**
	 * @param sinceSeq
	 * @return
	 * @see org.lightcouch.Replicator#sinceSeq(java.lang.Integer)
	 */
	public Replicator sinceSeq(Integer sinceSeq) {
		this.replicator = replicator.sinceSeq(sinceSeq);
		return this ;
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return replicator.toString();
	}

	/**
	 * @param userCtxName
	 * @return
	 * @see org.lightcouch.Replicator#userCtxName(java.lang.String)
	 */
	public Replicator userCtxName(String userCtxName) {
		this.replicator =  replicator.userCtxName(userCtxName);
		return this ;
	}
	
	
}
