/*
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

package com.cloudant.tests;


import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.jsonToObject;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

public class CouchDbUtilTest {

    /**
     * Assert that if doc's value in JSON object
     * is null, the result from jsonToObject
     * will be null and no exception occurs.
     */
    @Test
    public void mockQueryResultWithNullDocValue() {
        String queryResult = "{\"doc\":null}";

        Gson gson = new Gson();
        JsonObject jsonFromSearchQuery = new JsonParser()
                .parse(queryResult).getAsJsonObject();
        JsonObject mockResult = jsonToObject(gson, jsonFromSearchQuery, "doc", JsonObject.class);

        assertEquals(null, mockResult);
    }
}
