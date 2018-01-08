/*
 * Copyright © 2017 IBM Corp. All rights reserved.
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

package com.cloudant.client.api.query;

import com.cloudant.client.internal.query.NamedField;
import com.google.gson.annotations.SerializedName;

import java.util.Locale;

public class Sort extends NamedField {

    public enum Order {
        /**
         * ascending
         */
        @SerializedName("asc")
        ASC,
        /**
         * descending
         */
        @SerializedName("desc")
        DESC;

        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ENGLISH);
        }
    }

    private Order order = null;

    protected Sort(String name, Order order) {
        super(name);
        this.order = order;
    }

    public static Sort asc(String fieldName) {
        return new Sort(fieldName, Order.ASC);
    }

    public static Sort desc(String fieldName) {
        return new Sort(fieldName, Order.DESC);
    }

    /**
     * @return the sort order
     */
    public Order getOrder() {
        return this.order;
    }

    @Override
    public String toString() {
        return getName() + " : " + order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        Sort sort = (Sort) o;

        return order == sort.order;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (order != null ? order.hashCode() : 0);
        return result;
    }
}
