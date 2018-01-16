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

/**
 * <P>
 * Interface that identifies all {@link Operation}s and {@link Expression}s that can form
 * the basis of a selector.
 * </P>
 * <P>
 * Obtain a selector from an {@link Operation} or {@link Expression}. For example:
 * </P>
 * <pre>
 * {@code
 * // Selector for the field "year" equal to 2017, JSON form "year": {"$eq": 2017}
 * Selector expressionSelector = eq("year", 2017);
 *
 * // Selector for the field "name" equal to "example" and the field "year" less than 2017
 * // JSON form "$and": [{"name": {"$eq": "example"}}, {"year": {"$lt": 2017}}]
 * Selector operationSelector = and(eq("name", "example"), lt("year", 2017));
 * }
 * </pre>
 * <P>
 * For convenience and code brevity use the static imports for the operations and expressions you
 * need. For example:
 * </P>
 * <pre>
 * {@code
 * import static com.cloudant.client.api.query.Expression.eq;
 * import static com.cloudant.client.api.query.Expression.lt;
 * import static com.cloudant.client.api.query.Operation.and;
 * }
 * </pre>
 */
// Signifies the RHS of an operation which can be another operation or an expression.
public interface Selector {

}
