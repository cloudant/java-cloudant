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

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates search result entries
 *
 * @param <T> Object type T, an instance into which the rows[].doc/group[].rows[].doc
 *            attribute of the Search result response should be deserialized into.
 *            Same goes for the rows[].fields/group[].rows[].fields attribute
 * @author Mario Briggs
 */
public class SearchResult<T> {

    @SerializedName("total_rows")
    private long totalRows;
    private String bookmark;
    private List<SearchResultRow> rows = new ArrayList<SearchResultRow>();
    private List<SearchResultGroup> groups = new ArrayList<SearchResultGroup>();
    private Map<String, Map<String, Long>> counts = new HashMap<String, Map<String, Long>>();
    private Map<String, Map<String, Long>> ranges;


    /**
     * @return the totalRows
     */
    public long getTotalRows() {
        return totalRows;
    }

    /**
     * @param totalRows the totalRows to set
     */
    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
    }

    /**
     * @return the bookmark
     */
    public String getBookmark() {
        return bookmark;
    }

    /**
     * @param bookmark the bookmark to set
     */
    public void setBookmark(String bookmark) {
        this.bookmark = bookmark;
    }


    /**
     * @return the counts
     */
    public Map<String, Map<String, Long>> getCounts() {
        return counts;
    }


    /**
     * @param counts the counts to set
     */
    public void setCounts(Map<String, Map<String, Long>> counts) {
        this.counts = counts;
    }

    /**
     * @param ranges the ranges to set
     */
    public void setRanges(Map<String, Map<String, Long>> ranges) {
        this.ranges = ranges;
    }

    /**
     * @return the ranges
     */
    public Map<String, Map<String, Long>> getRanges() {
        return ranges;
    }


    /**
     * @return the rows
     */
    public List<SearchResultRow> getRows() {
        return rows;
    }

    /**
     * @param rows the rows to set
     */
    public void setRows(List<SearchResultRow> rows) {
        this.rows = rows;
    }


    /**
     * @return the groups
     */
    public List<SearchResultGroup> getGroups() {
        return groups;
    }


    /**
     * Encapsulates a SearchResult row.
     */
    public class SearchResultRow {
        private String id;
        private Object[] order;
        private T fields;
        private T doc;

        /**
         * @param id the id to set
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * @param order the order to set
         */
        public void setOrder(Object[] order) {
            this.order = Arrays.copyOf(order, order.length);
        }

        /**
         * @param fields the fields to set
         */
        public void setFields(T fields) {
            this.fields = fields;
        }

        /**
         * @param doc the doc to set
         */
        public void setDoc(T doc) {
            this.doc = doc;
        }

        /**
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * @return the order (each element can be a String/Number)
         */
        public Object[] getOrder() {
            return (order != null) ? Arrays.copyOf(order, order.length) : null;
        }

        /**
         * @return the fields
         */
        public T getFields() {
            return fields;
        }

        /**
         * @return the doc
         */
        public T getDoc() {
            return doc;
        }


    }

    /**
     * Encapsulates a SearchResult group.
     */
    public class SearchResultGroup {
        private String by;
        @SerializedName("total_rows")
        private Long totalRows;
        private List<SearchResultRow> rows = new ArrayList<SearchResultRow>();


        /**
         * @return the by
         */
        public String getBy() {
            return by;
        }

        /**
         * @param by the by to set
         */
        public void setBy(String by) {
            this.by = by;
        }

        /**
         * @return the totalRows
         */
        public Long getTotalRows() {
            return totalRows;
        }

        /**
         * @param totalRows the totalRows to set
         */
        public void setTotalRows(Long totalRows) {
            this.totalRows = totalRows;
        }

        /**
         * @param rows the rows to set
         */
        public void setRows(List<SearchResultRow> rows) {
            this.rows = rows;
        }

        /**
         * @return the rows
         */
        public List<SearchResultRow> getRows() {
            return rows;
        }


    }


}
