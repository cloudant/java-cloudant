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

package com.cloudant.tests.util;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;

import okhttp3.mockwebserver.MockWebServer;

/**
 * Class of tests that run against a MockWebServer. This class handles set up and tear down of the
 * mock server and associated client and db objects before/after each test.
 */
public abstract class MockedServerTest {

    protected MockWebServer server = new MockWebServer();
    protected CloudantClient client;
    protected Database db;

    protected CloudantClientMockServerResource clientResource = new
            CloudantClientMockServerResource(server);
    protected DatabaseResource dbResource = new DatabaseResource(clientResource);
    @Rule
    public RuleChain chain = RuleChain.outerRule(server).around(clientResource).around(dbResource);

    @Before
    public void setup() throws Exception {
        client = clientResource.get();
        db = dbResource.get();
    }
}
