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

package com.cloudant.tests.extensions;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.tests.CloudantClientHelper;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class CloudantClientExtension extends AbstractClientExtension implements
        BeforeAllCallback, AfterAllCallback {

    private ClientBuilder clientBuilder;

    private CloudantClient client;

    public CloudantClientExtension() {
        this.clientBuilder = CloudantClientHelper.getClientBuilder();
    }

    public CloudantClientExtension(ClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @Override
    public CloudantClient get() {
        return this.client;
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        client.shutdown();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        client = clientBuilder.build();
    }

    /**
     * Get the base URI of the client with credentials included which is useful for example for
     * replication.
     *
     * @return String representation of the URI
     */
    @Override
    public String getBaseURIWithUserInfo() {
        return CloudantClientHelper.SERVER_URI_WITH_USER_INFO;
    }
}
