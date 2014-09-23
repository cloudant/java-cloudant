package com.cloudant;

import java.util.Map;

import org.lightcouch.CouchDbClientBase;
import org.lightcouch.ReplicationResult;

public class Replication {
	private org.lightcouch.Replication replication ;
	
	public Replication(CouchDbClientBase client) {
		this.replication = new org.lightcouch.Replication(client);
	}
	
	Replication(org.lightcouch.Replication replication){
		this.replication = replication ;
	}

	/**
	 * @return
	 * @see org.lightcouch.Replication#trigger()
	 */
	public com.cloudant.ReplicationResult trigger() {
		ReplicationResult couchDbReplicationResult = replication.trigger();
		com.cloudant.ReplicationResult replicationResult = new com.cloudant.ReplicationResult(couchDbReplicationResult);
		return replicationResult ;
	}

	/**
	 * @param source
	 * @return
	 * @see org.lightcouch.Replication#source(java.lang.String)
	 */
	public Replication source(String source) {
		this.replication = replication.source(source);
		return this ;
	}

	/**
	 * @param target
	 * @return
	 * @see org.lightcouch.Replication#target(java.lang.String)
	 */
	public Replication target(String target) {
		 this.replication = replication.target(target);
		 return this ;
	}

	/**
	 * @param continuous
	 * @return
	 * @see org.lightcouch.Replication#continuous(java.lang.Boolean)
	 */
	public Replication continuous(Boolean continuous) {
		this.replication = replication.continuous(continuous);
		return this ;
	}

	/**
	 * @param filter
	 * @return
	 * @see org.lightcouch.Replication#filter(java.lang.String)
	 */
	public Replication filter(String filter) {
		this.replication =  replication.filter(filter);
		return this ;
	}

	/**
	 * @param queryParams
	 * @return
	 * @see org.lightcouch.Replication#queryParams(java.lang.String)
	 */
	public Replication queryParams(String queryParams) {
		this.replication = replication.queryParams(queryParams);
		return this ;
	}

	/**
	 * @param queryParams
	 * @return
	 * @see org.lightcouch.Replication#queryParams(java.util.Map)
	 */
	public Replication queryParams(
			Map<String, Object> queryParams) {
		this.replication =  replication.queryParams(queryParams);
		return this ;
	}

	/**
	 * @param docIds
	 * @return
	 * @see org.lightcouch.Replication#docIds(java.lang.String[])
	 */
	public Replication docIds(String... docIds) {
		this.replication =  replication.docIds(docIds);
		return this ;
	}

	/**
	 * @param proxy
	 * @return
	 * @see org.lightcouch.Replication#proxy(java.lang.String)
	 */
	public Replication proxy(String proxy) {
		this.replication = replication.proxy(proxy);
		return this ;
	}

	/**
	 * @param cancel
	 * @return
	 * @see org.lightcouch.Replication#cancel(java.lang.Boolean)
	 */
	public Replication cancel(Boolean cancel) {
		this.replication = replication.cancel(cancel);
		return this ;
	}

	/**
	 * @param createTarget
	 * @return
	 * @see org.lightcouch.Replication#createTarget(java.lang.Boolean)
	 */
	public Replication createTarget(Boolean createTarget) {
		this.replication = replication.createTarget(createTarget);
		return this ;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return replication.equals(obj);
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return replication.hashCode();
	}

	/**
	 * @param sinceSeq
	 * @return
	 * @see org.lightcouch.Replication#sinceSeq(java.lang.Integer)
	 */
	public Replication sinceSeq(Integer sinceSeq) {
		this.replication =  replication.sinceSeq(sinceSeq);
		return this ;
	}

	/**
	 * @param consumerSecret
	 * @param consumerKey
	 * @param tokenSecret
	 * @param token
	 * @return
	 * @see org.lightcouch.Replication#targetOauth(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public Replication targetOauth(String consumerSecret,
			String consumerKey, String tokenSecret, String token) {
		this.replication = replication.targetOauth(consumerSecret, consumerKey,
				tokenSecret, token);
		return this ;
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return replication.toString();
	}
	
	
}
