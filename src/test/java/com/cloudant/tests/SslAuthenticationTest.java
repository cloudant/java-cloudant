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

package com.cloudant.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.MockWebServerResource;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

public class SslAuthenticationTest {

    @ClassRule
    public static CloudantClientResource dbClientResource = new CloudantClientResource();
    private static CloudantClient dbClient = dbClientResource.get();

    @Rule
    public MockWebServerResource mockServerResource = new MockWebServerResource(true);

    private MockWebServer server;

    @Before
    public void getMockWebServer() {
        server = mockServerResource.getServer();
    }

    /**
     * Check the exception chain is as expected when the SSL host name authentication fails
     * to be sure we got a CouchDbException for the reason we expect.
     *
     * @param e the exception.
     */
    private static void validateClientAuthenticationException(CouchDbException e) {
        assertNotNull("Expected CouchDbException but got null", e);
        Throwable t = e.getCause();
        assertTrue("Expected SSLHandshakeException caused by client certificate check but got " +
                        t.getClass(),
                t instanceof SSLHandshakeException);
    }

    /**
     * Connect to the local simple https server with SSL authentication disabled.
     */
    @Test
    public void localSslAuthenticationDisabled() throws Exception {

        // Build a client that connects to the mock server with SSL authentication disabled
        dbClient = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .disableSSLAuthentication()
                .build();

        // Queue a 200 OK response
        server.enqueue(new MockResponse());

        // Make an arbitrary connection to the DB.
        dbClient.getAllDbs();

        // Test is successful if no exception is thrown, so no explicit check is needed.
    }

    /**
     * Connect to the local simple https server with SSL authentication enabled explicitly.
     * This should throw an exception because the SSL authentication fails.
     */
    @Test
    public void localSslAuthenticationEnabled() throws Exception {

        CouchDbException thrownException = null;
        try {
            dbClient = CloudantClientHelper.newMockWebServerClientBuilder(server)
                    .build();

            // Queue a 200 OK response
            server.enqueue(new MockResponse());

            // Make an arbitrary connection to the DB.
            dbClient.getAllDbs();
        } catch (CouchDbException e) {
            thrownException = e;
        }
        validateClientAuthenticationException(thrownException);
    }

    /**
     * Connect to the remote Cloudant server with SSL Authentication enabled.
     * This shouldn't throw an exception as the Cloudant server has a valid
     * SSL certificate, so should be authenticated.
     */
    @Test
    @Category(RequiresCloudantService.class)
    public void remoteSslAuthenticationEnabledTest() {

        dbClient = CloudantClientHelper.getClientBuilder().build();

        // Make an arbitrary connection to the DB.
        dbClient.getAllDbs();

        // Test is successful if no exception is thrown, so no explicit check is needed.
    }

    /**
     * Connect to the remote Cloudant server with SSL Authentication disabled.
     */
    @Test
    @Category(RequiresCloudantService.class)
    public void remoteSslAuthenticationDisabledTest() {

        dbClient = CloudantClientHelper.getClientBuilder()
                .disableSSLAuthentication()
                .build();

        // Make an arbitrary connection to the DB.
        dbClient.getAllDbs();

        // Test is successful if no exception is thrown, so no explicit check is needed.
    }

    /**
     * Assert that building a client with a custom SSL factory first, then setting the
     * SSL Authentication disabled will throw an IllegalStateException.
     */
    @Test(expected = IllegalStateException.class)
    public void testCustomSSLFactorySSLAuthDisabled() {

        dbClient = CloudantClientHelper.getClientBuilder()
                .customSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault())
                .disableSSLAuthentication()
                .build();
    }

    /**
     * Assert that building a client with SSL Authentication disabled first, then setting
     * a custom SSL factory will throw an IllegalStateException.
     */
    @Test(expected = IllegalStateException.class)
    public void testSSLAuthDisabledWithCustomSSLFactory() {

        dbClient = CloudantClientHelper.getClientBuilder()
                .disableSSLAuthentication()
                .customSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault())
                .build();

    }

    /**
     * Repeat the localSSLAuthenticationDisabled, but with the cookie auth enabled.
     * This test validates that the SSL settings also get applied to the cookie interceptor.
     */
    @Test
    public void localSSLAuthenticationDisabledWithCookieAuth() throws Exception {

        // Mock up an OK cookie response then an OK response for the getAllDbs()
        server.enqueue(MockWebServerResource.OK_COOKIE);
        server.enqueue(new MockResponse()); //OK 200

        // Use a username and password to enable the cookie auth interceptor
        dbClient = CloudantClientHelper.newMockWebServerClientBuilder(server).username("user")
                .password("password")
                .disableSSLAuthentication()
                .build();

        dbClient.getAllDbs();
    }

    /**
     * Repeat the localSSLAuthenticationEnabled, but with the cookie auth enabled.
     * This test validates that the SSL settings also get applied to the cookie interceptor.
     */
    @Test
    public void localSSLAuthenticationEnabledWithCookieAuth() throws Exception {

        // Mock up an OK cookie response then an OK response for the getAllDbs()
        server.enqueue(MockWebServerResource.OK_COOKIE);
        server.enqueue(new MockResponse()); //OK 200

        // Use a username and password to enable the cookie auth interceptor
        dbClient = CloudantClientHelper.newMockWebServerClientBuilder(server).username("user")
                .password("password")
                .build();

        try {
            dbClient.getAllDbs();
            fail("The SSL authentication failure should result in a CouchDbException");
        } catch(CouchDbException e) {
            validateClientAuthenticationException(e);
        }
    }
}

