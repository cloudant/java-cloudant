/*
 * Copyright © 2015, 2019 IBM Corp. All rights reserved.
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
import com.cloudant.tests.extensions.IamAuthCondition;

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
    //some tests need access to the credentials (e.g. auth interceptors, vcap)
    static final String SERVER_USER;
    static final String SERVER_PASSWORD;
    static final String SERVER_HOST;

    private static final CloudantClient CLIENT_INSTANCE;

    private static final String SERVER_PORT;
    private static final String SERVER_PROTOCOL;
    private static final URL SERVER_URL;

    static {

        try {
            //a URL might be supplied, otherwise use the separate properties
            String URL = System.getProperty("test.server.url");
            if (URL != null) {
                URL couch = new URL(URL);
                SERVER_PROTOCOL = couch.getProtocol();
                SERVER_HOST = couch.getHost();
                SERVER_PORT = (couch.getPort() < 0) ? null : Integer.toString(couch.getPort());
                String userInfo = couch.getUserInfo();
                if (userInfo != null) {
                    SERVER_USER = userInfo.substring(0, userInfo.indexOf(":"));
                    SERVER_PASSWORD = userInfo.substring(userInfo.indexOf(":") + 1);
                } else {
                    SERVER_USER = System.getProperty("test.server.user");
                    SERVER_PASSWORD = System.getProperty("test.server.password");
                }
            } else {
                SERVER_USER = System.getProperty("test.server.user");
                SERVER_PASSWORD = System.getProperty("test.server.password");
                SERVER_HOST = System.getProperty("test.server.host", "localhost");
                SERVER_PORT = System.getProperty("test.server.port", "5984");
                SERVER_PROTOCOL = System.getProperty("test.server.protocol", "http"); //should either be
                // http or https
            }

            //now build the URLs
            SERVER_URL = new URL(SERVER_PROTOCOL + "://"
                    + SERVER_HOST
                    + ((SERVER_PORT != null) ? ":" + SERVER_PORT : "")); //port if supplied

            // Ensure username and password are correctly URL encoded when included in the URI
            SERVER_URI_WITH_USER_INFO = SERVER_PROTOCOL + "://"
                    + ((!IamAuthCondition.IS_IAM_ENABLED && SERVER_USER != null) ?
                    URLEncoder.encode(SERVER_USER, "UTF-8") +
                    ":" + URLEncoder.encode(SERVER_PASSWORD, "UTF-8") + "@" : "") + SERVER_HOST + (
                    (SERVER_PORT != null) ? ":" + SERVER_PORT : ""); //port if supplied
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

    public static ClientBuilder newMockWebServerClientBuilder(MockWebServer mockServer) {
        return ClientBuilder.url(mockServer.url("/").url());
    }

    public static ClientBuilder getClientBuilder() {
        ClientBuilder builder = ClientBuilder.url(SERVER_URL);
        if (IamAuthCondition.IS_IAM_ENABLED) {
            builder.iamApiKey(IamAuthCondition.IAM_API_KEY);
        } else {
            builder.username(SERVER_USER)
                    .password(SERVER_PASSWORD);
        }
        return builder;
    }

    static String REP_SOURCE = null;

    /**
     * Uses the environment variable TEST_REPLICATION_SOURCE_URL
     * or system property test.replication.source.url
     * to determine the source replication URL for the named database.
     * If neither the environment variable or the property is set, defaults to
     * https://clientlibs-test.cloudant.com/ as the prefix.
     * Note the environment variable takes precedence over the system property.
     *
     * @param dbName
     * @return a URL to use as a source for replication
     */
    public static String getReplicationSourceUrl(String dbName) {
        if (REP_SOURCE == null) {
            REP_SOURCE = System.getProperty("test.replication.source.url", "https://clientlibs-test.cloudant.com/");
            if (!REP_SOURCE.endsWith("/")) {
                REP_SOURCE = REP_SOURCE + "/";
            }
        }
        return REP_SOURCE + dbName;
    }

}
