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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.http.Http;
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.cloudant.tests.util.HttpFactoryParameterizedTest;
import com.cloudant.tests.util.MockWebServerResources;

import org.junit.jupiter.api.AfterEach;
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
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.SslEngineSource;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.net.ssl.SSLEngine;

@ExtendWith(HttpProxyTest.ParameterProvider.class)
public class HttpProxyTest extends HttpFactoryParameterizedTest {

    static class ParameterProvider implements TestTemplateInvocationContextProvider {
        @Override
        public boolean supportsTestTemplate(ExtensionContext context) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts
                (ExtensionContext context) {

            // AFAICT there is no way to instruct HttpURLConnection to connect via SSL to a
            // proxy server - so for now we just test an unencrypted proxy.
            // Note this is independent of the SSL tunnelling to an https server and influences
            // only requests between client and proxy. With an https server client requests are
            // tunnelled directly to the https server, other than the original HTTP CONNECT
            // request. The reason for using a SSL proxy would be to encrypt proxy auth creds
            // but it appears this scenario is not readily supported.
            // TODO is better to list all of these explicitly or not?
            return Stream.of(
                    invocationContext(true, false, true, true),
                    invocationContext(true, false, true, false),
                    invocationContext(true, false, false, true),
                    // see also https://github.com/cloudant/java-cloudant/issues/423 - these
                    // tests current fail regardless of ordering
                    //invocationContext(true, false, false, false),
                    //invocationContext(false, false, true, true),
                    invocationContext(false, false, true, false),
                    invocationContext(false, false, false, true),
                    invocationContext(false, false, false, false));
        }

