package com.cloudant.client.api.model;

import java.util.List;

public class QueryResult<T> {

    public List<T> docs;

    public String warning;

    public ExecutionStats executionStats;

    public String bookmark;
}
