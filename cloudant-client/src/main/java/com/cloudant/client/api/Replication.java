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
     * Triggers a replication request, blocks while the replication is in progress.
     * @return ReplicationResult encapsulating the result
     */
    public com.cloudant.client.api.model.ReplicationResult trigger() {
        ReplicationResult couchDbReplicationResult = replication.trigger();
        com.cloudant.client.api.model.ReplicationResult replicationResult = new com.cloudant
                .client.api.model.ReplicationResult(couchDbReplicationResult);
        return replicationResult;
    }

    /**
     * Specify the name or URL of the database source of the replication. A URL is required if the
     * database is on a different server or account than the CloudantClient is connected to. If the
     * database requires credentials they should be URL encoded in the user info of the URL.
     *
     * @param source name or URL of the source database
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication source(String source) {
        this.replication = replication.source(source);
        return this;
    }

    /**
     * Specify the name or URL of the database target of the replication. A URL is required if the
     * database is on a different server or account than the CloudantClient is connected to. If the
     * database requires credentials they should be URL encoded in the user info of the URL.
     *
     * @param target name or URL of the target database
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication target(String target) {
        this.replication = replication.target(target);
        return this;
    }

    /**
     * @param continuous {@code true} if the Replication should be continuous
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication continuous(Boolean continuous) {
        this.replication = replication.continuous(continuous);
        return this;
    }

    /**
     * Specify a filter function to limit the documents that are replicated from the source
     * database.
     *
     * @param filter name of the filter function in the form of {@code designDoc/filter}
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication filter(String filter) {
        this.replication = replication.filter(filter);
        return this;
    }

    /**
     * Specify additional query parameters to be passed to the filter function. Takes a string form
     * of a JSON object of the key value parameters. For example:
     * {@code "{\"key1\": \"value1\", \"key2\": \"value2\"}"}
     *
     * @param queryParams string form of JSON object of query parameters
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication queryParams(String queryParams) {
        this.replication = replication.queryParams(queryParams);
        return this;
    }

    /**
     * Specify additional query parameters to be passed to the filter function.
     *
     * @param queryParams map of key-value parameters
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication queryParams(Map<String, Object> queryParams) {
        this.replication = replication.queryParams(queryParams);
        return this;
    }

    /**
     * Limit the replication to the specified document IDs.
     *
     * @param docIds one or more document IDs to include in the replication
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication docIds(String... docIds) {
        this.replication = replication.docIds(docIds);
        return this;
    }

    /**
     * @param proxy address of a proxy server through which the replication should occur
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication proxy(String proxy) {
        this.replication = replication.proxy(proxy);
        return this;
    }

    /**
     * @param cancel {@code true} to cancel the replication
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication cancel(Boolean cancel) {
        this.replication = replication.cancel(cancel);
        return this;
    }

    /**
     * @param createTarget {@code true} if the target database should be created
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication createTarget(Boolean createTarget) {
        this.replication = replication.createTarget(createTarget);
        return this;
    }

    /**
     * Starts a replication since an update sequence.
     *
     * @param sinceSeq sequence number
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication sinceSeq(Integer sinceSeq) {
        this.replication = replication.sinceSeq(sinceSeq);
        return this;
    }

    /**
     * Sets the
     * <a href="https://console.bluemix.net/docs/services/Cloudant/guides/iam.html#ibm-cloud-identity-and-access-management"
     * target="_blank">IAM</a> API key for the replication source.
     *
     * @param iamApiKey IAM API Key
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication sourceIamApiKey(String iamApiKey) {
        this.replication = replication.sourceIamApiKey(iamApiKey);
        return this;
    }

    /**
     * Sets the
     * <a href="https://console.bluemix.net/docs/services/Cloudant/guides/iam.html#ibm-cloud-identity-and-access-management"
     * target="_blank">IAM</a> API key for the replication target.
     *
     * @param iamApiKey IAM API Key
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication targetIamApiKey(String iamApiKey) {
        this.replication = replication.targetIamApiKey(iamApiKey);
        return this;
    }

    /**
     * Set OAuth 1 authentication credentials for the replication target
     *
     * @param consumerSecret client secret
     * @param consumerKey client identifier
     * @param tokenSecret OAuth server token secret
     * @param token OAuth server issued token
     * @return this Replication instance to set more options or trigger the replication
     */
    public Replication targetOauth(String consumerSecret,
                                   String consumerKey, String tokenSecret, String token) {
        this.replication = replication.targetOauth(consumerSecret, consumerKey,
                tokenSecret, token);
        return this;
    }

}
