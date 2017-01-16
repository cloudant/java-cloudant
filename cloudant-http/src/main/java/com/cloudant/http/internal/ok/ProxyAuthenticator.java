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

import com.cloudant.http.internal.interceptors.ProxyAuthInterceptor;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import java.io.IOException;

class ProxyAuthenticator implements Authenticator {

    private final String creds;

    ProxyAuthenticator(String creds) {
        this.creds = creds;
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (route.proxy() != null) {
            if (creds.equals(response.request().header("Proxy-Authorization"))) {
                // If the proxy creds have already been tried then give up
                return null;
            } else {
                return response.request().newBuilder().addHeader(ProxyAuthInterceptor
                        .PROXY_AUTH_HEADER, creds).build();
            }
        } else {
            // Don't interfere with normal Auth, this is just for proxies
            return null;
        }
    }
}
