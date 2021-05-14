/*
 * Copyright © 2021 IBM Corp. All rights reserved.
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

import static com.cloudant.http.internal.interceptors.IamCookieInterceptor.IAM_TOKEN_SERVER_URL_PROPERTY_KEY;
import static com.cloudant.tests.util.MockWebServerResources.iamTokenEndpoint;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import okhttp3.mockwebserver.MockWebServer;

public class MockIamWebServerExtension extends MockWebServerExtension {

    // Immutably store the real token server
    private final String tokenServer = System.getProperty(IAM_TOKEN_SERVER_URL_PROPERTY_KEY);

    // NB beforeEach/afterEach emulate before and after in the actual mock, because we can't call
    // these directly as they are marked as protected

    @Override
    public synchronized void beforeEach(ExtensionContext context) throws Exception {
        super.beforeEach(context);
        // Override the token server property
        System.setProperty(IAM_TOKEN_SERVER_URL_PROPERTY_KEY, this.get().url(iamTokenEndpoint)
                .toString());
    }

    @Override
    public synchronized void afterEach(ExtensionContext context) throws Exception {
        if (tokenServer != null) {
          // Restore the original token server
          System.setProperty(IAM_TOKEN_SERVER_URL_PROPERTY_KEY, tokenServer);
        } else {
          // Clear the temporary token server
          System.clearProperty(IAM_TOKEN_SERVER_URL_PROPERTY_KEY);
        }
        super.afterEach(context);
    }

}
