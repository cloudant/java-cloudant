/*
 * Copyright (c) 2015, 2018 IBM Corp. All rights reserved.
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

package com.cloudant.client.api.views;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Class for specifying the types of keys emitted by a view.
 * <P>Provides static utilities for generating
 * {@link com.cloudant.client.api.views.Key.ComplexKey} instances.
 * </P>
 * <P>Provides access to
 * {@link com.cloudant.client.api.views.Key.Type}.
 * </P>
 *
 * @since 2.0.0
 */
public class Key {

    /**
     * <p>
     * Key type identifier.
     * </p>
     * <P>
     * Accessed via the constants:
     * </P>
     * <UL>
     * <LI>{@link com.cloudant.client.api.views.Key.Type#BOOLEAN}</LI>
     * <LI>{@link com.cloudant.client.api.views.Key.Type#COMPLEX}</LI>
     * <LI>{@link com.cloudant.client.api.views.Key.Type#NUMBER}</LI>
     * <LI>{@link com.cloudant.client.api.views.Key.Type#STRING}</LI>
     * </UL>
     * <p>
     * New complex keys are initially created using the static helper methods:
     * </p>
     * <UL>
     * <LI>{@link com.cloudant.client.api.views.Key#complex(Boolean...)}</LI>
     * <LI>{@link com.cloudant.client.api.views.Key#complex(Number...)}</LI>
     * <LI>{@link com.cloudant.client.api.views.Key#complex(String...)}</LI>
     * </UL>
     * <p>
     * After creation further values can be added to the complex keys. For example to generate
     * the complex key {@code [true,1,"a","b"]}
     * </p>
     * <P>
     * {@code
     * Key.complex(true).add(1).add("a", "b");
     * }
     * </P>
     * <p>
     * To match all values as part of a complex key range, use {@code addHighSentinel()}.
     * For instance, to specify the range given in
     * <a href="http://docs.couchdb.org/en/latest/ddocs/views/collation.html#examples" target="_blank">
     * this example</a>, use the following:
     * </p>
     * <pre>
     * {@code
     * Key start = Key.complex("XYZ");
     * Key end = Key.complex("XYZ).addHighSentinel();
     * }
     * </pre>
     *
     * @param <T> {@link Boolean},
     *            {@link Number},
     *            {@link String} or
     *            {@link com.cloudant.client.api.views.Key.ComplexKey}
     * @since 2.0.0
     */
    public static final class Type<T> {

        /**
         * Type constant for boolean keys.
         *
         * @since 2.0.0
         */
        public static final Type<Boolean> BOOLEAN = new Type<Boolean>(Boolean.class);
        /**
         * Type constant for
         * <a target="_blank"
         * href="https://console.bluemix.net/docs/services/Cloudant/api/creating_views.html#map-function-examples">
         * complex keys.</a>
         *
         * @since 2.0.0
         */
        public static final Type<ComplexKey> COMPLEX = new Type<ComplexKey>(ComplexKey.class);
        /**
         * Type constant for numerical keys.
         *
         * @since 2.0.0
         */
        public static final Type<Number> NUMBER = new Type<Number>(Number.class);
        /**
         * Type constant for string keys.
         *
         * @since 2.0.0
         */
        public static final Type<String> STRING = new Type<String>(String.class);

        private final Class<T> type;

        private Type(Class<T> type) {
            this.type = type;
        }

        Class<T> getType() {
            return type;
        }
    }

    /**
     * Provides methods for adding values to build a complex key array.
     *
     * @since 2.0.0
     */
    public static final class ComplexKey {

        //default builder is acceptable as only primitive types
        private static final Gson gson = new GsonBuilder().create();

        // The high key sentinel value which is guaranteed to sort last
        private static final String HIGH_SENTINEL = "\ufff0";

        private JsonArray json = new JsonArray();

        private boolean canAddKeys = true;

        private ComplexKey() {

        }

        private ComplexKey(JsonArray json) {
            this.json = json;
        }

        private <T> Key.ComplexKey addObjects(T... keys) {
            if (!canAddKeys) {
                throw new IllegalStateException("Cannot add keys after addHighSentinel() has been called");
            }
            for (T key : keys) {
                json.add(gson.toJsonTree(key));
            }
            return this;
        }

        /**
         * Add one or more strings to the complex key.
         *
         * @param stringKeys string values to add
         * @return this ComplexKey to allow chained additions
         * @since 2.0.0
         */
        public Key.ComplexKey add(String... stringKeys) {
            return addObjects(stringKeys);
        }

        /**
         * Add one or more booleans to the complex key.
         *
         * @param booleanKeys boolean values to add
         * @return this ComplexKey to allow chained additions
         * @since 2.0.0
         */
        public Key.ComplexKey add(Boolean... booleanKeys) {
            return addObjects(booleanKeys);
        }

        /**
         * Add one or more numbers to the complex key.
         *
         * @param numberKeys numerical values to add
         * @return this ComplexKey to allow chained additions
         * @since 2.0.0
         */
        public Key.ComplexKey add(Number... numberKeys) {
            return addObjects(numberKeys);
        }

        /**
         * Add the high key sentinel value, which is guaranteed to sort last, to the complex key.
         * @return this ComplexKey. Note that chained additions are prohibited after calling this
         * method and any subsequent calls to {@code add()} will throw an {@code IllegalStateException}.
         * @since 2.13.0
         */
        public Key.ComplexKey addHighSentinel() {
            Key.ComplexKey k = addObjects(HIGH_SENTINEL);
            this.canAddKeys = false;
            return k;
        }

        /**
         * @return the JSON string representation of this complex key
         * @since 2.0.0
         */
        public String toJson() {
            return json.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ComplexKey that = (ComplexKey) o;

            return json.equals(that.json);

        }

        @Override
        public int hashCode() {
            return json.hashCode();
        }
    }

    /**
     * Create a new complex key with one or more boolean elements at the start
     *
     * @param booleanKeys boolean values to add
     * @return the new ComplexKey
     * @since 2.0.0
     */
    public static ComplexKey complex(Boolean... booleanKeys) {
        return new ComplexKey().add(booleanKeys);
    }

    /**
     * Create a new complex key with one or more numerical elements at the start
     *
     * @param numberKeys numerical values to add
     * @return the new ComplexKey
     * @since 2.0.0
     */
    public static ComplexKey complex(Number... numberKeys) {
        return new ComplexKey().add(numberKeys);
    }

    /**
     * Create a new complex key with one or more string elements at the start
     *
     * @param stringKeys string values to add
     * @return the new ComplexKey
     * @since 2.0.0
     */
    public static ComplexKey complex(String... stringKeys) {
        return new ComplexKey().add(stringKeys);
    }

    /**
     * GSON deserializer to convert JSON arrays to {@link com.cloudant.client.api.views.Key
     * .ComplexKey}
     */
    public static final class ComplexKeyDeserializer implements JsonSerializer<ComplexKey>,
            JsonDeserializer<ComplexKey> {

        @Override
        public Key.ComplexKey deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
                                          JsonDeserializationContext context) throws
                JsonParseException {
            if (json.isJsonArray()) {
                ComplexKey key = new ComplexKey(json.getAsJsonArray());
                return key;
            } else {
                return null;
            }
        }

        @Override
        public JsonElement serialize(ComplexKey src, java.lang.reflect.Type typeOfSrc,
                                     JsonSerializationContext context) {
            return src.json;
        }
    }
}
