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

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.rules.ExternalResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class MockWebServerResource extends ExternalResource {

    /**
     * A mock cookie response that is OK
     */
    public static final MockResponse OK_COOKIE = new MockResponse()
            .setResponseCode(200)
            .addHeader("Set-Cookie",
                    "AuthSession=\"a2ltc3RlYmVsOjUxMzRBQTUzOtiY2_IDUIdsTJEVNEjObAbyhrgz\";")
            .setBody("{\"ok\":true,\"name\":\"mockUser\",\"roles\":[]}");

    private static final Logger logger = Logger.getLogger(MockWebServerResource.class.getName());

    //Keystore information for https
    private static String KEYSTORE_FILE = "src/test/resources/SslAuthenticationTest.keystore";
    private static String KEYSTORE_PASSWORD = "password";
    private static String KEY_PASSWORD = "password";

    private final MockWebServer mockWebServer = new MockWebServer();

    public MockWebServerResource() {
        this(false);
    }

    public MockWebServerResource(boolean useHttps) {
        if (useHttps) {
            mockWebServer.useHttps(getSSLSocketFactory(), false);
        }
    }

    @Override
    protected void before() throws Throwable {
        mockWebServer.start();
    }

    @Override
    protected void after() {
        try {
            mockWebServer.shutdown();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOExeption during mock server shutdown", e);
        }
    }

    public MockWebServer getServer() {
        return mockWebServer;
    }

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
            logger.log(Level.SEVERE, "Error initializing SimpleHttpsServer", e);
            return null;
        }
    }
}
