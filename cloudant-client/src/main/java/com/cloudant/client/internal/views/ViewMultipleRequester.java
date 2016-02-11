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

import com.cloudant.client.api.views.ViewMultipleRequest;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewMultipleRequester<K, V> implements ViewMultipleRequest<K, V> {

    private final List<ViewQueryParameters<K, V>> requestParameters = new
            ArrayList<ViewQueryParameters<K, V>>();

    public List<ViewResponse<K, V>> getViewResponses() throws IOException {
        //build the queries array of data to POST
        JsonArray queries = new JsonArray();
        ViewQueryParameters<K, V> viewQueryParameters = null;
        for (ViewQueryParameters<K, V> params : requestParameters) {
            if (viewQueryParameters == null) {
                viewQueryParameters = params;
            }
            queries.add(params.asJson());
        }
        JsonObject queryJson = new JsonObject();
        queryJson.add("queries", queries);
        //construct and execute the POST request
        HttpConnection post = Http.POST(viewQueryParameters.getViewURIBuilder().build(),
                "application/json");
        post.setRequestBody(queryJson.toString());
        JsonObject jsonResponse = ViewRequester.executeRequestWithResponseAsJson
                (viewQueryParameters, post);
        //loop the returned array creating the ViewResponse objects
        List<ViewResponse<K, V>> responses = new ArrayList<ViewResponse<K, V>>();
        JsonArray jsonResponses = jsonResponse.getAsJsonArray("results");
        if (jsonResponses != null) {
            int index = 0;
            for (ViewQueryParameters<K, V> params : requestParameters) {
                JsonObject response = jsonResponses.get(index).getAsJsonObject();
                responses.add(new ViewResponseImpl<K, V>(params, response));
                index++;
            }
            return responses;
        } else {
            return Collections.emptyList();
        }
    }

    public void add(ViewQueryParameters<K, V> viewQueryParameters) {
        requestParameters.add(viewQueryParameters);
    }
}
