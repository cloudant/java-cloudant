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

package com.cloudant.client.internal.views;

import com.cloudant.client.api.views.PaginatedRequestBuilder;
import com.cloudant.client.api.views.ViewRequest;

public class PaginatedRequestBuilderImpl<K, V> extends
        CommonViewRequestBuilder<K, V, PaginatedRequestBuilder<K, V>> implements
        PaginatedRequestBuilder<K, V> {

    public PaginatedRequestBuilderImpl(ViewQueryParameters<K, V> params) {
        super(params);
        //default to 20 rows per page
        viewQueryParameters.setRowsPerPage(20);
    }

    @Override
    public PaginatedRequestBuilder<K, V> returnThis() {
        return this;
    }

    @Override
    public ViewRequest<K, V> build() {
        validateQuery();
        return new ViewRequestImpl<K, V>(viewQueryParameters);
    }
}
