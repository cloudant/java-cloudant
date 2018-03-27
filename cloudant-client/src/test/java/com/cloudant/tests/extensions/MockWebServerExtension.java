/*
 * Copyright © 2018 IBM Corp. All rights reserved.
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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import okhttp3.mockwebserver.MockWebServer;

public class MockWebServerExtension implements BeforeEachCallback, AfterEachCallback {

    private MockWebServer mockWebServer;
    private boolean started = false;

    public MockWebServerExtension() {
    }

    public MockWebServer get() {
        return this.mockWebServer;
    }

    // NB beforeEach/afterEach emulate before and after in the actual mock, because we can't call
    // these directly as they are marked as protected

    @Override
    public synchronized void beforeEach(ExtensionContext context) throws Exception {
        this.mockWebServer = new MockWebServer();
        if (started) {
            System.err.println("*** WARNING: MockWebServer already started");
            return;
        }
        this.mockWebServer.start();
        started = true;
    }

    @Override
    public synchronized void afterEach(ExtensionContext context) throws Exception {
        this.mockWebServer.shutdown();
        started = false;
    }

}
