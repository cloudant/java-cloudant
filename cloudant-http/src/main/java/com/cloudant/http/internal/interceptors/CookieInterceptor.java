/*
 * Copyright © 2015, 2021 IBM Corp. All rights reserved.
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
import com.cloudant.http.internal.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Extends the CookieInterceptorBase to provide Apache CouchDB _session cookie support.
 */
public class CookieInterceptor extends CookieInterceptorBase {

    private final byte[] auth;

    /**
     * Constructs a cookie interceptor. Credentials should be supplied not URL encoded, this class
     * will perform the necessary URL encoding.
     *
     * @param username The username to use when getting the cookie (not URL encoded)
     * @param password The password to use when getting the cookie (not URL encoded)
     * @param baseURL  The base URL to use when constructing an `_session` request.
     */
    public CookieInterceptor(String username, String password, String baseURL) {
        this(username, password, baseURL, null);
    }

    /**
     * Constructs a cookie interceptor with proxy url.
     * Credentials should be supplied not URL encoded, this class
     * will perform the necessary URL encoding.
     *
     * @param username The username to use when getting the cookie (not URL encoded)
     * @param password The password to use when getting the cookie (not URL encoded)
     * @param baseURL  The base URL to use when constructing an `_session` request.
     * @param proxyURL The URL of the proxy server
     */
    public CookieInterceptor(String username, String password, String baseURL, URL proxyURL) {
        // Use form encoding for the user/pass submission
        super(baseURL, "/_session", "application/x-www-form-urlencoded", proxyURL);
        try {
            this.auth = String.format("name=%s&password=%s", URLEncoder.encode(username, "UTF-8")
                            , URLEncoder.encode(password, "UTF-8"))
                    .getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //all JVMs should support UTF-8, so this should not happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns form encoded credentials to pass to _session.
     *
     * @param context interceptor context
     * @return form encoded credentials body payload
     */
    @Override
    protected byte[] getSessionRequestPayload(HttpConnectionInterceptorContext context) {
        return auth;
    }

    /**
     * Adds an additional check for HTTP 403 status codes with "credentials expired" messages that
     * are returned by some Cloudant versions.
     *
     * @param connection the connection to interrogate
     * @param statusCode the HTTP response status code
     * @return
     */
    @Override
    protected boolean shouldRenew(HttpURLConnection connection, int statusCode) {
        try {
            if (statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
                // Get the string value of the error stream
                InputStream errorStream = connection.getErrorStream();
                String errorString = null;
                if (errorStream != null) {
                    errorString = Utils.collectAndCloseStream(connection
                            .getErrorStream());
                    logger.log(Level.FINE, String.format(Locale.ENGLISH, "Intercepted " +
                            "response %d %s", statusCode, errorString));
                }
                // Check if it was an expiry case
                // Check using a regex to avoid dependency on a JSON library.
                // Note (?siu) flags used for . to also match line breaks and for
                // unicode
                // case insensitivity.
                if (errorString != null && errorString.matches("(?siu)" +
                        ".*\\\"error\\\"\\s*:\\s*\\\"credentials_expired\\\".*")) {
                    // Was expired - renew cookie
                    return true;
                } else {
                    // Wasn't a credentials expired, throw exception
                    HttpConnectionInterceptorException toThrow = new
                            HttpConnectionInterceptorException(errorString, null);
                    // Set the flag for deserialization
                    toThrow.deserialize = errorString != null;
                    throw toThrow;
                }
            }
        } catch (IOException e) {
            throw wrapIOException("Failed to read HTTP reponse code or body from", connection, e);
        }
        return false;
    }
}
