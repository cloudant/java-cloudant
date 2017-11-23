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

import static com.cloudant.client.internal.query.Helpers.quoteCurlyNoSquare;

// predicated operation - an operation which takes one or more predicates such as "$eq" 5 as its rhs
public class PredicatedOperation implements Selector {

    private final String lhs;
    private final String op;
    private final PredicateExpression[] rhs;

    private PredicatedOperation(String lhs, String op, PredicateExpression... rhs) {
        this.lhs = lhs;
        this.op = op;
        this.rhs = rhs;
    }

    public static PredicatedOperation elemMatch(String lhs, PredicateExpression... rhs) {
        // TODO each expression key must be unique
        return new PredicatedOperation(lhs, "$elemMatch", rhs);
    }

    @Override
    public String toString() {
        // op rhs format ($and etc)
        return String.format("\"%s\": {\"%s\": %s}", this.lhs, this.op, quoteCurlyNoSquare(this.rhs));
    }

}
