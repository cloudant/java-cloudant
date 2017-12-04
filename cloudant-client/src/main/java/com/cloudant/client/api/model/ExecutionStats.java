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
package com.cloudant.client.api.model;

import com.google.gson.annotations.SerializedName;

public class ExecutionStats {

    @SerializedName("total_keys_examined")
    private final long totalKeysExamined;
    @SerializedName("total_docs_examined")
    private final long totalDocsExamined;
    /**
     * Number of documents fetched from the database using an out-of-band document fetch. This is only non-zero when read quorum &gt; 1 is specified in the query parameters.
     */
    @SerializedName("total_quorum_docs_examined")
    private final long totalQuorumDocsExamined;
    /**
     * Number of results returned from the query. Ideally this should not be significantly lower than the total documents / keys examined.
     */
    @SerializedName("results_returned")
    private final long resultsReturned;
    /**
     * Total execution time in milliseconds as measured by the database.
     */
    @SerializedName("execution_time_ms")
    private final double executionTimeMs;

    /**
     * Number of index keys examined. Currently always 0.
     */
    public long getTotalKeysExamined() {
        return totalKeysExamined;
    }

    public long getTotalDocsExamined() {
        return totalDocsExamined;
    }

    public long getTotalQuorumDocsExamined() {
        return totalQuorumDocsExamined;
    }

    public long getResultsReturned() {
        return resultsReturned;
    }

    public double getExecutionTimeMs() {
        return executionTimeMs;
    }

    public ExecutionStats(long totalKeysExamined, long totalDocsExamined, long
            totalQuorumDocsExamined, long resultsReturned, long executionTimeMs) {
        this.totalKeysExamined = totalKeysExamined;
        this.totalDocsExamined = totalDocsExamined;
        this.totalQuorumDocsExamined = totalQuorumDocsExamined;
        this.resultsReturned = resultsReturned;
        this.executionTimeMs = executionTimeMs;
    }

    @Override
    public String toString() {
        return "ExecutionStats{" +
                "totalKeysExamined=" + totalKeysExamined +
                ", totalDocsExamined=" + totalDocsExamined +
                ", totalQuorumDocsExamined=" + totalQuorumDocsExamined +
                ", resultsReturned=" + resultsReturned +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}
