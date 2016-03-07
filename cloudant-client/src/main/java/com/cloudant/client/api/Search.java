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

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.JsonToObject;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.close;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getAsLong;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.getAsString;

import com.cloudant.client.api.model.SearchResult;
import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class provides access to the Cloudant <tt>Search</tt> APIs.
 * <p>Usage Example:</p>
 * <pre>
 * {@code
 *  List<Bird> birds = db.search("views101/animals")
 * 	.limit(10)
 * 	.includeDocs(true)
 * 	.query("class:bird", Bird.class);
 *
 * // groups
 * Map<String,List<Bird>> birdGroups = db.search("views101/animals")
 * 	.limit(10)
 * 	.includeDocs(true)
 * 	.queryGroups("class:bird", Bird.class);
 * for ( Entry<String, List<Bird>> group : birdGroups.entrySet()) {
 * 		System.out.println("Group Name : " +  group.getKey());
 * 		for ( Bird b : group.getValue() ) {
 * 			 System.out.println("\t" + b);
 * 		 }
 * 	}
 *
 *  // search results object
 *  SearchResult<Bird> result = db.search("views101/animals")
 *   .querySearchResult("class:bird", Bird.class);
 *
 *  // pagination
 *  SearchResult<Bird> nextPage = db.search("views101/animals")
 *   .bookmark(result.bookmark)
 *   .querySearchResult("class:bird", Bird.class);
 * }
 * </pre>
 *
 * @author Mario Briggs
 * @see Database#search(String)
 * @see SearchResult
 * @since 0.0.1
 */
public class Search {

    private static final Logger log = Logger.getLogger(Search.class.getCanonicalName());

    // search fields
    private Integer limit;
    private boolean includeDocs = false;
    private String bookmark;
    private CloudantClient client;
    private DatabaseURIHelper databaseHelper;


    Search(CloudantClient client, Database db, String searchIndexId) {
        assertNotEmpty(searchIndexId, "searchIndexId");
        this.client = client;
        String search = searchIndexId;
        if (searchIndexId.contains("/")) {
            String[] v = searchIndexId.split("/");
            this.databaseHelper = new DatabaseURIHelper(db.getDBUri()).path("_design")
                .path(v[0]).path("_search").path(v[1]);
        } else {
            this.databaseHelper = new DatabaseURIHelper(db.getDBUri()).path(search);
        }
    }

    // Query options

    /**
     * Performs a Cloudant Search and returns the result as an {@link InputStream}
     *
     * @param query the Lucene query to be passed to the Search index
     *              <p>The stream should be properly closed after usage, as to avoid connection
     *              leaks.
     * @return The result as an {@link InputStream}.
     */
    public InputStream queryForStream(String query) {
        key(query);
        URI uri = databaseHelper.build();
        HttpConnection get = Http.GET(uri);
        get.requestProperties.put("Accept", "application/json");
        return client.couchDbClient.executeToInputStream(get);
    }

