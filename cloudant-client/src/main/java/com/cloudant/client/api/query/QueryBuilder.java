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

import com.cloudant.client.internal.query.Helpers;

import java.util.LinkedList;

public class QueryBuilder {

    OperationOrExpression selector;
    String[] fields;
    String[][] sort;
    Long limit;
    Long skip;

    public QueryBuilder(OperationOrExpression selector) {
        this.selector = selector;
    }

    public QueryBuilder fields(String... fields) {
        this.fields = fields;
        return this;
    }

    // TODO this is likely to take an array of FieldSorts or similar when merged with Rich's code
    public QueryBuilder sort(String[]... sort) {
        this.sort = sort;
        return this;
    }

    public QueryBuilder limit(long limit) {
        this.limit = limit;
        return this;
    }

    public QueryBuilder skip(long skip) {
        this.skip = skip;
        return this;
    }

    public String build() {
        String selectorString = selector.toString();
        String fieldsString = this.fields == null ? null : Helpers.quote(this.fields);
        String sortString = this.sort == null ? null : quoteSort(this.sort);
        String limitString = this.limit == null ? null : Helpers.quote(this.limit);
        String skipString = this.skip == null ? null : Helpers.quote(this.skip);
        StringBuilder builder = new StringBuilder();
        // build up components...
        // selector
        builder.append(String.format("\"selector\": {%s}", selectorString));
        // fields
        if (fieldsString != null) {
            builder.append(String.format(", \"fields\": %s", fieldsString));
        }
        // sort
        if (sortString != null) {
            builder.append(String.format(", \"sort\": %s", sortString));
        }
        // limit
        if (limitString != null) {
            builder.append(String.format(", \"limit\": %s", limitString));
        }
        // skip
        if (skipString != null) {
            builder.append(String.format(", \"skip\": %s", skipString));
        }
        return String.format("{%s}", builder.toString());
    }

    // sorts are a bit more awkward and need a helper...
    private String quoteSort(String[][] sort) {
        LinkedList<String> sorts = new LinkedList<String>();
        for (String[] pair : this.sort) {
            sorts.add(String.format("{%s}", Helpers.quoteNoSquare(pair)));
        }
        return sorts.toString();
    }
}
