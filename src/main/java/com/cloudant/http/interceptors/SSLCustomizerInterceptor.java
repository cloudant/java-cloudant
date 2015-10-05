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

import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;

import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLCustomizerInterceptor implements HttpConnectionRequestInterceptor {

    public static final SSLCustomizerInterceptor SSL_AUTH_DISABLED_INTERCEPTOR = new
            SSLCustomizerInterceptor(getAllTrustingSSLSocketFactory(), new
            AllowAllHostnameVerifier());

    private static final Logger LOGGER = Logger.getLogger(SSLCustomizerInterceptor.class.getName());
    private final SSLSocketFactory sslSocketFactory;
    private final HostnameVerifier hostnameVerifier;

    public SSLCustomizerInterceptor(HostnameVerifier hostnameVerifier) {
        this(null, hostnameVerifier);
    }

    public SSLCustomizerInterceptor(SSLSocketFactory sslSocketFactory) {
        this(sslSocketFactory, null);
    }

    public SSLCustomizerInterceptor(SSLSocketFactory sslSocketFactory, HostnameVerifier
            hostnameVerifier) {
        this.sslSocketFactory = sslSocketFactory;
        this.hostnameVerifier = hostnameVerifier;
    }

    @Override
    public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext
                                                                     context) {

        HttpURLConnection connection = context.connection.getConnection();
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection secureConnection = (HttpsURLConnection) connection;
            if (sslSocketFactory != null) {
                secureConnection.setSSLSocketFactory(sslSocketFactory);
            }
            if (hostnameVerifier != null) {
                secureConnection.setHostnameVerifier(hostnameVerifier);
            }
        }
        return context;
    }

    private static final class AllowAllHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

    private static SSLSocketFactory getAllTrustingSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws
                        CertificateException {
                    //NO-OP everything trusted
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws
                        CertificateException {
                    //NO-OP everything trusted
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    //trust all issuers
                    return null;
                }
            }}, new SecureRandom());

            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException nsae) {
            LOGGER.log(Level.SEVERE, "An error occurred instantiating the SSL authentication " +
                    "disabled interceptor", nsae);
        } catch (KeyManagementException kme) {
            LOGGER.log(Level.SEVERE, "An error occurred instantiating the SSL authentication " +
                    "disabled interceptor", kme);
        }
        return null;
    }
}
