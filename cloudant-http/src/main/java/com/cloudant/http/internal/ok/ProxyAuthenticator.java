/*
 * Copyright © 2016 IBM Corp. All rights reserved.
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
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.Proxy;

class ProxyAuthenticator implements Authenticator {

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
