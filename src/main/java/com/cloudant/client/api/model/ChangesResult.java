package com.cloudant.client.api.model;

import com.google.gson.JsonObject;

import com.cloudant.client.org.lightcouch.Changes;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents Changes feed result of type <i>normal</i>.
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
     * Represent a row in Changes result.
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
         * Represent a Change rev.
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
