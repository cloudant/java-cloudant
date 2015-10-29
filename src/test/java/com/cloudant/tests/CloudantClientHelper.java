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

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.model.ConnectOptions;
import com.cloudant.http.interceptors.TimeoutCustomizationInterceptor;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by Rhys Short on 21/05/15.
 */
public abstract class CloudantClientHelper {

    private static final String COUCH_HOST;
    private static final String COUCH_PORT;
    private static final String HTTP_PROTOCOL;

    //some tests need access to the creds or URIs
    public static final String COUCH_USERNAME;
    public static final String COUCH_PASSWORD;
    public static final String SERVER_URI;
    public static final String SERVER_URI_WITH_USER_INFO;

    protected static final CloudantClient CLIENT_INSTANCE;

    static {

        try {
            //a URI might be supplied, otherwise use the separate properties
            String URI = System.getProperty("test.couch.uri");
            if (URI != null) {
                URL couch = new URI(URI).toURL();
                HTTP_PROTOCOL = couch.getProtocol();
                COUCH_HOST = couch.getHost();
                COUCH_PORT = (couch.getPort() < 0) ? Integer.toString(couch.getPort()) : null;
                String userInfo = couch.getUserInfo();
                if (userInfo != null) {
                    COUCH_USERNAME = userInfo.substring(0, userInfo.indexOf(":"));
                    COUCH_PASSWORD = userInfo.substring(userInfo.indexOf(":"));
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

            //now build the URLs
            SERVER_URI = HTTP_PROTOCOL + "://"
                    + COUCH_HOST
                    + ((COUCH_PORT != null) ? ":" + COUCH_PORT : ""); //port if supplied

            SERVER_URI_WITH_USER_INFO = HTTP_PROTOCOL + "://"
                    + ((COUCH_USERNAME != null) ? COUCH_USERNAME + ":" + COUCH_PASSWORD + "@" : "")
                    + COUCH_HOST
                    + ((COUCH_PORT != null) ? ":" + COUCH_PORT : ""); //port if supplied
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        CLIENT_INSTANCE = new CloudantClient(SERVER_URI.toString(),
                COUCH_USERNAME, COUCH_PASSWORD, new ConnectOptions().setConnectionTimeout(new
                TimeoutCustomizationInterceptor.TimeoutOption(1, TimeUnit.MINUTES))
                .setReadTimeout(new TimeoutCustomizationInterceptor.TimeoutOption(3, TimeUnit
                        .MINUTES)));
    }

    public static CloudantClient getClient() {
        return CLIENT_INSTANCE;
    }

}
