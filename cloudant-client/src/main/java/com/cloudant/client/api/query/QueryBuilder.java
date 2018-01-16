/*
 * Copyright © 2017, 2018 IBM Corp. All rights reserved.
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
package com.cloudant.client.api.query;

import com.cloudant.client.internal.query.Helpers;
import com.cloudant.http.HttpConnection;

import java.util.LinkedList;

/**
 * <p>
 *     Helper class for building query selector strings.
 * </p>
 * <p>
 *     Example usage to return the name and year of movies starring Alec Guinness since 1960
 *     with the results sorted by year descending:
 * </p>
 * <pre>
 * {@code
 * String query = db.query(new QueryBuilder(and(
 *   gt("Movie_year", 1960),
 *   eq("Person_name", "Alec Guinness"))).
 *   sort(Sort.desc("Movie_year")).
 *   fields("Movie_name", "Movie_year").
 *   build();
 * }
 * </pre>
 * <p>
 *     The resulting string can then be used to query the database using either
 *     {@link com.cloudant.client.api.Database#query(String, Class)} (which deserialises the results)
 *     or {@link com.cloudant.client.api.CloudantClient#executeRequest(HttpConnection)} (which can
 *     be used to make a "raw" request by POSTing the string to the {@code /_find} endpoint).
 * </p>
 * @see Selector
 */
public class QueryBuilder {

    private final Selector selector;
    private String[] fields;
    private Sort[] sort;
    private Long limit;
    private Long skip;
    private String bookmark;
    private boolean update = true;
    private boolean stable;
    private boolean executionStats;
    private String[] useIndex;

    /**
     * <p>
     * Construct a {@code QueryBuilder} object for use with
     * {@link com.cloudant.client.api.Database#query(String, Class)}
     * </p>
     * <p>
     * Obtain a selector from an {@link Operation} or {@link Expression}.
     * </p>
     * @param selector {@code Selector} object describing criteria used to select
     *                 documents.
     * @see <a
     * href="https://console.bluemix.net/docs/services/Cloudant/api/cloudant_query.html#selector-syntax"
     * target="_blank">selector syntax</a>
     * @see com.cloudant.client.api.Database#query(String, Class)
     * @see Selector
     */
    public QueryBuilder(Selector selector) {
        this.selector = selector;
    }

    /**
     * Set the fields option for the query builder.
     * @param fields List specifying which fields of each document should be returned. If it is
     *               omitted, the entire document is returned.
     * @return {@code QueryBuilder} object for method chaining.
     */
    public QueryBuilder fields(String... fields) {
        this.fields = fields;
        return this;
    }

    /**
     * Set the sort option for the query builder.
     * @param sort List specifying sort order of returned documents.
     * @return {@code QueryBuilder} object for method chaining.
     */
    public QueryBuilder sort(Sort... sort) {
        this.sort = sort;
        return this;
    }

    /**
     * Set the limit option for the query builder.
     * @param limit Maximum number of results returned. Default is 25.
     * @return {@code QueryBuilder} object for method chaining.
     */
    public QueryBuilder limit(long limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Set the skip option for the query builder.
     * @param skip Skip the first {@code n} results, where {@code n} is the value specified.
     * @return {@code QueryBuilder} object for method chaining.
     */
    public QueryBuilder skip(long skip) {
        this.skip = skip;
        return this;
    }

    /**
     * Set the bookmark option for the query builder.
     * @param bookmark  A string that enables you to specify which page of results you require.
     *                  Used for paging through result sets. Every query returns an opaque string
     *                  under the bookmark key that can then be passed back in a query to get the
     *                  next page of results. If any part of the selector query changes between
     *                  requests, the results are undefined.
     * @return {@code QueryBuilder} object for method chaining.
     */
    public QueryBuilder bookmark(String bookmark) {
        this.bookmark = bookmark;
        return this;
    }

    /**
     * Set the update option for the query builder.
     * @param update Whether to update the index prior to returning the result. Default is {@code
     * true}.
     * @return {@code QueryBuilder} object for method chaining.
     */
    public QueryBuilder update(boolean update) {
        this.update = update;
        return this;
    }

    /**
     * Set the stable option for the query builder.
     * @param stable Whether or not the view results should be returned from a "stable" set of shards.
     * @return {@code QueryBuilder} object for method chaining.
     */
    public QueryBuilder stable(boolean stable) {
        this.stable = stable;
        return this;
    }

    /**
     * Set the executionStats option for the query builder.
     * @param executionStats Include execution statistics in the query response. Defailt is
     * {@code false}
     * @return {@code QueryBuilder} object for method chaining.
     */
    public QueryBuilder executionStats(boolean executionStats) {
        this.executionStats = executionStats;
        return this;
    }

    /**
     * Instruct a query to use a specific index.
     * @param designDocument Design document to use.
     * @return {@code QueryBuilder} object for method chaining.
     */
    public QueryBuilder useIndex(String designDocument) {
        useIndex = new String[]{designDocument};
        return this;
    }

    /**
     * Instruct a query to use a specific index.
     * @param designDocument Design document to use.
     * @param indexName Index name to use.
     * @return {@code QueryBuilder} object for method chaining.
     */
    public QueryBuilder useIndex(String designDocument, String indexName) {
        useIndex = new String[]{designDocument, indexName};
        return this;
    }

    /**
     * Build string representation of query for use with
     * {@link com.cloudant.client.api.Database#query(String, Class)}.
     * @return String representation of query.
     */
    public String build() {
        String fieldsString = this.fields == null ? null : Helpers.quote(this.fields);
        String sortString = this.sort == null ? null : quoteSort(this.sort);
        String limitString = this.limit == null ? null : Helpers.quote(this.limit);
        String skipString = this.skip == null ? null : Helpers.quote(this.skip);
        String bookmarkString = this.bookmark == null ? null : Helpers.quote(this.bookmark);
        String useIndexString = this.useIndex == null ? null : Helpers.quote(this.useIndex);
        StringBuilder builder = new StringBuilder();
        // build up components...
        // selector
        builder.append(Helpers.withKey(Helpers.SELECTOR, this.selector));
        // fields
        if (fieldsString != null) {
            builder.append(String.format(", \"fields\": %s", fieldsString));
        }
        // sort
        if (sortString != null) {
            builder.append(String.format(", \"sort\": %s", sortString));
        }
        // limit
        if (limitString != null) {
            builder.append(String.format(", \"limit\": %s", limitString));
        }
        // skip
        if (skipString != null) {
            builder.append(String.format(", \"skip\": %s", skipString));
        }
        if (bookmarkString != null) {
            builder.append(String.format(", \"bookmark\": %s", bookmarkString));
        }
        if (!this.update) {
            builder.append(", \"update\": false");
        }
        if (this.stable) {
            builder.append(", \"stable\": true");
        }
        // execution_stats
        if (this.executionStats) {
            builder.append(", \"execution_stats\": true");
        }
        if (useIndexString != null) {
            builder.append(String.format(", \"use_index\": %s", useIndexString));
        }
        return String.format("{%s}", builder.toString());
    }

    // sorts are a bit more awkward and need a helper...
    private static String quoteSort(Sort[] sort) {
        LinkedList<String> sorts = new LinkedList<String>();
        for (Sort pair : sort) {
            sorts.add(String.format("{%s: %s}", Helpers.quote(pair.getName()), Helpers.quote(pair.getOrder().toString())));
        }
        return sorts.toString();
    }
}
