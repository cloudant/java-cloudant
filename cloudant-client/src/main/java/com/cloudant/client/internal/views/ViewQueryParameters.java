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
import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.client.internal.util.QueryParameter;
import com.cloudant.client.internal.util.QueryParameters;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.Map;

public class ViewQueryParameters<K, V> extends QueryParameters implements Cloneable {

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

    @QueryParameter
    public JsonElement endkey = null;

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

    @QueryParameter
    public JsonElement key = null;

    @QueryParameter
    public JsonArray keys = null;

    @QueryParameter
    public Integer limit = null;

    @QueryParameter
    public Boolean reduce = null;

    @QueryParameter
    public Long skip = null;

    @QueryParameter
    public String stale = null;

    @QueryParameter
    public JsonElement startkey = null;

    @QueryParameter
    public String startkey_docid = null;

    /* Constructors */

    /**
     * Create a new parameter set for the same client, database, design doc, view and key/value
     * types as the passed in parameters
     *
     * @param parameters to copy information from
     */
    ViewQueryParameters(ViewQueryParameters<K, V> parameters) {
        this(parameters.client, parameters.db, parameters.designDoc, parameters.viewName,
                parameters.keyType, parameters.valueType);
    }

    public ViewQueryParameters(CloudantClient client, Database db, String designDoc, String
            viewName, Class<K> keyType, Class<V> valueType) {
        this.client = client;
        this.db = db;
        //remove _design from design doc name if it exists
        this.designDoc = designDoc;
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
        return jsonToKey(endkey);
    }

    public void setEndKey(K endkey) {
        this.endkey = keyToJson(endkey);
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
        if (key != null) {
            return (K[]) new Object[]{jsonToKey(key)};
        } else {
            if (keys != null) {
                K[] keysToReturn = (K[]) new Object[keys.size()];
                int i = 0;
                for (JsonElement key : keys) {
                    keysToReturn[i] = jsonToKey(key);
                    i++;
                }
                return keysToReturn;
            } else {
                return null;
            }
        }
    }

    public void setKeys(K[] keys) {
        if (keys != null) {
            if (keys.length == 1) {
                this.key = keyToJson(keys[0]);
            } else {
                JsonArray jsonKeys = new JsonArray();
                for (K key : keys) {
                    jsonKeys.add(keyToJson(key));
                }
                this.keys = jsonKeys;
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
        return jsonToKey(startkey);
    }

    public void setStartKey(K startkey) {
        this.startkey = keyToJson(startkey);
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

    HttpConnection asGetRequest() {
        DatabaseURIHelper builder = getViewURIBuilder();
        for (Map.Entry<String, Object> queryParameter : processParameters().entrySet()) {
            builder.query(queryParameter.getKey(), queryParameter.getValue());
        }
        return Http.GET(builder.build());
    }

    protected DatabaseURIHelper getViewURIBuilder() {
        return new DatabaseURIHelper(db.getDBUri()).path("_design").path(designDoc).path("_view")
                .path(viewName);
    }

    JsonElement asJson() {
        Map<String, Object> parameters = processParameters();
        return gson.toJsonTree(parameters);
    }

    public Class<K> getKeyType() {
        return this.keyType;
    }

    public Class<V> getValueType() {
        return this.valueType;
    }

    /**
     * Used instead of calling clone() directly to isolate CloneNotSupportedException handling in
     * this class.
     *
     * @return a shallow copy of this ViewQueryParameters
     */
    @SuppressWarnings("unchecked")
    ViewQueryParameters<K, V> copy() {
        try {
            return (ViewQueryParameters<K, V>) this.clone();
        } catch (CloneNotSupportedException e) {
            //should not reach this code as this class implements Cloneable
            throw new RuntimeException(e);
        }
    }

    private JsonElement keyToJson(K key) {
        return gson.toJsonTree(key, keyType);
    }

    private K jsonToKey(JsonElement jsonKey) {
        return gson.fromJson(jsonKey, keyType);
    }
}
