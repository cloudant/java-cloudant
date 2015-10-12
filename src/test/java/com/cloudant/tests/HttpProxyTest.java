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

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.createPost;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.model.ConnectOptions;
import com.cloudant.http.interceptors.ProxyAuthInterceptor;
import com.cloudant.tests.util.SingleRequestHttpServer;

import org.junit.Test;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpProxyTest {

    /**
     * This test validates that proxy configuration is correctly used.
     * It does not test the actual function of a proxy, just that the URL and authentication
     * headers are set as expected.
     */
    @Test
    public void proxyConfiguration() throws Exception {
        int serverPort = 54321;
        SingleRequestHttpServer server = SingleRequestHttpServer.startServer(serverPort);
        //wait for the server to be ready
        server.waitForServer();

        //instantiating the client performs a single post request
        //create a client with a bogus address (TEST-NET)
        String mockProxyUser = "alpha";
        String mockProxyPass = "alphaPass";
        CloudantClient client = new CloudantClient("http://192.0.2.0", "", "", new ConnectOptions()
                .setProxyURL(new URL("http://localhost:" + serverPort))
                .setProxyUser(mockProxyUser)
                .setProxyPassword(mockProxyPass));

        client.executeRequest(createPost(client.getBaseUri(), null, "application/json"));
        //if it wasn't a 20x then an exception should have been thrown by now

        //assert that the request had the expected proxy auth header
        boolean foundProxyAuthHeader = false;
        for (String line : server.getRequestInput()) {
            if (line.contains("Proxy-Authorization")) {
                foundProxyAuthHeader = true;
                Matcher m = Pattern.compile("Proxy-Authorization\\s*:\\s*(.*)", Pattern
                        .CASE_INSENSITIVE).matcher(line);
                assertTrue("The Proxy-Authorization header should match the pattern", m.matches());
                assertEquals("There should be 2 groups, one for the header key and one for the " +
                        "value", 2, m.groupCount());

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
    }
}
