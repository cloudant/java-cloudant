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

package com.cloudant.client.api;

import com.cloudant.client.api.views.Key;
import com.cloudant.client.internal.util.DeserializationTypes;
import com.cloudant.client.internal.util.IndexDeserializer;
import com.cloudant.client.internal.util.SecurityDeserializer;
import com.cloudant.client.internal.util.ShardDeserializer;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.CouchDbProperties;
import com.cloudant.http.HttpConnectionInterceptor;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;
import com.cloudant.http.interceptors.CookieInterceptor;
import com.cloudant.http.interceptors.ProxyAuthInterceptor;
import com.cloudant.http.interceptors.SSLCustomizerInterceptor;
import com.cloudant.http.interceptors.TimeoutCustomizationInterceptor;
import com.google.gson.GsonBuilder;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocketFactory;

/**
 * This class builds new {@link CloudantClient} instances.
 *
 * <h2>Create a new CloudantClient instance for a Cloudant account</h2>
 * <pre>
 * {@code
 * CloudantClient client = ClientBuilder.account("yourCloudantAccount")
 *                          .username("yourUsername")
 *                          .password("yourPassword")
 *                          .build();
 * }
 * </pre>
 *
 * <h2>Create a new CloudantClient instance for a Cloudant Local</h2>
 * <pre>
 * {@code
 * CloudantClient client = ClientBuilder.url(new URL("https://yourCloudantLocalAddress.example"))
 *                          .username("yourUsername")
 *                          .password("yourPassword")
 *                          .build();
 * }
 * </pre>
 *
 * <h2>Examples creating instances with additional options</h2>
 * <h3>Configure a proxy server</h3>
 * <pre>
 * {@code
 * CloudantClient client = ClientBuilder.account("yourCloudantAccount")
 *                          .username("yourUsername")
 *                          .password("yourPassword")
 *                          .proxyURL(new URL("https://yourProxyServerAddress.example"))
 *                          .proxyUser(yourProxyUser)
 *                          .proxyPassword(yourProxyPass)
 *                          .build();
 * }
 * </pre>
 *
 * <h3>Client with a custom SSL socket factory</h3>
 * <pre>
 * {@code
 * CloudantClient client = ClientBuilder.account("yourCloudantAccount")
 *                          .username("yourUsername")
 *                          .password("yourPassword")
 *                          .customSSLSocketFactory(...)
 *                          .build();
 * }
 * </pre>
 *
 * <h3>Client with custom connection and read timeouts</h3>
 * <pre>
 * {@code
 * CloudantClient client = ClientBuilder.account("yourCloudantAccount")
 *                          .username("yourUsername")
 *                          .password("yourPassword")
 *                          .connectTimeout(1, TimeUnit.MINUTES)
 *                          .readTimeout(1, TimeUnit.MINUTES)
 *                          .build();
 * }
 * </pre>
 *
 * @since 2.0.0
 */
public class ClientBuilder {

    /**
     * Default max of 6 connections
     **/
    public static final int DEFAULT_MAX_CONNECTIONS = 6;
    /**
     * Connection timeout defaults to 5 minutes
     **/
    public static final long DEFAULT_CONNECTION_TIMEOUT = 5l;
    /**
     * Read timeout defaults to 5 minutes
     **/
    public static final long DEFAULT_READ_TIMEOUT = 5l;

    private static final Logger logger = Logger.getLogger(ClientBuilder.class.getName());

    private List<HttpConnectionRequestInterceptor> requestInterceptors = new ArrayList
            <HttpConnectionRequestInterceptor>();
    private List<HttpConnectionResponseInterceptor> responseInterceptors = new ArrayList
            <HttpConnectionResponseInterceptor>();
    private String password;
    private String username;
    private URL url;
    private GsonBuilder gsonBuilder;
    /**
     * Defaults to {@link #DEFAULT_MAX_CONNECTIONS}
     **/
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private URL proxyURL;
    private String proxyUser;
    private String proxyPassword;
    private boolean isSSLAuthenticationDisabled;
    private SSLSocketFactory authenticatedModeSSLSocketFactory;
    private long connectTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private TimeUnit connectTimeoutUnit = TimeUnit.MINUTES;
    private long readTimeout = DEFAULT_READ_TIMEOUT;
    private TimeUnit readTimeoutUnit = TimeUnit.MINUTES;

