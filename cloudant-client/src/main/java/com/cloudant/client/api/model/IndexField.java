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

/**
 * Encapsulates a Cloudant IndexField definition
 *
 * @author Mario Briggs
 * @since 0.0.1
 */
public class IndexField {

    /**
     * Ascending or descending sort order
     */
    public enum SortOrder {
        /**
         * ascending
         */
        asc,
        /**
         * descending
         */
        desc
    }

    private String name;
    private SortOrder order;


    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the order
     */
    public SortOrder getOrder() {
        return order;
    }


    /**
     * Encapsulates a Cloudant Sort Syntax for a json field. Used to specify
     * an element of the 'index.fields' array (POST db/_index) and 'sort' array (db/_find) @see <a
     * href = "http://docs.cloudant.com/api/cloudant-query.html#cloudant-query-sort-syntax"> sort
     * Syntax</a>
     *
     * @param name  can be any field (dotted notation is available for sub-document fields)
     * @param order can be "asc" or "desc"
     */
    public IndexField(String name, SortOrder order) {
        this.name = name;
        this.order = order;
    }

    public String toString() {
        return name + " : " + order;
    }

}
