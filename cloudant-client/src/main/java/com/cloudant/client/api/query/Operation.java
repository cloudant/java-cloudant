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

import static com.cloudant.client.internal.query.Helpers.quoteCurly;

// operation - used to combine an operator such as "$and" and a series of operations or expressions
// on the rhs
public class Operation implements OperationOrExpression {

    private final String op;
    private final OperationOrExpression[] rhs;

    private Operation(String op, OperationOrExpression... rhs) {
        this.op = op;
        this.rhs = rhs;
    }

    public static Operation and(OperationOrExpression... rhs) {
        return new Operation("$and", rhs);
    }

    public static Operation or(OperationOrExpression... rhs) {
        return new Operation("$or", rhs);
    }

    public static Operation not(OperationOrExpression rhs) {
        return new Operation("$not", rhs);
    }

    public static Operation nor(OperationOrExpression... rhs) {
        return new Operation("$nor", rhs);
    }

    @Override
    public String toString() {
        // op rhs format ($and etc)
        return String.format("\"%s\": %s", this.op, quoteCurly(this.rhs));
    }

}
