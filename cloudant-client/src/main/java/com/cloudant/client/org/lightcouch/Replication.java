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

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.close;

import com.cloudant.client.internal.DatabaseURIHelper;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides access to the database replication API; a replication request
 * is sent via HTTP {@code POST} to the {@code _replicate} endpoint.
 * <P>
 * Replicates source to target, the target must exist prior to replication. Use {@link
 * #createTarget(Boolean)} to have it created automatically.
 * </P>
 * <p>Usage Example:</p>
 * <pre>
 * {@code
 * ReplicationResult replication = db.replication()
 * 	.source("https://source.example/source-db")
 * 	.target("https://target.example/target-db")
 * 	.createTarget(true)
 * 	.filter("example/filter1")
 * 	.trigger();
 *
 * if (replication.isOk()) {
 *     //good
 * } else {
 *     //error handling
 * }
 *
 * //get replication history
 * List<ReplicationHistory> histories = replication.getHistories();
 * }
 * </pre>
 *
 * @author Ahmed Yehia
 * @see ReplicationResult
 * @see com.cloudant.client.api.model.ReplicationResult.ReplicationHistory
 * @see <a href="https://console.bluemix.net/docs/services/Cloudant/api/advanced_replication.html#the-_replicate-endpoint"
 * target="_blank">Replication - the _replicate endpoint</a>
 * @since 0.0.2
 */
public class Replication {

    static final Logger log = Logger.getLogger(Replication.class.getCanonicalName());

    private String source;
    private String target;
    private Boolean cancel;
    private Boolean continuous;
    private String filter;
    private JsonObject queryParams;
    private String[] docIds;
    private String proxy;
    private Boolean createTarget;
    private Integer sinceSeq;

    // IAM
    private String sourceIamApiKey;
    private String targetIamApiKey;

    // OAuth
    private JsonObject targetOauth;
    private String consumerSecret;
    private String consumerKey;
    private String tokenSecret;
    private String token;

    private CouchDbClient client;

    public Replication(CouchDbClient client) {
        this.client = client;
    }

    /**
     * Triggers a replication request.
     */
    public ReplicationResult trigger() {
        assertNotEmpty(source, "Source");
        assertNotEmpty(target, "Target");
        InputStream response = null;
        try {
            JsonObject json = createJson();
            if (log.isLoggable(Level.FINE)) {
                log.fine(json.toString());
            }

            final URI uri = new DatabaseURIHelper(client.getBaseUri()).path("_replicate").build();
            response = client.post(uri, json.toString());
            final InputStreamReader reader = new InputStreamReader(response, "UTF-8");
            return client.getGson().fromJson(reader, ReplicationResult.class);
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        } finally {
            close(response);
        }
    }

    // fields

    public Replication source(String source) {
        this.source = source;
        return this;
    }

    public Replication target(String target) {
        this.target = target;
        return this;
    }

    public Replication continuous(Boolean continuous) {
        this.continuous = continuous;
        return this;
    }

    public Replication filter(String filter) {
        this.filter = filter;
        return this;
    }

    public Replication queryParams(String queryParams) {
        this.queryParams = client.getGson().fromJson(queryParams, JsonObject.class);
        return this;
    }

    public Replication queryParams(Map<String, Object> queryParams) {
        this.queryParams = client.getGson().toJsonTree(queryParams).getAsJsonObject();
        return this;
    }

    public Replication docIds(String... docIds) {
        this.docIds = docIds;
        return this;
    }

    public Replication proxy(String proxy) {
        this.proxy = proxy;
        return this;
    }

    public Replication cancel(Boolean cancel) {
        this.cancel = cancel;
        return this;
    }

    public Replication createTarget(Boolean createTarget) {
        this.createTarget = createTarget;
        return this;
    }

    /**
     * Starts a replication since an update sequence.
     */
    public Replication sinceSeq(Integer sinceSeq) {
        this.sinceSeq = sinceSeq;
        return this;
    }

    public Replication sourceIamApiKey(String iamApiKey) {
        this.sourceIamApiKey = iamApiKey;
        return this;
    }

    public Replication targetIamApiKey(String iamApiKey) {
        this.targetIamApiKey = iamApiKey;
        return this;
    }

    /**
     * Authenticate with the target database using OAuth.
     *
     * @param consumerSecret consumer secret
     * @param consumerKey consumer key
     * @param tokenSecret token secret
     * @param token token
     * @return this Replication instance to set more options
     * @deprecated OAuth 1.0 implementation has been <a href="http://docs.couchdb.org/en/stable/whatsnew/2.1.html?highlight=oauth#upgrade-notes"
     * target="_blank">removed in CouchDB 2.1</a>
     */
    public Replication targetOauth(String consumerSecret, String consumerKey, String tokenSecret,
                                   String token) {
        targetOauth = new JsonObject();
        this.consumerSecret = consumerSecret;
        this.consumerKey = consumerKey;
        this.tokenSecret = tokenSecret;
        this.token = token;
        return this;
    }

    // helper

    private JsonObject createJson() {
        JsonObject json = new JsonObject();
        addProperty(json, "cancel", cancel);
        addProperty(json, "continuous", continuous);
        addProperty(json, "filter", filter);

        if (queryParams != null) {
            json.add("query_params", queryParams);
        }
        if (docIds != null) {
            json.add("doc_ids", client.getGson().toJsonTree(docIds, String[].class));
        }

        addProperty(json, "proxy", proxy);
        addProperty(json, "since_seq", sinceSeq);
        addProperty(json, "create_target", createTarget);

        // source

        if (sourceIamApiKey == null) {
            addProperty(json, "source", source);
        } else {
            JsonObject sourceAuthJson = new JsonObject();
            JsonObject sourceIamJson = new JsonObject();
            addProperty(sourceIamJson, "api_key", sourceIamApiKey);
            sourceAuthJson.add("iam", sourceIamJson);

            JsonObject sourceJson = new JsonObject();
            addProperty(sourceJson, "url", source);
            sourceJson.add("auth", sourceAuthJson);

            json.add("source", sourceJson);
        }

        // target

        JsonObject targetAuth = new JsonObject();

        if (targetIamApiKey != null) {
            JsonObject targetIamJson = new JsonObject();
            addProperty(targetIamJson, "api_key", targetIamApiKey);
            targetAuth.add("iam", targetIamJson);
        }
        if (targetOauth != null) {
            // Note: OAuth is only supported on replication target (not source)
            JsonObject oauth = new JsonObject();
            addProperty(oauth, "consumer_secret", consumerSecret);
            addProperty(oauth, "consumer_key", consumerKey);
            addProperty(oauth, "token_secret", tokenSecret);
            addProperty(oauth, "token", token);
            targetAuth.add("oauth", oauth);
        }

        if (targetAuth.size() == 0) {
            addProperty(json, "target", target);
        } else {
            JsonObject targetJson = new JsonObject();
            addProperty(targetJson, "url", target);
            targetJson.add("auth", targetAuth);

            json.add("target", targetJson);
        }

        return json;
    }

    private void addProperty(JsonObject json, String name, Object value) {
        if (value != null) {
            if (value instanceof Boolean) {
                json.addProperty(name, (Boolean) value);
            } else if (value instanceof String) {
                json.addProperty(name, (String) value);
            } else if (value instanceof Integer) {
                json.addProperty(name, (Integer) value);
            }
        }
    }
}
