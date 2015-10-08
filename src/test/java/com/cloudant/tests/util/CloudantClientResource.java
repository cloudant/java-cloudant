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

package com.cloudant.tests.util;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.tests.CloudantClientHelper;

import org.junit.rules.ExternalResource;

public class CloudantClientResource extends ExternalResource {
    private CloudantClient client;

    @Override
    public void before() {
        client = CloudantClientHelper.getClient();
    }

    @Override
    public void after() {
        client.shutdown();
    }

    public CloudantClient get() {
        return this.client;
    }
}
