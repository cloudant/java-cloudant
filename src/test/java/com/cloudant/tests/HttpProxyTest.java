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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.http.Http;
import com.cloudant.http.interceptors.ProxyAuthInterceptor;
import com.cloudant.tests.util.SimpleHttpServer;

import org.junit.ClassRule;
import org.junit.Test;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpProxyTest {

    @ClassRule
    public static SimpleHttpServer server = new SimpleHttpServer();

    /**
     * This test validates that proxy configuration is correctly used.
     * It does not test the actual function of a proxy, just that the URL and authentication
     * headers are set as expected.
     */
    @Test
    public void proxyConfiguration() throws Exception {

        //wait for the server to be ready
        server.await();

        //instantiating the client performs a single post request
        //create a client with a bogus address (TEST-NET)
        String mockProxyUser = "alpha";
        String mockProxyPass = "alphaPass";
        CloudantClient client = CloudantClientHelper.newTestAddressClient()
                .proxyURL(new URL(server.getUrl()))
                .proxyUser(mockProxyUser)
                .proxyPassword(mockProxyPass)
                .build();

        client.executeRequest(Http.GET(client.getBaseUri()));
        //if it wasn't a 20x then an exception should have been thrown by now

        //assert that the request had the expected proxy auth header
        boolean foundProxyAuthHeader = false;
        for (String line : server.getLastInputRequestLines()) {
            if (line.contains("Proxy-Authorization")) {
                foundProxyAuthHeader = true;
                Matcher m = Pattern.compile("Proxy-Authorization: Basic (.*)", Pattern
                        .CASE_INSENSITIVE).matcher(line);
                assertTrue("The Proxy-Authorization header should match the pattern", m.matches());
                assertEquals("There should be 1 group for the value", 1, m.groupCount());
                //create an interceptor with the same creds so we can easily get the expected value
                final String encodedCreds = new ProxyAuthInterceptor(mockProxyUser, mockProxyPass) {
                    String getEncodedCreds() {
                        return encodedAuth;
                    }
                }.getEncodedCreds();

                assertEquals("The encoded credentials should match the expected value",
                        encodedCreds, m.group(1));

            }
        }
        assertTrue(foundProxyAuthHeader);
    }
}
