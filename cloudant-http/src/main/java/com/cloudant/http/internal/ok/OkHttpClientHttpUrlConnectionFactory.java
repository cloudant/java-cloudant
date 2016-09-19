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
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
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
    public void setProxyAuthentication(PasswordAuthentication proxyAuthentication) {
        client.setAuthenticator(new ProxyAuthenticator(Credentials.basic(proxyAuthentication
                .getUserName(), new String(proxyAuthentication.getPassword()))));
    }

    public OkHttpClient getOkHttpClient() {
        return client;
    }

}
