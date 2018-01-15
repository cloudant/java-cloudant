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
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <P>
 * Class model of a text index definition.
 * </P>
 * <P>
 * This class should not be instantiated directly, use {@link TextIndex.Builder} to create a text
 * index definition string to pass to {@link com.cloudant.client.api.Database#createIndex(String)}
 * or use {@link Database#listIndexes()} and {@link Indexes#textIndexes()} to retrieve existing
 * text index definitions.
 * </P>
 * @see com.cloudant.client.api.Database#createIndex(String)
 */
public class TextIndex extends InternalIndex<TextIndex.Definition, TextIndex.Field> {

    private TextIndex() {
        super("text");
        this.def = new Definition();
    }

    /**
     * Get a new instance of a builder to configure a TextIndex.
     *
     * @return a builder for configuring a TextIndex
     */
    public static TextIndex.Builder builder() {
        return new TextIndex.Builder();
    }

    /**
     * Get the JSON string representation of the default field configuration.
     *
     * @return default field JSON as string
     */
    public String getDefaultField() {
        return this.def.default_field.toString();
    }

    /**
     * Get the JSON string representation of the analyzer configured for this text index.
     *
     * @return analyzer JSON as string
     */
    public String getAnalyzer() {
        return def.analyzer.toString();
    }

    /**
     * @return true if this index is configured to scan documents for arrays and store their lengths
     */
    public boolean getIndexArrayLengths() {
        return def.index_array_lengths;
    }

    /**
     * Model of a field in the text index including the field name and type.
     */
    @JsonAdapter(FieldAdapter.class)
    public static class Field extends com.cloudant.client.internal.query.NamedField {

        private Type type;

        /**
         * Instantiate a new Field
         *
         * @param name the name of the field
         * @param type the type of the field
         */
        private Field(String name, Type type) {
            super(name);
            this.type = type;
        }

        public Type getType() {
            return this.type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            Field field = (Field) o;

            return type == field.type;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }
    }

    private static class FieldAdapter implements JsonDeserializer<Field> {

        private static final java.lang.reflect.Type TEXT_FIELD = new TypeToken<Type>() {
        }.getType();

        @Override
        public Field deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
                                 JsonDeserializationContext context) throws JsonParseException {
            JsonObject fieldObject = json.getAsJsonObject();
            Field field = null;
            for (Map.Entry<String, JsonElement> fieldEntry : fieldObject.entrySet()) {
                field = new Field(fieldEntry.getKey(), context.<Type>deserialize
                        (fieldEntry.getValue(), TEXT_FIELD));
            }
            return field;
        }
    }

    /**
     * Internal class for configuring and deserializing index defintions.
     */
    static class Definition extends com.cloudant.client.internal.query.Definition<Field> {

        @SerializedName(value = "analyzer", alternate = {"default_analyzer"})
        private JsonElement analyzer;
        private JsonObject default_field;

        private Boolean index_array_lengths = null;

    }

    /**
     * Class for building a definition for a text type index.
     */
    public static class Builder extends com.cloudant.client.internal.query.Builder<TextIndex,
            Definition, Builder, Field> {

        private Builder() {
            // Prevent instantiation except by static TextIndex.builder()
        }

        @Override
        protected TextIndex.Builder returnThis() {
            return this;
        }

        /**
         * Configure the name of the index, if not set a name will be generated.
         *
         * @param indexName name of the index
         * @return the builder for chaining
         */
        public TextIndex.Builder name(String indexName) {
            return super.name(indexName);
        }

        /**
         * Configure the design document name, if not set a new design document will be created with
         * a generated name.
         *
         * @param designDocumentId design document ID (the _design prefix is added if not supplied)
         * @return the builder for chaining
         */
        public TextIndex.Builder designDocument(String designDocumentId) {
            return super.designDocument(designDocumentId);
        }

        /**
         * <p>
         * Configure a selector to choose documents that should be added to the index.
         * </p>
         * <p>
         * Obtain a selector from an {@link Operation} or {@link Expression}.
         * </p>
         * @param selector {@code Selector} object describing criteria used to add
         *                 documents to index
         * @return the builder for chaining
         * @see Selector
         * @see <a
         * href="https://console.bluemix.net/docs/services/Cloudant/api/cloudant_query.html#selector-syntax"
         * target="_blank">selector syntax</a>
         */
        @Override
        public TextIndex.Builder partialFilterSelector(Selector selector) {
            return super.partialFilterSelector(selector);
        }

        @Override
        protected TextIndex newInstance() {
            return new TextIndex();
        }

        /**
         * Configure the default field for the text index.
         *
         * @param enabled  true to enable the default field
         * @param analyzer analyzer to use for the default field
         * @return the builder for chaining
         */
        public TextIndex.Builder defaultField(boolean enabled, String analyzer) {
            instance.def.default_field = new JsonObject();
            instance.def.default_field.addProperty("enabled", enabled);
            instance.def.default_field.add("analyzer", new Gson().toJsonTree(analyzer));
            return this;
        }

        /**
         * <P>
         * Configure the analyzer for the text index.
         * </P>
         * <P>
         * The analyzer can be a simple string or a more complex JSON object, as shown in the
         * following examples:
         * </P>
         * <P>
         * Configure the "keyword" analyzer:
         * </P>
         * <pre>
         * {@code
         * TextIndex.builder().analyzer("keyword");
         * }
         * </pre>
         * <p>
         * Configure different analyzers for different fields:
         * </p>
         * <pre>
         * {@code
         * String analyzerString = "{\"name\": \"perfield\"," +
         *     "\"default\": \"english\"," +
         *     "\"fields\": {" +
         *     "\"spanish\": \"spanish\"," +
         *     "\"german\": \"german\"}}";
         * TextIndex.builder().analyzer(analyzerString);
         * }
         * </pre>
         *
         * @param analyzer either a simple string or a complex JSON object, represented as a string
         * @return the builder for chaining
         * @see <a href="https://console.bluemix.net/docs/services/Cloudant/api/search.html#analyzers"
         * target="_blank">Cloudant Search Analyzers</a>
         */
        public TextIndex.Builder analyzer(String analyzer) {
            instance.def.analyzer = new Gson().fromJson(analyzer, JsonElement.class);
            return this;
        }

        /**
         * @param indexArrayLengths true if the indexer should search for arrays and store their
         *                          lengths
         * @return the builder for chaining
         */
        public TextIndex.Builder indexArrayLengths(boolean indexArrayLengths) {
            instance.def.index_array_lengths = indexArrayLengths;
            return this;
        }

        /**
         * Add one or more fields containing string values to the text index configuration.
         *
         * @param fieldNames names of the fields to index
         * @return the builder for chaining
         */
        public TextIndex.Builder string(String... fieldNames) {
            return super.fields(fieldNamesToFieldList(Type.STRING, fieldNames));
        }

        /**
         * Add one or more fields containing numerical values to the text index configuration.
         *
         * @param fieldNames names of the fields to index
         * @return the builder for chaining
         */
        public TextIndex.Builder number(String... fieldNames) {
            return super.fields(fieldNamesToFieldList(Type.NUMBER, fieldNames));
        }

        /**
         * Add a field containing boolean values to the text index configuration.
         *
         * @param fieldNames name of the field to index
         * @return the builder for chaining
         */
        public TextIndex.Builder bool(String... fieldNames) {
            return super.fields(fieldNamesToFieldList(Type.BOOLEAN, fieldNames));
        }

        private List<Field> fieldNamesToFieldList(Type type, String... fieldNames) {
            List<Field> fields = new ArrayList<Field>(fieldNames.length);
            for (String fieldName : fieldNames) {
                fields.add(new Field(fieldName, type));
            }
            return fields;
        }
    }
}
