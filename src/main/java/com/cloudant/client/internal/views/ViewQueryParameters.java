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

package com.cloudant.client.internal.views;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.internal.util.QueryParameter;
import com.cloudant.client.internal.util.QueryParameters;
import com.cloudant.client.org.lightcouch.internal.URIBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.apache.http.client.methods.HttpGet;

import java.util.Arrays;
import java.util.Map;

public class ViewQueryParameters<K, V> extends QueryParameters {

    private final CloudantClient client;
    private final Database db;
    private final String designDoc;
    private final String viewName;
    private final Class<K> keyType;
    private final Class<V> valueType;
    private final Gson gson;
    private Integer rowsPerPage = null;

    /* Query Parameters
    * Note that null is used for unset parameters and in those cases the default will be applied
    * server side.
    */

    @QueryParameter
    public Boolean descending = null;

    @QueryParameter(json = true)
    public K endkey = null;

    @QueryParameter
    public String endkey_docid = null;

    @QueryParameter
    public Boolean group = null;

    @QueryParameter
    public Integer group_level = null;

    @QueryParameter
    public Boolean include_docs = null;

    @QueryParameter
    public Boolean inclusive_end = null;

    @QueryParameter(json = true)
    public K key = null;

    @QueryParameter(json = true)
    public K[] keys = null;

    @QueryParameter
    public Integer limit = null;

    @QueryParameter
    public Boolean reduce = null;

    @QueryParameter
    public Long skip = null;

    @QueryParameter
    public String stale = null;

    @QueryParameter(json = true)
    public K startkey = null;

    @QueryParameter
    public String startkey_docid = null;

    /* Constructors */

    /**
     * Create a new parameter set for the same client, database, design doc, view and key/value
     * types as the passed in parameters
     *
     * @param parameters to copy information from
     */
    public ViewQueryParameters(ViewQueryParameters<K, V> parameters) {
        this(parameters.client, parameters.db, parameters.designDoc, parameters.viewName,
                parameters.keyType, parameters.valueType);
    }

    public ViewQueryParameters(CloudantClient client, Database db, String designDoc, String
            viewName, Class<K> keyType, Class<V> valueType) {
        this.client = client;
        this.db = db;
        //ensure design doc name starts with _design
        this.designDoc = designDoc.startsWith("_design") ? designDoc : "_design/" + designDoc;
        this.viewName = viewName;
        this.keyType = keyType;
        this.valueType = valueType;
        this.gson = client.getGson();
    }

    /* Getters & setters
    * Note that boolean getters return the default value in the case where the parameter is unset.
    * */

    public CloudantClient getClient() {
        return client;
    }

    public boolean getDescending() {
        return descending == null ? false : descending;
    }

    public void setDescending(boolean descending) {
        this.descending = descending;
    }

    public K getEndkey() {
        return endkey;
    }

    public void setEndKey(K endkey) {
        this.endkey = endkey;
    }

    public String getEndKeyDocId() {
        return endkey_docid;
    }

    public void setEndKeyDocId(String endkey_docid) {
        this.endkey_docid = endkey_docid;
    }

