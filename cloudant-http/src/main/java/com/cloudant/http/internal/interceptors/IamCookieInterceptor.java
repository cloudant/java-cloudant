/*
 * Copyright © 2017, 2018 IBM Corp. All rights reserved.
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by tomblench on 21/06/2017.
 */

public class IamCookieInterceptor extends CookieInterceptorBase {

    // for refreshing the bearer token from IAM
    private byte[] iamTokenRequestBody;

    // where the IAM server endpoint lives
    private final URL iamServerUrl;

    public IamCookieInterceptor(String apiKey, String baseUrl) {

        // Configure this interceptor to get _iam_session cookies
        super("application/json", baseUrl, "/_iam_session");

        // Read iamServer from system property, or set default
        try {
            this.iamServerUrl = new URL(System.getProperty("com.cloudant.client.iamserver",
                    "https://iam.bluemix.net/identity/token"));
        } catch (MalformedURLException mue) {
            throw new RuntimeException("IAM server property was not a valid URL", mue);
        }

        // Configure the IAM token request content
        String tokenRequestBody = String.format(Locale.ENGLISH,
                "grant_type=urn:ibm:params:oauth:grant-type:apikey&response_type=cloud_iam&apikey" +
                        "=%s", apiKey);
        try {
            this.iamTokenRequestBody = tokenRequestBody.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //all JVMs should support UTF-8, so this should not happen
            throw new RuntimeException(e);
        }
    }

    // helper to store the bearer into a reference from an http connection response
    private static class StoreBearerCallable implements OnExecuteCallable {

        final AtomicReference<String> iamTokenResponse;

        StoreBearerCallable(AtomicReference<String> iamTokenResponse) {
            this.iamTokenResponse = iamTokenResponse;
        }

        @Override
        public boolean call(HttpConnection connection) throws IOException {
            iamTokenResponse.set(connection.responseAsString());
            return true;
        }
    }

    // get bearer token returned by IAM in JSON format
    private String getBearerToken(HttpConnectionInterceptorContext context) {
        final AtomicReference<String> iamTokenResponse = new AtomicReference<String>();
        boolean result = super.requestCookie(context, iamServerUrl, iamTokenRequestBody,
                "application/x-www-form-urlencoded", "application/json",
                new StoreBearerCallable(iamTokenResponse));
        if (result) {
            return iamTokenResponse.get();
        } else {
            return null;
        }
    }

    @Override
    boolean requestCookie(HttpConnectionInterceptorContext context) {

        // We are here because our session on the Cloudant server expired (or is otherwise not
        // valid)
        // First, we need to refresh the bearer token, and then refresh the session cookie using the
        // new bearer token.
        // Note that if the bearer token has a longer expiry value than the session cookie, it
        // could be possible to skip the first part and re-use the existing bearer token.
        // However, because we treat the bearer token as an opaque blob (partly to avoid
        // depending on a json deserialiser), we don't read the expiry value of the bearer token.

        if (!shouldAttemptCookieRequest.get()) {
            return false;
        }

        String token = getBearerToken(context);
        if (token == null) {
            return false;
        }
        try {
            // IAM token we retrieve looks like:
            // {
            //   "access_token": "eyJhbGciOiJIUz......sgrKIi8hdFs",
            //   "refresh_token": "SPrXw5tBE3......KBQ+luWQVY=",
            //   "token_type": "Bearer",
            //   "expires_in": 3600,
            //   "expiration": 1473188353
            // }
            // we can send this verbatim to Cloudant and they will only use the "access_token" part
            this.sessionRequestBody = token.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //all JVMs should support UTF-8, so this should not happen
            throw new RuntimeException(e);

        }
        // got bearer token, get session cookie
        return super.requestCookie(context);
    }

}
