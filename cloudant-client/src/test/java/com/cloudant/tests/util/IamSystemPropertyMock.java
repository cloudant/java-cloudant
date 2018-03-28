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

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Mock System.getProperty to fake the value of the IAM server
 *
 * Do this instead of setting the property in the real System because it pollutes the JVM
 */
public class IamSystemPropertyMock extends MockUp<System> {

    public final AtomicReference<String> mockIamTokenEndpointUrl = new AtomicReference<String>();

    public void setMockIamTokenEndpointUrl(String mockIamTokenEndpointUrl) {
        this.mockIamTokenEndpointUrl.set(mockIamTokenEndpointUrl);
    }

    @Mock
    public String getProperty(Invocation inv, String key) {
        if (key.equals("com.cloudant.client.iamserver")) {
            return mockIamTokenEndpointUrl.get();
        }
        return inv.proceed(key);
    }

    @Mock
    public String getProperty(Invocation inv, String key, String def) {
        if (key.equals("com.cloudant.client.iamserver")) {
            return mockIamTokenEndpointUrl.get();
        }
        return inv.proceed(key, def);
    }
}
