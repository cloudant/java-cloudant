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

package com.cloudant.client.internal.query;

import com.cloudant.client.api.query.Field;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListableIndex extends InternalIndex<ListableIndex.Definition, Field> {

    /**
     * Constructor for sub-classes to instantiate
     *
     * @param type the type of the index
     */
    protected ListableIndex(String type) {
        super(type);
    }

    @JsonAdapter(Adapter.class)
    static class Definition extends com.cloudant.client.internal.query.Definition<Field> {

    }

    static class Adapter implements JsonDeserializer<Definition> {

        @Override
        public Definition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
                context) throws JsonParseException {
            Definition def = new Definition();
            JsonObject d = json.getAsJsonObject();
            JsonArray fields = d.getAsJsonArray("fields");
            List<Field> namedFields = new ArrayList<Field>();
            for (JsonElement field : fields) {
                JsonObject f = field.getAsJsonObject();
                for (Map.Entry<String, JsonElement> fieldEntry : f.entrySet()) {
                    namedFields.add(new NamedField(fieldEntry.getKey()));
                }
            }
            def.fields = namedFields;
            return def;
        }
    }

    public String toString() {
        StringBuilder index = new StringBuilder();
        index.append("ddoc: ");
        index.append(getDesignDocumentID());
        index.append(", name: ");
        index.append(getName());
        index.append(", type: ");
        index.append(getType());
        index.append(", fields: ");
        index.append(getFields().toString());
        index.append(", partial_filter_selector: ");
        index.append(getPartialFilterSelector());
        return index.toString();
    }
}
