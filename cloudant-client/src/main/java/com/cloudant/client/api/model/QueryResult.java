package com.cloudant.client.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class QueryResult<T> {

    public List<T> docs;

    public String warning;

    @SerializedName("execution_stats")
    public ExecutionStats executionStats;

    public String bookmark;
}
