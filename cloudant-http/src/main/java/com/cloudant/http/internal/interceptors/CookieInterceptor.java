/*
 * Copyright © 2015, 2017 IBM Corp. All rights reserved.
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
import com.cloudant.http.internal.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.logging.Level;

/**
 * Adds cookie authentication support to http requests.
 *
 * It does this by adding the cookie header for CouchDB
 * using request interceptor pipeline in {@link HttpConnection}.
 *
 * If a response has a response code of 401, it will fetch a cookie from
 * the server using provided credentials and tell {@link HttpConnection} to reply
 * the request by setting {@link HttpConnectionInterceptorContext#replayRequest} property to true.
 *
 * If the request to get the cookie for use in future request fails with a 401 status code
 * (or any status that indicates client error) cookie authentication will not be attempted again.
 */
public class CookieInterceptor extends CookieInterceptorBase {

    /**
     * Constructs a cookie interceptor. Credentials should be supplied not URL encoded, this class
     * will perform the necessary URL encoding.
     *
     * @param username The username to use when getting the cookie (not URL encoded)
     * @param password The password to use when getting the cookie (not URL encoded)
     * @param baseURL  The base URL to use when constructing an `_session` request.
     */
    public CookieInterceptor(String username, String password, String baseURL) {
        super("application/x-www-form-urlencoded", baseURL, "/_session");
        try {
            this.sessionRequestBody = String.format("name=%s&password=%s", URLEncoder.encode(username, "UTF-8"), URLEncoder.encode(password, "UTF-8"))
                    .getBytes("UTF-8"); ;
        } catch (UnsupportedEncodingException e) {
            //all JVMs should support UTF-8, so this should not happen
            throw new RuntimeException(e);
        }
    }


    @Override
    public HttpConnectionInterceptorContext interceptResponse(HttpConnectionInterceptorContext
                                                                      context) {

        // Check if this interceptor is valid before attempting any kind of renewal
        if (shouldAttemptCookieRequest.get()) {

            HttpURLConnection connection = context.connection.getConnection();

            // If we got a 401 or 403 we might need to renew the cookie
            try {
                boolean renewCookie = false;
                int statusCode = connection.getResponseCode();

                if (statusCode == HttpURLConnection.HTTP_FORBIDDEN || statusCode ==
                        HttpURLConnection.HTTP_UNAUTHORIZED) {
                    // Get the string value of the error stream
                    InputStream errorStream = connection.getErrorStream();
                    String errorString = null;
                    if (errorStream != null) {
                        errorString = Utils.collectAndCloseStream(connection
                                .getErrorStream());
                        logger.log(Level.FINE, String.format(Locale.ENGLISH, "Intercepted " +
                                "response %d %s", statusCode, errorString));
                    }
                    switch (statusCode) {
                        case HttpURLConnection.HTTP_FORBIDDEN: //403
                            // Check if it was an expiry case
                            // Check using a regex to avoid dependency on a JSON library.
                            // Note (?siu) flags used for . to also match line breaks and for
                            // unicode
                            // case insensitivity.
                            if (errorString != null && errorString.matches("(?siu)" +
                                    ".*\\\"error\\\"\\s*:\\s*\\\"credentials_expired\\\".*")) {
                                // Was expired - set boolean to renew cookie
                                renewCookie = true;
                            } else {
                                // Wasn't a credentials expired, throw exception
                                HttpConnectionInterceptorException toThrow = new
                                        HttpConnectionInterceptorException(errorString);
                                // Set the flag for deserialization
                                toThrow.deserialize = errorString != null;
                                throw toThrow;
                            }
                            break;
                        case HttpURLConnection.HTTP_UNAUTHORIZED: //401
                            // We need to get a new cookie
                            renewCookie = true;
                            break;
                        default:
                            break;
                    }

                    if (renewCookie) {
                        logger.finest("Cookie was invalid. Will attempt to get new cookie.");
                        boolean success = requestCookie(context);
                        if (success) {
                            // New cookie obtained, replay the request
                            context.replayRequest = true;
                        } else {
                            // Didn't successfully renew, maybe creds are invalid
                            context.replayRequest = false; // Don't replay
                            shouldAttemptCookieRequest.set(false); // Set the flag to stop trying
                        }
                    }
                } else {
                    // Store any cookies provided on the response
                    storeCookiesFromResponse(connection);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error reading response code or body from request", e);
            }
        }
        return context;

    }

}
