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

package com.cloudant.tests.util;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class MockWebServerResources {

    // Fields for a default timeout of 10 seconds to avoid tests running on too long if something
    // isn't correct with the mock web server.
    public static final long TIMEOUT = 10l;
    public static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    public static final String EXPECTED_OK_COOKIE =
            "a2ltc3RlYmVsOjUxMzRBQTUzOtiY2_IDUIdsTJEVNEjObAbyhrgz";
    public static final String EXPECTED_OK_COOKIE_2 =
            "dG9tYmxlbmNoOjU5NTM0QzgyOhqHa60IlqPmGR8vTVIK-tzhopMR";

    public static final String IAM_TOKEN =   "{\"access_token\":\"eyJraWQiOiIyMDE3MDQwMi0wMDowMDowMCIsImFsZyI6IlJTMjU2In0.eyJpYW1faWQiOiJJQk1pZC0yNzAwMDdHRjBEIiwiaWQiOiJJQk1pZC0yNzAwMDdHRjBEIiwicmVhbG1pZCI6IklCTWlkIiwiaWRlbnRpZmllciI6IjI3MDAwN0dGMEQiLCJnaXZlbl9uYW1lIjoiVG9tIiwiZmFtaWx5X25hbWUiOiJCbGVuY2giLCJuYW1lIjoiVG9tIEJsZW5jaCIsImVtYWlsIjoidGJsZW5jaEB1ay5pYm0uY29tIiwic3ViIjoidGJsZW5jaEB1ay5pYm0uY29tIiwiYWNjb3VudCI6eyJic3MiOiI1ZTM1ZTZhMjlmYjJlZWNhNDAwYWU0YzNlMWZhY2Y2MSJ9LCJpYXQiOjE1MDA0NjcxMDIsImV4cCI6MTUwMDQ3MDcwMiwiaXNzIjoiaHR0cHM6Ly9pYW0ubmcuYmx1ZW1peC5uZXQvb2lkYy90b2tlbiIsImdyYW50X3R5cGUiOiJ1cm46aWJtOnBhcmFtczpvYXV0aDpncmFudC10eXBlOmFwaWtleSIsInNjb3BlIjoib3BlbmlkIiwiY2xpZW50X2lkIjoiZGVmYXVsdCJ9.XAPdb5K4n2nYih-JWTWBGoKkxTXM31c1BB1g-Ciauc2LxuoNXVTyz_mNqf1zQL07FUde1Cb_dwrbotjickNcxVPost6byQztfc0mRF1x2S6VR8tn7SGiRmXBjLofkTh1JQq-jutp2MS315XbTG6K6m16uYzL9qfMnRvQHxsZWErzfPiJx-Trg_j7OX-qNFjdNUGnRpU7FmULy0r7RxLd8mhG-M1yxVzRBAZzvM63s0XXfMnk1oLi-BuUUTqVOdrM0KyYMWfD0Q72PTo4Exa17V-R_73Nq8VPCwpOvZcwKRA2sPTVgTMzU34max8b5kpTzVGJ6SXSItTVOUdAygZBng\",\"refresh_token\":\"MO61FKNvVRWkSa4vmBZqYv_Jt1kkGMUc-XzTcNnR-GnIhVKXHUWxJVV3RddE8Kqh3X_TZRmyK8UySIWKxoJ2t6obUSUalPm90SBpTdoXtaljpNyormqCCYPROnk6JBym72ikSJqKHHEZVQkT0B5ggZCwPMnKagFj0ufs-VIhCF97xhDxDKcIPMWG02xxPuESaSTJJug7e_dUDoak_ZXm9xxBmOTRKwOxn5sTKthNyvVpEYPE7jIHeiRdVDOWhN5LomgCn3TqFCLpMErnqwgNYbyCBd9rNm-alYKDb6Jle4njuIBpXxQPb4euDwLd1osApaSME3nEarFWqRBzhjoqCe1Kv564s_rY7qzD1nHGvKOdpSa0ZkMcfJ0LbXSQPs7gBTSVrBFZqwlg-2F-U3Cto62-9qRR_cEu_K9ZyVwL4jWgOlngKmxV6Ku4L5mHp4KgEJSnY_78_V2nm64E--i2ZA1FhiKwIVHDOivVNhggE9oabxg54vd63glp4GfpNnmZsMOUYG9blJJpH4fDX4Ifjbw-iNBD7S2LRpP8b8vG9pb4WioGzN43lE5CysveKYWrQEZpThznxXlw1snDu_A48JiL3Lrvo1LobLhF3zFV-kQ=\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"expiration\":1500470702}";
    public static final String IAM_TOKEN_2 = "{\"access_token\":\"eyJraWQiOiIyMDE3MDQwMi0wMDowMDowMCIsImFsZyI6IlJTMjU2In0.eyJpYW1faWQiOiJJQk1pZC0yNzAwMDdHRjBEIiwiaWQiOiJJQk1pZC0yNzAwMDdHRjBEIiwicmVhbG1pZCI6IklCTWlkIiwiaWRlbnRpZmllciI6IjI3MDAwN0dGMEQiLCJnaXZlbl9uYW1lIjoiVG9tIiwiZmFtaWx5X25hbWUiOiJCbGVuY2giLCJuYW1lIjoiVG9tIEJsZW5jaCIsImVtYWlsIjoidGJsZW5jaEB1ay5pYm0uY29tIiwic3ViIjoidGJsZW5jaEB1ay5pYm0uY29tIiwiYWNjb3VudCI6eyJic3MiOiI1ZTM1ZTZhMjlmYjJlZWNhNDAwYWU0YzNlMWZhY2Y2MSJ9LCJpYXQiOjE1MDA0NjcxMTEsImV4cCI6MTUwMDQ3MDcxMSwiaXNzIjoiaHR0cHM6Ly9pYW0ubmcuYmx1ZW1peC5uZXQvb2lkYy90b2tlbiIsImdyYW50X3R5cGUiOiJ1cm46aWJtOnBhcmFtczpvYXV0aDpncmFudC10eXBlOmFwaWtleSIsInNjb3BlIjoib3BlbmlkIiwiY2xpZW50X2lkIjoiZGVmYXVsdCJ9.wJ5Glsvee3xRbfxr847pNgVj-U_ZLLzOiScHcjkrHk0jQdg8D4KurAV1QGa_MwWzd_QxS55lNqCzi6HV1p3kSyjcdJSGe-l-B3_xjw-7Q3BMoPjcO-X1mNYsKQyCtSAJsuByCYQVPoNKuBifsQcds65mKh87gUtc00vP5J-vzdYpzkrjncFO3lzJJwYSnbqFaAPtNnEYwEEIpS0n9H4mgHiLqletzYs9acggssxZpUl2wdkUaQ_diuTJg-u2o6Oy3aVJCWV78DIc3NVwgQCuJ40as6QpFPWluXJmfgdW5lFkQ_etieI9JDgXk_HQUpYcj0Droec6wTXEGUYWjukhsw\",\"refresh_token\":\"M0oCn5XLXUWAFUSqC7FRv1d83-SOfPvYmKKRdZpT33C81KsTaZx3Y3jMXRGkR1sIAohEm-gkpwGQcm1I_lfs5zlqwaKlsLOv4jvjvjiaPFwoU7QP62bHWGsq0j-RNN-_kHXsp3G1R7AtndZL0XQ4se4Jlgt68Cw3_YyEcxS6E65iTv1hZ9lg1EjJqzFLd4ArQVT6gFCpSaRaH2ilie4hat5ZFI2JALHPzVnBlRBqeIUferQOL6Yw2b_Z9TvYa6AaqOsQzI5ma2yIQTw6tzjrc5xXqnqnkH566pNlY8pKvETvCsdLgEclMoa8zoe9SAXDFEIl7svNMRG9FsoR7G4rwojs2BawDPPwkEcm6aC1K5azX23GbnekhvNfXloASWc2ETerN2RxYRZNnFnO4f0enCNReMhoPCUBObgO6iq0a56VslRTT-BHYBCax_YklBz9acbhJnF-C9PWjyrYwZHFajMhpFjOmY3hlrQXVXtjOqKs5WbMhpQ8BWN5KBUDYY7F7OMvv4bYTF7kfu5Uc_ge9_Nj4EGvPwA6vehvZjSj-0td6D32p2zMDmu_yoTLRpv6N7u5BRA5_PmhH_hsffXSKX5fDNL_CqGaNvcI5tVBry8=\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"expiration\":1500470711}";

    // helper for asserts etc
    public static String authSession(String cookie) {
        return String.format(Locale.ENGLISH, "AuthSession=\"%s\"", cookie);
    }

    public static String authSessionUnquoted(String cookie) {
        return String.format(Locale.ENGLISH, "AuthSession=%s", cookie);
    }

    public static String iamSession(String cookie) {
        return String.format(Locale.ENGLISH, "IAMSession=\"%s\"", cookie);
    }

    public static String iamSessionUnquoted(String cookie) {
        return String.format(Locale.ENGLISH, "IAMSession=%s", cookie);
    }

    // helper for asserts etc
    public static String setCookie(String cookie) {
        return String.format(Locale.ENGLISH, "%s%s", cookie, COOKIE_PROPS);
    }

    /*
     * Note explicitly declares domain .local to workaround
     * https://bugs.openjdk.java.net/browse/JDK-7169142
     */
    public static final String COOKIE_PROPS = "; Version=1; Path=/; HttpOnly; Domain=.local";

    /**
     * A mock cookie response that is OK
     */
    public static final MockResponse OK_COOKIE = new MockResponse()
            .setResponseCode(200)
            .addHeader("Set-Cookie",
                    setCookie(authSession(EXPECTED_OK_COOKIE)))
            .setBody("{\"ok\":true,\"name\":\"mockUser\",\"roles\":[]}");

    public static final MockResponse OK_COOKIE_2 = new MockResponse()
            .setResponseCode(200)
            .addHeader("Set-Cookie",
                    setCookie(authSession(EXPECTED_OK_COOKIE_2)))
            .setBody("{\"ok\":true,\"name\":\"mockUser\",\"roles\":[]}");

    public static final MockResponse OK_IAM_COOKIE = new MockResponse()
            .setResponseCode(200)
            .addHeader("Set-Cookie",
                    setCookie(iamSession(EXPECTED_OK_COOKIE)))
            .setBody("{\"ok\":true,\"name\":\"mockUser\",\"roles\":[]}");

    public static final MockResponse OK_IAM_COOKIE_2 = new MockResponse()
            .setResponseCode(200)
            .addHeader("Set-Cookie",
                    setCookie(iamSession(EXPECTED_OK_COOKIE_2)))
            .setBody("{\"ok\":true,\"name\":\"mockUser\",\"roles\":[]}");


    public static final MockResponse JSON_OK = new MockResponse().setResponseCode(200).setBody
            ("{\"ok\":true}");

    public static final MockResponse PERMISSIONS = new MockResponse().setResponseCode(200)
            .setBody("{\"_id\":\"security\", \"cloudant\":{\"user\": [\"_reader\"]}}");

    public static final Dispatcher ALL_429 = new ConstantResponseDispatcher(get429());

    private static final Logger logger = Logger.getLogger(MockWebServerResources.class.getName());

    //Keystore information for https
    private static String KEYSTORE_FILE = "src/test/resources/SslAuthenticationTest.keystore";
    private static String KEYSTORE_PASSWORD = "password";
    private static String KEY_PASSWORD = "password";

    private static SSLContext sslContext = null;

    public static SSLContext getSSLContext() {
        if (sslContext != null) return sslContext;
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(KEYSTORE_FILE), KEYSTORE_PASSWORD
                    .toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            kmf.init(keystore, KEY_PASSWORD.toCharArray());
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing test SSLSocketFactory", e);
            return null;
        }
    }

    public static SSLSocketFactory getSSLSocketFactory() {
        return (getSSLContext() != null) ? getSSLContext().getSocketFactory() : null;
    }

    /**
     * Utility to call takeRequest on a MockWebServer, but using the default timeouts specified in
     * this class to avoid waiting to infinity or a global test timeout.
     *
     * @param mws the mock web server to get the request from
     * @return the recorded request
     * @throws InterruptedException if the wait was interrupted
     */
    public static RecordedRequest takeRequestWithTimeout(MockWebServer mws) throws
            InterruptedException {
        return mws.takeRequest(TIMEOUT, TIMEOUT_UNIT);
    }

    /**
     * A dispatcher that repeatedly returns the same status code for all requests.
     */
    public static class ConstantResponseDispatcher extends Dispatcher {

        private final MockResponse response;

        public ConstantResponseDispatcher(MockResponse response) {
            this.response = response;
        }

        public ConstantResponseDispatcher(int statusCode) {
            this(new MockResponse().setResponseCode(statusCode));
        }

        @Override
        public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
            return response;
        }
    }

    public static MockResponse get429() {
        return new MockResponse().setResponseCode(429)
                .setBody("{\"error\":\"too_many_requests\", \"reason\":\"example reason\"}\r\n");
    }
}
