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

import com.cloudant.client.api.model.Shard;
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

public class ShardDeserializer implements JsonDeserializer<List<Shard>> {


    public List<Shard> deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext context) throws JsonParseException {

        final List<Shard> shards = new ArrayList<Shard>();

        final JsonObject jsonObject = json.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> shardsObj = jsonObject.get("shards").getAsJsonObject
                ().entrySet();

        for (Map.Entry<String, JsonElement> entry : shardsObj) {
            String range = entry.getKey();
            List<String> nodeNames = context.deserialize(entry.getValue(), DeserializationTypes
                    .STRINGS);
            shards.add(new Shard(range, nodeNames));
        }

        return shards;
    }
}
