/*
 * Copyright © 2019 IBM Corp. All rights reserved.
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

package com.cloudant.http.internal.interceptors;

import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.interceptors.BasicAuthInterceptor;

import java.net.URL;

/**
 * Created by samsmith on 10/01/2019.
 */
public class IamServerBasicAuthInterceptor extends BasicAuthInterceptor {

    private final URL iamServerUrl;

    /**
     * Add basic access authentication to all requests matching the
     * {@code iamServerUrl}.
     *
     * @param iamServerUrl IAM token server URL
     * @param iamServerClientId Client ID used to authenticate with IAM token server
     * @param iamServerClientSecret Client secret used to authenticate with IAM token server
     */
    public IamServerBasicAuthInterceptor(URL iamServerUrl, String iamServerClientId,
                                         String iamServerClientSecret) {
        super(iamServerClientId + ":" + iamServerClientSecret);
        this.iamServerUrl = iamServerUrl;
    }

    @Override
    public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext
                                                                     context) {
        if (this.iamServerUrl != context.connection.url) {
            return context; // noop
        }
        return super.interceptRequest(context);
    }
}
