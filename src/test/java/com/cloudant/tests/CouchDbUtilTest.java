/*
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

package com.cloudant.tests;


import static org.junit.Assert.assertEquals;
import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.JsonToObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

public class CouchDbUtilTest {

    /**
     * Assert that if doc's value in JSON object
     * is null, the result from JsonToObject
     * will be null and no exception occurs.
     */
    @Test
    public void mockQueryResultWithNullDocValue() {
        String queryResult = "{\"doc\":null}";

        Gson gson = new Gson();
        JsonObject jsonFromSearchQuery = new JsonParser()
                .parse(queryResult).getAsJsonObject();
        JsonObject mockResult = JsonToObject(gson, jsonFromSearchQuery, "doc", JsonObject.class);

        assertEquals(null, mockResult);
    }
}
