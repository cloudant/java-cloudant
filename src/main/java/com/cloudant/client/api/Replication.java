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

package com.cloudant.client.api;

import com.cloudant.client.org.lightcouch.ReplicationResult;
import com.cloudant.client.org.lightcouch.ReplicationResult.ReplicationHistory;
import com.cloudant.client.org.lightcouch.Replicator;

import java.util.Map;

/**
 * This class provides access to the database replication API; a replication request
 * is sent via HTTP POST to <code>_replicate</code> URI.
 * <h3>Usage Example:</h3>
 * <pre>
 * ReplicationResult replication = db.replication()
 * 	.source("source-db")
 * 	.target("target-db")
 * 	.createTarget(true)
 * 	.filter("example/filter1")
 * 	.trigger();
 *
 * {@code
 * List<ReplicationHistory> histories = replication.getHistories();
 * }
 * </pre>
 *
 * @author Ganesh K Choudhary
 * @see CloudantClient#replication()
 * @see ReplicationResult
 * @see ReplicationHistory
 * @see Replicator
 * @since 0.0.1
 */
public class Replication {
    private com.cloudant.client.org.lightcouch.Replication replication;

    Replication(com.cloudant.client.org.lightcouch.Replication replication) {
        this.replication = replication;
    }

    /**
     * Triggers a replication request.
     */
    public com.cloudant.client.api.model.ReplicationResult trigger() {
        ReplicationResult couchDbReplicationResult = replication.trigger();
        com.cloudant.client.api.model.ReplicationResult replicationResult = new com.cloudant
                .client.api.model.ReplicationResult(couchDbReplicationResult);
        return replicationResult;
    }

    /**
     * @param source
     * @return
     */
    public Replication source(String source) {
        this.replication = replication.source(source);
        return this;
    }

    /**
     * @param target
     * @return
     */
    public Replication target(String target) {
        this.replication = replication.target(target);
        return this;
    }

    /**
     * @param continuous
     * @return
     */
    public Replication continuous(Boolean continuous) {
        this.replication = replication.continuous(continuous);
        return this;
    }

    /**
     * @param filter
     * @return
     */
    public Replication filter(String filter) {
        this.replication = replication.filter(filter);
        return this;
    }

    /**
     * @param queryParams
     * @return
     */
    public Replication queryParams(String queryParams) {
        this.replication = replication.queryParams(queryParams);
        return this;
    }

    /**
     * @param queryParams
     * @return
     */
    public Replication queryParams(
            Map<String, Object> queryParams) {
        this.replication = replication.queryParams(queryParams);
        return this;
    }

    /**
     * @param docIds
     * @return
     * @see com.cloudant.client.org.lightcouch.Replication#docIds(java.lang.String[])
     */
    public Replication docIds(String... docIds) {
        this.replication = replication.docIds(docIds);
        return this;
    }

    /**
     * @param proxy
     * @return
     */
    public Replication proxy(String proxy) {
        this.replication = replication.proxy(proxy);
        return this;
    }

    /**
     * @param cancel
     * @return
     */
    public Replication cancel(Boolean cancel) {
        this.replication = replication.cancel(cancel);
        return this;
    }

    /**
     * @param createTarget
     * @return
     */
    public Replication createTarget(Boolean createTarget) {
        this.replication = replication.createTarget(createTarget);
        return this;
    }


    /**
     * Starts a replication since an update sequence.
     */
    public Replication sinceSeq(Integer sinceSeq) {
        this.replication = replication.sinceSeq(sinceSeq);
        return this;
    }

    /**
     * @param consumerSecret
     * @param consumerKey
     * @param tokenSecret
     * @param token
     * @return
     */
    public Replication targetOauth(String consumerSecret,
                                   String consumerKey, String tokenSecret, String token) {
        this.replication = replication.targetOauth(consumerSecret, consumerKey,
                tokenSecret, token);
        return this;
    }

}
