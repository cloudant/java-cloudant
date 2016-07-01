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
package com.cloudant.http;

/**
 * Created by tomblench on 30/03/15.
 */

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides the context for a {@link HttpConnectionInterceptor}.
 * <P>
 * The context is new for each request or replay of a request. If an interceptor instance needs to
 * carry some state across replays of a request then the state must be stored using the
 * {@link #setState(HttpConnectionInterceptor, String, Object)} method and retrieved using the
 * {@link #getState(HttpConnectionInterceptor, String, Class)} method.
 * </P>
 *
 * @since 2.0.0
 */
public class HttpConnectionInterceptorContext {

    public boolean replayRequest;
    public final HttpConnection connection;
    final Map<HttpConnectionInterceptor, Map<String, Object>> interceptorStates;

    /**
     * Constructor
     *
     * @param connection HttpConnection
     */
    public HttpConnectionInterceptorContext(HttpConnection connection) {
        this(connection, new
                ConcurrentHashMap<HttpConnectionInterceptor, Map<String, Object>>());
    }

    /**
     * Constructor
     *
     * @param connection HttpConnection
     */
    HttpConnectionInterceptorContext(HttpConnection connection, Map<HttpConnectionInterceptor,
            Map<String, Object>> interceptorStates) {
        this.replayRequest = false;
        this.connection = connection;
        this.interceptorStates = interceptorStates;
    }

    /**
     * Shallow copy constructor
     *
     * @param other Context to copy
     */
    public HttpConnectionInterceptorContext(HttpConnectionInterceptorContext other) {
        this.replayRequest = other.replayRequest;
        this.connection = other.connection;
        this.interceptorStates = other.interceptorStates;
    }

    /**
     * Store some state on this request context associated with the specified interceptor instance.
     * Used where a single interceptor instance needs to associate state with each HTTP request.
     *
     * @param interceptor        the interceptor instance
     * @param stateName          the key to store the state object under
     * @param stateObjectToStore the state object to store
     * @param <T>                the type of the state object to store
     * @see #getState(HttpConnectionInterceptor, String, Class)
     * @since 2.6.0
     */
    public <T> void setState(HttpConnectionInterceptor interceptor, String stateName, T
            stateObjectToStore) {
        Map<String, Object> state = interceptorStates.get(interceptor);
        if (state == null) {
            interceptorStates.put(interceptor, (state = new ConcurrentHashMap<String, Object>()));
        }
        state.put(stateName, stateObjectToStore);
    }

    /**
     * Retrieve the state object associated with the specified interceptor instance and property
     * name on this request context.
     *
     * @param interceptor the interceptor instance
     * @param stateName   the name key that the state object was stored under
     * @param stateType   class of the type the stored state should be returned as
     * @param <T>         the type the stored state should be returned as
     * @return the stored state object
     * @see #setState(HttpConnectionInterceptor, String, Object)
     * @since 2.6.0
     */
    public <T> T getState(HttpConnectionInterceptor interceptor, String stateName, Class<T>
            stateType) {
        Map<String, Object> state = interceptorStates.get(interceptor);
        if (state != null) {
            return stateType.cast(state.get(stateName));
        } else {
            return null;
        }
    }
}
