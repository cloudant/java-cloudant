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

package com.cloudant.http.internal;

import com.cloudant.http.HttpConnection;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.util.Locale;
import java.util.logging.Logger;

public class DefaultHttpUrlConnectionFactory implements HttpConnection.HttpUrlConnectionFactory {

    private static final Logger logger = Logger.getLogger(DefaultHttpUrlConnectionFactory.class
            .getName());

    protected Proxy proxy = null;

    @Override
    public HttpURLConnection openConnection(URL url) throws IOException {
        if (proxy != null) {
            return (HttpURLConnection) url.openConnection(proxy);
        } else {
            return (HttpURLConnection) url.openConnection();
        }
    }

    @Override
    public void setProxy(URL proxyUrl) {
        if ("http".equals(proxyUrl.getProtocol())) {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(),
                    proxyUrl.getPort()));
            logger.config(String.format("Configured HTTP proxy url %s", proxyUrl));
        } else {
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "The proxy URL %s " +
                    "is invalid. Only HTTP type proxies are supported.", proxyUrl));
        }
    }

    @Override
    public void setProxyAuthentication(PasswordAuthentication proxyAuthentication) {
        // Currently a no-op.
        // HttpURLConnection doesn't allow the setting of an Authenticator per instance and it would
        // be irresponsible to set the default Authenticator because it applies globally and we
        // might disrupt other applications in the JVM.
    }

    @Override
    public void shutdown() {
        // No - op
    }
}
