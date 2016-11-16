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
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final CookieManager cookieManager = new CookieManager();
    private final AtomicBoolean shouldAttemptCookieRequest = new AtomicBoolean(true);
    private final URL sessionURL;

    /**
     * Constructs a cookie interceptor.
     * @param username The username to use when getting the cookie
     * @param password The password to use when getting the cookie
     * @param baseURL  The base URL to use when constructing an `_session` request.
     */
    public CookieInterceptor(String username, String password, String baseURL) {

        try {
            this.sessionURL = new URL(String.format("%s/_session", baseURL));
            username = URLEncoder.encode(username, "UTF-8");
            password = URLEncoder.encode(password, "UTF-8");
            this.sessionRequestBody = String.format("name=%s&password=%s", username, password)
                    .getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //all JVMs should support UTF-8, so this should not happen
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            // this should be a valid URL since the builder is passing it in
            logger.log(Level.SEVERE, "Failed to create URL for _session endpoint", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext
                                                                     context) {

        HttpURLConnection connection = context.connection.getConnection();

        // First time we will have no cookies
        if (cookieManager.getCookieStore().getCookies().isEmpty() && shouldAttemptCookieRequest
                .get()) {
            if (!requestCookie(context)) {
                // Requesting a cookie failed, set a flag if we failed so we won't try again
                shouldAttemptCookieRequest.set(false);
            }
        }

        if (shouldAttemptCookieRequest.get()) {

            // Debug logging
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Attempt to add cookie to request.");
                logger.finest("Cookies are stored for URIs: " + cookieManager.getCookieStore()
                        .getURIs());
            }

            // Apply any saved cookies to the request
            try {
                Map<String, List<String>> requestCookieHeaders = cookieManager.get(connection
                        .getURL().toURI(), connection.getRequestProperties());
                for (Map.Entry<String, List<String>> requestCookieHeader :
                        requestCookieHeaders.entrySet()) {
                    List<String> cookies = requestCookieHeader.getValue();
                    if (cookies != null && !cookies.isEmpty()) {
                        connection.setRequestProperty(requestCookieHeader.getKey(),
                                listToSemicolonSeparatedString(cookies));
                    } else {
                        logger.finest("No cookie values to set.");
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to read request properties", e);
            } catch (URISyntaxException e) {
                logger.log(Level.SEVERE, "Failed to convert request URL to URI for cookie " +
                        "retrieval.");
            }
        }
        return context;
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
                    InputStream errorStream = connection.getErrorStream();
                    String errorString = null;
                    if (errorStream != null) {
                        try {
                            // Get the string value of the error stream
                            errorString = IOUtils.toString(errorStream, "UTF-8");
                        } finally {
                            IOUtils.closeQuietly(errorStream);
                        }
                    }
                    logger.log(Level.FINE, String.format(Locale.ENGLISH, "Intercepted " +
                            "response %d %s", statusCode, errorString));
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
                        logger.finest("Cookie was invalid attempt to get new cookie.");
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

    private boolean requestCookie(HttpConnectionInterceptorContext context) {
        try {
            HttpConnection conn = Http.POST(sessionURL, "application/x-www-form-urlencoded");
            conn.setRequestBody(sessionRequestBody);

            //when we request the session we need all interceptors except this one

            conn.requestInterceptors.addAll(context.connection.requestInterceptors);
            conn.requestInterceptors.remove(this);
            conn.responseInterceptors.addAll(context.connection.responseInterceptors);
            conn.responseInterceptors.remove(this);


            HttpURLConnection connection = conn.execute().getConnection();
            int responseCode = connection.getResponseCode();

            if (responseCode / 100 == 2) {

                if (sessionHasStarted(connection.getInputStream())) {
                    return storeCookiesFromResponse(connection);
                } else {
                    return false;
                }
            } else {
                InputStream errorStream = connection.getErrorStream();
                try {
                    if (errorStream != null) {
                        // Consume the error stream to avoid leaking connections
                        String error = IOUtils.toString(errorStream, "UTF-8");
                        // Log the error stream content
                        logger.fine(error);
                    }
                } finally {
                    IOUtils.closeQuietly(errorStream);
                }
                if (responseCode == 401) {
                    logger.severe("Credentials are incorrect, cookie authentication will not be" +
                            " attempted again by this interceptor object");
                } else {
                    // catch any other response code
                    logger.log(Level.SEVERE,
                            "Failed to get cookie from server, response code {0}, " +
                                    "cookie authentication will not be attempted again",
                            responseCode);
                }
            }
        }  catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read cookie response", e);
        }
        return false;
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

    private boolean storeCookiesFromResponse(HttpURLConnection connection) {

        // Store any cookies from the response in the CookieManager
        try {
            logger.finest("Storing cookie.");
            cookieManager.put(connection.getURL().toURI(), connection.getHeaderFields());
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read cookie response header", e);
            return false;
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Failed to convert request URL to URI for cookie storage.");
            return false;
        }
    }

    private String listToSemicolonSeparatedString(List<String> cookieStrings) {
        // RFC 6265 says multiple cookie pairs should be "; " separated
        StringBuilder builder = new StringBuilder();
        int index = 0; // Count from 0 since we will increment before comparing to size
        for (String cookieString : cookieStrings) {
            builder.append(cookieString);
            if (++index != cookieStrings.size()) {
                builder.append("; ");
            }
        }
        return builder.toString();
    }

}
