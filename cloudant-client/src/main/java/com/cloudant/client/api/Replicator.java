/*
 * Copyright (c) 2015, 2019 IBM Corp. All rights reserved.
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

import com.cloudant.client.api.query.Selector;
import com.cloudant.client.org.lightcouch.Replication;
import com.cloudant.client.org.lightcouch.ReplicatorDocument;
import com.cloudant.client.org.lightcouch.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class provides access to the <tt>_replicator</tt> database introduced in CouchDB version
 * 1.1.0
 * <p>A replication is triggered by persisting a document, and cancelled by removing the document
 * that triggered the replication.
 * <h3>Usage Example:</h3>
 * <pre>
 * {@code
 * // first get a client instance
 * CloudantClient client = ClientBuilder.account("my-cloudant-account").
 *   username("username").
 *   password("password").
 *   build();
 *
 * // create a replicator doc
 * Response response = client.replicator()
 * 	.source("source-db")
 * 	.target("target-db")
 * 	.continuous(true)
 * 	.createTarget(true)
 * 	.replicatorDB("replicator-db-name")    // optional, defaults to _replicator
 * 	.replicatorDocId("doc-id")             // optional, defaults to UUID
 * 	.selector(eq("_id", "Schwarzenegger")) // optional replication selector
 * 	.save(); // trigger replication
 *
 * // find an existing replicator doc
 * ReplicatorDocument replicatorDoc = client.replicator()
 * 	.replicatorDocId("doc-id")
 * 	.replicatorDocRev("doc-rev") // optional
 * 	.find();
 *
 * // get all replicator docs
 * List<ReplicatorDocument> replicatorDocs = client.replicator().findAll();
 *
 * // delete a replicator doc
 * Response response = client.replicator()
 * 	.replicatorDocId("doc-id")
 * 	.replicatorDocRev("doc-rev")
 * 	.remove(); // cancels a replication
 * }
 * </pre>
 *
 * @author Ganesh K Choudhary
 * @see CloudantClient#replicator()
 * @see Replication
 * @see ReplicatorDocument
 * @since 0.0.1
 */
public class Replicator {
    private com.cloudant.client.org.lightcouch.Replicator replicator;

    Replicator(com.cloudant.client.org.lightcouch.Replicator replicator) {
        this.replicator = replicator;
    }

    /**
     * Adds a new document to the replicator database.
     *
     * @return {@link Response}
     */
    public com.cloudant.client.api.model.Response save() {
        Response couchDbResponse = replicator.save();
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    /**
     * Finds a document in the replicator database.
     *
     * @return {@link ReplicatorDocument}
     */
    public com.cloudant.client.api.model.ReplicatorDocument find() {
        ReplicatorDocument couchDbReplicatorDoc = replicator.find();
        com.cloudant.client.api.model.ReplicatorDocument replicatorDoc = new com.cloudant.client
                .api.model.ReplicatorDocument(couchDbReplicatorDoc);
        return replicatorDoc;
    }

    /**
     * Finds all documents in the replicator database.
     *
     * @return the list of ReplicatorDocuments
     */
    public List<com.cloudant.client.api.model.ReplicatorDocument> findAll() {
        List<ReplicatorDocument> couchDbReplicatorDocList = replicator.findAll();
        List<com.cloudant.client.api.model.ReplicatorDocument> replicatorDocList = new
                ArrayList<com.cloudant.client.api.model.ReplicatorDocument>();
        for (ReplicatorDocument couchDbReplicatorDoc : couchDbReplicatorDocList) {
            com.cloudant.client.api.model.ReplicatorDocument replicatorDoc = new com.cloudant
                    .client.api.model.ReplicatorDocument(couchDbReplicatorDoc);
            replicatorDocList.add(replicatorDoc);
        }
        return replicatorDocList;
    }

    /**
     * Removes a document from the replicator database.
     *
     * @return {@link Response}
     */
    public com.cloudant.client.api.model.Response remove() {
        Response couchDbResponse = replicator.remove();
        com.cloudant.client.api.model.Response response = new com.cloudant.client.api.model
                .Response(couchDbResponse);
        return response;
    }

    public Replicator source(String source) {
        this.replicator = replicator.source(source);
        return this;
    }

    public Replicator target(String target) {
        this.replicator = replicator.target(target);
        return this;
    }

    public Replicator continuous(boolean continuous) {
        this.replicator = replicator.continuous(continuous);
        return this;
    }

    public Replicator filter(String filter) {
        this.replicator = replicator.filter(filter);
        return this;
    }

    public Replicator queryParams(String queryParams) {
        this.replicator = replicator.queryParams(queryParams);
        return this;
    }

    public Replicator queryParams(Map<String, Object> queryParams) {
        this.replicator = replicator.queryParams(queryParams);
        return this;
    }

    public Replicator docIds(String... docIds) {
        this.replicator = replicator.docIds(docIds);
        return this;
    }

    public Replicator proxy(String proxy) {
        this.replicator = replicator.proxy(proxy);
        return this;
    }

    public Replicator createTarget(Boolean createTarget) {
        this.replicator = replicator.createTarget(createTarget);
        return this;
    }

    public Replicator workerProcesses(int workerProcesses) {
        this.replicator = replicator.workerProcesses(workerProcesses);
        return this;
    }

    public Replicator connectionTimeout(long connectionTimeout) {
        this.replicator = replicator.connectionTimeout(connectionTimeout);
        return this;
    }

    public Replicator replicatorDB(String replicatorDB) {
        this.replicator = replicator.replicatorDB(replicatorDB);
        return this;
    }

    public Replicator replicatorDocId(String replicatorDocId) {
        this.replicator = replicator.replicatorDocId(replicatorDocId);
        return this;
    }

    public Replicator replicatorDocRev(String replicatorDocRev) {
        this.replicator = replicator.replicatorDocRev(replicatorDocRev);
        return this;
    }

    public Replicator workerBatchSize(int workerBatchSize) {
        this.replicator = replicator.workerBatchSize(workerBatchSize);
        return this;
    }

    public Replicator httpConnections(int httpConnections) {
        this.replicator = replicator.httpConnections(httpConnections);
        return this;
    }

    public Replicator retriesPerRequest(int retriesPerRequest) {
        this.replicator = replicator.retriesPerRequest(retriesPerRequest);
        return this;
    }

    public Replicator userCtxRoles(String... userCtxRoles) {
        this.replicator = replicator.userCtxRoles(userCtxRoles);
        return this;
    }

    public Replicator sinceSeq(Integer sinceSeq) {
        this.replicator = replicator.sinceSeq(sinceSeq);
        return this;
    }

    public Replicator userCtxName(String userCtxName) {
        this.replicator = replicator.userCtxName(userCtxName);
        return this;
    }

    public Replicator sourceIamApiKey(String iamApiKey) {
        this.replicator = replicator.sourceIamApiKey(iamApiKey);
        return this;
    }

    public Replicator targetIamApiKey(String iamApiKey) {
        this.replicator = replicator.targetIamApiKey(iamApiKey);
        return this;
    }

    public Replicator selector(Selector selector) {
        this.replicator = replicator.selector(selector);
        return this;
    }

}
