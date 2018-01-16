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

// predicate expression, such as "$eq" 5
// currently only used by $elemMatch

/**
 * {@code PredicateExpression}s are the same as ordinary {@code Expression}s, but instead of
 * taking a field name as one of their arguments, they are combined with a {@code
 * PredicatedOperation}.
 *
 * @see Expression
 * @see PredicatedOperation
 */
public class PredicateExpression {

    private final String op;
    private final Object[] rhs;
    private boolean single;

    private PredicateExpression(String op, Object... rhs) {
        this.op = op;
        this.rhs = rhs;
    }

    /**
     * The field is less than the argument
     * @param rhs The argument for the comparison
     * @return PredicateExpression: $lt rhs
     */
    public static PredicateExpression lt(Object rhs) {
        return new PredicateExpression("$lt", rhs);
    }

    /**
     * The field is less than or equal to the argument
     * @param rhs The argument for the comparison
     * @return PredicateExpression: $lte rhs
     */
    public static PredicateExpression lte(Object rhs) {
        return new PredicateExpression("$lte", rhs);
    }

    /**
     * The field is equal to the argument
     * @param rhs The argument for the comparison
     * @return PredicateExpression: $eq rhs
     */
    public static PredicateExpression eq(Object rhs) {
        return new PredicateExpression("$eq", rhs);
    }

    /**
     * The field is not equal to the argument
     * @param rhs The argument for the comparison
     * @return PredicateExpression: $ne rhs
     */
    public static PredicateExpression ne(Object rhs) {
        return new PredicateExpression("$ne", rhs);
    }


    /**
     * The field is greater than or equal to the argument
     * @param rhs The argument for the comparison
     * @return PredicateExpression: $gte rhs
     */
    public static PredicateExpression gte(Object rhs) {
        return new PredicateExpression("$gte", rhs);
    }

     /**
     * The field is greater than the argument
     * @param rhs The argument for the comparison
     * @return PredicateExpression: $gt rhs
     */
    public static PredicateExpression gt(Object rhs) {
        return new PredicateExpression("$gt", rhs);
    }

    /**
     * The field exists or not, regardless of its value
     * @param rhs The argument (true or false)
     * @return PredicateExpression: $exists rhs
     */
    public static PredicateExpression exists(boolean rhs) {
        return new PredicateExpression("$exists", rhs);
    }

    /**
     * The field has a given type
     * @param rhs The type
     * @return PredicateExpression: $type rhs
     */
    public static PredicateExpression type(Type rhs) {
        return new PredicateExpression("$type", rhs.toString());
    }

    /**
     * The document field must exist in the list provided
     * @param rhs The argument - one or more values
     * @return PredicateExpression: $in rhs
     */
    public static PredicateExpression in(Object... rhs) {
        PredicateExpression ex = new PredicateExpression("$in", rhs);
        if (rhs.length == 1) {
            ex.single = true;
        }
        return ex;
    }

    /**
     * The document field must not exist in the list provided
     * @param rhs The argument - one or more values
     * @return PredicateExpression: $nin rhs
     */
    public static PredicateExpression nin(Object... rhs) {
        PredicateExpression ex = new PredicateExpression("$nin", rhs);
        if (rhs.length == 1) {
            ex.single = true;
        }
        return ex;
    }

    /**
     * Special condition to match the length of an array field in a document. Non-array fields
     * cannot match this condition
     * @param rhs The length of the array
     * @return PredicateExpression: $size rhs
     */
    public static PredicateExpression size(Long rhs) {
        return new PredicateExpression("$size", rhs);
    }

    /**
     * Divisor and Remainder are both positive or negative integers. Non-integer values result in a
     * 404 status. Matches documents where the expression (field % Divisor == Remainder) is true,
     * and only when the document field is an integer
     * @param divisor The divisor argument of the mod operation
     * @param remainder The remainder argument of the mod operation
     * @return PredicateExpression: $mod divisor remainder
     */
    public static PredicateExpression mod(Long divisor, Long remainder) {
        return new PredicateExpression("$mod", divisor, remainder);
    }

    /**
     * A regular expression pattern to match against the document field. Matches only when the field
     * is a string value and matches the supplied regular expression
     * @param rhs The regular expression
     * @return PredicateExpression: $regex rhs
     */
    public static PredicateExpression regex(String rhs) {
        return new PredicateExpression("$regex", rhs);
    }

    /**
     * Matches an array value if it contains all the elements of the argument array
     * @param rhs The arguments
     * @return PredicateExpression: $all rhs
     */
    public static PredicateExpression all(Object... rhs) {
        PredicateExpression ex = new PredicateExpression( "$all", rhs);
        if (rhs.length == 1) {
            ex.single = true;
        }
        return ex;
    }


    @Override
    public String toString() {
        // op rhs format
        return String.format("\"%s\": %s", this.op, quote(this.rhs, this.single));
    }

}
