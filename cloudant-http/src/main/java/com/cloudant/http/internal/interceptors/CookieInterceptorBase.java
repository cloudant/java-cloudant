/*
 * Copyright © 2017, 2021 IBM Corp. All rights reserved.
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
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adds cookie authentication support to http requests.
 *
 * It does this by adding the cookie header using request interceptor pipeline
 * in {@link HttpConnection}.
 *
 * If a response has a response code of 401, it will fetch a cookie from
 * the server using provided credentials and tell {@link HttpConnection} to reply
 * the request by setting {@link HttpConnectionInterceptorContext#replayRequest} property to true.
 *
 * If the request to get the cookie for use in future request fails with a 401 status code
 * (or any status that indicates client error) a HttpConnectionInterceptorException will be thrown.
 */
public abstract class CookieInterceptorBase implements HttpConnectionRequestInterceptor,
        HttpConnectionResponseInterceptor {

    protected final static Logger logger = Logger.getLogger(CookieInterceptorBase.class
            .getCanonicalName());
    private final URL sessionRequestUrl;
    private final String sessionRequestMimeType;
    private final String sessionStateName = "sessionUuid";
    private final CookieManager cookieManager = new CookieManager();
    private final ReadWriteLock sessionLock = new ReentrantReadWriteLock(true);
    private volatile UUID sessionId = UUID.randomUUID();
    private final URL proxyURL;

    /**
     * @param baseUrl         the server URL to get cookies from
     * @param endpoint        the server endpoint to get cookies from
     * @param requestMimeType the MIME Content-Type to use for the session request
     * @param proxyURL        the proxy URL
     */
    protected CookieInterceptorBase(String baseUrl, String endpoint, String requestMimeType, URL proxyURL) {
        try {
            baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            endpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
            this.sessionRequestUrl = new URL(String.format("%s%s", baseUrl, endpoint));
            this.sessionRequestMimeType = (requestMimeType != null) ? requestMimeType :
                    "application/json";
        } catch (MalformedURLException e) {
            // this should be a valid URL since the builder is passing it in
            logger.log(Level.SEVERE, "Failed to create URL for session endpoint", e);
            throw new RuntimeException(e);
        }
        this.proxyURL = proxyURL;
    }

    /**
     * @param baseUrl         the server URL to get cookies from
     * @param endpoint        the server endpoint to get cookies from
     * @param requestMimeType the MIME Content-Type to use for the session request
     */
    protected CookieInterceptorBase(String baseUrl, String endpoint, String requestMimeType) {
        this(baseUrl, endpoint, requestMimeType, null);
    }

    /**
     * Override in sub-classes to provide the session request payload.
     *
     * @param context interceptor context
     * @return the payload bytes
     */
    protected abstract byte[] getSessionRequestPayload(HttpConnectionInterceptorContext context);

    private void requestCookie(HttpConnectionInterceptorContext context) throws IOException {
        // Check if the session was already updated on another thread before getting a cookie
        sessionLock.readLock().lock();
        try {
            if (sessionId.equals(context.getState(this, sessionStateName, UUID.class))) {
                // Get a write lock and re-check the state
                sessionLock.readLock().unlock();
                sessionLock.writeLock().lock();
                try {
                    if (sessionId.equals(context.getState(this, sessionStateName, UUID.class))) {
                        HttpConnection sessionConn = makeSessionRequest(sessionRequestUrl,
                                getSessionRequestPayload(context), sessionRequestMimeType, context);
                        HttpURLConnection sessionUrlConnection = sessionConn.getConnection();
                        try {
                            storeCookiesFromResponse(sessionUrlConnection);
                        } finally {
                            // We use collect rather than consume as we don't want to log
                            // a warning, even though we don't actually need the body
                            Utils.collectAndCloseStream(sessionUrlConnection.getInputStream());
                        }
                        // We renewed a cookie, update the global sessionID and this request's context
                        sessionId = UUID.randomUUID();
                        context.setState(this, sessionStateName, sessionId);
                    }
                } finally {
                    // Downgrade to the read lock
                    sessionLock.readLock().lock();
                    sessionLock.writeLock().unlock();
                }
            }
        } finally {
            sessionLock.readLock().unlock();
        }
    }

    HttpConnection makeSessionRequest(URL url, byte[] payload, String contentMimeType,
                                      HttpConnectionInterceptorContext context) {

        HttpConnection conn = Http.POST(url, contentMimeType);
        conn.requestProperties.put("accept", "application/json");
        conn.setRequestBody(payload);

        if (proxyURL != null) {
            conn.connectionFactory.setProxy(this.proxyURL);
        }

        //when we request the session we need all interceptors except this one

        conn.requestInterceptors.addAll(context.connection.requestInterceptors);
        conn.requestInterceptors.remove(this);
        conn.responseInterceptors.addAll(context.connection.responseInterceptors);
        conn.responseInterceptors.remove(this);

        try {
            HttpConnection connection = conn.execute();
            int responseCode = connection.getConnection().getResponseCode();

            if (responseCode / 100 == 2) {
                return connection;
            } else {
                // Consume the error stream to avoid leaking connections
                String error = Utils.collectAndCloseStream(connection.getConnection()
                        .getErrorStream());
                // Log the error stream content
                logger.fine(error);
                HttpConnectionInterceptorException e;
                if (responseCode == 401) {
                     e = new HttpConnectionInterceptorException(String.format("Credentials are " +
                            "incorrect for server %s", url));
                } else {
                    // catch any other response code
                    e = new HttpConnectionInterceptorException(String.format("HTTP response " +
                            "error getting session at %s.", url));
                }
                e.statusCode = responseCode;
                throw e;
            }
        } catch (IOException e) {
            throw wrapIOException("Failed to read server response from ", conn.getConnection(), e);
        }
    }

    @Override
    public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext context) {
        // Set the sessionId for this request
        context.setState(this, sessionStateName, sessionId);
        HttpURLConnection connection = context.connection.getConnection();
        try {
            // First time we will have no cookies
            if (cookieManager.getCookieStore().getCookies().isEmpty()) {
                requestCookie(context);
            }

            // Debug logging
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Attempt to add cookie to request.");
                logger.finest("Cookies are stored for URIs: " + cookieManager.getCookieStore()
                        .getURIs());
            }

            // Apply any saved cookies to the request
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
            throw wrapIOException("Failed to read stored cookies for", connection, e);
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Failed to convert request URL to URI for cookie " +
                    "retrieval.");
        }
        return context;
    }

    @Override
    public HttpConnectionInterceptorContext interceptResponse(HttpConnectionInterceptorContext context) {
        HttpURLConnection connection = context.connection.getConnection();
        try {
            int statusCode = connection.getResponseCode();
            boolean renew = shouldRenew(connection, statusCode);
            if (!renew && statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                renew = true;
                Utils.consumeAndCloseStream(connection.getErrorStream());
            } else {
                // Store any cookies provided on the response
                storeCookiesFromResponse(connection);
            }
            if (renew) {
                requestCookie(context);
                context.replayRequest = true;
            }
        } catch (IOException e) {
            throw wrapIOException("Failed to read HTTP response code or body from", connection, e);
        }
        return context;
    }

    protected boolean shouldRenew(HttpURLConnection connection, int statusCode) {
        return false;
    }

    private void storeCookiesFromResponse(HttpURLConnection connection) {

        // Store any cookies from the response in the CookieManager
        try {
            logger.finest("Storing cookie.");
            cookieManager.put(connection.getURL().toURI(), connection.getHeaderFields());
        } catch (IOException e) {
            throw wrapIOException("Failed to read cookie response header from", connection, e);
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Failed to convert request URL to URI for cookie storage.");
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

    HttpConnectionInterceptorException wrapIOException(String msg, HttpURLConnection conn, IOException e) {
        String errorMsg = (conn == null) ? msg : msg + " " + conn.getURL().toString();
        return new HttpConnectionInterceptorException(new IOException(errorMsg, e));
    }
}
