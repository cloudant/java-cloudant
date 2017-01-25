/*
 * Copyright © 2015, 2016 IBM Corp. All rights reserved.
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

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class MockWebServerResources {

    // Fields for a default timeout of 10 seconds to avoid tests running on too long if something
    // isn't correct with the mock web server.
    public static final long TIMEOUT = 10l;
    public static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    public static final String EXPECTED_OK_COOKIE =
            "AuthSession=\"a2ltc3RlYmVsOjUxMzRBQTUzOtiY2_IDUIdsTJEVNEjObAbyhrgz\"";
    /*
     * Note explicitly declares domain .local to workaround
     * https://bugs.openjdk.java.net/browse/JDK-7169142
     */
    public static final String COOKIE_PROPS = "; Version=1; Path=/; HttpOnly; Domain=.local";

    /**
     * A mock cookie response that is OK
     */
    public static final MockResponse OK_COOKIE = new MockResponse()
            .setResponseCode(200)
            .addHeader("Set-Cookie",
                    EXPECTED_OK_COOKIE + COOKIE_PROPS)
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

    private static SSLContext sslContext = null;

    public static SSLContext getSSLContext() {
        if (sslContext != null) return sslContext;
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD
                    .toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            kmf.init(keystore, KEY_PASSWORD.toCharArray());
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing test SSLSocketFactory", e);
            return null;
        }
    }

    public static SSLSocketFactory getSSLSocketFactory() {
        return (getSSLContext() != null) ? getSSLContext().getSocketFactory() : null;
    }

    /**
     * Utility to call takeRequest on a MockWebServer, but using the default timeouts specified in
     * this class to avoid waiting to infinity or a global test timeout.
     *
     * @param mws the mock web server to get the request from
     * @return the recorded request
     * @throws InterruptedException if the wait was interrupted
     */
    public static RecordedRequest takeRequestWithTimeout(MockWebServer mws) throws
            InterruptedException {
        return mws.takeRequest(TIMEOUT, TIMEOUT_UNIT);
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