    /**
     * Queries a Search Index and returns ungrouped results. In case the query
     * used grouping, an empty list is returned
     *
     * @param <T>      Object type T
     * @param query    the Lucene query to be passed to the Search index
     * @param classOfT The class of type T
     * @return The result of the search query as a {@code List<T> }
     */
    public <T> List<T> query(String query, Class<T> classOfT) {
        InputStream instream = null;
        List<T> result = new ArrayList<T>();
        try {
            Reader reader = new InputStreamReader(instream = queryForStream(query), "UTF-8");
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            if (json.has("rows")) {
                if (!includeDocs) {
                    log.warning("includeDocs set to false and attempting to retrieve doc. " +
                            "null object will be returned");
                }
                for (JsonElement e : json.getAsJsonArray("rows")) {
                    result.add(JsonToObject(client.getGson(), e, "doc", classOfT));
                }
            } else {
                log.warning("No ungrouped result available. Use queryGroups() if grouping set");
            }
            return result;
        } catch (UnsupportedEncodingException e1) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e1);
        } finally {
            close(instream);
        }
    }

    /**
     * Queries a Search Index and returns grouped results in a map where key
     * of the map is the groupName. In case the query didnt use grouping,
     * an empty map is returned
     *
     * @param <T>      Object type T
     * @param query    the Lucene query to be passed to the Search index
     * @param classOfT The class of type T
     * @return The result of the grouped search query as a ordered {@code Map<String,T> }
     */
    public <T> Map<String, List<T>> queryGroups(String query, Class<T> classOfT) {
        InputStream instream = null;
        try {
            Reader reader = new InputStreamReader(instream = queryForStream(query), "UTF-8");
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            Map<String, List<T>> result = new LinkedHashMap<String, List<T>>();
            if (json.has("groups")) {
                for (JsonElement e : json.getAsJsonArray("groups")) {
                    String groupName = e.getAsJsonObject().get("by").getAsString();
                    List<T> orows = new ArrayList<T>();
                    if (!includeDocs) {
                        log.warning("includeDocs set to false and attempting to retrieve doc. " +
                                "null object will be returned");
                    }
                    for (JsonElement rows : e.getAsJsonObject().getAsJsonArray("rows")) {
                        orows.add(JsonToObject(client.getGson(), rows, "doc", classOfT));
                    }
                    result.put(groupName, orows);
                }// end for(groups)
            }// end hasgroups
            else {
                log.warning("No grouped results available. Use query() if non grouped query");
            }
            return result;
        } catch (UnsupportedEncodingException e1) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e1);
        } finally {
            close(instream);
        }
    }


    /**
     * Performs a Cloudant Search and returns the result as an {@link SearchResult}
     *
     * @param <T>      Object type T, an instance into which the rows[].doc/group[].rows[].doc
     *                 attribute of the Search result response should be deserialized into. Same
     *                 goes for the rows[].fields/group[].rows[].fields attribute
     * @param query    the Lucene query to be passed to the Search index
     * @param classOfT The class of type T.
     * @return The Search result entries
     */
    public <T> SearchResult<T> querySearchResult(String query, Class<T> classOfT) {
        InputStream instream = null;
        try {
            Reader reader = new InputStreamReader(instream = queryForStream(query), "UTF-8");
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            SearchResult<T> sr = new SearchResult<T>();
            sr.setTotalRows(getAsLong(json, "total_rows"));
            sr.setBookmark(getAsString(json, "bookmark"));
            if (json.has("rows")) {
                sr.setRows(getRows(json.getAsJsonArray("rows"), sr, classOfT));
            } else if (json.has("groups")) {
                setGroups(json.getAsJsonArray("groups"), sr, classOfT);
            }

            if (json.has("counts")) {
                sr.setCounts(getFieldsCounts(json.getAsJsonObject("counts").entrySet()));
            }

            if (json.has("ranges")) {
                sr.setRanges(getFieldsCounts(json.getAsJsonObject("ranges").entrySet()));
            }
            return sr;
        } catch (UnsupportedEncodingException e) {
            // This should never happen as every implementation of the java platform is required
            // to support UTF-8.
            throw new RuntimeException(e);
        } finally {
            close(instream);
        }
    }

    /**
     * @param limit limit the number of documents in the result
     * @return this for additional parameter setting or to query
     */
    public Search limit(Integer limit) {
        this.limit = limit;
        databaseHelper.query("limit", this.limit);
        return this;
    }


    /**
     * Control which page of results to get. The bookmark value is obtained by executing
     * the query()/queryForStream() once and getting it from the bookmark field
     * in the response
     *
     * @param bookmark see the next page of results after this bookmark result
     * @return this for additional parameter setting or to query
     */
    public Search bookmark(String bookmark) {
        this.bookmark = bookmark;
        databaseHelper.query("bookmark", this.bookmark);
        return this;
    }

    /**
     * Specify the sort order for the result.
     *
     * @param sortJson JSON string specifying the sort order
     * @return this for additional parameter setting or to query
     * @see <a target="_blank" href="http://docs.cloudant.com/api/search.html">sort query
     * parameter format</a>
     */
    public Search sort(String sortJson) {
        assertNotEmpty(sortJson, "sort");
        databaseHelper.query("sort", sortJson);
        return this;
    }


    /**
     * Group results by the specified field.
     *
     * @param fieldName by which to group results
     * @param isNumber  whether field isNumeric.
     * @return this for additional parameter setting or to query
     */
    public Search groupField(String fieldName, boolean isNumber) {
        assertNotEmpty(fieldName, "fieldName");
        if (isNumber) {
            databaseHelper.query("group_field", fieldName + "<number>");
        } else {
            databaseHelper.query("group_field", fieldName);
        }
        return this;
    }

    /**
     * Maximum group count when groupField is set
     *
     * @param limit the maximum group count
     * @return this for additional parameter setting or to query
     */
    public Search groupLimit(int limit) {
        databaseHelper.query("group_limit", limit);
        return this;
    }

    /**
     * the sort order of the groups when groupField is set
     *
     * @param groupsortJson JSON string specifying the group sort
     * @return this for additional parameter setting or to query
     * @see <a target="_blank" href="http://docs.cloudant.com/api/search.html">sort query
     * parameter format</a>
     */
    public Search groupSort(String groupsortJson) {
        assertNotEmpty(groupsortJson, "groupsortJson");
        databaseHelper.query("group_sort", groupsortJson);
        return this;
    }

    /**
     * Ranges for faceted searches
     *
     * @param rangesJson JSON string specifying the ranges
     * @return this for additional parameter setting or to query
     * @see <a target="_blank" href="http://docs.cloudant.com/api/search.html">ranges query
     * argument format</a>
     */
    public Search ranges(String rangesJson) {
        assertNotEmpty(rangesJson, "rangesJson");
        databaseHelper.query("ranges", rangesJson);
        return this;
    }

    /**
     * Array of fieldNames for which counts should be produced
     *
     * @param countsfields array of the field names
     * @return this for additional parameter setting or to query
     */
    public Search counts(String[] countsfields) {
        assert (countsfields.length > 0);
        JsonArray countsJsonArray = new JsonArray();
        for(String countsfield : countsfields) {
            JsonPrimitive element = new JsonPrimitive(countsfield);
            countsJsonArray.add(element);
        }
        databaseHelper.query("counts", countsJsonArray);
        return this;
    }

    /**
     * @param fieldName  the name of the field
     * @param fieldValue the value of the field
     * @return this for additional parameter setting or to query
     * @see <a target="_blank" href="https://docs.cloudant.com/search.html#faceting">drilldown
     * query parameter</a>
     */
    public Search drillDown(String fieldName, String fieldValue) {
        assertNotEmpty(fieldName, "fieldName");
        assertNotEmpty(fieldValue, "fieldValue");
        JsonArray drillDownArray = new JsonArray();
        JsonPrimitive fieldNamePrimitive = new JsonPrimitive(fieldName);
        drillDownArray.add(fieldNamePrimitive);
        JsonPrimitive fieldValuePrimitive = new JsonPrimitive(fieldValue);
        drillDownArray.add(fieldValuePrimitive);
        databaseHelper.query("drilldown", drillDownArray, false);
        return this;
    }


    /**
     * @param stale Accept values: ok
     * @return this for additional parameter setting or to query
     */
    public Search stale(boolean stale) {
        if (stale) {
            databaseHelper.query("stale", "ok");
        }
        return this;
    }

    /**
     * @param includeDocs whether to include the document in the result
     * @return this for additional parameter setting or to query
     */
    public Search includeDocs(Boolean includeDocs) {
        this.includeDocs = includeDocs;
        databaseHelper.query("include_docs", this.includeDocs);
        return this;
    }


    private void key(String query) {
        databaseHelper.query("q", query);
    }

    private Map<String, Map<String, Long>> getFieldsCounts(Set<Map.Entry<String, JsonElement>>
                                                                   fldset) {
        Map<String, Map<String, Long>> map = new HashMap<String, Map<String, Long>>();
        for (Entry<String, JsonElement> fld : fldset) {
            String field = fld.getKey();
            Map<String, Long> ovalues = new HashMap<String, Long>();
            if (fld.getValue().isJsonObject()) {
                Set<Map.Entry<String, JsonElement>> values = fld.getValue().getAsJsonObject()
                        .entrySet();
                for (Entry<String, JsonElement> value : values) {
                    ovalues.put(value.getKey(), value.getValue().getAsLong());
                }
            }
            map.put(field, ovalues);
        }
        return map;
    }

    private <T> List<SearchResult<T>.SearchResultRow> getRows(
            JsonArray jsonrows, SearchResult<T> sr, Class<T> classOfT) {

        List<SearchResult<T>.SearchResultRow> ret = new ArrayList<SearchResult<T>
                .SearchResultRow>();
        for (JsonElement e : jsonrows) {
            SearchResult<T>.SearchResultRow row = sr.new SearchResultRow();
            JsonObject oe = e.getAsJsonObject();
            row.setId(oe.get("id").getAsString());
            row.setOrder(JsonToObject(client.getGson(), e, "order", Object[].class));
            row.setFields(JsonToObject(client.getGson(), e, "fields", classOfT));
            if (includeDocs) {
                row.setDoc(JsonToObject(client.getGson(), e, "doc", classOfT));
            }
            ret.add(row);
        }
        return ret;
    }

    private <T> void setGroups(JsonArray jsongroups, SearchResult<T> sr, Class<T> classOfT) {
        for (JsonElement e : jsongroups) {
            SearchResult<T>.SearchResultGroup group = sr.new SearchResultGroup();
            JsonObject oe = e.getAsJsonObject();
            group.setBy(oe.get("by").getAsString());
            group.setTotalRows(oe.get("total_rows").getAsLong());
            group.setRows(getRows(oe.getAsJsonArray("rows"), sr, classOfT));
            sr.getGroups().add(group);
        }
    }
}

