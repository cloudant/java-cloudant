/*
 * Copyright (C) 2011 lightcouch.org
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

package com.cloudant.client.org.lightcouch.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class GsonHelper {

    /**
     * Builds {@link Gson} and registers any required serializer/deserializer.
     *
     * @return {@link Gson} instance
     */
    public static GsonBuilder initGson(GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(JsonObject.class, new JsonDeserializer<JsonObject>() {
            public JsonObject deserialize(JsonElement json,
                                          Type typeOfT, JsonDeserializationContext context)
                    throws JsonParseException {
                return json.getAsJsonObject();
            }
        });
        gsonBuilder.registerTypeAdapter(JsonObject.class, new JsonSerializer<JsonObject>() {
            public JsonElement serialize(JsonObject src, Type typeOfSrc,
                                         JsonSerializationContext context) {
                return src.getAsJsonObject();
            }

        });

        return gsonBuilder;
    }
}
