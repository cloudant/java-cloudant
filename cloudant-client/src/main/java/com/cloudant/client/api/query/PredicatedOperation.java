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

import static com.cloudant.client.internal.query.Helpers.quoteCurlyNoSquare;

// predicated operation - an operation which takes one or more predicates such as "$eq" 5 as its rhs

/**
 * <p>
 * A {@code PredicatedOperation} allows multiple predicates (such as "equals 5") to be combined.
 * Currently the only {@code PredicatedOperation} is {@code $elemMatch}.
 * </p>
 * <p>
 * These are also known as <a
 * href="https://console.bluemix.net/docs/services/Cloudant/api/cloudant_query.html#combination-operators"
 * target="_blank">Combination Operators</a>.
 * </p>
 *
 * @see PredicateExpression
 */
public class PredicatedOperation implements Selector {

    private final String lhs;
    private final String op;
    private final PredicateExpression[] rhs;

    private PredicatedOperation(String lhs, String op, PredicateExpression... rhs) {
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }

    /**
     * Matches and returns all documents that contain an array field with at least one element that
     * matches all the specified query criteria
     * @param lhs The field to check
     * @param rhs The query criteria
     * @return PredicatedOperation: lhs $elemMatch $rhs
     */
    public static PredicatedOperation elemMatch(String lhs, PredicateExpression... rhs) {
        // note each expression key must be unique or the JSON is malformed
        return new PredicatedOperation(lhs, "$elemMatch", rhs);
    }

    @Override
    public String toString() {
        // op rhs format ($and etc)
        return String.format("\"%s\": {\"%s\": %s}", this.lhs, this.op, quoteCurlyNoSquare(this.rhs));
    }

}
