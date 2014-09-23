package com.cloudant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.lightcouch.Attachment;
import org.lightcouch.ReplicatorDocument.UserCtx;

import com.google.gson.JsonObject;

public class ReplicatorDocument {
	private org.lightcouch.ReplicatorDocument replicatorDocument ;

	
	public ReplicatorDocument(){
		replicatorDocument = new org.lightcouch.ReplicatorDocument();
	}
	
	ReplicatorDocument(org.lightcouch.ReplicatorDocument replicatorDocument){
		this.replicatorDocument = replicatorDocument ;
	}
	
	
	/**
	 * @return
	 * @see org.lightcouch.Document#getId()
	 */
	public String getId() {
		return replicatorDocument.getId();
	}

	/**
	 * @return
	 * @see org.lightcouch.Document#getRevision()
	 */
	public String getRevision() {
		return replicatorDocument.getRevision();
	}

	/**
	 * @return
	 * @see org.lightcouch.Document#getAttachments()
	 */
	public Map<String, com.cloudant.Attachment> getAttachments() {
		Map<String, Attachment> couchDbAttachments = replicatorDocument.getAttachments();
		Map<String, com.cloudant.Attachment> attachments = new HashMap<>();
		Iterator<String> iterator = couchDbAttachments.keySet().iterator();
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			Attachment couchDbAttachment = couchDbAttachments.get(key);
			com.cloudant.Attachment attachment = new com.cloudant.Attachment(couchDbAttachment);
			attachments.put(key, attachment);
			
		}
		return attachments;
	}

	/**
	 * @param id
	 * @see org.lightcouch.Document#setId(java.lang.String)
	 */
	public void setId(String id) {
		replicatorDocument.setId(id);
	}

	/**
	 * @param revision
	 * @see org.lightcouch.Document#setRevision(java.lang.String)
	 */
	public void setRevision(String revision) {
		replicatorDocument.setRevision(revision);
	}

	/**
	 * @param attachments
	 * @see org.lightcouch.Document#setAttachments(java.util.Map)
	 */
	public void setAttachments(Map<String, Attachment> attachments) {
		replicatorDocument.setAttachments(attachments);
	}

	/**
	 * @param name
	 * @param attachment
	 * @see org.lightcouch.Document#addAttachment(java.lang.String, org.lightcouch.Attachment)
	 */
	public void addAttachment(String name, Attachment attachment) {
		replicatorDocument.addAttachment(name, attachment);
	}

	/**
	 * @return
	 * @see org.lightcouch.Document#hashCode()
	 */
	public int hashCode() {
		return replicatorDocument.hashCode();
	}

	/**
	 * @param obj
	 * @return
	 * @see org.lightcouch.Document#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return replicatorDocument.equals(obj);
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getSource()
	 */
	public String getSource() {
		return replicatorDocument.getSource();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getTarget()
	 */
	public String getTarget() {
		return replicatorDocument.getTarget();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getContinuous()
	 */
	public Boolean getContinuous() {
		return replicatorDocument.getContinuous();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getFilter()
	 */
	public String getFilter() {
		return replicatorDocument.getFilter();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getQueryParams()
	 */
	public JsonObject getQueryParams() {
		return replicatorDocument.getQueryParams();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getDocIds()
	 */
	public String[] getDocIds() {
		return replicatorDocument.getDocIds();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getProxy()
	 */
	public String getProxy() {
		return replicatorDocument.getProxy();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getCreateTarget()
	 */
	public Boolean getCreateTarget() {
		return replicatorDocument.getCreateTarget();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getReplicationId()
	 */
	public String getReplicationId() {
		return replicatorDocument.getReplicationId();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getReplicationState()
	 */
	public String getReplicationState() {
		return replicatorDocument.getReplicationState();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getReplicationStateTime()
	 */
	public String getReplicationStateTime() {
		return replicatorDocument.getReplicationStateTime();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getUserCtx()
	 */
	public UserCtx getUserCtx() {
		org.lightcouch.ReplicatorDocument.UserCtx couchDbUserCtx = replicatorDocument.getUserCtx();
		UserCtx userCtx = new UserCtx(couchDbUserCtx);
		return userCtx ;
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getWorkerProcesses()
	 */
	public Integer getWorkerProcesses() {
		return replicatorDocument.getWorkerProcesses();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getWorkerBatchSize()
	 */
	public Integer getWorkerBatchSize() {
		return replicatorDocument.getWorkerBatchSize();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getHttpConnections()
	 */
	public Integer getHttpConnections() {
		return replicatorDocument.getHttpConnections();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getConnectionTimeout()
	 */
	public Long getConnectionTimeout() {
		return replicatorDocument.getConnectionTimeout();
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getRetriesPerRequest()
	 */
	public Integer getRetriesPerRequest() {
		return replicatorDocument.getRetriesPerRequest();
	}

	/**
	 * @param source
	 * @see org.lightcouch.ReplicatorDocument#setSource(java.lang.String)
	 */
	public void setSource(String source) {
		replicatorDocument.setSource(source);
	}

	/**
	 * @param target
	 * @see org.lightcouch.ReplicatorDocument#setTarget(java.lang.String)
	 */
	public void setTarget(String target) {
		replicatorDocument.setTarget(target);
	}

	/**
	 * @param continuous
	 * @see org.lightcouch.ReplicatorDocument#setContinuous(java.lang.Boolean)
	 */
	public void setContinuous(Boolean continuous) {
		replicatorDocument.setContinuous(continuous);
	}

	/**
	 * @param filter
	 * @see org.lightcouch.ReplicatorDocument#setFilter(java.lang.String)
	 */
	public void setFilter(String filter) {
		replicatorDocument.setFilter(filter);
	}

	/**
	 * @param queryParams
	 * @see org.lightcouch.ReplicatorDocument#setQueryParams(com.google.gson.JsonObject)
	 */
	public void setQueryParams(JsonObject queryParams) {
		replicatorDocument.setQueryParams(queryParams);
	}

	/**
	 * @param docIds
	 * @see org.lightcouch.ReplicatorDocument#setDocIds(java.lang.String[])
	 */
	public void setDocIds(String[] docIds) {
		replicatorDocument.setDocIds(docIds);
	}

	/**
	 * @param proxy
	 * @see org.lightcouch.ReplicatorDocument#setProxy(java.lang.String)
	 */
	public void setProxy(String proxy) {
		replicatorDocument.setProxy(proxy);
	}

	/**
	 * @param createTarget
	 * @see org.lightcouch.ReplicatorDocument#setCreateTarget(java.lang.Boolean)
	 */
	public void setCreateTarget(Boolean createTarget) {
		replicatorDocument.setCreateTarget(createTarget);
	}

	/**
	 * @param replicationId
	 * @see org.lightcouch.ReplicatorDocument#setReplicationId(java.lang.String)
	 */
	public void setReplicationId(String replicationId) {
		replicatorDocument.setReplicationId(replicationId);
	}

	/**
	 * @param replicationState
	 * @see org.lightcouch.ReplicatorDocument#setReplicationState(java.lang.String)
	 */
	public void setReplicationState(String replicationState) {
		replicatorDocument.setReplicationState(replicationState);
	}

	/**
	 * @param replicationStateTime
	 * @see org.lightcouch.ReplicatorDocument#setReplicationStateTime(java.lang.String)
	 */
	public void setReplicationStateTime(String replicationStateTime) {
		replicatorDocument.setReplicationStateTime(replicationStateTime);
	}

	/**
	 * @param userCtx
	 * @see org.lightcouch.ReplicatorDocument#setUserCtx(org.lightcouch.ReplicatorDocument.UserCtx)
	 */
	public void setUserCtx(UserCtx userCtx) {
		replicatorDocument.setUserCtx(userCtx.getUserCtx());
	}

	/**
	 * @param workerProcesses
	 * @see org.lightcouch.ReplicatorDocument#setWorkerProcesses(java.lang.Integer)
	 */
	public void setWorkerProcesses(Integer workerProcesses) {
		replicatorDocument.setWorkerProcesses(workerProcesses);
	}

	/**
	 * @param workerBatchSize
	 * @see org.lightcouch.ReplicatorDocument#setWorkerBatchSize(java.lang.Integer)
	 */
	public void setWorkerBatchSize(Integer workerBatchSize) {
		replicatorDocument.setWorkerBatchSize(workerBatchSize);
	}

	/**
	 * @param httpConnections
	 * @see org.lightcouch.ReplicatorDocument#setHttpConnections(java.lang.Integer)
	 */
	public void setHttpConnections(Integer httpConnections) {
		replicatorDocument.setHttpConnections(httpConnections);
	}

	/**
	 * @param connectionTimeout
	 * @see org.lightcouch.ReplicatorDocument#setConnectionTimeout(java.lang.Long)
	 */
	public void setConnectionTimeout(Long connectionTimeout) {
		replicatorDocument.setConnectionTimeout(connectionTimeout);
	}

	/**
	 * @param retriesPerRequest
	 * @see org.lightcouch.ReplicatorDocument#setRetriesPerRequest(java.lang.Integer)
	 */
	public void setRetriesPerRequest(Integer retriesPerRequest) {
		replicatorDocument.setRetriesPerRequest(retriesPerRequest);
	}

	/**
	 * @return
	 * @see org.lightcouch.ReplicatorDocument#getSinceSeq()
	 */
	public Integer getSinceSeq() {
		return replicatorDocument.getSinceSeq();
	}

	/**
	 * @param sinceSeq
	 * @see org.lightcouch.ReplicatorDocument#setSinceSeq(java.lang.Integer)
	 */
	public void setSinceSeq(Integer sinceSeq) {
		replicatorDocument.setSinceSeq(sinceSeq);
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return replicatorDocument.toString();
	}
	
	public class UserCtx {
		private org.lightcouch.ReplicatorDocument.UserCtx userCtx ;

		public UserCtx(){
			this.userCtx = replicatorDocument.new UserCtx();
		}
		
		UserCtx(org.lightcouch.ReplicatorDocument.UserCtx userCtx){
			this.userCtx = userCtx ;
		}
		
		/**
		 * @return the userCtx
		 */
		public org.lightcouch.ReplicatorDocument.UserCtx getUserCtx() {
			return userCtx;
		}

		/**
		 * @param obj
		 * @return
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			return userCtx.equals(obj);
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicatorDocument.UserCtx#getName()
		 */
		public String getName() {
			return userCtx.getName();
		}

		/**
		 * @return
		 * @see org.lightcouch.ReplicatorDocument.UserCtx#getRoles()
		 */
		public String[] getRoles() {
			return userCtx.getRoles();
		}

		/**
		 * @return
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return userCtx.hashCode();
		}

		/**
		 * @param name
		 * @see org.lightcouch.ReplicatorDocument.UserCtx#setName(java.lang.String)
		 */
		public void setName(String name) {
			userCtx.setName(name);
		}

		/**
		 * @param roles
		 * @see org.lightcouch.ReplicatorDocument.UserCtx#setRoles(java.lang.String[])
		 */
		public void setRoles(String[] roles) {
			userCtx.setRoles(roles);
		}

		/**
		 * @return
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return userCtx.toString();
		}
		
		
		
	}
	
}
