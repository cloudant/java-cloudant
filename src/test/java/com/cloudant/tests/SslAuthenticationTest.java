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

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.SimpleHttpsServer;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

public class SslAuthenticationTest {

    @ClassRule
    public static CloudantClientResource dbClientResource = new CloudantClientResource();
    private static CloudantClient dbClient = dbClientResource.get();

    @ClassRule
    public static SimpleHttpsServer server = new SimpleHttpsServer(){

        //for these tests we only want a 200
        @Override
        protected void serverAction(InputStream is, OutputStream os) throws IOException {
            try {
                super.writeOK(os);
            } catch(SSLHandshakeException e) {
                //we don't have a valid cert chain here, so we suppress this exception
                //we only need proof that we attempted to connect with the right options
            }
        }
    };

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

        server.await();

        dbClient = CloudantClientHelper.newSimpleHttpServerClient(server)
                .disableSSLAuthentication()
                .build();


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

        server.await();

        CouchDbException thrownException = null;
        try {
            dbClient = CloudantClientHelper.newSimpleHttpServerClient(server)
                    .build();

            // Make an arbitrary connection to the DB.
            dbClient.getAllDbs();
        } catch (CouchDbException e) {
            thrownException = e;
        }
        validateClientAuthenticationException(thrownException);
    }

    /**
     * Connect to the local simple https server with SSL authentication enabled implicitly.
     * This should throw an exception because the SSL authentication fails.
     */
    @Test
    public void localSslAuthenticationEnabledDefault() throws Exception {

        server.await();

        CouchDbException thrownException = null;
        try {
            dbClient = CloudantClientHelper.newSimpleHttpServerClient(server)
                    .build();

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

        dbClient = CloudantClientHelper.getClientBuilder()
                .disableSSLAuthentication()
                .build();

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

}

