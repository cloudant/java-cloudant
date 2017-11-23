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

import com.cloudant.client.api.query.Selector;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class SelectorUtils {

    // Selector object keys
    public static final String SELECTOR = "selector";
    public static final String PARTIAL_FILTER_SELECTOR = "partial_filter_selector";

    /**
     * <P>
     * Encloses the selector in a JSON object and returns the string form.
     * </P>
     * <pre>
     * {@code
     * Selector selector = eq("year", 2017);
     * System.out.println(selector.toString());
     * // Output: "year": {"$eq" : 2017}
     * System.out.println(SelectorUtils.enclose(selector));
     * // Output: {"year": {"$eq": 2017}}
     * }
     * </pre>
     *
     * @param selector the selector to enclose
     * @return the string form of the selector enclosed in a JSON object
     */
    public static String enclose(Selector selector) {
        return String.format("{%s}", selector.toString());
    }

    /**
     * <P>
     * Returns the string form of the specified key mapping to a JSON object enclosing the selector.
     * </P>
     * <pre>
     * {@code
     * Selector selector = eq("year", 2017);
     * System.out.println(selector.toString());
     * // Output: "year": {"$eq" : 2017}
     * System.out.println(SelectorUtils.withKey("selector", selector));
     * // Output: "selector": {"year": {"$eq": 2017}}
     * }
     * </pre>
     *
     * @param key      key to use for the selector (usually "selector" or "partial_filter_selector")
     * @param selector the selector
     * @return the string form of the selector enclosed in a JSON object, keyed by key
     */
    public static String withKey(String key, Selector selector) {
        return String.format("\"%s\": %s", key, enclose(selector));
    }

    /**
     * <P>
     * Returns the string form of a JSON object with the specified key mapping to a JSON object
     * enclosing the selector.
     * </P>
     * <pre>
     * {@code
     * Selector selector = eq("year", 2017);
     * System.out.println(selector.toString());
     * // Output: "year": {"$eq" : 2017}
     * System.out.println(SelectorUtils.encloseWithKey("selector", selector));
     * // Output: {"selector": {"year": {"$eq": 2017}}}
     * }
     * </pre>
     *
     * @param key      key to use for the selector (usually "selector" or "partial_filter_selector")
     * @param selector the selector to enclose
     * @return the string form of the selector enclosed in a JSON object with the specified key
     */
    public static String encloseWithKey(String key, Selector selector) {
        return String.format("{%s}", withKey(key, selector));
    }

    public static JsonObject getJsonObjectFromSelector(Selector selector) {
        return new Gson().fromJson(enclose(selector), JsonObject.class);
    }

    public static JsonObject getSelectorFromString(String selectorJson) {
        return getSelectorFromString(SELECTOR, selectorJson);
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
