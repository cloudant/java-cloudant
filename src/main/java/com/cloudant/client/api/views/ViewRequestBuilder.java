/*
 * Copyright (c) 2015 IBM Corp. All rights reserved.
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

package com.cloudant.client.api.views;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.internal.views.MultipleRequestBuilderImpl;
import com.cloudant.client.internal.views.PaginatedRequestBuilderImpl;
import com.cloudant.client.internal.views.UnpaginatedRequestBuilderImpl;
import com.cloudant.client.internal.views.ViewQueryParameters;

/**
 * Provides methods for obtaining builders for view requests.
 * <P>
 * The database, design document and view are specified when this class is instantiated.
 * </P>
 * <P>
 * A view request builder instance should be obtained from a call to
 * {@link Database#getViewRequestBuilder(String, String)}.
 * </P>
 */
public class ViewRequestBuilder {

    private final CloudantClient client;
    private final Database database;
    private final String designDoc;
    private final String viewName;

    /**
     * <P>
     * Create a new ViewRequestBuilder for the specified database, design document and view name.
     * </P>
     * <P>
     * Rather than calling this constructor directly a view request builder instance should be
     * obtained from a call to {@link Database#getViewRequestBuilder(String, String)}.
     * </P>
     *
     * @param client    the cloudant client
     * @param database  the database
     * @param designDoc the design doc containing the view (optionally prefixed with "_design/")
     * @param viewName  the view to build a query request for
     */
    public ViewRequestBuilder(CloudantClient client, Database database, String designDoc, String
            viewName) {
        this.client = client;
        this.database = database;
        this.designDoc = designDoc;
        this.viewName = viewName;
    }

    /**
     * Create a new builder for an unpaginated request on the view.
     * <P>
     * Note that the maximum number of rows returnable in an unpaginated request is
     * {@link Integer#MAX_VALUE}. If you need to return more results than this then use
     * {@link ViewRequestBuilder#newPaginatedRequest(Key.Type, Class)} to spread the rows across
     * multiple pages.
     * </P>
     *
     * @param keyType   {@link com.cloudant.client.api.views.Key.Type} of the key emitted by the
     *                  view
     * @param valueType class of the type of value emitted by the view
     * @param <K>       type of key emitted by the view
     * @param <V>       type of value emitted by the view
     * @return a new {@link UnpaginatedRequestBuilder} for the database view specified by this
     * ViewRequestBuilder
     */
    public <K, V> UnpaginatedRequestBuilder<K, V> newRequest(Key.Type<K> keyType, Class<V>
            valueType) {
        return new UnpaginatedRequestBuilderImpl<K, V>(newViewRequestParameters(keyType.getType(),
                valueType));
    }

    /**
     * Create a new builder for a paginated request on the view.
     * <P>
     * Defaults to 20 rows per page if not specified by
     * {@link PaginatedRequestBuilder#rowsPerPage(int)}
     * </P>
     *
     * @param keyType   {@link com.cloudant.client.api.views.Key.Type} of the key emitted by the
     *                  view
     * @param valueType class of the type of value emitted by the view
     * @param <K>       type of key emitted by the view
     * @param <V>       type of value emitted by the view
     * @return a new {@link PaginatedRequestBuilder} for the database view specified by this
     * ViewRequestBuilder
     */
    public <K, V> PaginatedRequestBuilder<K, V> newPaginatedRequest(Key.Type<K> keyType,
                                                                    Class<V> valueType) {
        return new PaginatedRequestBuilderImpl<K, V>(newViewRequestParameters(keyType.getType(),
                valueType));
    }

    /**
     * Create a new builder for multiple unpaginated requests on the view.
     *
     * @param keyType   {@link com.cloudant.client.api.views.Key.Type} of the key emitted by the
     *                  view
     * @param valueType class of the type of value emitted by the view
     * @param <K>       type of key emitted by the view
     * @param <V>       type of value emitted by the view
     * @return a new {@link MultipleRequestBuilder} for the database view specified by this
     * ViewRequestBuilder
     */
    public <K, V> MultipleRequestBuilder<K, V> newMultipleRequest(Key.Type<K> keyType,
                                                                  Class<V> valueType) {
        return new MultipleRequestBuilderImpl<K, V>(newViewRequestParameters(keyType.getType(),
                valueType));
    }

    private <K, V> ViewQueryParameters<K, V> newViewRequestParameters(Class<K> keyType, Class<V>
            valueType) {
        return new ViewQueryParameters<K, V>(client, database, designDoc, viewName, keyType,
                valueType);
    }

}
