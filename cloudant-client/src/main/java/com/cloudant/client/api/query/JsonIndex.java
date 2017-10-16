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

import com.cloudant.client.api.Database;
import com.cloudant.client.internal.query.InternalIndex;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * <P>
 * Class model of a JSON index definition.
 * </P>
 * <P>
 * This class should not be instantiated directly, use {@link JsonIndex.Builder} to create an
 * index definition string to pass to {@link com.cloudant.client.api.Database#createIndex(String)}
 * or use {@link Database#listIndexes()} and {@link Indexes#jsonIndexes()} to retrieve existing
 * JSON index definitions.
 * </P>
 */
public class JsonIndex extends InternalIndex<JsonIndex.Definition, JsonIndex.Field> {

    private JsonIndex() {
        super("json");
        this.def = new Definition();
    }

    static class Definition extends com.cloudant.client.internal.query.Definition<Field> {

    }

    /**
     * Model of a field in a JSON index including the field name and sort order.
     */
    @JsonAdapter(Field.FieldAdapter.class)
    public static class Field extends Sort {

        /**
         * Instantiate a field with the default ascending sort order.
         *
         * @param name the name of the field
         */
        public Field(String name) {
            super(name);
        }

        /**
         * Instantiate a new Field
         *
         * @param name  the name of the field
         * @param order the sort order to apply
         */
        public Field(String name, Sort.Order order) {
            super(name, order);
        }

        private static class FieldAdapter implements JsonSerializer<Field>,
                JsonDeserializer<Field> {

            private static final Type SORT_ORDER = new TypeToken<Sort.Order>() {
            }.getType();

            @Override
            public Field deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
                    context) throws JsonParseException {
                JsonObject fieldObject = json.getAsJsonObject();
                Field field = null;
                for (Map.Entry<String, JsonElement> fieldEntry : fieldObject.entrySet()) {
                    field = new Field(fieldEntry.getKey(), context.<Sort.Order>deserialize
                            (fieldEntry.getValue(), SORT_ORDER));
                }
                return field;
            }

            @Override
            public JsonElement serialize(Field src, Type typeOfSrc, JsonSerializationContext
                    context) {
                if (src.getOrder() != null) {
                    JsonObject field = new JsonObject();
                    field.add(src.getName(), context.serialize(src.getOrder()));
                    return field;
                } else {
                    return new JsonPrimitive(src.getName());
                }
            }
        }
    }

    /**
     * Class for building a definition for a JSON type index.
     */
    public static class Builder extends com.cloudant.client.internal.query.Builder<JsonIndex,
            Definition, Builder, Field> {

        protected JsonIndex newInstance() {
            return new JsonIndex();
        }

        @Override
        protected Builder returnThis() {
            return this;
        }
    }
}
