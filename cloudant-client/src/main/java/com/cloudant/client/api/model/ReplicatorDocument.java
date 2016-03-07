/*
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

package com.cloudant.client.api.model;

import com.google.gson.JsonObject;

import com.cloudant.client.org.lightcouch.Attachment;
import com.cloudant.client.org.lightcouch.Replicator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Encapsulates a <tt>_replicator</tt> database replication document.
 *
 * @author Ganesh K Choudhary
 * @see Replicator
 * @since 0.0.1
 */
public class ReplicatorDocument {
    private com.cloudant.client.org.lightcouch.ReplicatorDocument replicatorDocument;


    public ReplicatorDocument() {
        replicatorDocument = new com.cloudant.client.org.lightcouch.ReplicatorDocument();
    }

    public ReplicatorDocument(com.cloudant.client.org.lightcouch.ReplicatorDocument replicatorDocument) {
        this.replicatorDocument = replicatorDocument;
    }

    public String getId() {
        return replicatorDocument.getId();
    }

    public String getRevision() {
        return replicatorDocument.getRevision();
    }

    public Map<String, com.cloudant.client.api.model.Attachment> getAttachments() {
        Map<String, Attachment> couchDbAttachments = replicatorDocument.getAttachments();
        Map<String, com.cloudant.client.api.model.Attachment> attachments = new HashMap<String,
                com.cloudant.client.api.model.Attachment>();
        Iterator<String> iterator = couchDbAttachments.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            Attachment couchDbAttachment = couchDbAttachments.get(key);
            com.cloudant.client.api.model.Attachment attachment = new com.cloudant.client.api
                    .model.Attachment(couchDbAttachment);
            attachments.put(key, attachment);

        }
        return attachments;
    }

    public void setId(String id) {
        replicatorDocument.setId(id);
    }

    public void setRevision(String revision) {
        replicatorDocument.setRevision(revision);
    }

    public void setAttachments(Map<String, com.cloudant.client.api.model.Attachment> attachments) {
        Map<String, Attachment> lightCouchAttachments = new HashMap<String, Attachment>();
        Iterator<String> iterator = attachments.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            com.cloudant.client.api.model.Attachment attachment = attachments.get(key);
            Attachment lightCouchAttachment = attachment.getAttachement();
            lightCouchAttachments.put(key, lightCouchAttachment);
        }
        replicatorDocument.setAttachments(lightCouchAttachments);
    }

    public void addAttachment(String name, com.cloudant.client.api.model.Attachment attachment) {
        replicatorDocument.addAttachment(name, attachment.getAttachement());
    }

    public String getSource() {
        return replicatorDocument.getSource();
    }

    public String getTarget() {
        return replicatorDocument.getTarget();
    }

    public Boolean getContinuous() {
        return replicatorDocument.getContinuous();
    }

    public String getFilter() {
        return replicatorDocument.getFilter();
    }

    public JsonObject getQueryParams() {
        return replicatorDocument.getQueryParams();
    }

    public String[] getDocIds() {
        return replicatorDocument.getDocIds();
    }

    public String getProxy() {
        return replicatorDocument.getProxy();
    }

    public Boolean getCreateTarget() {
        return replicatorDocument.getCreateTarget();
    }

    public String getReplicationId() {
        return replicatorDocument.getReplicationId();
    }

    public String getReplicationState() {
        return replicatorDocument.getReplicationState();
    }

    public String getReplicationStateTime() {
        return replicatorDocument.getReplicationStateTime();
    }

    public UserCtx getUserCtx() {
        com.cloudant.client.org.lightcouch.ReplicatorDocument.UserCtx couchDbUserCtx = replicatorDocument.getUserCtx();
        UserCtx userCtx = new UserCtx(couchDbUserCtx);
        return userCtx;
    }

    public Integer getWorkerProcesses() {
        return replicatorDocument.getWorkerProcesses();
    }

    public Integer getWorkerBatchSize() {
        return replicatorDocument.getWorkerBatchSize();
    }

    public Integer getHttpConnections() {
        return replicatorDocument.getHttpConnections();
    }

    public Long getConnectionTimeout() {
        return replicatorDocument.getConnectionTimeout();
    }

    public Integer getRetriesPerRequest() {
        return replicatorDocument.getRetriesPerRequest();
    }

    public void setSource(String source) {
        replicatorDocument.setSource(source);
    }

    public void setTarget(String target) {
        replicatorDocument.setTarget(target);
    }

    public void setContinuous(Boolean continuous) {
        replicatorDocument.setContinuous(continuous);
    }

    public void setFilter(String filter) {
        replicatorDocument.setFilter(filter);
    }

    public void setQueryParams(JsonObject queryParams) {
        replicatorDocument.setQueryParams(queryParams);
    }

    public void setDocIds(String[] docIds) {
        replicatorDocument.setDocIds(docIds);
    }

    public void setProxy(String proxy) {
        replicatorDocument.setProxy(proxy);
    }

    public void setCreateTarget(Boolean createTarget) {
        replicatorDocument.setCreateTarget(createTarget);
    }

    public void setReplicationId(String replicationId) {
        replicatorDocument.setReplicationId(replicationId);
    }

    public void setReplicationState(String replicationState) {
        replicatorDocument.setReplicationState(replicationState);
    }

    public void setReplicationStateTime(String replicationStateTime) {
        replicatorDocument.setReplicationStateTime(replicationStateTime);
    }

    public void setUserCtx(UserCtx userCtx) {
        replicatorDocument.setUserCtx(userCtx.getLightCouchUserCtx());
    }

    public void setWorkerProcesses(Integer workerProcesses) {
        replicatorDocument.setWorkerProcesses(workerProcesses);
    }

    public void setWorkerBatchSize(Integer workerBatchSize) {
        replicatorDocument.setWorkerBatchSize(workerBatchSize);
    }

    public void setHttpConnections(Integer httpConnections) {
        replicatorDocument.setHttpConnections(httpConnections);
    }

    public void setConnectionTimeout(Long connectionTimeout) {
        replicatorDocument.setConnectionTimeout(connectionTimeout);
    }

    public void setRetriesPerRequest(Integer retriesPerRequest) {
        replicatorDocument.setRetriesPerRequest(retriesPerRequest);
    }

    public Integer getSinceSeq() {
        return replicatorDocument.getSinceSeq();
    }

    public void setSinceSeq(Integer sinceSeq) {
        replicatorDocument.setSinceSeq(sinceSeq);
    }


    public class UserCtx {
        private com.cloudant.client.org.lightcouch.ReplicatorDocument.UserCtx userCtx;

        public UserCtx() {
            this.userCtx = replicatorDocument.new UserCtx();
        }

        UserCtx(com.cloudant.client.org.lightcouch.ReplicatorDocument.UserCtx userCtx) {
            this.userCtx = userCtx;
        }

        /**
         * @return the userCtx
         */
        public UserCtx getUserCtx() {
            return this;
        }

        public String getName() {
            return userCtx.getName();
        }

        public String[] getRoles() {
            return userCtx.getRoles();
        }

        public void setName(String name) {
            userCtx.setName(name);
        }

        public void setRoles(String[] roles) {
            userCtx.setRoles(roles);
        }

        private com.cloudant.client.org.lightcouch.ReplicatorDocument.UserCtx getLightCouchUserCtx() {
            return userCtx;

        }
    }

}