    public boolean getGroup() {
        return group == null ? false : group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public Integer getGroupLevel() {
        return group_level;
    }

    public void setGroupLevel(Integer group_level) {
        this.group_level = group_level;
    }

    public boolean getIncludeDocs() {
        return include_docs == null ? false : include_docs;
    }

    public void setIncludeDocs(boolean include_docs) {
        this.include_docs = include_docs;
    }

    public boolean getInclusiveEnd() {
        return inclusive_end == null ? true : inclusive_end;
    }

    public void setInclusiveEnd(boolean inclusive_end) {
        this.inclusive_end = inclusive_end;
    }

    @SuppressWarnings("unchecked")
    public K[] getKeys() {
        if (keys != null) {
            return Arrays.copyOf(keys, keys.length);
        } else {
            return (K[]) new Object[]{key}; //suppress warning we know key is a K
        }
    }

    public void setKeys(K[] keys) {
        if (keys != null) {
            if (keys.length == 1) {
                this.key = keys[0];
            } else {
                this.keys = Arrays.copyOf(keys, keys.length);
            }
        }
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public boolean getReduce() {
        return reduce == null ? true : reduce;
    }

    public void setReduce(boolean reduce) {
        this.reduce = reduce;
    }

    public Long getSkip() {
        return skip;
    }

    public void setSkip(Long skip) {
        this.skip = skip;
    }

    public String getStale() {
        return stale;
    }

    public void setStale(String stale) {
        this.stale = stale;
    }

    public K getStartKey() {
        return startkey;
    }

    public void setStartKey(K startkey) {
        this.startkey = startkey;
    }

    public String getStartKeyDocId() {
        return startkey_docid;
    }

    public void setStartKeyDocId(String startkey_docid) {
        this.startkey_docid = startkey_docid;
    }

    public Integer getRowsPerPage() {
        if (rowsPerPage == null || rowsPerPage <= 0) {
            return null;
        } else {
            return rowsPerPage;
        }
    }

    public void setRowsPerPage(Integer rowsPerPage) {
        if (rowsPerPage == null || rowsPerPage < 1 || rowsPerPage > Integer.MAX_VALUE - 1) {
            throw new IllegalArgumentException(String.format("Rows per page must be between %s " +
                    "and %s", 1, Integer.MAX_VALUE - 1));
        }
        this.rowsPerPage = rowsPerPage;
        this.limit = rowsPerPage + 1;
    }

    /* Parameter output methods */

    public HttpGet asGetRequest() {
        URIBuilder builder = getViewURIBuilder();
        for (Map.Entry<String, Object> queryParameter : processParameters(gson).entrySet()) {
            builder.query(queryParameter.getKey(), queryParameter.getValue());
        }
        return new HttpGet(builder.buildEncoded());
    }

    protected URIBuilder getViewURIBuilder() {
        return URIBuilder.buildUri(db.getDBUri()).path(designDoc +
                "/_view/" + viewName);
    }

    public JsonElement asJson() {
        Map<String, Object> parameters = processParameters(gson);
        return gson.toJsonTree(parameters);
    }

    public ViewQueryParameters<K, V> forwardPaginationQueryParameters(K startkey, String
            startkey_docid) {
        ViewQueryParameters<K, V> pageParameters = new ViewQueryParameters<K, V>(this);
        //set the start parameters
        pageParameters.setStartKey(startkey);
        pageParameters.setStartKeyDocId(startkey_docid);

        //stay the same for forward
        pageParameters.inclusive_end = inclusive_end;
        pageParameters.descending = descending;
        if (endkey != null) {
            pageParameters.setEndKey(endkey);
        }
        pageParameters.setEndKeyDocId(endkey_docid);

        //all other parameters stay the same
        //note we set directly rather than using the setter so that unset parameters retain their
        //unset state
        pageParameters.limit = limit;
        pageParameters.group_level = group_level;
        pageParameters.group = group;
        pageParameters.include_docs = include_docs;
        pageParameters.reduce = reduce;
        pageParameters.stale = stale;
        //use the setter for keys because of the array handling
        pageParameters.setKeys(getKeys());
        return pageParameters;
    }

    //pages only have a reference to the start key, when paging backwards this is the
    // startkey of the following page (i.e. the last element of the previous page) so to
    // correctly present page results when paging backwards requires some parameters to
    // be reversed for the previous page request
    public ViewQueryParameters<K, V> reversePaginationQueryParameters(K startkey, String
            startkey_docid) {
        //get a copy of the parameters, using the forward pagination method
        ViewQueryParameters<K, V> reversedParameters = forwardPaginationQueryParameters(startkey,
                startkey_docid);

        //now reverse some of the parameters to page backwards
        //paging backward is descending from the original direction
        reversedParameters.setDescending(!getDescending());

        //we must always include our start key if paging backwards so inclusive end is true
        reversedParameters.setInclusiveEnd(true);

        //any initial startkey is now the end key because we are reversed from original direction
        if (startkey != null) {
            reversedParameters.setEndKey(this.startkey);
        }
        if (startkey_docid != null) {
            reversedParameters.setEndKeyDocId(this.startkey_docid);
        }

        return reversedParameters;
    }

    public Class<K> getKeyType() {
        return this.keyType;
    }

    public Class<V> getValueType() {
        return this.valueType;
    }

}
