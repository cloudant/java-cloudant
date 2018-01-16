/*
 * Copyright © 2017, 2018 IBM Corp. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;
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
 * @see com.cloudant.client.api.Database#createIndex(String)
 */
public class JsonIndex extends InternalIndex<JsonIndex.Definition, JsonIndex.Field> {

    private JsonIndex() {
        super("json");
        this.def = new Definition();
    }

    /**
     * Get a new instance of a builder to configure a JsonIndex.
     *
     * @return a builder for configuring a JsonIndex
     */
    public static JsonIndex.Builder builder() {
        return new JsonIndex.Builder();
    }

    static class Definition extends com.cloudant.client.internal.query.Definition<Field> {

    }

    /**
     * Model of a field in a JSON index including the field name and sort order.
     */
    @JsonAdapter(Field.FieldAdapter.class)
    public static class Field extends Sort {

        /**
         * Instantiate a new Field
         *
         * @param name  the name of the field
         * @param order the sort order to apply
         */
        private Field(String name, Sort.Order order) {
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

        private Builder() {
            // Prevent instantiation except by static JsonIndex.builder()
        }

        protected JsonIndex newInstance() {
            return new JsonIndex();
        }

        @Override
        protected Builder returnThis() {
            return this;
        }

        /**
         * Configure the name of the index, if not set a name will be generated.
         *
         * @param indexName name of the index
         * @return the builder for chaining
         */
        public Builder name(String indexName) {
            return super.name(indexName);
        }

        /**
         * Configure the design document name, if not set a new design document will be created with
         * a generated name.
         *
         * @param designDocumentId design document ID (the _design prefix is added if not supplied)
         * @return the builder for chaining
         */
        public Builder designDocument(String designDocumentId) {
            return super.designDocument(designDocumentId);
        }

        /**
         * <p>
         * Configure a selector to choose documents that should be added to the index.
         * </p>
         * <p>
         * Obtain a selector from an {@link Operation} or {@link Expression}.
         * </p>
         * @param selector string representation of a JSON object describing criteria used to add
         *                 documents to index
         * @return the builder for chaining
         * @see Selector
         * @see <a
         * href="https://console.bluemix.net/docs/services/Cloudant/api/cloudant_query.html#selector-syntax"
         * target="_blank">selector syntax</a>
         */
        @Override
        public Builder partialFilterSelector(Selector selector) {
            return super.partialFilterSelector(selector);
        }

        /**
         * Add one or more fields to the JsonIndex configuration in ascending order.
         *
         * @param fieldNames names of the fields to index
         * @return the builder for chaining
         */
        public Builder asc(String... fieldNames) {
            return super.fields(fieldNamesToFieldList(Sort.Order.ASC, fieldNames));
        }

        /**
         * Add one or more fields to the JsonIndex configuration in descending order.
         *
         * @param fieldNames names of the fields to index
         * @return the builder for chaining
         */
        public Builder desc(String... fieldNames) {
            return super.fields(fieldNamesToFieldList(Sort.Order.DESC, fieldNames));
        }

        private List<Field> fieldNamesToFieldList(Sort.Order order, String... fieldNames) {
            List<Field> fields = new ArrayList<Field>(fieldNames.length);
            for (String fieldName : fieldNames) {
                fields.add(new Field(fieldName, order));
            }
            return fields;
        }
    }
}
