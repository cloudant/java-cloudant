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

import com.google.gson.JsonObject;

import com.cloudant.client.org.lightcouch.Changes;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a Changes feed result of type <i>normal</i>.
 *
 * @author Ganesh K Choudhary
 * @see Changes
 * @since 0.0.1
 */
public class ChangesResult {

    private com.cloudant.client.org.lightcouch.ChangesResult changesResult;

    public ChangesResult() {
        this.changesResult = new com.cloudant.client.org.lightcouch.ChangesResult();
    }

    public ChangesResult(com.cloudant.client.org.lightcouch.ChangesResult changesResult) {
        this.changesResult = changesResult;
    }

    public List<Row> getResults() {
        List<com.cloudant.client.org.lightcouch.ChangesResult.Row> lightCouchResults = changesResult.getResults();
        List<Row> rows = new ArrayList<Row>();
        for (com.cloudant.client.org.lightcouch.ChangesResult.Row couchRow : lightCouchResults) {
            Row row = new Row(couchRow);
            rows.add(row);
        }
        return rows;
    }

    public String getLastSeq() {
        return changesResult.getLastSeq();
    }


    /**
     * Encapsulates a Changes feed result row.
     */
    public static class Row {
        private com.cloudant.client.org.lightcouch.ChangesResult.Row row;

        public Row(com.cloudant.client.org.lightcouch.ChangesResult.Row row) {
            this.row = row;
        }


        public String getSeq() {
            return row.getSeq();
        }


        public String getId() {
            return row.getId();
        }


        public List<Rev> getChanges() {
            List<com.cloudant.client.org.lightcouch.ChangesResult.Row.Rev> lightCouchChanges = row.getChanges();
            List<Rev> changes = new ArrayList<Rev>();
            for (com.cloudant.client.org.lightcouch.ChangesResult.Row.Rev rev : lightCouchChanges) {
                changes.add(new Rev(rev));
            }
            return changes;
        }

        public JsonObject getDoc() {
            return row.getDoc();
        }


        public boolean isDeleted() {
            return row.isDeleted();
        }

        /**
         * Encapsulates the revision of a change result row.
         */
        public static class Rev {
            private com.cloudant.client.org.lightcouch.ChangesResult.Row.Rev rev;

            public Rev(com.cloudant.client.org.lightcouch.ChangesResult.Row.Rev rev) {
                this.rev = rev;
            }

            public String getRev() {
                return rev.getRev();
            }
        }
    }

}
