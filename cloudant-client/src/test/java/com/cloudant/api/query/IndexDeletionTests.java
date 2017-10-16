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

package com.cloudant.api.query;

import static org.junit.Assert.assertEquals;

import com.cloudant.tests.util.MockWebServerResources;
import com.cloudant.tests.util.MockedServerTest;

import org.junit.Before;
import org.junit.Test;

import okhttp3.mockwebserver.RecordedRequest;

import java.util.concurrent.TimeUnit;

public class IndexDeletionTests extends MockedServerTest {

    @Before
    public void enqueueOK() {
        server.enqueue(MockWebServerResources.JSON_OK);
    }

    @Test
    public void deleteDefaultJsonIndex() throws Exception {
        db.deleteIndex("testddoc", "testindex");
        assertDelete("testddoc", "testindex", "json");
    }

    @Test
    public void deleteJsonIndex() throws Exception {
        db.deleteIndex("testddoc", "testindex", "json");
        assertDelete("testddoc", "testindex", "json");
    }

    @Test
    public void deleteTextIndex() throws Exception {
        db.deleteIndex("testddoc", "testindex", "text");
        assertDelete("testddoc", "testindex", "text");
    }

    private void assertDelete(String name, String ddoc, String type) throws Exception {
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("The request body should match the expected", "/" + dbResource.getDatabaseName() +
                "/_index/_design/" + ddoc + "/" + type + "/" + name, request.getPath());
    }
}
