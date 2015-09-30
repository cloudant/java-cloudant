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
 *
 * @author Ahmed Yehia
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
     * Get a database
     *
     * @param name   name of database to access
     * @param create flag indicating whether to create the database if doesnt exist.
     * @return CouchDatabase object
     */
    public CouchDatabase database(String name, boolean create) {
        return new CouchDatabase(this, name, create);
    }
}
