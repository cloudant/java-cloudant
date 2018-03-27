/*
 * Copyright © 2017, 2018 IBM Corp. All rights reserved.
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

package com.cloudant.tests.base;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.tests.extensions.CloudantClientMockServerExtension;
import com.cloudant.tests.extensions.DatabaseExtension;
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.cloudant.tests.extensions.MultiExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import okhttp3.mockwebserver.MockWebServer;

/**
 * Class of tests that run against a MockWebServer. This class handles set up and tear down of the
 * mock server and associated client and db objects before/after each test.
 */
public abstract class TestWithMockedServer {

    public static MockWebServerExtension mockWebServerExt = new MockWebServerExtension();
    public static CloudantClientMockServerExtension clientResource = new
            CloudantClientMockServerExtension(mockWebServerExt);
    public static DatabaseExtension.PerTest dbResource = new DatabaseExtension.PerTest
            (clientResource);
    @RegisterExtension
    public static MultiExtension extensions = new MultiExtension(
            mockWebServerExt,
            clientResource,
            dbResource
    );

    protected MockWebServer server;
    protected CloudantClient client;
    protected Database db;

    @BeforeEach
    public void beforeEach() {
        server = mockWebServerExt.get();
        client = clientResource.get();
        db = dbResource.get();
    }

}
