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

import com.cloudant.client.api.model.Permissions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SecurityDeserializer implements JsonDeserializer<Map<String, EnumSet<Permissions>>> {


    public Map<String, EnumSet<Permissions>> deserialize(JsonElement json, Type typeOfT,
                                                         JsonDeserializationContext context)
            throws JsonParseException {

        Map<String, EnumSet<Permissions>> perms = new HashMap<String, EnumSet<Permissions>>();
        JsonElement elem = json.getAsJsonObject().get("cloudant");
        if (elem == null) {
            return perms;
        }
        Set<Map.Entry<String, JsonElement>> permList = elem.getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry : permList) {
            String user = entry.getKey();
            EnumSet<Permissions> p = context.deserialize(entry.getValue(), DeserializationTypes
                    .PERMISSIONS);
            perms.put(user, p);
        }
        return perms;

    }
}
