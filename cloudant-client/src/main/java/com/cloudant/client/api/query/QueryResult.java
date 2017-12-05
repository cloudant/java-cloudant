/*
 * Copyright © 2017 IBM Corp. All rights reserved.
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

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class QueryResult<T> {

    private final List<T> docs;

    private final String warning;

    @SerializedName("execution_stats")
    private final ExecutionStats executionStats;

    private final String bookmark;

    public QueryResult(List<T> docs, String warning, ExecutionStats executionStats, String
            bookmark) {
        this.docs = docs;
        this.warning = warning;
        this.executionStats = executionStats;
        this.bookmark = bookmark;
    }

    /**
     * @return
     * Array of documents matching the search. In each matching document, the fields specified in
     * the fields part of the request body are listed, along with their values.
     */
    public List<T> getDocs() {
        return docs;
    }

    /**
     * @return
     * Execution warnings.
     */
    public String getWarning() {
        return warning;
    }

    /**
     * @return
     * Execution statistics.
     */
    public ExecutionStats getExecutionStats() {
        return executionStats;
    }

    /**
     * @return
     * An opaque string used for paging. See {@link com.cloudant.client.api.query.QueryBuilder#bookmark}
     * for usage details.
     *
     * @see com.cloudant.client.api.query.QueryBuilder#bookmark
     */
    public String getBookmark() {
        return bookmark;
    }
}
