/*
 * Copyright © 2019, 2021 IBM Corp. All rights reserved.
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

import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionInterceptorContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Extends CookieInterceptorBase with support for API key to token exchange with an IAM server and
 * IAM token to session cookie exchange via Cloudant's _iam_session endpoint.
 */
public class IamCookieInterceptor extends CookieInterceptorBase {

    public static final String IAM_TOKEN_SERVER_URL_PROPERTY_KEY = "com.cloudant.client.iamserver";
    public final URL iamServerUrl;
    private final byte[] tokenRequestPayload;

    public IamCookieInterceptor(String apiKey, String baseUrl) {
        this(apiKey, baseUrl, null);
    }

    public IamCookieInterceptor(String apiKey, String baseUrl, URL proxyURL) {
        super(baseUrl, "/_iam_session", null, proxyURL);

        // Read iamServer from system property, or set default
        try {
            this.iamServerUrl = new URL(System.getProperty(IAM_TOKEN_SERVER_URL_PROPERTY_KEY,
                    "https://iam.cloud.ibm.com/identity/token"));
        } catch (MalformedURLException mue) {
            throw new RuntimeException("IAM server property was not a valid URL", mue);
        }

        try {
            this.tokenRequestPayload = String.format(Locale.ENGLISH,
                    "grant_type=urn:ibm:params:oauth:grant-type:apikey&response_type=cloud_iam" +
                            "&apikey=%s", apiKey).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //all JVMs should support UTF-8, so this should not happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Get IAM server URL.
     *
     * @return IAM server URL
     */
    public URL getIamServerUrl() {
        return iamServerUrl;
    }

    /**
     * Exchanges an IAM API key with the IAM server to get a JSON token that can subsequently be
     * passed to Cloudant's _iam_session endpoint.
     *
     * @param context interceptor context
     * @return the IAM token exchange JSON response
     */
    @Override
    protected byte[] getSessionRequestPayload(HttpConnectionInterceptorContext context) {
        HttpConnection tokenConn = super.makeSessionRequest(iamServerUrl, tokenRequestPayload,
                "application/x-www-form-urlencoded", context);
        try {
            return tokenConn.responseAsBytes();
        } catch (IOException e) {
            throw wrapIOException("Error reading token response body from",
                    tokenConn.getConnection(), e);
        }
    }
}
