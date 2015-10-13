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

import java.util.List;

import javax.net.ssl.SSLSocketFactory;

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
    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPassword;
    private boolean disableSSLAuthentication;
    private SSLSocketFactory authenticatedModeSSLSocketFactory;

    private List<HttpConnectionRequestInterceptor> requestInterceptors;
    private List<HttpConnectionResponseInterceptor> responseInterceptors;

    public CouchDbProperties() {
        // default constructor
    }

    public CouchDbProperties(String protocol, String host, int port,
                             String authCookie) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.authCookie = authCookie;
    }

    public CouchDbProperties(String protocol,
                             String host, int port, String username, String password) {

        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    //Retrieve user’s credentials in URI format
    public String getUserInfo() {
        if(username != null && password != null) {
            return String.format("%s:%s", username, password);
        } else {
            return null;
        }
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

    public CouchDbProperties setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }

    public CouchDbProperties setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }

    public CouchDbProperties setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
        return this;
    }

    public CouchDbProperties setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    public String getAuthCookie() {
        return authCookie;
    }

    public void setAuthCookie(String authCookie) {
        this.authCookie = authCookie;
    }

    public void clearPassword() {
        setPassword("");
        setPassword(null);
    }

    /**
     * Enables/disables hostname verification, certificate chain validation,
     * and the use of the optional
     * {@link #getAuthenticatedModeSSLSocketFactory()}.
     *
     * @param disabled set to true to disable or false to enable.
     * @return the updated {@link CouchDbProperties} object.
     * @see #isSSLAuthenticationDisabled
     */
    public CouchDbProperties disableSSLAuthentication(boolean disabled) {
        this.disableSSLAuthentication = disabled;
        return this;
    }

    /**
     * @return true if hostname verification, certificate chain validation,
     * and the use of the optional
     * {@link #getAuthenticatedModeSSLSocketFactory()} are disabled, or
     * false otherwise.
     * @see #disableSSLAuthentication(boolean)
     */
    public boolean isSSLAuthenticationDisabled() {
        return disableSSLAuthentication;
    }

    /**
     * Returns the SSLSocketFactory that gets used when connecting to
     * CouchDB over a <code>https</code> URL, when SSL authentication is
     * enabled.
     *
     * @return An SSLSocketFactory, or <code>null</code>, which stands for
     *         the default SSLSocketFactory of the JRE.
     * @see #setAuthenticatedModeSSLSocketFactory(javax.net.ssl.SSLSocketFactory)
     */
    public SSLSocketFactory getAuthenticatedModeSSLSocketFactory() {
        return authenticatedModeSSLSocketFactory;
    }

    /**
     * Specifies the SSLSocketFactory to use when connecting to CouchDB
     * over a <code>https</code> URL, when SSL authentication is enabled.
     *
     * @param factory An SSLSocketFactory, or <code>null</code> for the
     *                default SSLSocketFactory of the JRE.
     * @see #getAuthenticatedModeSSLSocketFactory()
     */
    public CouchDbProperties setAuthenticatedModeSSLSocketFactory(SSLSocketFactory factory) {
        this.authenticatedModeSSLSocketFactory = factory;
        return this;
    }

    public List<HttpConnectionRequestInterceptor> getRequestInterceptors() {
        return requestInterceptors;
    }

    public CouchDbProperties setRequestInterceptors(List<HttpConnectionRequestInterceptor> requestInterceptors) {
        this.requestInterceptors = requestInterceptors;
        return this;
    }

    public List<HttpConnectionResponseInterceptor> getResponseInterceptors() {
        return responseInterceptors;
    }

    public CouchDbProperties setResponseInterceptors(List<HttpConnectionResponseInterceptor>
                                                responseInterceptors) {
        this.responseInterceptors = responseInterceptors;
        return this;
    }
}
