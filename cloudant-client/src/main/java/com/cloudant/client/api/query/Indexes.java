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

package com.cloudant.client.api.query;

import com.cloudant.client.internal.query.ListableIndex;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Class modelling the indexes defined for a database.
 */
public class Indexes {
    private JsonArray indexes;

    /**
     * @return a list of the text indexes defined in the database
     */
    public List<TextIndex> textIndexes() {
        return listIndexType("text", TextIndex.class);
    }

    /**
     * @return a list of the JSON indexes defined in the database
     */
    public List<JsonIndex> jsonIndexes() {
        return listIndexType("json", JsonIndex.class);
    }

    /**
     * All the indexes defined in the database. Type widening means that the returned Index objects
     * are limited to the name, design document and type of the index and the names of the fields.
     *
     * @return a list of defined indexes with name, design document, type and field names.
     */
    public List<Index<Field>> allIndexes() {
        List<Index<Field>> indexesOfAnyType = new ArrayList<Index<Field>>();
        indexesOfAnyType.addAll(listIndexType(null, ListableIndex.class));
        return indexesOfAnyType;
    }

    /**
     * Utility to list indexes of a given type.
     *
     * @param type      the type of index to list, null means all types
     * @param modelType the class to deserialize the index into
     * @param <T>       the type of the index
     * @return the list of indexes of the specified type
     */
    private <T extends Index> List<T> listIndexType(String type, Class<T> modelType) {
        List<T> indexesOfType = new ArrayList<T>();
        Gson g = new Gson();
        for (JsonElement index : indexes) {
            if (index.isJsonObject()) {
                JsonObject indexDefinition = index.getAsJsonObject();
                JsonElement indexType = indexDefinition.get("type");
                if (indexType != null && indexType.isJsonPrimitive()) {
                    JsonPrimitive indexTypePrimitive = indexType.getAsJsonPrimitive();
                    if (type == null || (indexTypePrimitive.isString() && indexTypePrimitive
                            .getAsString().equals(type))) {
                        indexesOfType.add(g.fromJson(indexDefinition, modelType));
                    }
                }
            }
        }
        return indexesOfType;
    }
}
