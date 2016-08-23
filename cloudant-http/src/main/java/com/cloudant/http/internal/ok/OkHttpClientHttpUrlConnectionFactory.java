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

package com.cloudant.http.internal.ok;

import com.cloudant.http.interceptors.ProxyAuthInterceptor;
import com.cloudant.http.internal.DefaultHttpUrlConnectionFactory;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Provides HttpUrlConnections by using an OkHttpClient.
 */
public class OkHttpClientHttpUrlConnectionFactory extends DefaultHttpUrlConnectionFactory {

    private static final Logger logger = Logger.getLogger(OkHttpClientHttpUrlConnectionFactory
            .class.getName());

    private final OkHttpClient client;
    private final OkUrlFactory factory;

    private final static boolean okUsable;

    static {
        Class<?> okFactoryClass;
        try {
            okFactoryClass = Class.forName("com.squareup.okhttp.OkUrlFactory");
        } catch (Throwable t) {
            okFactoryClass = null;
        }
        okUsable = (okFactoryClass != null);
    }

    public static boolean isOkUsable() {
        return okUsable;
    }


    public OkHttpClientHttpUrlConnectionFactory() {
        client = new OkHttpClient();
        client.setConnectionSpecs(Arrays.asList(
                new ConnectionSpec[]{
                        ConnectionSpec.CLEARTEXT, // for http
                        new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                                .allEnabledTlsVersions()
                                .allEnabledCipherSuites()
                                .build() // for https
                }));
        factory = new OkUrlFactory(client);
    }

    @Override
    public HttpURLConnection openConnection(URL url) throws IOException {
        return factory.open(url);
    }

    @Override
    public void setProxy(URL proxyUrl) {
        super.setProxy(proxyUrl);
        client.setProxy(proxy);
        logger.config(String.format("Configured HTTP proxy url %s", proxyUrl));
    }

    @Override
    public void setProxyAuthentication(final PasswordAuthentication proxyAuthentication) {
        client.setAuthenticator(new ProxyAuthenticator(Credentials.basic(proxyAuthentication
                .getUserName(), new String(proxyAuthentication.getPassword()))));
    }

    public OkHttpClient getOkHttpClient() {
        return client;
    }

    private static class ProxyAuthenticator implements Authenticator {

        private final String creds;

        ProxyAuthenticator(String creds) {
            this.creds = creds;
        }

        @Override
        public Request authenticate(Proxy proxy, Response response) throws IOException {
            // Don't interfere with normal auth, this is just for proxies.
            return null;
        }

        @Override
        public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
            if (creds.equals(response.request().header("Proxy-Authorization"))) {
                // If the proxy creds have already been tried then give up
                return null;
            } else {
                return response.request().newBuilder().addHeader(ProxyAuthInterceptor
                        .PROXY_AUTH_HEADER, creds).build();
            }
        }
    }

}
