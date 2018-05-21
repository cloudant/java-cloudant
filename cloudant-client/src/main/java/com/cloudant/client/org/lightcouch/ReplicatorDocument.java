/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright (c) 2015 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.client.org.lightcouch;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;

/**
 * Represents a replication document in the <tt>_replicator</tt> database.
 *
 * @author Ahmed Yehia
 * @see Replicator
 * @since 0.0.2
 */
public class ReplicatorDocument extends Document {

    @SerializedName("source")
    private JsonElement source;
    @SerializedName("target")
    private JsonElement target;
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
        return getEndpointUrl(source);
    }

    public String getTarget() {
        return getEndpointUrl(target);
    }

    private String getEndpointUrl(JsonElement element) {
        if (element == null) {
            return null;
        }
        JsonPrimitive urlString = null;
        if (element.isJsonPrimitive()) {
            urlString = element.getAsJsonPrimitive();
        } else {
            JsonObject replicatorEndpointObject = element.getAsJsonObject();
            urlString = replicatorEndpointObject.getAsJsonPrimitive("url");
        }
        if (urlString == null) {
            return null;
        }
        return urlString.getAsString();
    }

    private JsonElement getDestination(JsonElement oldDestination, String newDestination) {
        if (oldDestination == null || oldDestination.isJsonPrimitive()) {
            return new JsonPrimitive(newDestination);
        }
        JsonObject json = oldDestination.getAsJsonObject();
        json.remove("url");
        json.addProperty("url", newDestination);
        return json;
    }

    private String getIamApiKey(JsonElement destination) {
        if (destination.isJsonPrimitive()) {
            return null;
        }
        JsonObject json = destination.getAsJsonObject();
        JsonObject authJson = json.getAsJsonObject("auth");
        if (authJson == null) {
            return null;
        }
        JsonObject iamJson = authJson.getAsJsonObject("iam");
        if (iamJson == null) {
            return null;
        }
        return iamJson.getAsJsonPrimitive("api_key").getAsString();
    }

    private JsonObject getDestinationIamJson(String url, String iamApiKey) {
        JsonObject iamJson = new JsonObject();
        if (iamApiKey != null) {
            iamJson.addProperty("api_key", iamApiKey);
        }

        JsonObject authJson = new JsonObject();
        authJson.add("iam", iamJson);

        JsonObject json = new JsonObject();
        if (url != null) {
            json.addProperty("url", url);
        }
        json.add("auth", authJson);

        return json;
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
        return (docIds != null) ? Arrays.copyOf(docIds, docIds.length) : null;
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
        this.source = getDestination(this.source, source);
    }

    public void setTarget(String target) {
        this.target = getDestination(this.target, target);
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
        this.docIds = Arrays.copyOf(docIds, docIds.length);
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

    public String getSourceIamApiKey() {
        return getIamApiKey(source);
    }

    public String getTargetIamApiKey() {
        return getIamApiKey(target);
    }

    public void setSourceIamApiKey(String iamApiKey) {
        source = getDestinationIamJson(this.getSource(), iamApiKey);
    }

    public void setTargetIamApiKey(String iamApiKey) {
        target = getDestinationIamJson(this.getTarget(), iamApiKey);
    }

    public static class UserCtx {
        private String name;
        private String[] roles;

        public String getName() {
            return name;
        }

        public String[] getRoles() {
            return (roles != null) ? Arrays.copyOf(roles, roles.length) : null;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setRoles(String[] roles) {
            this.roles = Arrays.copyOf(roles, roles.length);
        }
    } // /class UserCtx

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        ReplicatorDocument that = (ReplicatorDocument) o;

        if (source != null ? !source.equals(that.source) : that.source != null) {
            return false;
        }
        if (target != null ? !target.equals(that.target) : that.target != null) {
            return false;
        }
        if (continuous != null ? !continuous.equals(that.continuous) : that.continuous != null) {
            return false;
        }
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) {
            return false;
        }
        if (queryParams != null ? !queryParams.equals(that.queryParams) : that.queryParams !=
                null) {
            return false;
        }
        if (!Arrays.equals(docIds, that.docIds)) {
            return false;
        }
        if (proxy != null ? !proxy.equals(that.proxy) : that.proxy != null) {
            return false;
        }
        if (createTarget != null ? !createTarget.equals(that.createTarget) : that.createTarget !=
                null) {
            return false;
        }
        if (replicationId != null ? !replicationId.equals(that.replicationId) : that
                .replicationId != null) {
            return false;
        }
        if (replicationState != null ? !replicationState.equals(that.replicationState) : that
                .replicationState != null) {
            return false;
        }
        if (replicationStateTime != null ? !replicationStateTime.equals(that
                .replicationStateTime) : that.replicationStateTime != null) {
            return false;
        }
        if (workerProcesses != null ? !workerProcesses.equals(that.workerProcesses) : that
                .workerProcesses != null) {
            return false;
        }
        if (workerBatchSize != null ? !workerBatchSize.equals(that.workerBatchSize) : that
                .workerBatchSize != null) {
            return false;
        }
        if (httpConnections != null ? !httpConnections.equals(that.httpConnections) : that
                .httpConnections != null) {
            return false;
        }
        if (connectionTimeout != null ? !connectionTimeout.equals(that.connectionTimeout) : that
                .connectionTimeout != null) {
            return false;
        }
        if (retriesPerRequest != null ? !retriesPerRequest.equals(that.retriesPerRequest) : that
                .retriesPerRequest != null) {
            return false;
        }
        if (userCtx != null ? !userCtx.equals(that.userCtx) : that.userCtx != null) {
            return false;
        }
        return !(sinceSeq != null ? !sinceSeq.equals(that.sinceSeq) : that.sinceSeq != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (continuous != null ? continuous.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (queryParams != null ? queryParams.hashCode() : 0);
        result = 31 * result + (docIds != null ? Arrays.hashCode(docIds) : 0);
        result = 31 * result + (proxy != null ? proxy.hashCode() : 0);
        result = 31 * result + (createTarget != null ? createTarget.hashCode() : 0);
        result = 31 * result + (replicationId != null ? replicationId.hashCode() : 0);
        result = 31 * result + (replicationState != null ? replicationState.hashCode() : 0);
        result = 31 * result + (replicationStateTime != null ? replicationStateTime.hashCode() : 0);
        result = 31 * result + (workerProcesses != null ? workerProcesses.hashCode() : 0);
        result = 31 * result + (workerBatchSize != null ? workerBatchSize.hashCode() : 0);
        result = 31 * result + (httpConnections != null ? httpConnections.hashCode() : 0);
        result = 31 * result + (connectionTimeout != null ? connectionTimeout.hashCode() : 0);
        result = 31 * result + (retriesPerRequest != null ? retriesPerRequest.hashCode() : 0);
        result = 31 * result + (userCtx != null ? userCtx.hashCode() : 0);
        result = 31 * result + (sinceSeq != null ? sinceSeq.hashCode() : 0);
        return result;
    }
}
