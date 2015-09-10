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

package com.cloudant.client.api.model;

import com.cloudant.client.org.lightcouch.Replication;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the result of a replication request, along with previous sessions history.
 *
 * @author Ganesh K Choudhary
 * @see Replication
 * @since 0.0.1
 */
public class ReplicationResult {
    private com.cloudant.client.org.lightcouch.ReplicationResult replicationResult;

    public ReplicationResult() {
        replicationResult = new com.cloudant.client.org.lightcouch.ReplicationResult();
    }

    public ReplicationResult(com.cloudant.client.org.lightcouch.ReplicationResult replicationResult) {
        this.replicationResult = replicationResult;
    }

    /**
     * @return
     */
    public boolean isOk() {
        return replicationResult.isOk();
    }

    /**
     * @return
     */
    public String getSessionId() {
        return replicationResult.getSessionId();
    }

    /**
     * @return
     */
    public String getSourceLastSeq() {
        return replicationResult.getSourceLastSeq();
    }

    /**
     * @return
     */
    public String getLocalId() {
        return replicationResult.getLocalId();
    }

    /**
     * @return
     */
    public List<ReplicationHistory> getHistories() {
        List<com.cloudant.client.org.lightcouch.ReplicationResult.ReplicationHistory> couchDbreplicationHistories =
                replicationResult.getHistories();
        List<ReplicationHistory> histories = new ArrayList<ReplicationHistory>();
        for (com.cloudant.client.org.lightcouch.ReplicationResult.ReplicationHistory couchDbReplicationHistory :
                couchDbreplicationHistories) {
            ReplicationHistory replicationHistory = new ReplicationHistory
                    (couchDbReplicationHistory);
            histories.add(replicationHistory);
        }
        return histories;

    }


    /**
     * Represents a replication session history.
     *
     * @author Ganesh K Choudhary
     */
    public static class ReplicationHistory {
        private com.cloudant.client.org.lightcouch.ReplicationResult.ReplicationHistory replicationHistory;

        public ReplicationHistory() {
            // default constructor
        }

        public ReplicationHistory(
                com.cloudant.client.org.lightcouch.ReplicationResult.ReplicationHistory replicationHistory) {
            this.replicationHistory = replicationHistory;
        }

        /**
         * @return
         */
        public String getSessionId() {
            return replicationHistory.getSessionId();
        }

        /**
         * @return
         */
        public String getStartTime() {
            return replicationHistory.getStartTime();
        }

        /**
         * @return
         */
        public String getEndTime() {
            return replicationHistory.getEndTime();
        }

        /**
         * @return
         */
        public String getStartLastSeq() {
            return replicationHistory.getStartLastSeq();
        }

        /**
         * @return
         */
        public String getEndLastSeq() {
            return replicationHistory.getEndLastSeq();
        }

        /**
         * @return
         */
        public String getRecordedSeq() {
            return replicationHistory.getRecordedSeq();
        }

        /**
         * @return
         */
        public long getMissingChecked() {
            return replicationHistory.getMissingChecked();
        }

        /**
         * @return
         */
        public long getMissingFound() {
            return replicationHistory.getMissingFound();
        }

        /**
         * @return
         */
        public long getDocsRead() {
            return replicationHistory.getDocsRead();
        }

        /**
         * @return
         */
        public long getDocsWritten() {
            return replicationHistory.getDocsWritten();
        }

        /**
         * @return
         */
        public long getDocWriteFailures() {
            return replicationHistory.getDocWriteFailures();
        }


    }

}
