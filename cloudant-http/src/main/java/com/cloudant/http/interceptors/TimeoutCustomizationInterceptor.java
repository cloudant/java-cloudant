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
import java.util.concurrent.TimeUnit;

public class TimeoutCustomizationInterceptor implements HttpConnectionRequestInterceptor {


    private final int connectTimeout;
    private final int readTimeout;

    /**
     * Construct a new timeout interceptor with the specified connect and read timeouts.
     * <P>
     * Example to create a timeout interceptor with 10 second connect timeout and 5 minute read
     * timeout.
     * </P>
     * <pre>
     *     {@code
     *     new TimeoutCustomizationInterceptor(10, TimeUnit.SECONDS, 5, TimeUnit.MINUTES);
     *     }
     * </pre>
     *
     * @param connectTimeoutValue value of the connect timeout
     * @param connectTimeoutUnit  TimeUnit for the duration of the connect timeout
     * @param readTimeoutValue    value of the read timeout
     * @param readTimeoutUnit     TimeUnit for the duration of the read timeout
     */
    public TimeoutCustomizationInterceptor(long connectTimeoutValue, TimeUnit connectTimeoutUnit,
                                           long readTimeoutValue, TimeUnit readTimeoutUnit) {
        this.connectTimeout = asIntMillis(connectTimeoutValue, connectTimeoutUnit);
        this.readTimeout = asIntMillis(readTimeoutValue, readTimeoutUnit);
    }

    @Override
    public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext
                                                                     context) {
        HttpURLConnection connection = context.connection.getConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        return context;
    }

    private int asIntMillis(long timeout, TimeUnit timeoutUnit) {
        int timeoutMillis;
        Long timeoutLongMillis = timeoutUnit.toMillis(timeout);
        if (timeoutLongMillis < 0) {
            timeoutMillis = 0;
        } else if (timeoutLongMillis > Integer.MAX_VALUE) {
            timeoutMillis = Integer.MAX_VALUE;
        } else {
            timeoutMillis = timeoutLongMillis.intValue();
        }
        return timeoutMillis;
    }
}
