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

    private static final TimeoutOption DEFAULT_TIMEOUT = new TimeoutOption(5l, TimeUnit.MINUTES);

    private final int connectTimeout;
    private final int readTimeout;

    /**
     * Construct a default interceptor with a connect timeout and read timeout of 5 minutes.
     */
    public TimeoutCustomizationInterceptor() {
        this(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
    }

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
     */
    public TimeoutCustomizationInterceptor(TimeoutOption connectTimeout, TimeoutOption
            readTimeout) {
        this.connectTimeout = (connectTimeout != null) ? connectTimeout.asIntMillis() :
                DEFAULT_TIMEOUT.asIntMillis();
        this.readTimeout = (readTimeout != null) ? readTimeout.asIntMillis() : DEFAULT_TIMEOUT
                .asIntMillis();
    }

    @Override
    public HttpConnectionInterceptorContext interceptRequest(HttpConnectionInterceptorContext
                                                                     context) {
        HttpURLConnection connection = context.connection.getConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        return context;
    }

    public static final class TimeoutOption {

        private final int timeoutMillis;

        public TimeoutOption(long timeout, TimeUnit timeoutUnit) {
            Long to = timeoutUnit.toMillis(timeout);
            if (timeout < 0) {
                timeoutMillis = 0;
            } else if (timeout > Integer.MAX_VALUE) {
                timeoutMillis = Integer.MAX_VALUE;
            } else {
                timeoutMillis = to.intValue();
            }
        }

        private int asIntMillis() {
            return timeoutMillis;
        }
    }
}
