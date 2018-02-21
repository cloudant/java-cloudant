package com.cloudant.client.api.scheduler;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SchedulerDocsResponse {

    private List<Doc> docs;
    private long offset;
    @SerializedName("total_rows")
    private long totalRows;

    public SchedulerDocsResponse(List<Doc> docs, long offset, long totalRows) {
        this.docs = docs;
        this.offset = offset;
        this.totalRows = totalRows;
    }

    public List<Doc> getDocs() {
        return docs;
    }

    public long getOffset() {
        return offset;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public class Doc {

        public Doc(String database, String docId, long errorCount, String id, Object info,
                   String lastUpdated, String node, String proxy, String source, String
                           startTime, String state, String target) {
            this.database = database;
            this.docId = docId;
            this.errorCount = errorCount;
            this.id = id;
            this.info = info;
            this.lastUpdated = lastUpdated;
            this.node = node;
            this.proxy = proxy;
            this.source = source;
            this.startTime = startTime;
            this.state = state;
            this.target = target;
        }

        public String getDatabase() {
            return database;
        }

        public String getDocId() {
            return docId;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public String getId() {
            return id;
        }

        public Object getInfo() {
            return info;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }

        public String getNode() {
            return node;
        }

        public String getProxy() {
            return proxy;
        }

        public String getSource() {
            return source;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getState() {
            return state;
        }

        public String getTarget() {
            return target;
        }

        private String database;
        @SerializedName("doc_id")
        private String docId;
        @SerializedName("error_count")
        private long errorCount;
        private String id;
        private Object info;
        @SerializedName("last_updated")
        private String lastUpdated;
        private String node;
        private String proxy;
        private String source;
        @SerializedName("start_time")
        private String startTime;
        private String state;
        private String target;
    }

}
