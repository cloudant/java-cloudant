/*
 * Copyright © 2016, 2018 IBM Corp. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudant.http.internal.DefaultHttpUrlConnectionFactory;
import com.cloudant.http.internal.ok.OkHelper;
import com.cloudant.tests.base.TestWithDbPerClass;

import org.junit.jupiter.api.BeforeEach;

import mockit.Mock;
import mockit.MockUp;

public abstract class HttpFactoryParameterizedTest extends TestWithDbPerClass {

    /**
     * A parameter governing whether to allow okhttp or not. This lets us exercise both
     * HttpURLConnection types in these tests.
     */
    public boolean isOkUsable;

    /**
     * A mock OkHelper that always returns false to force use of the JVM HttpURLConnection
     * via the {@link DefaultHttpUrlConnectionFactory}
     */
    static class OkHelperMock extends MockUp<OkHelper> {
        @Mock
        public static boolean isOkUsable() {
            return false;
        }
    }

    @BeforeEach
    public void changeHttpConnectionFactory(boolean isOkUsable) throws Exception {
        this.isOkUsable = isOkUsable;
        if (!isOkUsable) {
            // New up the mock that will stop okhttp's factory being used
            new OkHelperMock();
        }
        // Verify that we are getting the behaviour we expect.
        assertEquals(
                isOkUsable, OkHelper.isOkUsable(), "The OK usable value was not what was expected" +
                        " for the test parameter.");
    }
}
