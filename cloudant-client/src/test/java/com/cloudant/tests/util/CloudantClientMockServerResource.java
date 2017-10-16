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

import com.cloudant.tests.CloudantClientHelper;

import okhttp3.mockwebserver.MockWebServer;

public class CloudantClientMockServerResource extends CloudantClientResource {

    private final MockWebServer server;

    public CloudantClientMockServerResource(MockWebServer mockWebServer) {
        super(CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer));
        this.server = mockWebServer;
    }

    @Override
    public void after() {
        // Queue a 200 for the _session DELETE that is called on shutdown.
        server.enqueue(MockWebServerResources.JSON_OK);
        super.after();
    }
}
