package com.cloudant.client.api.scheduler;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SchedulerJobsResponse {

    private long offset;
    @SerializedName("total_rows")
    private long totalRows;
    private String id;
    private String database;
    @SerializedName("doc_id")
    private String docId;
    private List<History> history;
    private String pid;
    private String node;
    private String source;
    private String target;
    @SerializedName("total_rows")
    private String startTime;

    public SchedulerJobsResponse(long offset, long totalRows, String id, String database, String
            docId, List<History> history, String pid, String node, String source, String target,
                                 String startTime) {
        this.offset = offset;
        this.totalRows = totalRows;
        this.id = id;
        this.database = database;
        this.docId = docId;
        this.history = history;
        this.pid = pid;
        this.node = node;
        this.source = source;
        this.target = target;
        this.startTime = startTime;
    }

    public long getOffset() {
        return offset;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public String getId() {
        return id;
    }

    public String getDatabase() {
        return database;
    }

    public String getDocId() {
        return docId;
    }

    public List<History> getHistory() {
        return history;
    }

    public String getPid() {
        return pid;
    }

    public String getNode() {
        return node;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getStartTime() {
        return startTime;
    }

    static class History {
        private String timestamp;
        private String type;

        public History(String timestamp, String type) {
            this.timestamp = timestamp;
            this.type = type;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getType() {
            return type;
        }
    }

}