    /**
     * Constructs a new ClientBuilder for building a CloudantClient instance to connect to the
     * Cloudant server with the specified account.
     *
     * @param account the Cloudant account name to connect to e.g. "example" is the account name
     *                for the "example.cloudant.com" endpoint
     * @return a new ClientBuilder for the account
     * @throws IllegalArgumentException if the specified account name forms an invalid endpoint URL
     */
    public static ClientBuilder account(String account) {
        logger.config("Account: " + account);
        try {
            URL url = new URL(String.format("https://%s.cloudant.com", account));
            return ClientBuilder.url(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not generate url from account name.", e);
        }
    }

    /**
     * Constructs a new ClientBuilder for building a CloudantClient instance to connect to the
     * Cloudant server with the specified URL.
     *
     * @param url server URL e.g. "https://yourCloudantLocalAddress.example"
     * @return a new ClientBuilder for the account
     */
    public static ClientBuilder url(URL url) {
        return new ClientBuilder(url);
    }


    private ClientBuilder(URL url) {
        logger.config("URL: " + url);
        String urlProtocol = url.getProtocol();
        String urlHost = url.getHost();
        //Check if port exists
        int urlPort = url.getPort();
        if (urlPort < 0) {
            urlPort = url.getDefaultPort();
        }
        if (url.getUserInfo() != null) {
            //Get username and password and replace credential variables
            this.username = url.getUserInfo().substring(0, url
                    .getUserInfo()
                    .indexOf(":"));
            this.password = url.getUserInfo().substring(url
                    .getUserInfo()
                    .indexOf(":") + 1);
        }
        
        // Check if a path exists and sanitize it by removing whitespace and any trailing /
        String urlPath = url.getPath().trim();
        urlPath = urlPath.endsWith("/") ? urlPath.substring(0, urlPath.length() - 1) : urlPath;

        try {
            // Reconstruct URL without user credentials
            this.url = new URL(urlProtocol
                    + "://"
                    + urlHost
                    + ":"
                    + urlPort
                    + urlPath);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Build the {@link CloudantClient} instance based on the endpoint used to construct this
     * client builder and the options that have been set on it before calling this method.
     *
     * @return the {@link CloudantClient} instance for the specified end point and options
     */
    public CloudantClient build() {

        logger.config("Building client using URL: " + url);

        //Build properties and couchdb client
        CouchDbProperties props = new CouchDbProperties(url);

        //Create cookie interceptor
        if (this.username != null && this.password != null) {
            //make interceptor if both username and password are not null

            //Create cookie interceptor and set in HttpConnection interceptors
            CookieInterceptor cookieInterceptor = new CookieInterceptor(username, password);

            props.addRequestInterceptors(cookieInterceptor);
            props.addResponseInterceptors(cookieInterceptor);
            logger.config("Added cookie interceptor");
        } else {
            //If username or password is null, throw an exception
            if (username != null || password != null) {
                //Username and password both have to contain values
                throw new CouchDbException("Either a username and password must be provided, or " +
                        "both values must be null. Please check the credentials and try again.");
            }
        }

        //If setter methods for read and connection timeout are not called, default values
        // are used.
        logger.config(String.format("Connect timeout: %s %s", connectTimeout,
                connectTimeoutUnit));
        logger.config(String.format("Read timeout: %s %s", readTimeout, readTimeoutUnit));

        // Log a warning if the DNS cache time is too long
        try {
            boolean shouldLogValueWarning = false;
            boolean isUsingDefaultTTLValue = true;
            String ttlString = Security.getProperty("networkaddress.cache.ttl");
            // Was able to access the property
            if (ttlString != null) {
                try {
                    int ttl = Integer.parseInt(ttlString);
                    isUsingDefaultTTLValue = false;
                    logger.finest("networkaddress.cache.ttl was " + ttl);
                    if (ttl > 30 || ttl < 0) {
                        shouldLogValueWarning = true;
                    }
                } catch (NumberFormatException nfe) {
                    // Suppress the exception, this will result in the default being used
                    logger.finest("networkaddress.cache.ttl was not an int.");
                }
            }

            if (isUsingDefaultTTLValue && System.getSecurityManager() != null) {
                //If we're using a default value and there is a SecurityManager we need to warn
                shouldLogValueWarning = true;
            }

            if (shouldLogValueWarning) {
                logger.warning("DNS cache lifetime may be too long. DNS cache lifetimes in excess" +
                        " of 30 seconds may impede client operation during cluster failover.");
            }
        } catch (SecurityException e) {
            // Couldn't access the property; log a warning
            logger.warning("Permission denied to check Java DNS cache TTL. If the cache " +
                    "lifetime is too long cluster failover will be impeded.");
        }

        props.addRequestInterceptors(new TimeoutCustomizationInterceptor(connectTimeout,
                connectTimeoutUnit, readTimeout, readTimeoutUnit));

        //Set connect options
        props.setMaxConnections(maxConnections);
        props.setProxyURL(proxyURL);
        if (proxyUser != null) {
            //if there was proxy auth information set up proxy auth
            if ("http".equals(url.getProtocol())) {
                // If we are using http, create an interceptor to add the Proxy-Authorization header
                props.addRequestInterceptors(new ProxyAuthInterceptor(proxyUser,
                        proxyPassword));
                logger.config("Added proxy auth interceptor");
            } else {
                // Set up an authenticator
                props.setProxyAuthentication(new PasswordAuthentication(proxyUser,
                        proxyPassword.toCharArray()));
            }
        }
        if (isSSLAuthenticationDisabled) {
            props.addRequestInterceptors(SSLCustomizerInterceptor
                    .SSL_AUTH_DISABLED_INTERCEPTOR);
            logger.config("SSL authentication is disabled");
        }
        if (authenticatedModeSSLSocketFactory != null) {
            props.addRequestInterceptors(new SSLCustomizerInterceptor(
                    authenticatedModeSSLSocketFactory
            ));
            logger.config("Added custom SSL socket factory");
        }

        //Set http connection interceptors
        if (requestInterceptors != null) {
            for (HttpConnectionRequestInterceptor requestInterceptor : requestInterceptors) {
                props.addRequestInterceptors(requestInterceptor);
                logger.config("Added request interceptor: " + requestInterceptor.getClass()
                        .getName());
            }
        }
        if (responseInterceptors != null) {
            for (HttpConnectionResponseInterceptor responseInterceptor : responseInterceptors) {
                props.addResponseInterceptors(responseInterceptor);
                logger.config("Added response interceptor: " + responseInterceptor.getClass()
                        .getName());
            }
        }

        //if no gsonBuilder has been provided, create a new one
        if (gsonBuilder == null) {
            gsonBuilder = new GsonBuilder();
            logger.config("Using default GSON builder");
        } else {
            logger.config("Using custom GSON builder");
        }
        //always register additional TypeAdapaters for derserializing some Cloudant specific
        // types before constructing the CloudantClient
        gsonBuilder.registerTypeAdapter(DeserializationTypes.SHARDS, new ShardDeserializer())
                .registerTypeAdapter(DeserializationTypes.INDICES, new IndexDeserializer())
                .registerTypeAdapter(DeserializationTypes.PERMISSIONS_MAP, new
                        SecurityDeserializer())
                .registerTypeAdapter(Key.ComplexKey.class, new Key.ComplexKeyDeserializer());

        return new CloudantClient(props, gsonBuilder);
    }

    /**
     * Sets a username or API key for the client connection.
     *
     * @param username the user or API key for the session
     * @return this ClientBuilder object for setting additional options
     */
    public ClientBuilder username(String username) {
        this.username = username;
        return this;
    }

    /**
     * Sets the password for the client connection. The password is the one for the username or
     * API key set by the {@link #username(String)} method.
     *
     * @param password user password or API key passphrase
     * @return this ClientBuilder object for setting additional options
     */
    public ClientBuilder password(String password) {
        this.password = password;
        return this;
    }

    /**
     * Set a custom GsonBuilder to use when serializing and de-serializing JSON in requests and
     * responses between the CloudantClient and the server.
     * <P>
     * Note: the supplied GsonBuilder will be augmented with some internal TypeAdapters.
     * </P>
     *
     * @param gsonBuilder the custom GsonBuilder to use
     * @return this ClientBuilder object for setting additional options
     */
    public ClientBuilder gsonBuilder(GsonBuilder gsonBuilder) {
        this.gsonBuilder = gsonBuilder;
        return this;
    }

    /**
     * Set the maximum number of connections to maintain in the connection pool.
     * <P>
     * Note: this setting only applies if using the optional OkHttp dependency. If OkHttp is not
     * present then the JVM configuration is used for pooling. Consult the JVM documentation for
     * the {@code http.maxConnections} property for further details.
     * </P>
     * Defaults to {@link #DEFAULT_MAX_CONNECTIONS}
     *
     * @param maxConnections the maximum number of simultaneous connections to open to the server
     * @return this ClientBuilder object for setting additional options
     */
    public ClientBuilder maxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    /**
     * <p>
     * Sets a proxy url for the client connection.
     * </p>
     * <p>
     * Note that this method can only set the configuration for an unencrypted HTTP proxy.
     * Even when using this type of proxy communication from the client to a https database server
     * is encrypted via a SSL tunnel.
     * </p>
     *
     * @param proxyURL the URL of the proxy server
     * @return this ClientBuilder object for setting additional options
     * @see <a href="{@docroot}overview-summary.html#Proxies">Advanced configuration: Proxies</a>
     */
    public ClientBuilder proxyURL(URL proxyURL) {
        this.proxyURL = proxyURL;
        return this;
    }

    /**
     * <p>
     * Sets an optional proxy username for the client connection.
     * </p>
     * <p>
     * Note: Use {@link java.net.Authenticator#setDefault(Authenticator)} to configure proxy
     * authentication when using the JVM default HttpURLConnection (i.e. not using the optional
     * okhttp dependency) in combination with a HTTPS database server.
     * </p>
     *
     * @param proxyUser username for the proxy server
     * @return this ClientBuilder object for setting additional options
     * @see <a href="{@docroot}overview-summary.html#Proxies">Advanced configuration: Proxies</a>
     */
    public ClientBuilder proxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
        return this;
    }

    /**
     * <p>
     * Sets an optional proxy password for the proxy user specified by
     * {@link #proxyUser(String)}.
     * </p>
     * <p>
     * Note: Use {@link java.net.Authenticator#setDefault(Authenticator)} to configure proxy
     * authentication when using the JVM default HttpURLConnection (i.e. not using the optional
     * okhttp dependency) in combination with a HTTPS database server.
     * </p>
     *
     * @param proxyPassword password for the proxy server user
     * @return this ClientBuilder object for setting additional options
     * @see <a href="{@docroot}overview-summary.html#Proxies">Advanced configuration: Proxies</a>
     */
    public ClientBuilder proxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        return this;
    }

    /**
     * Flag to disable hostname verification and certificate chain validation. This is not
     * recommended, but for example could be useful for testing with a self-signed certificate.
     * <P>
     * The SSL authentication is enabled by default meaning that hostname verification
     * and certificate chain validation is done using the JVM default settings.
     * </P>
     *
     * @return this ClientBuilder object for setting additional options
     * @throws IllegalStateException if {@link #customSSLSocketFactory(SSLSocketFactory)}
     *                               has been called on this ClientBuilder
     * @see #customSSLSocketFactory(SSLSocketFactory)
     */
    public ClientBuilder disableSSLAuthentication() {
        if (authenticatedModeSSLSocketFactory == null) {
            this.isSSLAuthenticationDisabled = true;
        } else {
            throw new IllegalStateException("Cannot disable SSL authentication when a " +
                    "custom SSLSocketFactory has been set.");
        }
        return this;
    }


    /**
     * Specifies the custom SSLSocketFactory to use when connecting to Cloudant over a
     * <code>https</code> URL, when SSL authentication is enabled.
     *
     * @param factory An SSLSocketFactory, or <code>null</code> for the
     *                default SSLSocketFactory of the JRE.
     * @return this ClientBuilder object for setting additional options
     * @throws IllegalStateException if {@link #disableSSLAuthentication()}
     *                               has been called on this ClientBuilder
     * @see #disableSSLAuthentication()
     */
    public ClientBuilder customSSLSocketFactory(SSLSocketFactory factory) {
        if (!isSSLAuthenticationDisabled) {
            this.authenticatedModeSSLSocketFactory = factory;
        } else {
            throw new IllegalStateException("Cannot use a custom SSLSocketFactory when " +
                    "SSL authentication is disabled.");
        }
        return this;
    }

    /**
     * This method adds {@link HttpConnectionInterceptor}s to be used on the CloudantClient
     * connection. Interceptors can be used to modify the HTTP requests and responses between the
     * CloudantClient and the server.
     * <P>
     * An example interceptor use might be to apply a custom authorization mechanism. For
     * instance to use BasicAuth instead of CookieAuth it is possible to use a
     * {@link com.cloudant.http.interceptors.BasicAuthInterceptor} that adds the BasicAuth
     * {@code Authorization} header to the request:
     * </P>
     * <pre>
     * {@code
     * CloudantClient client = ClientBuilder.account("yourCloudantAccount")
     *      .interceptors(new BasicAuthInterceptor("yourUsername:yourPassword"))
     *      .build()
     * }
     * </pre>
     *
     * @param interceptors one or more HttpConnectionInterceptor objects
     * @return this ClientBuilder object for setting additional options
     * @see HttpConnectionInterceptor
     */
    public ClientBuilder interceptors(HttpConnectionInterceptor... interceptors) {
        for (HttpConnectionInterceptor interceptor : interceptors) {
            if (interceptor instanceof HttpConnectionRequestInterceptor) {
                requestInterceptors.add((HttpConnectionRequestInterceptor) interceptor);
            }
            if (interceptor instanceof HttpConnectionResponseInterceptor) {
                responseInterceptors.add((HttpConnectionResponseInterceptor) interceptor);
            }
        }
        return this;
    }

    /**
     * Sets the specified timeout value when opening the client connection. If the timeout
     * expires before the connection can be established, a
     * {@link java.net.SocketTimeoutException} is raised.
     * <P>
     * Example creating a {@link CloudantClient} with a connection timeout of 2 seconds:
     * </P>
     * <pre>
     * {@code
     * CloudantClient client = ClientBuilder.account("yourCloudantAccount")
     *      .username("yourUsername")
     *      .password("yourPassword")
     *      .connectTimeout(2, TimeUnit.SECONDS)
     *      .build();
     * }
     * </pre>
     * Defaults to {@link #DEFAULT_CONNECTION_TIMEOUT} with {@link TimeUnit#MINUTES}.
     *
     * @param connectTimeout     duration of the read timeout
     * @param connectTimeoutUnit unit of measurement of the read timeout parameter
     * @return this ClientBuilder object for setting additional options
     * @see java.net.HttpURLConnection#setConnectTimeout(int)
     **/
    public ClientBuilder connectTimeout(long connectTimeout, TimeUnit connectTimeoutUnit) {
        this.connectTimeout = connectTimeout;
        this.connectTimeoutUnit = connectTimeoutUnit;
        return this;
    }

    /**
     * Sets the specified timeout value when reading from a {@link java.io.InputStream} with an
     * established client connection. If the timeout expires before there is data available for
     * read, a {@link java.net.SocketTimeoutException} is raised.
     * <P>
     * Example creating a {@link CloudantClient} with a read timeout of 2 seconds:
     * </P>
     * <pre>
     * {@code
     * CloudantClient client = ClientBuilder.account("yourCloudantAccount")
     *      .username("yourUsername")
     *      .password("yourPassword")
     *      .readTimeout(2, TimeUnit.SECONDS)
     *      .build();
     * }
     * </pre>
     * Defaults to {@link #DEFAULT_READ_TIMEOUT} with {@link TimeUnit#MINUTES}.
     *
     * @param readTimeout     duration of the read timeout
     * @param readTimeoutUnit unit of measurement of the read timeout parameter
     * @return this ClientBuilder object for setting additional options
     * @see java.net.HttpURLConnection#setReadTimeout(int)
     **/
    public ClientBuilder readTimeout(long readTimeout, TimeUnit readTimeoutUnit) {
        this.readTimeout = readTimeout;
        this.readTimeoutUnit = readTimeoutUnit;
        return this;
    }

}
