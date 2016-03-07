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

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.org.lightcouch.internal.CouchDbUtil;
import com.cloudant.http.HttpConnection;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;

class ViewRequester {

    static JsonObject getResponseAsJson(ViewQueryParameters parameters) throws IOException {
        return executeRequestWithResponseAsJson(parameters, parameters.asGetRequest());
    }

    static JsonObject executeRequestWithResponseAsJson(ViewQueryParameters parameters,
                                                       HttpConnection request) throws IOException {
        CloudantClient client = parameters.getClient();
        InputStream response = client.executeRequest(request).responseAsInputStream();
        return CouchDbUtil.getResponse(response, JsonObject.class, client.getGson());
    }
}
