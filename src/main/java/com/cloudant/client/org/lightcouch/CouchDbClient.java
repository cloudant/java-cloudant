/*
 * Copyright (C) 2011 lightcouch.org
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

package com.cloudant.client.org.lightcouch;

import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Presents a <i>client</i> to CouchDB database server.
 * <p>This class is the main object to use to gain access to the APIs.
 * <h3>Usage Example:</h3>
 * <p>Create a new client instance:
 * <pre>
 * CouchDbClient dbClient = new CouchDbClient();
 * </pre>
 * <p/>
 * <p>Start using the API by the client:
 * <p/>
 * <p>DB server APIs is accessed by the client directly eg.: {@link CouchDbClientBase#getAllDbs()
 * dbClient.getAllDbs()}
 * <p>DB is accessed by getting to the CouchDatabase from the client
 * <pre>
 * CouchDatabase db = dbClient.database("customers",false);
 * </pre>
 * <p>Documents <code>CRUD</code> APIs is accessed from the CouchDatabase eg.: {@link
 * CouchDatabaseBase#find(Class, String) db.find(Foo.class, "doc-id")}
 * <p>View APIs {@link View db.view()}
 * <p>Change Notifications {@link Changes db.changes()}
 * <p>Design documents {@link CouchDbDesign db.design()}
 * <p/>
 * <p>Replication {@link Replication dbClient.replication()} and {@link Replicator dbClient
 * .replicator()}
 * <p/>
 * <p/>
 * <p>At the end of a client usage; it's useful to call: {@link #shutdown()} to ensure proper
 * release of resources.
 *
 * @author Ahmed Yehia
 * @see CouchDbClientAndroid
 * @since 0.0.2
 */
public class CouchDbClient extends CouchDbClientBase {

    //User-Agent value for the client
    public static final String USER_AGENT;

    static {
        String ua = "java-cloudant";
        String version = "unknown";
        final URL url = CouchDbClient.class.getClassLoader().getResource("client.properties");
        final Properties properties = new Properties();
        InputStream propStream = null;
        try {
            properties.load((propStream = url.openStream()));
            ua = properties.getProperty("user.agent.name", ua);
            version = properties.getProperty("user.agent.version", version);
        } catch (Exception ex) {
            //swallow exception and keep using default values
        } finally {
            if (propStream != null) {
                try {
                    propStream.close();
                } catch (IOException e) {
                    //can't do anything else
                }
            }
        }
        USER_AGENT = String.format("%s/%s [Java (%s; %s; %s) %s; %s; %s]",
                ua,
                version,
                System.getProperty("os.arch"),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("java.vendor"),
                System.getProperty("java.version"),
                System.getProperty("java.runtime.version"));
    }

