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

package com.cloudant.client.internal.util;

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class SelectorUtils {

    public static JsonObject getPartialFilterSelectorFromString(String selectorJson) {
        return getSelectorFromString("partial_filter_selector", selectorJson);
    }

    public static JsonObject getSelectorFromString(String selectorJson) {
        return getSelectorFromString("selector", selectorJson);
    }

    private static JsonObject getSelectorFromString(String key, String selectorJson) {
        selectorJson = sanitizeSelectorString(selectorJson);
        JsonObject selector = selectorStringAsJsonObject(key, selectorJson);
        selector = extractNestedSelectorObject(key, selector);
        return selector;
    }

    private static String sanitizeSelectorString(String selectorJson) {
        assertNotNull(selectorJson, "selectorJson");
        // If it wasn't null we can safely trim
        selectorJson = selectorJson.trim();
        assertNotEmpty(selectorJson, "selectorJson");
        return selectorJson;
    }

    private static JsonObject selectorStringAsJsonObject(String key, String selectorJson) {
        Gson gson = new Gson();
        JsonObject selectorObject = null;
        boolean isObject = true;
        try {
            selectorObject = gson.fromJson(selectorJson, JsonObject.class);
        } catch (JsonParseException e) {
            isObject = false;
        }

        if (!isObject) {
            if (selectorJson.startsWith(key) || selectorJson.startsWith("\"" + key + "\"")) {
                selectorJson = selectorJson.substring(selectorJson.indexOf(":") + 1,
                        selectorJson.length()).trim();
                selectorObject = gson.fromJson(selectorJson, JsonObject.class);
            } else {
                throw new JsonParseException("selectorJson should be valid json or like " +
                        "\"" + key + "\": {...} ");
            }
        }
        return selectorObject;
    }

    private static JsonObject extractNestedSelectorObject(String key, JsonObject selectorObject) {
        if (selectorObject.has(key)) {
            return selectorObject.get(key).getAsJsonObject();
        } else {
            return selectorObject;
        }
    }
}
