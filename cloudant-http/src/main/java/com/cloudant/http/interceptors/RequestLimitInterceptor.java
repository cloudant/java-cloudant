/*
 * Copyright (c) 2016 IBM Corp. All rights reserved.
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
import com.cloudant.http.HttpConnectionResponseInterceptor;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RequestLimitInterceptor implements HttpConnectionResponseInterceptor {

    private static final Logger logger = Logger.getLogger(RequestLimitInterceptor.class.getName());
    // Initial backoff time is 250 ms.
    // Note: not final to allow modification via reflection for faster tests.
    private static int initialSleep = 250;
    private int exp = 0;

    @Override
    public HttpConnectionInterceptorContext interceptResponse(HttpConnectionInterceptorContext
                                                                      context) {
        try {
            HttpURLConnection urlConnection = context.connection.getConnection();
            int code = urlConnection.getResponseCode();
            if (429 == code && context.connection.getNumberOfRetriesRemaining() >= 1) {
                // Too many requests, we need to backoff before retrying

                // If the response includes a Retry-After then that is when we will retry, otherwise
                // we use a doubling sleep
                long sleepTime = 0l;
                String retryAfter = urlConnection.getHeaderField("Retry-After");
                if (retryAfter != null) {
                    // See https://tools.ietf.org/html/rfc6585#section-4
                    // Whilst not specified for 429 for 3xx or 503 responses the Retry-After header
                    // is expressed as an integer number of seconds or a date in one of the 3 HTTP
                    // date formats https://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3
                    // Cloudant servers should give us an integer number of seconds, so don't worry
                    // about parsing dates for now.
                    sleepTime = Long.parseLong(retryAfter) * 1000;
                } else {
                    // Calculate the backoff time, 2^n * initial sleep
                    sleepTime = initialSleep * (int) Math.pow(2, exp);
                }
                // Read the reasons and log a warning
                InputStream errorStream = urlConnection.getErrorStream();
                try {
                    String errorString = IOUtils.toString(errorStream, "UTF-8");
                    logger.warning(errorString + " will retry in " +
                            sleepTime + " ms");

                } finally {
                    errorStream.close();
                }


                logger.fine("Too many requests backing off for " + sleepTime + " ms.");

                // Sleep the thread for the appropriate backoff time
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    logger.fine("Interrupted during 429 backoff wait.");
                    // If the thread was interrupted we'll just continue and try again a bit earlier
                    // than planned.
                }

                // Get ready to replay the request after the backoff time
                context.replayRequest = true;
                exp++;
                return context;
            } else {
                return context;
            }
        } catch (IOException e) {
            throw new HttpConnectionInterceptorException(e);
        }
    }
}
