/*
 * Copyright © 2018 IBM Corp. All rights reserved.
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

package com.cloudant.client.api.scheduler;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SchedulerDocsResponse {

    private List<Doc> docs;
    private long offset;
    @SerializedName("total_rows")
    private long totalRows;

    public List<Doc> getDocs() {
        return docs;
    }

    public long getOffset() {
        return offset;
    }

    public long getTotalRows() {
        return totalRows;
    }

    public static class Doc {

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

    }

}