    /**
     * Constructs a new instance of this class, expects a configuration file named
     * <code>couchdb.properties</code> to be available in your application default classpath.
     */
    public CouchDbClient() {
        super();
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param protocol   The protocol to use (i.e http or https)
     * @param host       The database host address
     * @param port       The database listening port
     * @param authCookie The cookie obtained from last login
     */
    public CouchDbClient(String protocol, String host, int port,
                         String authCookie) {
        super(new CouchDbConfig(new CouchDbProperties(protocol, host, port,
                authCookie)));
    }


    /**
     * Constructs a new instance of this class.
     *
     * @param configFileName The configuration file name.
     */
    public CouchDbClient(String configFileName) {
        super(new CouchDbConfig(configFileName));
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param protocol The protocol to use (i.e http or https)
     * @param host     The database host address
     * @param port     The database listening port
     * @param username The Username credential
     * @param password The Password credential
     */
    public CouchDbClient(String protocol, String host, int port, String username, String
            password) {
        super(new CouchDbConfig(new CouchDbProperties(protocol, host, port, username, password)));
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param properties An object containing configuration properties.
     * @see {@link CouchDbProperties}
     */
    public CouchDbClient(CouchDbProperties properties) {
        super(new CouchDbConfig(properties));
    }

    /**
     * @return {@link CloseableHttpClient} instance.
     */
    @Override
    HttpClient createHttpClient(CouchDbProperties props) {
        try {
            Registry<ConnectionSocketFactory> registry = createRegistry(props);
            PoolingHttpClientConnectionManager ccm = createConnectionManager(props, registry);
            HttpClientBuilder clientBuilder = HttpClients.custom().setUserAgent(USER_AGENT)
                    .setConnectionManager(ccm)
                    .setDefaultConnectionConfig(ConnectionConfig.custom()
                            .setCharset(Consts.UTF_8).build())
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setSocketTimeout(props.getSocketTimeout())
                            .setConnectTimeout(props.getConnectionTimeout()).build());
            if (props.getProxyHost() != null) {
                clientBuilder.setProxy(new HttpHost(props.getProxyHost(), props.getProxyPort()));
            }
            clientBuilder.setDefaultCookieStore(cookies); // use AUTH cookies
            if (props.getUsername() != null) {
                // this one is for non account endpoints.
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(new AuthScope(props.getHost(),
                                props.getPort()),
                        new UsernamePasswordCredentials(props.getUsername(),
                                props.getPassword()));
                clientBuilder.setDefaultCredentialsProvider(credsProvider);
                //props.clearPassword();
            }
            registerInterceptors(clientBuilder);
            return clientBuilder.build();
        } catch (Exception e) {
            throw new IllegalStateException("Error Creating HTTPClient: ", e);
        }
    }

    @Override
    HttpContext createContext() {

        AuthCache authCache = new BasicAuthCache();
        authCache.put(host, new BasicScheme());
        HttpContext context = new BasicHttpContext();
        context.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
        return context;
    }


    private PoolingHttpClientConnectionManager createConnectionManager(
            CouchDbProperties props, Registry<ConnectionSocketFactory> registry) {
        PoolingHttpClientConnectionManager ccm = new PoolingHttpClientConnectionManager(registry);
        if (props.getMaxConnections() != 0) {
            ccm.setMaxTotal(props.getMaxConnections());
            ccm.setDefaultMaxPerRoute(props.getMaxConnections());
        }
        return ccm;
    }

    private Registry<ConnectionSocketFactory> createRegistry(CouchDbProperties props) throws
            KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        RegistryBuilder<ConnectionSocketFactory> registry = RegistryBuilder
                .<ConnectionSocketFactory>create();

        if (props.isSSLAuthenticationDisabled()) {
            // No SSL authentication.
            // No need for a custom SSLSocketFactory in this case.
            SSLContext sslcontext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustStrategy() {
                        public boolean isTrusted(X509Certificate[] chain, String authType)
                                throws CertificateException {
                            return true;
                        }
                    }).build();

            registry.register("https", new SSLConnectionSocketFactory(sslcontext,
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER));
        } else {
            // With SSL authentication enabled.
            // A custom SSLSocketFactory can be set to enhance security in specific
            // environments.
            SSLSocketFactory factory = props.getAuthenticatedModeSSLSocketFactory();
            if (factory != null) {
                registry.register(
                        "https",
                        new SSLConnectionSocketFactory(factory,
                                SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER));
            } else {
                // Use the default SSL configuration and truststore of the JRE.
                registry.register(
                        "https",
                        new SSLConnectionSocketFactory(SSLContexts.createDefault(),
                                SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER));
            }
        }

        //always return a plain http socket factory as well as whatever SSL was registered.
        return registry.register("http", PlainConnectionSocketFactory.INSTANCE).build();

    }

    /**
     * Adds request/response interceptors for logging and validation.
     *
     * @param clientBuilder
     */
    private void registerInterceptors(HttpClientBuilder clientBuilder) {
        clientBuilder.addInterceptorFirst(new HttpRequestInterceptor() {
            public void process(final HttpRequest request,
                                final HttpContext context) throws IOException {
                if (log.isDebugEnabled()) {
                    RequestLine req = request.getRequestLine();
                    log.debug("> " + req.getMethod() + " " + URLDecoder.decode(req.getUri(),
                            "UTF-8"));
                }
            }
        });
        clientBuilder.addInterceptorFirst(new HttpResponseInterceptor() {
            public void process(final HttpResponse response,
                                final HttpContext context) throws IOException {
                if (log.isDebugEnabled()) {
                    log.debug("< Status: " + response.getStatusLine().getStatusCode());
                }
                validate(response);
            }
        });
    }


    /**
     * Get a database
     *
     * @param name   name of database to access
     * @param create flag indicating whether to create the database if doesnt exist.
     * @return CouchDatabase object
     */
    public CouchDatabase database(String name, boolean create) {
        return new CouchDatabase(this, name, create);
    }

    public void shutdown() {
        HttpClientUtils.closeQuietly(this.httpClient);
    }
}
