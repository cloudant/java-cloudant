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

import java.net.PasswordAuthentication;
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
    private URL url;

    //default to 6 connections
    private int maxConnections = 6;

    private URL proxyURL;
    private PasswordAuthentication proxyAuthentication = null;

    private List<HttpConnectionRequestInterceptor> requestInterceptors = new ArrayList
            <HttpConnectionRequestInterceptor>();
    private List<HttpConnectionResponseInterceptor> responseInterceptors = new ArrayList
            <HttpConnectionResponseInterceptor>();

    public CouchDbProperties(URL url) {
        this.url = url;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public URL getProxyURL() {
        return proxyURL;
    }

    public URL getCouchDbURL() {
        return url;
    }

    public CouchDbProperties setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public CouchDbProperties setProxyURL(URL proxyURL) {
        this.proxyURL = proxyURL;
        return this;
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

    public PasswordAuthentication getProxyAuthentication() {
        return proxyAuthentication;
    }

    public CouchDbProperties setProxyAuthentication(PasswordAuthentication authentication) {
        this.proxyAuthentication = authentication;
        return this;
    }
}
