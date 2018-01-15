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

    @Override
    public String toString() {
        // op rhs format
        return String.format("\"%s\": %s", this.op, quote(this.rhs));
    }

}
