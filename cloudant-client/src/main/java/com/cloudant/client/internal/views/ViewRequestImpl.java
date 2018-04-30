/*
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewResponse;

import java.io.IOException;
import java.util.List;

class ViewRequestImpl<K, V> implements ViewRequest<K, V> {

    protected final ViewQueryParameters<K, V> viewQueryParameters;

    ViewRequestImpl(ViewQueryParameters<K, V> viewQueryParameters) {
        this.viewQueryParameters = viewQueryParameters;
    }

    @Override
    public ViewResponse<K, V> getResponse() throws IOException {
        return getResponse(null);
    }

    @Override
    public ViewResponse<K, V> getResponse(String paginationToken) throws IOException {
        //decode the PageMetadata if present
        PageMetadata<K, V> pageMetadata = (paginationToken != null) ? PaginationToken
                .mergeTokenAndQueryParameters
                (paginationToken, viewQueryParameters) : null;
        return makeResponse(pageMetadata);
    }

    @Override
    public V getSingleValue() throws IOException {
        List<V> values = getResponse().getValues();
        if (values.size() > 0) {
            return values.get(0);
        } else {
            return null;
        }
    }

    protected ViewResponseImpl<K, V> makeResponse(PageMetadata<K, V> metadata) throws IOException {
        ViewQueryParameters<K, V> requestParameters = (metadata != null) ? metadata
                .pageRequestParameters : viewQueryParameters;
        return new ViewResponseImpl<K, V>(viewQueryParameters, ViewRequester.getResponseAsJson
                (requestParameters), metadata);
    }
}
