package com.cloudant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.lightcouch.Attachment;
import org.lightcouch.Replicator;
import org.lightcouch.ReplicatorDocument.UserCtx;

import com.google.gson.JsonObject;
/**
 * Represents a replication document in the <tt>_replicator</tt> database.
 * @see Replicator
 * @since 0.0.1
 * @author Ganesh K Choudhary
 * 
 */
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
	 */
	public String getId() {
		return replicatorDocument.getId();
	}

	/**
	 * @return
	 */
	public String getRevision() {
		return replicatorDocument.getRevision();
	}

	/**
	 * @return
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
	 */
	public void setId(String id) {
		replicatorDocument.setId(id);
	}

	/**
	 * @param revision
	 */
	public void setRevision(String revision) {
		replicatorDocument.setRevision(revision);
	}

	/**
	 * @param attachments
	 */
	public void setAttachments(Map<String, com.cloudant.Attachment> attachments) {
		Map<String, Attachment> lightCouchAttachments = new HashMap<String,Attachment>();
		Iterator<String> iterator = attachments.keySet().iterator();
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			com.cloudant.Attachment attachment = attachments.get(key);
			Attachment lightCouchAttachment = attachment.getAttachement();
			lightCouchAttachments.put(key, lightCouchAttachment);
		}
		replicatorDocument.setAttachments(lightCouchAttachments);
	}

	/**
	 * @param name
	 * @param attachment
	 */
	public void addAttachment(String name, com.cloudant.Attachment attachment) {
		replicatorDocument.addAttachment(name, attachment.getAttachement());
	}

	/**
	 * @return
	 */
	public String getSource() {
		return replicatorDocument.getSource();
	}

	/**
	 * @return
	 */
	public String getTarget() {
		return replicatorDocument.getTarget();
	}

	/**
	 * @return
	 */
	public Boolean getContinuous() {
		return replicatorDocument.getContinuous();
	}

	/**
	 * @return
	 */
	public String getFilter() {
		return replicatorDocument.getFilter();
	}

	/**
	 * @return
	 */
	public JsonObject getQueryParams() {
		return replicatorDocument.getQueryParams();
	}

	/**
	 * @return
	 */
	public String[] getDocIds() {
		return replicatorDocument.getDocIds();
	}

	/**
	 * @return
	 */
	public String getProxy() {
		return replicatorDocument.getProxy();
	}

	/**
	 * @return
	 */
	public Boolean getCreateTarget() {
		return replicatorDocument.getCreateTarget();
	}

	/**
	 * @return
	 */
	public String getReplicationId() {
		return replicatorDocument.getReplicationId();
	}

	/**
	 * @return
	 */
	public String getReplicationState() {
		return replicatorDocument.getReplicationState();
	}

	/**
	 * @return
	 */
	public String getReplicationStateTime() {
		return replicatorDocument.getReplicationStateTime();
	}

	/**
	 * @return
	 */
	public UserCtx getUserCtx() {
		org.lightcouch.ReplicatorDocument.UserCtx couchDbUserCtx = replicatorDocument.getUserCtx();
		UserCtx userCtx = new UserCtx(couchDbUserCtx);
		return userCtx ;
	}

	/**
	 * @return
	 */
	public Integer getWorkerProcesses() {
		return replicatorDocument.getWorkerProcesses();
	}

	/**
	 * @return
	 */
	public Integer getWorkerBatchSize() {
		return replicatorDocument.getWorkerBatchSize();
	}

	/**
	 * @return
	 */
	public Integer getHttpConnections() {
		return replicatorDocument.getHttpConnections();
	}

	/**
	 * @return
	 */
	public Long getConnectionTimeout() {
		return replicatorDocument.getConnectionTimeout();
	}

	/**
	 * @return
	 */
	public Integer getRetriesPerRequest() {
		return replicatorDocument.getRetriesPerRequest();
	}

	/**
	 * @param source
	 */
	public void setSource(String source) {
		replicatorDocument.setSource(source);
	}

	/**
	 * @param target
	 */
	public void setTarget(String target) {
		replicatorDocument.setTarget(target);
	}

	/**
	 * @param continuous
	 */
	public void setContinuous(Boolean continuous) {
		replicatorDocument.setContinuous(continuous);
	}

	/**
	 * @param filter
	 */
	public void setFilter(String filter) {
		replicatorDocument.setFilter(filter);
	}

	/**
	 * @param queryParams
	 */
	public void setQueryParams(JsonObject queryParams) {
		replicatorDocument.setQueryParams(queryParams);
	}

	/**
	 * @param docIds
	 */
	public void setDocIds(String[] docIds) {
		replicatorDocument.setDocIds(docIds);
	}

	/**
	 * @param proxy
	 */
	public void setProxy(String proxy) {
		replicatorDocument.setProxy(proxy);
	}

	/**
	 * @param createTarget
	 */
	public void setCreateTarget(Boolean createTarget) {
		replicatorDocument.setCreateTarget(createTarget);
	}

	/**
	 * @param replicationId
	 */
	public void setReplicationId(String replicationId) {
		replicatorDocument.setReplicationId(replicationId);
	}

	/**
	 * @param replicationState
	 */
	public void setReplicationState(String replicationState) {
		replicatorDocument.setReplicationState(replicationState);
	}

	/**
	 * @param replicationStateTime
	 */
	public void setReplicationStateTime(String replicationStateTime) {
		replicatorDocument.setReplicationStateTime(replicationStateTime);
	}

	/**
	 * @param userCtx
	 */
	public void setUserCtx(UserCtx userCtx) {
		replicatorDocument.setUserCtx(userCtx.getLightCouchUserCtx());
	}

	/**
	 * @param workerProcesses
	 */
	public void setWorkerProcesses(Integer workerProcesses) {
		replicatorDocument.setWorkerProcesses(workerProcesses);
	}

	/**
	 * @param workerBatchSize
	 */
	public void setWorkerBatchSize(Integer workerBatchSize) {
		replicatorDocument.setWorkerBatchSize(workerBatchSize);
	}

	/**
	 * @param httpConnections
	 */
	public void setHttpConnections(Integer httpConnections) {
		replicatorDocument.setHttpConnections(httpConnections);
	}

	/**
	 * @param connectionTimeout
	 */
	public void setConnectionTimeout(Long connectionTimeout) {
		replicatorDocument.setConnectionTimeout(connectionTimeout);
	}

	/**
	 * @param retriesPerRequest
	 */
	public void setRetriesPerRequest(Integer retriesPerRequest) {
		replicatorDocument.setRetriesPerRequest(retriesPerRequest);
	}

	/**
	 * @return
	 */
	public Integer getSinceSeq() {
		return replicatorDocument.getSinceSeq();
	}

	/**
	 * @param sinceSeq
	 */
	public void setSinceSeq(Integer sinceSeq) {
		replicatorDocument.setSinceSeq(sinceSeq);
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
		public UserCtx getUserCtx() {
			return this;
		}

		/**
		 * @return
		 */
		public String getName() {
			return userCtx.getName();
		}

		/**
		 * @return
		 */
		public String[] getRoles() {
			return userCtx.getRoles();
		}

		/**
		 * @param name
		 */
		public void setName(String name) {
			userCtx.setName(name);
		}

		/**
		 * @param roles
		 */
		public void setRoles(String[] roles) {
			userCtx.setRoles(roles);
		}
		
		private org.lightcouch.ReplicatorDocument.UserCtx getLightCouchUserCtx() {
			return userCtx ;

		}
	}
	
}
