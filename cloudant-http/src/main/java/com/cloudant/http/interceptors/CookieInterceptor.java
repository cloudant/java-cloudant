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

package com.cloudant.http.interceptors;

import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class CookieInterceptor implements HttpConnectionRequestInterceptor,
        HttpConnectionResponseInterceptor {

    private final static Logger logger = Logger.getLogger(CookieInterceptor.class
            .getCanonicalName());
    private final byte[] sessionRequestBody;
    private String cookie = null;
    private boolean shouldAttemptCookieRequest = true;

    /**
     * Constructs a cookie interceptor.
     *
     * @param username The username to use when getting the cookie
     * @param password The password to use when getting the cookie
     */
    public CookieInterceptor(String username, String password) {

        try {
            username = URLEncoder.encode(username, "UTF-8");
            password = URLEncoder.encode(password, "UTF-8");
            this.sessionRequestBody = String.format("name=%s&password=%s", username, password)
                    .getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //all JVMs should support UTF-8, so this should not happen
            throw new RuntimeException(e);
        }
    }

    @Override
    public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext
                                                                     context) {

        HttpURLConnection connection = context.connection.getConnection();

        if (shouldAttemptCookieRequest) {
            if (cookie == null) {
                cookie = getCookie(connection.getURL(), context);
            }
            connection.setRequestProperty("Cookie", cookie);
        }

        return context;
    }

    @Override
    public HttpConnectionInterceptorContext interceptResponse(HttpConnectionInterceptorContext
                                                                      context) {
        HttpURLConnection connection = context.connection.getConnection();

        String cookieHeader = connection.getHeaderField("Set-Cookie");
        if (cookieHeader != null) {
            cookie = this.extractCookieFromHeaderValue(cookieHeader);
            return context;
        }

        try {
            boolean renewCookie = false;
            int statusCode = connection.getResponseCode();
            switch (statusCode) {
                case HttpURLConnection.HTTP_FORBIDDEN: //403
                    //check if it was an expiry case
                    InputStream errorStream = connection.getErrorStream();
                    try {
                        String errorString = IOUtils.toString(errorStream, "UTF-8");
                        // Check using a regex to avoid dependency on a JSON library.
                        // Note (?siu) flags used for . to also match line breaks and for unicode
                        // case insensitivity.
                        if (!errorString.matches("(?siu)" +
                                ".*\\\"error\\\"\\s*:\\s*\\\"credentials_expired\\\".*")) {
                            // Wasn't a credentials expired, throw exception
                            HttpConnectionInterceptorException toThrow = new
                                    HttpConnectionInterceptorException(errorString);
                            // Set the flag for deserialization
                            toThrow.deserialize = true;
                            throw toThrow;
                        } else {
                            // Was expired - set boolean to renew cookie
                            renewCookie = true;
                        }
                    } finally {
                        errorStream.close();
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
                cookie = getCookie(connection.getURL(), context);
                // Don't resend request, failed to get cookie
                if (cookie != null) {
                    context.replayRequest = true;
                } else {
                    context.replayRequest = false;
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to get response code from request", e);
        }
        return context;

    }

    private String getCookie(URL url, HttpConnectionInterceptorContext context) {
        try {
            URL sessionURL = new URL(String.format("%s://%s:%d/_session",
                    url.getProtocol(),
                    url.getHost(),
                    url.getPort()));

            HttpConnection conn = Http.POST(sessionURL, "application/x-www-form-urlencoded");
            conn.setRequestBody(sessionRequestBody);

            //when we request the session we need all interceptors except this one

            conn.requestInterceptors.addAll(context.connection.requestInterceptors);
            conn.requestInterceptors.remove(this);
            conn.responseInterceptors.addAll(context.connection.responseInterceptors);
            conn.responseInterceptors.remove(this);


            HttpURLConnection connection = conn.execute().getConnection();
            String cookieHeader = connection.getHeaderField("Set-Cookie");
            int responseCode = connection.getResponseCode();

            if (responseCode / 100 == 2) {

                if (sessionHasStarted(connection.getInputStream())) {
                    return this.extractCookieFromHeaderValue(cookieHeader);
                } else {
                    return null;
                }

            } else if (responseCode == 401) {
                shouldAttemptCookieRequest = false;
                logger.severe("Credentials are incorrect, cookie authentication will not be" +
                        " attempted again by this interceptor object");
            } else if (responseCode / 100 == 5) {
                logger.log(Level.SEVERE,
                        "Failed to get cookie from server, response code %s, cookie auth",
                        responseCode);
            } else {
                // catch any other response code
                logger.log(Level.SEVERE,
                        "Failed to get cookie from server, response code %s, " +
                                "cookie authentication will not be attempted again",
                        responseCode);
                shouldAttemptCookieRequest = false;
            }

        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Failed to create URL for _session endpoint", e);
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "Failed to encode cookieRequest body", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read cookie response header", e);
        }
        return null;
    }

    private boolean sessionHasStarted(InputStream responseStream) throws IOException {
        try {
            // Get the response body as a string
            String response = IOUtils.toString(responseStream, "UTF-8");

            // Only check for ok:true, https://issues.apache.org/jira/browse/COUCHDB-1356
            // means we cannot check that the name returned is the one we sent.

            // Check the response body for "ok" : true using a regex because we don't want a JSON
            // library dependency for something so simple in a shared HTTP artifact used in both
            // java-cloudant and sync-android. Note (?siu) flags used for . to also match line
            // breaks and for unicode case insensitivity.

            return response.matches("(?s)(?i)(?u).*\\\"ok\\\"\\s*:\\s*true.*");
        } finally {
            IOUtils.closeQuietly(responseStream);
        }

    }

    private String extractCookieFromHeaderValue(String cookieHeaderValue) {
        return cookieHeaderValue.substring(0, cookieHeaderValue.indexOf(";"));
    }
}
