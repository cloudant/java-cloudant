/*
 * Copyright (c) 2016 IBM Corp. All rights reserved.
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

package com.cloudant.client.internal.util;

import com.cloudant.client.api.model.Index;
import com.cloudant.client.api.model.IndexField;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IndexDeserializer implements JsonDeserializer<List<Index>> {


    public List<Index> deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext context) throws JsonParseException {

        final List<Index> indices = new ArrayList<Index>();

        final JsonObject jsonObject = json.getAsJsonObject();
        JsonArray indArray = jsonObject.get("indexes").getAsJsonArray();
        for (int i = 0; i < indArray.size(); i++) {
            JsonObject ind = indArray.get(i).getAsJsonObject();
            String ddoc = null;
            if (!ind.get("ddoc").isJsonNull()) { // ddoc is optional
                ddoc = ind.get("ddoc").getAsString();
            }
            Index idx = new Index(ddoc, ind.get("name").getAsString(),
                    ind.get("type").getAsString());
            JsonArray fldArray = ind.get("def").getAsJsonObject().get("fields").getAsJsonArray();
            for (int j = 0; j < fldArray.size(); j++) {
                Set<Map.Entry<String, JsonElement>> fld = fldArray.get(j).getAsJsonObject()
                        .entrySet();
                for (Map.Entry<String, JsonElement> entry : fld) {
                    idx.addIndexField(entry.getKey(),
                            IndexField.SortOrder.valueOf(entry.getValue().getAsString())
                    );
                }
            }//end fldArray
            indices.add(idx);

        }// end indexes

        return indices;
    }
}
