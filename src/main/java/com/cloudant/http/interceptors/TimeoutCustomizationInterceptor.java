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

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;

import java.net.HttpURLConnection;

public class TimeoutCustomizationInterceptor implements HttpConnectionRequestInterceptor {


    private final int connectTimeout;
    private final int readTimeout;

    /**
     * Construct a new timeout interceptor with the specified connect and read TimeoutOptions.
     * <P>
     * Example to create a timeout interceptor with 10 second connect timeout and 5 minute read
     * timeout.
     * </P>
     * <pre>
     *     {@code
     *     new TimeoutCustomizationInterceptor(new TimeoutOption(10, TimeUnit.SECONDS),
     *                                         new TimeoutOption(5, TimeUnit.MINUTES));
     *     }
     * </pre>
     *
     * @param connectTimeout value of the connect timeout
     * @param readTimeout    value of the read timeout
     *
     * @throws CouchDbException if connect or read timeout are null
     */
    public TimeoutCustomizationInterceptor(ClientBuilder.TimeoutOption connectTimeout,
                                           ClientBuilder.TimeoutOption
            readTimeout) {
        if(connectTimeout != null && connectTimeout.asIntMillis() >= 0) {
            this.connectTimeout = connectTimeout.asIntMillis();
        } else {
            throw new CouchDbException("Connection timeout cannot be null.");
        }
        if(readTimeout != null && readTimeout.asIntMillis() >= 0) {
            this.readTimeout = readTimeout.asIntMillis();
        } else {
            throw new CouchDbException("Read timeout cannot be null.");
        }
    }

    @Override
    public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext
                                                                     context) {
        HttpURLConnection connection = context.connection.getConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        return context;
    }
}
