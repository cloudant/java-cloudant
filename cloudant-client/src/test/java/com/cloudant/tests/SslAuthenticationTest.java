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

package com.cloudant.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.cloudant.tests.util.HttpFactoryParameterizedTest;
import com.cloudant.tests.util.MockWebServerResources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.api.function.Executable;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

@ExtendWith(SslAuthenticationTest.ParameterProvider.class)
public class SslAuthenticationTest extends HttpFactoryParameterizedTest {

    static class ParameterProvider implements TestTemplateInvocationContextProvider {
        @Override
        public boolean supportsTestTemplate(ExtensionContext context) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts
                (ExtensionContext context) {
            return Stream.of(invocationContext(false),
                    invocationContext(true));
        }

        public static TestTemplateInvocationContext invocationContext(final boolean okUsable) {
            return new TestTemplateInvocationContext() {
                @Override
                public String getDisplayName(int invocationIndex) {
                    return String.format("okUsable:%s", okUsable);
                }

                @Override
                public List<Extension> getAdditionalExtensions() {
                    return Collections.<Extension>singletonList(new ParameterResolver() {
                        @Override
                        public boolean supportsParameter(ParameterContext parameterContext,
                                                         ExtensionContext extensionContext) {
                            switch (parameterContext.getIndex()) {
                                case 0:
                                    return parameterContext.getParameter().getType().equals
                                            (boolean.class);
                            }
                            return false;
                        }

                        @Override
                        public Object resolveParameter(ParameterContext parameterContext,
                                                       ExtensionContext extensionContext) {
                            switch (parameterContext.getIndex()) {
                                case 0:
                                    return okUsable;
                            }
                            return null;
                        }
                    });
                }
            };
        }
    }


    @RegisterExtension
    public static MockWebServerExtension mockWebServerExt = new MockWebServerExtension();

    protected MockWebServer server;

    @BeforeEach
    public void beforeEach() {
        server = mockWebServerExt.get();
        server.useHttps(MockWebServerResources.getSSLSocketFactory(), false);
    }

    /**
     * Check the exception chain is as expected when the SSL host name authentication fails
     * to be sure we got a CouchDbException for the reason we expect.
     *
     * @param e the exception.
     */
    private static void validateClientAuthenticationException(CouchDbException e) {
        assertNotNull(e, "Expected CouchDbException but got null");
        Throwable t = e.getCause();
        assertTrue(t instanceof SSLHandshakeException, "Expected SSLHandshakeException caused by " +
                "client certificate check but got " + t.getClass());
    }

    /**
     * Connect to the local simple https server with SSL authentication disabled.
     */
    @TestTemplate
    public void localSslAuthenticationDisabled() throws Exception {

        // Build a client that connects to the mock server with SSL authentication disabled
        CloudantClient dbClient = CloudantClientHelper.newMockWebServerClientBuilder(server)
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
    @TestTemplate
    public void localSslAuthenticationEnabled() throws Exception {

        CouchDbException thrownException = null;
        try {
            CloudantClient dbClient = CloudantClientHelper.newMockWebServerClientBuilder(server)
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
    @TestTemplate
    @RequiresCloudantService
    public void remoteSslAuthenticationEnabledTest() {

        CloudantClient dbClient = CloudantClientHelper.getClientBuilder().build();

        // Make an arbitrary connection to the DB.
        dbClient.getAllDbs();

        // Test is successful if no exception is thrown, so no explicit check is needed.
    }

    /**
     * Connect to the remote Cloudant server with SSL Authentication disabled.
     */
    @TestTemplate
    @RequiresCloudantService
    public void remoteSslAuthenticationDisabledTest() {

        CloudantClient dbClient = CloudantClientHelper.getClientBuilder()
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
    @TestTemplate
    public void testCustomSSLFactorySSLAuthDisabled() {
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() throws Throwable {

                CloudantClient dbClient = CloudantClientHelper.getClientBuilder()
                        .customSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault())

                        .disableSSLAuthentication()
                        .build();
            }
        });
    }

    /**
     * Assert that building a client with SSL Authentication disabled first, then setting
     * a custom SSL factory will throw an IllegalStateException.
     */
    @TestTemplate
    public void testSSLAuthDisabledWithCustomSSLFactory() {
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() throws Throwable {

                CloudantClient dbClient = CloudantClientHelper.getClientBuilder()
                        .disableSSLAuthentication()
                        .customSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault())
                        .build();
            }
        });
    }

    /**
     * Repeat the localSSLAuthenticationDisabled, but with the cookie auth enabled.
     * This test validates that the SSL settings also get applied to the cookie interceptor.
     */
    @TestTemplate
    public void localSSLAuthenticationDisabledWithCookieAuth() throws Exception {

        // Mock up an OK cookie response then an OK response for the getAllDbs()
        server.enqueue(MockWebServerResources.OK_COOKIE);
        server.enqueue(new MockResponse()); //OK 200

        // Use a username and password to enable the cookie auth interceptor
        CloudantClient dbClient = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .username("user")
                .password("password")
                .disableSSLAuthentication()
                .build();

        dbClient.getAllDbs();
    }

    /**
     * Repeat the localSSLAuthenticationEnabled, but with the cookie auth enabled.
     * This test validates that the SSL settings also get applied to the cookie interceptor.
     */
    @TestTemplate
    public void localSSLAuthenticationEnabledWithCookieAuth() throws Exception {

        // Mock up an OK cookie response then an OK response for the getAllDbs()
        server.enqueue(MockWebServerResources.OK_COOKIE);
        server.enqueue(new MockResponse()); //OK 200

        // Use a username and password to enable the cookie auth interceptor
        CloudantClient dbClient = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .username("user")
                .password("password")
                .build();

        try {
            dbClient.getAllDbs();
            fail("The SSL authentication failure should result in a CouchDbException");
        } catch (CouchDbException e) {
            validateClientAuthenticationException(e);
        }
    }

}

