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

import com.cloudant.client.api.CloudantClient;
import com.cloudant.http.Http;
import com.cloudant.http.interceptors.ProxyAuthInterceptor;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpProxyTest {

    @Rule
    public MockWebServer server = new MockWebServer();

    /**
     * This test validates that proxy configuration is correctly used.
     * It does not test the actual function of a proxy, just that the URL and authentication
     * headers are set as expected.
     */
    @Test
    public void proxyConfiguration() throws Exception {

        //mock a 200 OK
        server.enqueue(new MockResponse());

        //instantiating the client performs a single post request
        //create a client with a bogus address (TEST-NET)
        String mockProxyUser = "alpha";
        String mockProxyPass = "alphaPass";
        CloudantClient client = CloudantClientHelper.newTestAddressClient()
                .proxyURL(server.url("/").url())
                .proxyUser(mockProxyUser)
                .proxyPassword(mockProxyPass)
                .build();

        String response = client.executeRequest(Http.GET(client.getBaseUri())).responseAsString();
        assertTrue("There should be no response body on the mock response", response.isEmpty());
        //if it wasn't a 20x then an exception should have been thrown by now

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        String proxyAuthHeader = request.getHeader("Proxy-Authorization");
        assertNotNull("The Proxy-Authorization header should be present", proxyAuthHeader);

        Matcher m = Pattern.compile("Basic (.*)", Pattern
                .CASE_INSENSITIVE).matcher(proxyAuthHeader);
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
