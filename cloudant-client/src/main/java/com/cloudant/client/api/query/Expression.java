/*
 * Copyright © 2017, 2018 IBM Corp. All rights reserved.
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

/**
 * {@code Expression}s are used to evaluate true/false criteria to select documents. For example,
 * an expression to select documents where the field "widget_count" is equal to 5 would be
 * constructed as follows
 *
 * <pre>
 * {@code
 * Expression e = Expression.eq("widget_count", 5);
 * }
 * </pre>
 */
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

    /**
     * The field is less than the argument
     * @param lhs The field to compare
     * @param rhs The argument for the comparison
     * @return Expression: lhs $lt rhs
     */
    public static Expression lt(String lhs, Object rhs) {
        return new Expression(lhs, "$lt", rhs);
    }

    /**
     * The field is less than or equal to the argument
     * @param lhs The field to compare
     * @param rhs The argument for the comparison
     * @return Expression: lhs $lte rhs
     */
    public static Expression lte(String lhs, Object rhs) {
        return new Expression(lhs, "$lte", rhs);
    }

    /**
     * The field is equal to the argument
     * @param lhs The field to compare
     * @param rhs The argument for the comparison
     * @return Expression: lhs $eq rhs
     */
    public static Expression eq(String lhs, Object rhs) {
        return new Expression(lhs, "$eq", rhs);
    }

    /**
     * The field is not equal to the argument
     * @param lhs The field to compare
     * @param rhs The argument for the comparison
     * @return Expression: lhs $ne rhs
     */
    public static Expression ne(String lhs, Object rhs) {
        return new Expression(lhs, "$ne", rhs);
    }

    /**
     * The field is greater than or equal to the argument
     * @param lhs The field to compare
     * @param rhs The argument for the comparison
     * @return Expression: lhs $gte rhs
     */
    public static Expression gte(String lhs, Object rhs) {
        return new Expression(lhs, "$gte", rhs);
    }

    /**
     * The field is greater than the argument
     * @param lhs The field to compare
     * @param rhs The argument for the comparison
     * @return Expression: lhs $gt rhs
     */
    public static Expression gt(String lhs, Object rhs) {
        return new Expression(lhs, "$gt", rhs);
    }

    /**
     * Check whether the field exists or not, regardless of its value
     * @param lhs The field to check
     * @param rhs The argument (true or false)
     * @return Expression: lhs $exists rhs
     */
    public static Expression exists(String lhs, boolean rhs) {
        return new Expression(lhs, "$exists", rhs);
    }

    /**
     * Check the document field's type
     * and object
     * @param lhs The field to check
     * @param rhs The type
     * @return Expression: lhs $type rhs
     */
    public static Expression type(String lhs, Type rhs) {
        return new Expression(lhs, "$type", rhs.toString());
    }

    /**
     * The document field must exist in the list provided
     * @param lhs The field to check
     * @param rhs The argument - one or more values
     * @return Expression: lhs $in rhs
     */
    public static Expression in(String lhs, Object... rhs) {
        Expression ex = new Expression(lhs, "$in", rhs);
        if (rhs.length == 1) {
            ex.single = true;
        }
        return ex;
    }

    /**
     * The document field must not exist in the list provided
     * @param lhs The field to check
     * @param rhs The argument - one or more values
     * @return Expression: lhs $nin rhs
     */
    public static Expression nin(String lhs, Object... rhs) {
        Expression ex = new Expression(lhs, "$nin", rhs);
        if (rhs.length == 1) {
            ex.single = true;
        }
        return ex;
    }

    /**
     * Special condition to match the length of an array field in a document. Non-array fields
     * cannot match this condition
     * @param lhs The field to check
     * @param rhs The length of the array
     * @return Expression: lhs $size rhs
     */
    public static Expression size(String lhs, Long rhs) {
        return new Expression(lhs, "$size", rhs);
    }

    /**
     * Divisor and Remainder are both positive or negative integers. Non-integer values result in a
     * 404 status. Matches documents where the expression (field % Divisor == Remainder) is true,
     * and only when the document field is an integer
     * @param lhs The field to check
     * @param divisor The divisor argument of the mod operation
     * @param remainder The remainder argument of the mod operation
     * @return Expression: lhs $mod divisor remainder
     */
    public static Expression mod(String lhs, Long divisor, Long remainder) {
        return new Expression(lhs, "$mod", divisor, remainder);
    }

    /**
     * A regular expression pattern to match against the document field. Matches only when the field
     * is a string value and matches the supplied regular expression
     * @param lhs The field to match
     * @param rhs The regular expression
     * @return Expression: lhs $regex rhs
     */
    public static Expression regex(String lhs, String rhs) {
        return new Expression(lhs, "$regex", rhs);
    }

    /**
     * Matches an array value if it contains all the elements of the argument array
     * @param lhs The field to match
     * @param rhs The arguments
     * @return Expression: lhs $all rhs
     */
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
