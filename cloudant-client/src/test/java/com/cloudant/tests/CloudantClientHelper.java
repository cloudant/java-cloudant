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

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import okhttp3.mockwebserver.MockWebServer;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

/**
 * Created by Rhys Short on 21/05/15.
 */
public abstract class CloudantClientHelper {

    //some tests need access to the URI with user info (e.g. replication)
    public static final String SERVER_URI_WITH_USER_INFO;
    //some tests need access to the credentials (e.g. auth interceptors)
    public static final String COUCH_USERNAME;
    public static final String COUCH_PASSWORD;

    protected static final CloudantClient CLIENT_INSTANCE;

    private static final String COUCH_HOST;
    private static final String COUCH_PORT;
    private static final String HTTP_PROTOCOL;
    private static final URL SERVER_URL;
    private static final String COUCH_IAM_API_KEY;

    static {

        try {
            //a URL might be supplied, otherwise use the separate properties
            String URL = System.getProperty("test.couch.url");
            if (URL != null) {
                URL couch = new URL(URL);
                HTTP_PROTOCOL = couch.getProtocol();
                COUCH_HOST = couch.getHost();
                COUCH_PORT = (couch.getPort() < 0) ? null : Integer.toString(couch.getPort());
                String userInfo = couch.getUserInfo();
                if (userInfo != null) {
                    COUCH_USERNAME = userInfo.substring(0, userInfo.indexOf(":"));
                    COUCH_PASSWORD = userInfo.substring(userInfo.indexOf(":") + 1);
                } else {
                    COUCH_USERNAME = null;
                    COUCH_PASSWORD = null;
                }
            } else {
                COUCH_USERNAME = System.getProperty("test.couch.username");
                COUCH_PASSWORD = System.getProperty("test.couch.password");
                COUCH_HOST = System.getProperty("test.couch.host", "localhost");
                COUCH_PORT = System.getProperty("test.couch.port", "5984");
                HTTP_PROTOCOL = System.getProperty("test.couch.http", "http"); //should either be
                // http or https
            }

            COUCH_IAM_API_KEY = System.getProperty("test.couch.iam.api.key");

            //now build the URLs
            SERVER_URL = new URL(HTTP_PROTOCOL + "://"
                    + COUCH_HOST
                    + ((COUCH_PORT != null) ? ":" + COUCH_PORT : "")); //port if supplied

            // Ensure username and password are correctly URL encoded when included in the URI
            SERVER_URI_WITH_USER_INFO = HTTP_PROTOCOL + "://"
                    + ((COUCH_USERNAME != null) ? URLEncoder.encode(COUCH_USERNAME, "UTF-8") +
                    ":" + URLEncoder.encode(COUCH_PASSWORD, "UTF-8") + "@" : "") + COUCH_HOST + (
                    (COUCH_PORT != null) ? ":" + COUCH_PORT : ""); //port if supplied
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        CLIENT_INSTANCE = getClientBuilder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(3, TimeUnit.MINUTES)
                .build();
    }

    public static CloudantClient getClient() {
        return CLIENT_INSTANCE;
    }

    private static ClientBuilder testAddressClient(boolean isHttpsProtocolClient)
            throws MalformedURLException {
        URL url = null;
        if (isHttpsProtocolClient) {
            url = new URL("https://192.0.2.0");
        } else {
            url = new URL("http://192.0.2.0");
        }
        return ClientBuilder.url(url);
    }

    public static ClientBuilder newHttpsTestAddressClient() throws MalformedURLException {
        return testAddressClient(true);
    }

    public static ClientBuilder newTestAddressClient() throws MalformedURLException {
        return testAddressClient(false);
    }

    public static ClientBuilder newMockWebServerClientBuilder(MockWebServer mockServer) throws
            MalformedURLException {
        return ClientBuilder.url(mockServer.url("/").url());
    }

    public static ClientBuilder getClientBuilder() {
        return ClientBuilder.url(SERVER_URL)
                .username(COUCH_USERNAME)
                .password(COUCH_PASSWORD)
                .iamApiKey(COUCH_IAM_API_KEY);
    }

}
