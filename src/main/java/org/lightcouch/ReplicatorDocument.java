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

import com.google.gson.annotations.SerializedName;

/**
 * Represents a replicator document in a replicator database.
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
	private String queryParams;
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
	@SerializedName("user_ctx")
	private UserCtx userCtx;

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

	public String getQueryParams() {
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

	public void setQueryParams(String queryParams) {
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
	}
}
