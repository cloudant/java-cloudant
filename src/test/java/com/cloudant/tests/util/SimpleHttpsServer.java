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

package com.cloudant.tests.util;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.logging.Level;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * A very simple HTTPS Server that runs on the localhost and has a simple certificate that won't
 * work for hostname verification.
 * <p/>
 * A suitable simple certificate can be generated using the command:<br />
 * <code>
 * keytool -genkey -alias alias -keypass password -keystore SslAuthenticationTest.keystore
 * -storepass password
 * </code>
 */
public class SimpleHttpsServer extends SimpleHttpServer {

    private static String KEYSTORE_FILE = "src/test/resources/SslAuthenticationTest.keystore";
    private static String KEYSTORE_PASSWORD = "password";
    private static String KEY_PASSWORD = "password";

    public SimpleHttpsServer() {
        super(getSSLSocketFactory());
        PROTOCOL = "https";
    }

    private static SSLServerSocketFactory getSSLSocketFactory() {
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD
                    .toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            kmf.init(keystore, KEY_PASSWORD.toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext.getServerSocketFactory();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error initializing SimpleHttpsServer", e);
            return null;
        }
    }

}
