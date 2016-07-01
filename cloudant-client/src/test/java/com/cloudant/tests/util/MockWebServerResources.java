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

import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class MockWebServerResources {

    /**
     * A mock cookie response that is OK
     */
    public static final MockResponse OK_COOKIE = new MockResponse()
            .setResponseCode(200)
            .addHeader("Set-Cookie",
                    "AuthSession=\"a2ltc3RlYmVsOjUxMzRBQTUzOtiY2_IDUIdsTJEVNEjObAbyhrgz\";")
            .setBody("{\"ok\":true,\"name\":\"mockUser\",\"roles\":[]}");

    public static final MockResponse JSON_OK = new MockResponse().setResponseCode(200).setBody
            ("{\"ok\":true}");

    public static final MockResponse PERMISSIONS = new MockResponse().setResponseCode(200)
            .setBody("{\"_id\":\"security\", \"cloudant\":{\"user\": [\"_reader\"]}}");

    public static final Dispatcher ALL_429 = new ConstantResponseDispatcher(get429());

    private static final Logger logger = Logger.getLogger(MockWebServerResources.class.getName());

    //Keystore information for https
    private static String KEYSTORE_FILE = "src/test/resources/SslAuthenticationTest.keystore";
    private static String KEYSTORE_PASSWORD = "password";
    private static String KEY_PASSWORD = "password";

    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD
                    .toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            kmf.init(keystore, KEY_PASSWORD.toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing test SSLSocketFactory", e);
            return null;
        }
    }

    /**
     * A dispatcher that sleeps for the time specified at construction on each request before
     * responding. Useful for getting a SocketTimeoutException.
     */
    public static class SleepingDispatcher extends Dispatcher {

        private final long sleepTime;
        private final TimeUnit unit;

        public SleepingDispatcher(long sleepTime, TimeUnit unit) {
            this.sleepTime = sleepTime;
            this.unit = unit;
        }

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            unit.sleep(sleepTime);
            return new MockResponse();
        }
    }

    /**
     * A dispatcher that repeatedly returns the same status code for all requests.
     */
    public static class ConstantResponseDispatcher extends Dispatcher {

        private final MockResponse response;

        public ConstantResponseDispatcher(MockResponse response) {
            this.response = response;
        }

        public ConstantResponseDispatcher(int statusCode) {
            this(new MockResponse().setResponseCode(statusCode));
        }

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            return response;
        }
    }

    public static MockResponse get429() {
        return new MockResponse().setResponseCode(429)
                .setBody("{\"error\":\"too_many_requests\", \"reason\":\"example reason\"}\r\n");
    }
}
