/**
 * Copyright (C) 2013 Cloudant
 *
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

package com.cloudant.client.internal;

import com.cloudant.client.org.lightcouch.Params;
import com.google.gson.Gson;

import java.net.URI;
import java.util.Map;

/**
 * Subclass for building a Cloudant database URI.
 * This class provides fluent style setters to create and build a URI.
 * If a client URI is required, see {@link URIBase}.
 */
public class DatabaseURIHelper extends URIBaseMethods<DatabaseURIHelper> {

    private static final String DESIGN_PREFIX = "_design/";

    /**
     * Constructs a {@code DatabaseURIHelper} for a given Cloudant or CouchDB URI and database name.
     */
    public DatabaseURIHelper(URI uri, String dbName) {
        this.baseUri = new URIBase(uri).path(dbName).build();
    }

    /**
     * Constructs a {@code DatabaseURIHelper} from a database URI.
     * @param uri uri containing Cloudant account and database
     */
    public DatabaseURIHelper(URI uri) {
        this.baseUri = uri;
    }

    @Override
    public DatabaseURIHelper returnThis() {
        return this;
    }

    /**
     * Returns the database URI for this instance.
     */
    public URI getDatabaseUri() {
        return this.baseUri;
    }

    /**
     * Encode a CouchDb Document or Attachment ID in a manner suitable for a GET request
     * @param in The Document or Attachment ID to encode, eg "a/document"
     * @return The encoded Document or Attachment ID, eg "a%2Fdocument"
     */
    public String encodeId(String in) {
        return encodePath(in);
    }

    /**
     * Returns URI for {@code _changes} endpoint using passed
     * {@code query}.
     */
    public URI changesUri(Map<String, Object> query) {

        //lets find the since parameter
        if(query.containsKey("since")){
            Object since = query.get("since");
            if(!(since instanceof String)){
                //json encode the seq number since it isn't a string
                Gson gson = new Gson();
                query.put("since",gson.toJson(since));
            }
        }
        return this.path("_changes").query(query).build();
    }

    /**
     * Returns URI for {@code _changes} endpoint using passed
     * {@code query}.
     */
    public URI changesUri(String queryKey, Object queryValue) {

        if(queryKey.equals("since")){
            if(!(queryValue instanceof String)){
                //json encode the seq number since it isn't a string
                Gson gson = new Gson();
                queryValue = gson.toJson(queryValue);
            }
        }
        return this.path("_changes").query(queryKey, queryValue).build();
    }

    /**
     * Returns URI for {@code _bulk_docs} endpoint.
     */
    public URI bulkDocsUri() {
        return this.path("_bulk_docs").build();
    }

    /**
     * Returns URI for {@code _revs_diff} endpoint.
     */
    public URI revsDiffUri() {
        return this.path("_revs_diff").build();
    }

    /**
     * Returns URI for {@code documentId}.
     */
    public URI documentUri(String documentId) {
        return documentUri(documentId, null, null);
    }

    /**
     * Returns URI for {@code documentId} and {@code revId}.
     */
    public URI documentUri(String documentId, String revId) {
        return documentUri(documentId, "rev", revId);
    }

    /**
     * Returns URI for {@code documentId} with {@code query} key and value.
     */
    public URI documentUri(String documentId, String key, Object value)  {
        return this.documentId(documentId).query(key, value).build();
    }

    /**
     * Returns URI for {@code documentId} with {@code query} key and value.
     */
    public URI documentUri(String documentId, Params params)  {
        return this.documentId(documentId).query(params).build();
    }

    /**
     * Returns URI for Attachment having {@code attachmentId} for {@code documentId}.
     */
    public URI attachmentUri(String documentId, String attachmentId) {
        return this.documentId(documentId).attachmentId(attachmentId).build();
    }

    /**
     * Returns URI for Attachment having {@code attachmentId} for {@code documentId}
     * and {@code revId}.
     */
    public URI attachmentUri(String documentId, String revId, String attachmentId) {
        return this.documentId(documentId).revId(revId).attachmentId(attachmentId).build();
    }

    public DatabaseURIHelper attachmentId(String attachmentId) {
        this.path(attachmentId);
        return returnThis();
    }

    public DatabaseURIHelper documentId(String documentId) {
        //Handle design documents
        if(documentId.startsWith(DESIGN_PREFIX)) {
            ensureDesignPrefix(documentId);
        } else {
            this.path(documentId);
        }
        return returnThis();
    }

    private DatabaseURIHelper revId(String revId) {
        this.query("rev", revId);
        return returnThis();
    }

    public DatabaseURIHelper query(Map<String, Object> query) {
        if(query != null && query.size() > 0) {
            for (Map.Entry<String, Object> entry : query.entrySet()) {
                query(entry.getKey(), entry.getValue());
            }
        }
        return returnThis();
    }

    public DatabaseURIHelper query(String query) {
        this.completeQuery = query;
        return returnThis();
    }

    public DatabaseURIHelper query(Params params) {
        this.qParams = params;
        return returnThis();
    }

    /**
     * Certify that the id in the design document contains the necessary `_design` prefix.
     * The _design prefix is passed separately to path method.
     * @param id The design document's id
     */
    private DatabaseURIHelper ensureDesignPrefix(String id)
    {
        id = id.startsWith(DESIGN_PREFIX)
                ? id.replace(DESIGN_PREFIX, "")
                : id;
        this.path("_design").path(id);
        return returnThis();
    }
}
