/*
 * Copyright © 2017 IBM Corp. All rights reserved.
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

import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;
import com.cloudant.http.internal.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by tomblench on 21/06/2017.
 */

public abstract class CookieInterceptorBase implements HttpConnectionRequestInterceptor,
        HttpConnectionResponseInterceptor {

    protected final static Logger logger = Logger.getLogger(CookieInterceptor.class
            .getCanonicalName());
    byte[] sessionRequestBody;
    private final String sessionRequestMimeType;
    private final CookieManager cookieManager = new CookieManager();
    final AtomicBoolean shouldAttemptCookieRequest = new AtomicBoolean(true);
    private final URL sessionURL;

    CookieInterceptorBase(String sessionRequestMimeType, String baseUrl, String endpoint) {
        this.sessionRequestMimeType = sessionRequestMimeType;
        try {
            baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1): baseUrl;
            endpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
            this.sessionURL = new URL(String.format("%s%s", baseUrl, endpoint));
        } catch (MalformedURLException e) {
            // this should be a valid URL since the builder is passing it in
            logger.log(Level.SEVERE, "Failed to create URL for session endpoint", e);
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
        try {
            if (shouldAttemptCookieRequest.get()) {

                HttpURLConnection connection = context.connection.getConnection();
                int statusCode = connection.getResponseCode();
                if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    boolean success = requestCookie(context);
                    if (success) {
                        context.replayRequest = true;
                        // Consume the error stream to avoid leaking connections
                        Utils.consumeAndCloseStream(connection.getErrorStream());
                        logger.log(Level.FINEST, "Consumed error response");
                    } else {
                        context.replayRequest = false; // Don't replay
                        shouldAttemptCookieRequest.set(false); // Set the flag to stop trying
                    }
                } else {
                    // Store any cookies provided on the response
                    storeCookiesFromResponse(connection);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading response code or body from request", e);
        }
        return context;
    }

    // helper for requestCookie
    interface OnExecuteCallable {
        boolean call(HttpConnection connection) throws IOException;
    }

    protected boolean requestCookie(HttpConnectionInterceptorContext context,
                                    URL url,
                                    byte[] payload,
                                    String mimeType,
                                    String accept,
                                    OnExecuteCallable onResponseOk) {

        try {
            HttpConnection conn = Http.POST(url, mimeType);
            conn.requestProperties.put("accept", accept);
            conn.setRequestBody(payload);

            //when we request the session we need all interceptors except this one

            conn.requestInterceptors.addAll(context.connection.requestInterceptors);
            conn.requestInterceptors.remove(this);
            conn.responseInterceptors.addAll(context.connection.responseInterceptors);
            conn.responseInterceptors.remove(this);

            HttpConnection connection = conn.execute();
            int responseCode = connection.getConnection().getResponseCode();

            if (responseCode / 100 == 2) {
                return onResponseOk.call(connection);
            } else {
                // Consume the error stream to avoid leaking connections
                String error = Utils.collectAndCloseStream(connection.getConnection()
                        .getErrorStream());
                // Log the error stream content
                logger.fine(error);
                if (responseCode == 401) {
                    logger.log(Level.SEVERE, "Credentials are incorrect for server {0}, cookie " +
                            "authentication will not be attempted again by this interceptor " +
                            "object", url);
                } else {
                    // catch any other response code
                    logger.log(Level.SEVERE, "Failed to get cookie from server {0}, response code" +
                            " {1}, cookie authentication will not be attempted again", new
                            Object[]{url, responseCode});
                }
            }
        }  catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read cookie response", e);
        }
        return false;
    }

    boolean requestCookie(HttpConnectionInterceptorContext context) {
        return requestCookie(context,
                sessionURL,
                sessionRequestBody,
                sessionRequestMimeType,
                "application/json",
                new OnExecuteCallable() {
                    @Override
                    public boolean call(HttpConnection connection) throws IOException {
                        if (sessionHasStarted(connection.responseAsInputStream())) {
                            return storeCookiesFromResponse(connection.getConnection());
                        } else {
                            // If the session did not start, consume the error stream to avoid
                            // leaking connections.
                            Utils.consumeAndCloseStream(connection.getConnection().getErrorStream
                                    ());
                            return false;
                        }
                    }
                }
        );
    }

    private boolean sessionHasStarted(InputStream responseStream) throws IOException {
        // Get the response body as a string
        String response = Utils.collectAndCloseStream(responseStream);

        // Only check for ok:true, https://issues.apache.org/jira/browse/COUCHDB-1356
        // means we cannot check that the name returned is the one we sent.

        // Check the response body for "ok" : true using a regex because we don't want a JSON
        // library dependency for something so simple in a shared HTTP artifact used in both
        // java-cloudant and sync-android. Note (?siu) flags used for . to also match line
        // breaks and for unicode case insensitivity.

        return response.matches("(?s)(?i)(?u).*\\\"ok\\\"\\s*:\\s*true.*");
    }

    protected boolean storeCookiesFromResponse(HttpURLConnection connection) {

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
