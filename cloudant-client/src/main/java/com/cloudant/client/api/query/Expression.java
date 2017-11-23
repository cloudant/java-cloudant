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

import static com.cloudant.client.internal.query.Helpers.quote;

// expression, such as "widget_count" "$eq" 5
public class Expression implements Selector {

    private final String lhs;
    private final String op;
    private final Object[] rhs;
    private boolean single;

    private Expression(String lhs, String op, Object... rhs) {
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }

    public static Expression lt(String lhs, Object rhs) {
        return new Expression(lhs, "$lt", rhs);
    }

    public static Expression lte(String lhs, Object rhs) {
        return new Expression(lhs, "$lte", rhs);
    }

    public static Expression eq(String lhs, Object rhs) {
        return new Expression(lhs, "$eq", rhs);
    }

    public static Expression ne(String lhs, Object rhs) {
        return new Expression(lhs, "$ne", rhs);
    }

    public static Expression gte(String lhs, Object rhs) {
        return new Expression(lhs, "$gte", rhs);
    }

    public static Expression gt(String lhs, Object rhs) {
        return new Expression(lhs, "$gt", rhs);
    }

    public static Expression exists(String lhs, boolean rhs) {
        return new Expression(lhs, "$exists", rhs);
    }

    public static Expression type(String lhs, Type rhs) {
        return new Expression(lhs, "$type", rhs.toString());
    }

    public static Expression in(String lhs, Object... rhs) {
        Expression ex = new Expression(lhs, "$in", rhs);
        if (rhs.length == 1) {
            ex.single = true;
        }
        return ex;
    }

    public static Expression nin(String lhs, Object... rhs) {
        Expression ex = new Expression(lhs, "$nin", rhs);
        if (rhs.length == 1) {
            ex.single = true;
        }
        return ex;
    }

    public static Expression size(String lhs, Long rhs) {
        return new Expression(lhs, "$size", rhs);
    }

    public static Expression mod(String lhs, Long rhs) {
        return new Expression(lhs, "$mod", rhs);
    }

    public static Expression regex(String lhs, String rhs) {
        return new Expression(lhs, "$regex", rhs);
    }

    public static Expression all(String lhs, Object... rhs) {
        Expression ex = new Expression(lhs, "$all", rhs);
        if (rhs.length == 1) {
            ex.single = true;
        }
        return ex;
    }

    @Override
    public String toString() {
        // lhs op rhs format
        return String.format("\"%s\": {\"%s\": %s}", this.lhs, this.op, quote(this.rhs, this.single));
    }

}
