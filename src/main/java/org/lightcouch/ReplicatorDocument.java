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

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

/**
 * Represents a replication document in a replicator database.
 * @see Replicator
 * @author Ahmed Yehia
 * 
 */
public class ReplicatorDocument extends Document {

	@SerializedName("source")
	private String source;
	@SerializedName("target")
	private String target;
	@SerializedName("continuous")
	private Boolean continuous;
	@SerializedName("filter")
	private String filter;
	@SerializedName("query_params")
	private JsonObject queryParams;
	@SerializedName("doc_ids")
	private String[] docIds;
	@SerializedName("proxy")
	private String proxy;
	@SerializedName("create_target")
	private Boolean createTarget;
	@SerializedName("_replication_id")
	private String replicationId;
	@SerializedName("_replication_state")
	private String replicationState;
	@SerializedName("_replication_state_time")
	private String replicationStateTime;
	@SerializedName("worker_processes")
	private Integer workerProcesses;
	@SerializedName("worker_batch_size")
	private Integer workerBatchSize;
	@SerializedName("http_connections")
	private Integer httpConnections;
	@SerializedName("connection_timeout ")
	private Long connectionTimeout;
	@SerializedName("retries_per_request")
	private Integer retriesPerRequest;
	@SerializedName("user_ctx")
	private UserCtx userCtx;
	@SerializedName("since_seq")
	private Integer sinceSeq;

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

	public Boolean getContinuous() {
		return continuous;
	}

	public String getFilter() {
		return filter;
	}

	public JsonObject getQueryParams() {
		return queryParams;
	}

	public String[] getDocIds() {
		return docIds;
	}

	public String getProxy() {
		return proxy;
	}

	public Boolean getCreateTarget() {
		return createTarget;
	}

	public String getReplicationId() {
		return replicationId;
	}

	public String getReplicationState() {
		return replicationState;
	}

	public String getReplicationStateTime() {
		return replicationStateTime;
	}

	public UserCtx getUserCtx() {
		return userCtx;
	}
	
	public Integer getWorkerProcesses() {
		return workerProcesses;
	}

	public Integer getWorkerBatchSize() {
		return workerBatchSize;
	}

	public Integer getHttpConnections() {
		return httpConnections;
	}

	public Long getConnectionTimeout() {
		return connectionTimeout;
	}

	public Integer getRetriesPerRequest() {
		return retriesPerRequest;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setContinuous(Boolean continuous) {
		this.continuous = continuous;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public void setQueryParams(JsonObject queryParams) {
		this.queryParams = queryParams;
	}

	public void setDocIds(String[] docIds) {
		this.docIds = docIds;
	}

	public void setProxy(String proxy) {
		this.proxy = proxy;
	}

	public void setCreateTarget(Boolean createTarget) {
		this.createTarget = createTarget;
	}

	public void setReplicationId(String replicationId) {
		this.replicationId = replicationId;
	}

	public void setReplicationState(String replicationState) {
		this.replicationState = replicationState;
	}

	public void setReplicationStateTime(String replicationStateTime) {
		this.replicationStateTime = replicationStateTime;
	}

	public void setUserCtx(UserCtx userCtx) {
		this.userCtx = userCtx;
	}

	public void setWorkerProcesses(Integer workerProcesses) {
		this.workerProcesses = workerProcesses;
	}

	public void setWorkerBatchSize(Integer workerBatchSize) {
		this.workerBatchSize = workerBatchSize;
	}

	public void setHttpConnections(Integer httpConnections) {
		this.httpConnections = httpConnections;
	}

	public void setConnectionTimeout(Long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public void setRetriesPerRequest(Integer retriesPerRequest) {
		this.retriesPerRequest = retriesPerRequest;
	}

	public Integer getSinceSeq() {
		return sinceSeq;
	}

	public void setSinceSeq(Integer sinceSeq) {
		this.sinceSeq = sinceSeq;
	}

	public class UserCtx {
		private String name;
		private String[] roles;
		
		public String getName() {
			return name;
		}
		public String[] getRoles() {
			return roles;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setRoles(String[] roles) {
			this.roles = roles;
		}
	} // /class UserCtx
}
