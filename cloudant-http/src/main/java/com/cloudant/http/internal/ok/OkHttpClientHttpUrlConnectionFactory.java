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

package com.cloudant.http.internal.ok;

import com.cloudant.http.internal.DefaultHttpUrlConnectionFactory;

import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Provides HttpUrlConnections by using an OkHttpClient.
 */
public class OkHttpClientHttpUrlConnectionFactory extends DefaultHttpUrlConnectionFactory {

    private static final Logger logger = Logger.getLogger(OkHttpClientHttpUrlConnectionFactory
            .class.getName());

    private final OkHttpClient.Builder clientBuilder = new OkHttpClient().newBuilder();
    private OkUrlFactory factory = null;

    public OkHttpClientHttpUrlConnectionFactory() {
        clientBuilder.connectionSpecs(Arrays.asList(
                new ConnectionSpec[]{
                        ConnectionSpec.CLEARTEXT, // for http
                        new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                                .allEnabledTlsVersions()
                                .allEnabledCipherSuites()
                                .build() // for https
                }));
    }

    @Override
    public HttpURLConnection openConnection(URL url) throws IOException {
        if (factory == null) {
            factory = new OkUrlFactory(clientBuilder.build());
        }
        return factory.open(url);
    }

    @Override
    public void setProxy(URL proxyUrl) {
        super.setProxy(proxyUrl);
        clientBuilder.proxy(proxy).build();
        logger.config(String.format("Configured HTTP proxy url %s", proxyUrl));
    }

    public OkHttpClient.Builder getOkHttpClientBuilder() {
        return clientBuilder;
    }

    @Override
    public void setProxyAuthentication(PasswordAuthentication proxyAuthentication) {
        clientBuilder.proxyAuthenticator(new ProxyAuthenticator(Credentials.basic
                (proxyAuthentication.getUserName(), new String(proxyAuthentication.getPassword())
                )));
    }

    @Override
    public void shutdown() {
        try {
            factory.client().dispatcher().executorService().shutdown();
            factory.client().dispatcher().executorService().awaitTermination(5, TimeUnit.MINUTES);
            // Evict all the connections
            factory.client().connectionPool().evictAll();
        } catch (InterruptedException e) {
            // Oh well; we were only trying to aggressively shutdown
        }
    }
}
