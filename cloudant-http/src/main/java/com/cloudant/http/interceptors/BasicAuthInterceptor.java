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

import com.cloudant.http.internal.Base64OutputStreamFactory;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adds basic authentication support to HTTP requests by adding an Authorization header.
 *
 * Direct use of this class is rarely required as the client will construct one as needed.
 *
 */
public class BasicAuthInterceptor implements HttpConnectionRequestInterceptor {

    protected final String authHeader;
    protected final String encodedAuth;

    /**
     * Constructs a {@code BasicAuthInterceptor} using userinfo details.
     *
     * @param userinfo the username and password separated by a single colon (":") character
     */
    public BasicAuthInterceptor(String userinfo) {
        this(userinfo, "Authorization");
    }

    protected BasicAuthInterceptor(String userinfo, String authHeader) {
        this.encodedAuth = encodedCreds(userinfo);
        this.authHeader = authHeader;
    }

    /**
     * Returns a {@link BasicAuthInterceptor} configured using the username and password provided.
     *
     * @param username the username to use
     * @param password the password to use
     * @return configured {@code BasicAuthInterceptor}
     */
    public static BasicAuthInterceptor createFromCredentials(String username, String password) {
        return new BasicAuthInterceptor(username + ":" + password);
    }

    @Override
    public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext
                                                                     context) {
        context.connection.requestProperties.put(authHeader, String.format("Basic %s",
                encodedAuth));
        return context;
    }

    private String encodedCreds(String userInfo) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream bos = Base64OutputStreamFactory.get(baos);
            bos.write(userInfo.getBytes("UTF-8"));
            bos.flush();
            bos.close();
            return baos.toString("UTF-8");
        } catch (IOException e) {
            Logger.getLogger(BasicAuthInterceptor.class.getName()).log(Level.SEVERE, "IOException" +
                    "during credential encoding", e);
            return null;
        }
    }
}
