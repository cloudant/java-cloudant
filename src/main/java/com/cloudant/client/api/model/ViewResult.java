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

import com.cloudant.client.org.lightcouch.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a view result entries.
 *
 * @author Ganesh K Choudhary
 * @see View
 * @since 0.0.1
 */
public class ViewResult<K, V, T> {
    private com.cloudant.client.org.lightcouch.ViewResult<K, V, T> viewResult;

    public ViewResult() {
        viewResult = new com.cloudant.client.org.lightcouch.ViewResult<K, V, T>();
    }

    public ViewResult(com.cloudant.client.org.lightcouch.ViewResult<K, V, T> viewResult) {
        this.viewResult = viewResult;
    }

    /**
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
        return viewResult.equals(obj);
    }

    /**
     * @return
     */
    public long getTotalRows() {
        return viewResult.getTotalRows();
    }

    /**
     * @return
     */
    public long getUpdateSeq() {
        return viewResult.getUpdateSeq();
    }

    /**
     * Offset should not be used in clustered environments
     * @return
     */
    @Deprecated
    public int getOffset() {
        return viewResult.getOffset();
    }

    /**
     * @return
     */
    public List<Rows> getRows() {
        List<Rows> rows = new ArrayList<Rows>();
        List<com.cloudant.client.org.lightcouch.ViewResult<K, V, T>.Rows> couchDbRows = viewResult.getRows();
        for (int i = 0; i < couchDbRows.size(); i++) {
            com.cloudant.client.org.lightcouch.ViewResult<K, V, T>.Rows couchDbRow = couchDbRows.get(i);
            Rows row = new Rows(couchDbRow);
            rows.add(row);
        }
        return rows;
    }

    /**
     * @param totalRows
     */
    public void setTotalRows(long totalRows) {
        viewResult.setTotalRows(totalRows);
    }

    /**
     * @param updateSeq
     */
    public void setUpdateSeq(long updateSeq) {
        viewResult.setUpdateSeq(updateSeq);
    }

    /**
     * Offset should not be used in clustered environments
     * @param offset
     */
    @Deprecated
    public void setOffset(int offset) {
        viewResult.setOffset(offset);
    }

    /**
     * @param rows
     */
    public void setRows(List<Rows> rows) {
        List<com.cloudant.client.org.lightcouch.ViewResult<K, V, T>.Rows> rowsList = new ArrayList<com.cloudant.client.org.lightcouch.ViewResult<K, V, T>.Rows>();
        for (int i = 0; i < rows.size(); i++) {
            Rows row = rows.get(i);
            com.cloudant.client.org.lightcouch.ViewResult<K, V, T>.Rows lightcouchRows = row.getRows().getrows();
            rowsList.add(lightcouchRows);
        }
        viewResult.setRows(rowsList);
    }

    /**
     * @return
     */
    public String toString() {
        return viewResult.toString();
    }

    public class Rows {
        private com.cloudant.client.org.lightcouch.ViewResult<K, V, T>.Rows rows;


        public Rows() {
            rows = viewResult.new Rows();
        }

        Rows(com.cloudant.client.org.lightcouch.ViewResult<K, V, T>.Rows rows) {
            this.rows = rows;
        }

        /**
         * @return the rows
         */
        public ViewResult<K, V, T>.Rows getRows() {
            return this;
        }

        /**
         * @return
         */
        public String getId() {
            return rows.getId();
        }

        /**
         * @return
         */
        public K getKey() {
            return rows.getKey();
        }

        /**
         * @return
         */
        public V getValue() {
            return rows.getValue();
        }

        /**
         * @return
         */
        public T getDoc() {
            return rows.getDoc();
        }


        /**
         * @param id
         */
        public void setId(String id) {
            rows.setId(id);
        }

        /**
         * @param key
         */
        public void setKey(K key) {
            rows.setKey(key);
        }

        /**
         * @param value
         */
        public void setValue(V value) {
            rows.setValue(value);
        }

        /**
         * @param doc
         */
        public void setDoc(T doc) {
            rows.setDoc(doc);
        }

        /**
         * @return
         */
        public String toString() {
            return rows.toString();
        }

        com.cloudant.client.org.lightcouch.ViewResult<K, V, T>.Rows getrows() {
            return rows;

        }

    }

}
