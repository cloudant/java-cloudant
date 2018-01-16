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

import static com.cloudant.client.internal.query.Helpers.quoteCurly;

// operation - used to combine an operator such as "$and" and a series of operations or expressions
// on the rhs

/**
 * <p>
 * An {@code Operation} allows {@code Expressions} or other {@code Operations} to be combined using
 * logical operators or negated.
 * </p>
 * <p>
 * These are also known as <a
 * href="https://console.bluemix.net/docs/services/Cloudant/api/cloudant_query.html#combination-operators"
 * target="_blank">Combination Operators</a>.
 * </p>
 *
 * @see Expression
 */
public class Operation implements Selector {

    private final String op;
    private final Selector[] rhs;

    private Operation(String op, Selector... rhs) {
        this.op = op;
        this.rhs = rhs;
    }

    /**
     * Combine two or more selectors using the "and" operator
     * @param rhs varargs list of {@code Selector}s
     * @return Operation: $and rhs1 rhs2...
     */
    public static Operation and(Selector... rhs) {
        return new Operation("$and", rhs);
    }

    /**
     * Combine two or more selectors using the "or" operator
     * @param rhs varargs list of {@code Selector}s
     * @return Operation: $or rhs1 rhs2...
     */
    public static Operation or(Selector... rhs) {
        return new Operation("$or", rhs);
    }

    /**
     * Negate the logic of the selector
     * @param rhs a {@code Selector}
     * @return Operation: $not rhs
     */
    public static Operation not(Selector rhs) {
        return new Operation("$not", rhs);
    }

    /**
     * Combine two or more selectors using the "nor" operator
     * @param rhs varargs list of {@code Selector}s
     * @return Operation: $nor rhs1 rhs2...
     */
    public static Operation nor(Selector... rhs) {
        return new Operation("$nor", rhs);
    }

    @Override
    public String toString() {
        // op rhs format ($and etc)
        return String.format("\"%s\": %s", this.op, quoteCurly(this.rhs));
    }

}
