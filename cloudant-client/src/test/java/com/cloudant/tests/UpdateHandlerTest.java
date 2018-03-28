/*
 * Copyright (C) 2011 lightcouch.org
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Response;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.cloudant.tests.util.Utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@RequiresDB
public class UpdateHandlerTest extends TestWithDbPerClass {

    @BeforeAll
    public static void beforeAll() throws Exception {
        Utils.putDesignDocs(db);
    }

    @Test
    public void updateHandler_queryString() {
        final String oldValue = "foo";
        final String newValue = "foo bar+plus=equals&ampersand";

        Response response = db.save(new Foo(null, oldValue));

        Params params = new Params()
                .addParam("field", "title")
                .addParam("value", newValue);

        String output = db.invokeUpdateHandler("example/example_update", response.getId(), params);

        // retrieve from db to verify
        Foo foo = db.find(Foo.class, response.getId());

        assertNotNull(output);
        assertEquals(foo.getTitle(), newValue);
    }

    @Test
    public void updateHandler_postUuid() {
        String output =
                db.invokeUpdateHandler("example/get-uuid", null, new Params());

        assertNotNull(output);
        assertTrue(output.length() > 0);
    }

    @Test
    public void updateHandler_queryParams() {
        final String oldValue = "foo";
        final String newValue = "foo bar+plus=equals&ampersand";

        Response response = db.save(new Foo(null, oldValue));

        Params params = new Params()
                .addParam("field", "title")
                .addParam("value", newValue);
        String output = db.invokeUpdateHandler("example/example_update", response.getId(), params);

        // retrieve from db to verify
        Foo foo = db.find(Foo.class, response.getId());

        assertNotNull(output);
        assertEquals(foo.getTitle(), newValue);
    }
}
