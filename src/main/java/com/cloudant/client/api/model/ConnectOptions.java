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

package com.cloudant.client.api.model;

import javax.net.ssl.SSLSocketFactory;

/**
 * Represents optional configuration properties for connecting to CloudantDB.
 *
 * @author Ganesh K Choudhary
 */
public class ConnectOptions {

    private int socketTimeout;
    private int connectionTimeout;
    private int maxConnections;

    private String proxyHost;
    private int proxyPort;
    private String proxyUser;
    private String proxyPassword;
    private boolean isSSLAuthenticationDisabled;
    private SSLSocketFactory authenticatedModeSSLSocketFactory;

    public ConnectOptions() {
        // default constructor
    }

    public ConnectOptions setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
        return this;
    }

    public ConnectOptions setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public ConnectOptions setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public ConnectOptions setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        return this;
    }

    public ConnectOptions setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        return this;
    }

    public ConnectOptions setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
        return this;
    }

    public ConnectOptions setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    /**
     * Sets whether hostname verification, certificate chain validation,
     * and the use of the optional
     * {@link #getAuthenticatedModeSSLSocketFactory()} should be disabled.
     *
     * @param disabled set to true to disable or false to enable.
     * @return the updated {@link ConnectOptions} object.
     * @see #isSSLAuthenticationDisabled
     */
    public ConnectOptions setSSLAuthenticationDisabled(boolean disabled) {
        this.isSSLAuthenticationDisabled = disabled;
        return this;
    }

    /**
     * Specifies the SSLSocketFactory to use when connecting to CloudantDB
     * over a <code>https</code> URL, when SSL authentication is enabled.
     *
     * @param factory An SSLSocketFactory, or <code>null</code> for the
     *                default SSLSocketFactory of the JRE.
     * @see #getAuthenticatedModeSSLSocketFactory()
     */
    public ConnectOptions setAuthenticatedModeSSLSocketFactory(SSLSocketFactory factory) {
        this.authenticatedModeSSLSocketFactory = factory;
        return this;
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

    /**
     * @return true if hostname verification, certificate chain validation,
     * and the use of the optional
     * {@link #getAuthenticatedModeSSLSocketFactory()} are disabled, or
     * false otherwise.
     * @see #setSSLAuthenticationDisabled(boolean)
     */
    public boolean isSSLAuthenticationDisabled() {
        return isSSLAuthenticationDisabled;
    }

    /**
     * Returns the SSLSocketFactory that gets used when connecting to
     * CloudantDB over a <code>https</code> URL, when SSL authentication is
     * enabled.
     *
     * @return An SSLSocketFactory, or <code>null</code>, which stands for
     *         the default SSLSocketFactory of the JRE.
     * @see #setAuthenticatedModeSSLSocketFactory(javax.net.ssl.SSLSocketFactory)
     */
    public SSLSocketFactory getAuthenticatedModeSSLSocketFactory() {
        return authenticatedModeSSLSocketFactory;
    }
}
