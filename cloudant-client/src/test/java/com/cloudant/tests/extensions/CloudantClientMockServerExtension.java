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

package com.cloudant.tests.extensions;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.tests.CloudantClientHelper;
import com.cloudant.tests.util.MockWebServerResources;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import okhttp3.mockwebserver.MockWebServer;

public class CloudantClientMockServerExtension extends AbstractClientExtension implements
        BeforeEachCallback, AfterEachCallback {

    private final MockWebServerExtension mockWebServerExt;
    private MockWebServer mockWebServer;
    private CloudantClient client;

    public CloudantClientMockServerExtension(MockWebServerExtension mockWebServerExt) {
        this.mockWebServerExt = mockWebServerExt;
    }

    @Override
    public void beforeEach(ExtensionContext ctx) {
        this.mockWebServer = this.mockWebServerExt.get();
        this.client = CloudantClientHelper.newMockWebServerClientBuilder(this.mockWebServer)
                .build();
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        // Queue a 200 for the _session DELETE that is called on shutdown.
        mockWebServer.enqueue(MockWebServerResources.JSON_OK);
    }

    @Override
    public CloudantClient get() {
        return client;
    }

    @Override
    public String getBaseURIWithUserInfo() {
        return CloudantClientHelper.SERVER_URI_WITH_USER_INFO;
    }


}
