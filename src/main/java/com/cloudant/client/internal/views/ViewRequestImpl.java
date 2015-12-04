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

import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewResponse;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;

class ViewRequestImpl<K, V> implements ViewRequest<K, V> {

    private final ViewQueryParameters<K, V> viewQueryParameters;

    ViewRequestImpl(ViewQueryParameters<K, V> viewQueryParameters) {
        this.viewQueryParameters = viewQueryParameters;
    }

    @Override
    public ViewResponse<K, V> getResponse() throws IOException {
        JsonObject response = ViewRequester.getResponseAsJson(viewQueryParameters);
        return new ViewResponseImpl<K, V>(viewQueryParameters, response);
    }

    @Override
    public ViewResponse<K, V> getResponse(String paginationToken) throws IOException {
        if (paginationToken == null) {
            return getResponse();
        } else {
            //decode the PageMetadata
            PageMetadata<K, V> pageMetadata = PaginationToken.mergeTokenAndQueryParameters
                    (paginationToken, viewQueryParameters);
            return new ViewResponseImpl<K, V>(viewQueryParameters, ViewRequester
                    .getResponseAsJson(pageMetadata.pageRequestParameters), pageMetadata);
        }
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

}
