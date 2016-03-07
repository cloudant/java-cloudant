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

import com.cloudant.client.api.model.Document;
import com.cloudant.client.api.views.ViewResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class RowImpl<K, V> implements ViewResponse.Row<K, V> {

    private final ViewQueryParameters<K, V> parameters;
    private final Gson gson;
    private final JsonObject row;

    RowImpl(ViewQueryParameters<K, V> parameters, JsonElement row) {
        this.parameters = parameters;
        this.gson = parameters.getClient().getGson();
        if (row.isJsonObject()) {
            this.row = row.getAsJsonObject();
        } else {
            this.row = new JsonObject();
        }
    }

    @Override
    public String getId() {
        JsonElement id = row.get("id");
        if (id != null) {
            return id.getAsString();
        } else {
            return null;
        }
    }

    @Override
    public K getKey() {
        return gson.fromJson(row.get("key"), parameters.getKeyType());
    }

    @Override
    public V getValue() {
        return gson.fromJson(row.get("value"), parameters.getValueType());
    }

    @Override
    public Document getDocument() {
        return gson.fromJson(row.get("doc"), Document.class);
    }

    public <D> D getDocumentAsType(Class<D> docType) {
        return gson.fromJson(row.get("doc"), docType);
    }

}
