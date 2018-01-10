/*
 * Copyright © 2018 IBM Corp. All rights reserved.
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

/**
 * <p>
 * This package provides access to the
 * <a
 * href="https://console.bluemix.net/docs/services/Cloudant/api/cloudant_query.html"
 * target="_blank">query API</a>.
 * </p>
 * <h1>Overview</h1>
 * <p>
 * This package contains classes which make it easier to interact with the query API in the
 * following ways:
 * </p>
 * <ul>
 *     <li>
 *         {@link com.cloudant.client.api.query.QueryBuilder A class for building complex queries}
 *         which makes the construction of the query JSON easier.
 *     </li>
 *     <li>
 *         Classes for building {@link com.cloudant.client.api.query.Selector complex selectors},
 *         used in the construction of queries (above), and in the construction of partial indexes,
 *         which makes the construction of the selector JSON easier.
 *     </li>
 *     <li>
 *         Classes for reading the {@link com.cloudant.client.api.query.QueryResult results of
 *         queries}.
 *     </li>
 * </ul>
 * <p>
 * For a worked example showing the construction of queries, see
 * {@link com.cloudant.client.api.Database#query(java.lang.String, java.lang.Class)}.
 * </p>
 * <p>
 * For worked examples showing the construction of partial indexes, see
 * {@link com.cloudant.client.api.Database#createIndex(java.lang.String)}.
 * </p>
 */
package com.cloudant.client.api.query;