        public static TestTemplateInvocationContext invocationContext(final boolean okUsable,
                                                                      final boolean useSecureProxy,
                                                                      final boolean useHttpsServer,
                                                                      final boolean useProxyAuth) {
            return new TestTemplateInvocationContext() {
                @Override
                public String getDisplayName(int invocationIndex) {
                    return String.format("okhttp: %s; secure proxy: %s; https server %s: proxy " +
                                    "auth: %s",
                            okUsable, useSecureProxy, useHttpsServer, useProxyAuth);
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
                                case 1:
                                    return parameterContext.getParameter().getType().equals
                                            (boolean.class);
                                case 2:
                                    return parameterContext.getParameter().getType().equals
                                            (boolean.class);
                                case 3:
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
                                case 1:
                                    return useSecureProxy;
                                case 2:
                                    return useHttpsServer;
                                case 3:
                                    return useProxyAuth;
                            }
                            return null;
                        }
                    });
                }
            };
        }
    }

    @RegisterExtension
    public MockWebServerExtension serverExt = new MockWebServerExtension();
    public MockWebServer server;

    HttpProxyServer proxy;
    String mockProxyUser = "alpha";
    String mockProxyPass = "alphaPass";

    // Unfortunately getting the System property jdk.http.auth.tunneling.disabledSchemes doesn't
    // actually give us the default value (it returns null so the property being unset enables some
    // default behaviour). It is not possible to unset the value after we have set it so the best we
    // can do is set it back to a value we think is appropirate. According to release notes for the
    // fix for CVE-2016-5597 the Basic scheme is disabled so we'll reset to that value.
    private final String defaultDisabledList = "Basic";

    /**
     * Enables https on the mock web server receiving our requests if useHttpsServer is true.
     *
     * @throws Exception
     */
    @BeforeEach
    public void setupMockServerSSLIfNeeded(final boolean okUsable,
                                           final boolean useSecureProxy,
                                           final boolean useHttpsServer,
                                           final boolean useProxyAuth) throws Exception {
        server = serverExt.get();
        if (useHttpsServer) {
            server.useHttps(MockWebServerResources.getSSLSocketFactory(), false);
        }
    }

    /**
     * Starts a littleproxy instance that will proxy the requests. Applies appropriate configuration
     * options to the proxy based on the test parameters.
     *
     * @throws Exception
     */
    @BeforeEach
    public void setupAndStartProxy(final boolean okUsable,
                                   final boolean useSecureProxy,
                                   final boolean useHttpsServer,
                                   final boolean useProxyAuth) throws Exception {

        HttpProxyServerBootstrap proxyBoostrap = DefaultHttpProxyServer.bootstrap()
                .withAllowLocalOnly(true) // only run on localhost
                .withAuthenticateSslClients(false); // we aren't checking client certs

        if (useProxyAuth) {
            // check the proxy user and password
            ProxyAuthenticator pa = new ProxyAuthenticator() {
                @Override
                public boolean authenticate(String userName, String password) {
                    return (mockProxyUser.equals(userName) && mockProxyPass.equals(password));
                }

                @Override
                public String getRealm() {
                    return null;
                }
            };
            proxyBoostrap.withProxyAuthenticator(pa);
        }

        if (useSecureProxy) {
            proxyBoostrap.withSslEngineSource(new SslEngineSource() {
                @Override
                public SSLEngine newSslEngine() {
                    return MockWebServerResources.getSSLContext().createSSLEngine();
                }

                @Override
                public SSLEngine newSslEngine(String peerHost, int peerPort) {
                    return MockWebServerResources.getSSLContext().createSSLEngine(peerHost,
                            peerPort);
                }
            });
        }

        // Start the proxy server
        proxy = proxyBoostrap.start();
    }

    /**
     * Shutdown the proxy server at the end of the test.
     *
     * @throws Exception
     */
    @AfterEach
    public void shutdownProxy() throws Exception {
        proxy.stop();
    }

    /**
     * The Proxy-Authorization header that we add to requests gets encrypted in the case of a SSL
     * tunnel connection to a HTTPS server. The default HttpURLConnection does not add the header
     * to the CONNECT request so in that case we require an Authenticator to provide credentials
     * to the proxy server. The client code does not set an Authenticator automatically because
     * it is a global default so it must be set by the application developer or system
     * administrators in accordance with their environment. For the purposes of this test we can
     * add and remove the Authenticator before and after testing.
     */
    @BeforeEach
    public void setAuthenticatorIfNeeded(final boolean okUsable,
                                         final boolean useSecureProxy,
                                         final boolean useHttpsServer,
                                         final boolean useProxyAuth) {
        // If we are not using okhttp and we have an https server and a proxy that needs auth then
        // we need to set the default Authenticator
        if (useProxyAuth && useHttpsServer && !isOkUsable) {
            // Allow https tunnelling through http proxy for the duration of the test
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY) {
                        return new PasswordAuthentication(mockProxyUser, mockProxyPass
                                .toCharArray());
                    } else {
                        return null;
                    }
                }
            });
        }
    }

    /**
     * Reset the Authenticator after the test.
     *
     * @see #setAuthenticatorIfNeeded(boolean, boolean, boolean, boolean)
     */
    @AfterEach
    public void resetAuthenticator(final boolean okUsable,
                                   final boolean useSecureProxy,
                                   final boolean useHttpsServer,
                                   final boolean useProxyAuth) {
        // If we are not using okhttp and we have an https server and a proxy that needs auth then
        // we need to set the default Authenticator
        if (useProxyAuth && useHttpsServer && !isOkUsable) {
            Authenticator.setDefault(null);
            // Reset the disabled schemes property
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", defaultDisabledList);
        }
    }

    /**
     * This test validates that a request can successfully traverse a proxy to our mock server.
     */
    @TestTemplate
    public void proxiedRequest(final boolean okUsable,
                               final boolean useSecureProxy,
                               final boolean useHttpsServer,
                               final boolean useProxyAuth) throws Exception {

        //mock a 200 OK
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse();
            }
        });

        InetSocketAddress address = proxy.getListenAddress();
        URL proxyUrl = new URL((useSecureProxy) ? "https" : "http", address.getHostName(), address
                .getPort(), "/");
        ClientBuilder builder = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .proxyURL(proxyUrl);
        if (useProxyAuth) {
            builder.proxyUser(mockProxyUser).proxyPassword(mockProxyPass);
        }

        // We don't use SSL authentication for this test
        CloudantClient client = builder.disableSSLAuthentication().build();

        String response = client.executeRequest(Http.GET(client.getBaseUri())).responseAsString();

        assertTrue(response.isEmpty(), "There should be no response body on the mock response");
        //if it wasn't a 20x then an exception should have been thrown by now

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request);
    }
}
