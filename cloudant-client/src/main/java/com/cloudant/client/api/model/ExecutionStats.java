package com.cloudant.client.api.model;

import com.google.gson.annotations.SerializedName;

public class ExecutionStats {

    /**
     * Number of index keys examined. Currently always 0.
     */
    @SerializedName("total_keys_examined")
    public final long totalKeysExamined;
    /**
     * Number of documents fetched from the database / index, equivalent to using include_docs=true in a view. These may then be filtered in-memory to further narrow down the result set based on the selector.
     */
    @SerializedName("total_docs_examined")
    public final long totalDocsExamined;
    /**
     * Number of documents fetched from the database using an out-of-band document fetch. This is only non-zero when read quorum &gt; 1 is specified in the query parameters.
     */
    @SerializedName("total_quorum_docs_examined")
    public final long totalQuorumDocsExamined;
    /**
     * Number of results returned from the query. Ideally this should not be significantly lower than the total documents / keys examined.
     */
    @SerializedName("results_returned")
    public final long resultsReturned;
    /**
     * Total execution time in milliseconds as measured by the database.
     */
    @SerializedName("execution_time_ms")
    public final double executionTimeMs;

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
