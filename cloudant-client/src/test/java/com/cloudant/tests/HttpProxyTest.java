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

package com.cloudant.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.http.Http;
import com.cloudant.http.internal.ok.OkHelper;
import com.cloudant.tests.util.MockWebServerResources;
import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.ProxyAuthenticator;
import org.littleshoot.proxy.SslEngineSource;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;


@RunWith(Parameterized.class)
public class HttpProxyTest {

    @Parameterized.Parameters(name = "okhttp: {0}; secure proxy: {1}; https server: {2}; proxy " +
            "auth: {3}")
    public static List<Object[]> combinations() {
        boolean[] tf = new boolean[]{true, false};
        List<Object[]> combos = new ArrayList<Object[]>();
        for (boolean okUsable : tf) {
            for (boolean secureProxy : new boolean[]{false}) {
                // AFAICT there is no way to instruct HttpURLConnection to connect via SSL to a
                // proxy server - so for now we just test an unencrypted proxy.
                // Note this is independent of the SSL tunnelling to an https server and influences
                // only requests between client and proxy. With an https server client requests are
                // tunnelled directly to the https server, other than the original HTTP CONNECT
                // request. The reason for using a SSL proxy would be to encrypt proxy auth creds
                // but it appears this scenario is not readily supported.
                for (boolean httpsServer : tf) {
                    for (boolean proxyAuth : tf) {
                        combos.add(new Object[]{okUsable, secureProxy, httpsServer, proxyAuth});
                    }
                }
            }
        }
        return combos;
    }

    /**
     * A parameter governing whether to allow okhttp or not. This lets us exercise both
     * HttpURLConnection types in these tests.
     */
    @Parameterized.Parameter(0)
    public boolean okUsable;

    @Before
    public void changeHttpConnectionFactory() throws Exception {
        if (!okUsable) {
            // New up the mock that will stop okhttp's factory being used
            new HttpTest.OkHelperMock();
        }
        // Verify that we are getting the behaviour we expect.
        assertEquals("The OK usable value was not what was expected for the test parameter.",
                okUsable, OkHelper.isOkUsable());
    }

    @Parameterized.Parameter(1)
    public boolean secureProxy;

    @Parameterized.Parameter(2)
    public boolean useHttpsServer;

    @Parameterized.Parameter(3)
    public boolean useProxyAuth;

    @Rule
    public MockWebServer server = new MockWebServer();

    HttpProxyServer proxy;
    String mockProxyUser = "alpha";
    String mockProxyPass = "alphaPass";

    /**
     * Enables https on the mock web server receiving our requests if useHttpsServer is true.
     *
     * @throws Exception
     */
    @Before
    public void setupMockServerSSLIfNeeded() throws Exception {
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
    @Before
    public void setupAndStartProxy() throws Exception {

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

        if (secureProxy) {
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
    @After
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
    @Before
    public void setAuthenticatorIfNeeded() {
        // If we are not using okhttp and we have an https server and a proxy that needs auth then
        // we need to set the default Authenticator
        if (useProxyAuth && useHttpsServer && !okUsable) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mockProxyUser, mockProxyPass.toCharArray());
                }
            });
        }
    }

    /**
     * Reset the Authenticator after the test.
     *
     * @see #setAuthenticatorIfNeeded()
     */
    @After
    public void resetAuthenticator() {
        // If we are not using okhttp and we have an https server and a proxy that needs auth then
        // we need to set the default Authenticator
        if (useProxyAuth && useHttpsServer && !okUsable) {
            Authenticator.setDefault(null);
        }
    }

    /**
     * This test validates that a request can successfully traverse a proxy to our mock server.
     */
    @Test
    public void proxiedRequest() throws Exception {

        //mock a 200 OK
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse();
            }
        });

        InetSocketAddress address = proxy.getListenAddress();
        URL proxyUrl = new URL((secureProxy) ? "https" : "http", address.getHostName(), address
                .getPort(), "/");
        ClientBuilder builder = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .proxyURL(proxyUrl);
        if (useProxyAuth) {
            builder.proxyUser(mockProxyUser).proxyPassword(mockProxyPass);
        }

        // We don't use SSL authentication for this test
        CloudantClient client = builder.disableSSLAuthentication().build();

        String response = client.executeRequest(Http.GET(client.getBaseUri())).responseAsString();

        assertTrue("There should be no response body on the mock response", response.isEmpty());
        //if it wasn't a 20x then an exception should have been thrown by now

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request);
    }
}
