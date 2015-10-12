/*
 * Copyright (C) 2011 lightcouch.org
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

package com.cloudant.client.org.lightcouch;

import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents configuration properties for connecting to CouchDB.
 *
 * @author Ahmed Yehia
 * @author Daan van Berkel
 */
public class CouchDbProperties {

    // required
    private String protocol;
    private String host;
    private String path;
    private int port;
    private String username;
    private String password;

    private String authCookie;

    // optional, default to 5 minutes
    private int socketTimeout = 300000;
    private int connectionTimeout = 300000;
    //default to 6 connections
    private int maxConnections = 6;
    private URL proxyURL;

    private List<HttpConnectionRequestInterceptor> requestInterceptors = new ArrayList
            <HttpConnectionRequestInterceptor>();
    private List<HttpConnectionResponseInterceptor> responseInterceptors = new ArrayList
            <HttpConnectionResponseInterceptor>();

    public CouchDbProperties() {
        // default constructor
    }

    public CouchDbProperties(String protocol, String host, int port) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public int getPort() {
        return port;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public URL getProxyURL() {
        return proxyURL;
    }

    public CouchDbProperties setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public CouchDbProperties setHost(String host) {
        this.host = host;
        return this;
    }

    public CouchDbProperties setPath(String path) {
        this.path = path;
        return this;
    }

    public CouchDbProperties setPort(int port) {
        this.port = port;
        return this;
    }

    public CouchDbProperties setUsername(String username) {
        this.username = username;
        return this;
    }

    public CouchDbProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public CouchDbProperties setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public CouchDbProperties setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public CouchDbProperties setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public CouchDbProperties setProxyURL(URL proxyURL) {
        this.proxyURL = proxyURL;
        return this;
    }

    public void clearPassword() {
        setPassword("");
        setPassword(null);
    }

    public List<HttpConnectionRequestInterceptor> getRequestInterceptors() {
        return requestInterceptors;
    }

    public CouchDbProperties addRequestInterceptors(HttpConnectionRequestInterceptor...
                                                            requestInterceptors) {
        this.requestInterceptors.addAll(Arrays.asList(requestInterceptors));
        return this;
    }

    public List<HttpConnectionResponseInterceptor> getResponseInterceptors() {
        return responseInterceptors;
    }

    public CouchDbProperties addResponseInterceptors(HttpConnectionResponseInterceptor
                                                             responseInterceptors) {
        this.responseInterceptors.addAll(Arrays.asList(responseInterceptors));
        return this;
    }
}
