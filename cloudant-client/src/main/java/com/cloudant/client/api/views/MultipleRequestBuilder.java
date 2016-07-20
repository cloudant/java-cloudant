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

package com.cloudant.client.api.views;

/**
 * Interface for building {@link ViewMultipleRequest}s.
 * <P>
 * Example usage:
 * </P>
 * <pre>
 * {@code
 *
 * ViewMultipleRequest<String,String> multiRequest =
 *
 * //get a builder for the"alpha"view of the"directory"design doc
 * db.getViewRequestBuilder("directory","alpha")
 *     //create a new multi request expecting String keys and values
 *     .newMultipleRequest(Key.Type.STRING,String.class)
 *
 *     //add three request queries
 *     .startKey("A").endKey("B").add() //add a request from A to B
 *     .startKey("H").endKey("I").add() //add a request from H to I
 *     .startKey("N").endKey("O").add() //add a request from N to O
 *
 *     //build the request
 *     .build()
 * }
 * </pre>
 *
 * @param <K> the type of key emitted by the view,fixed by the
 *            {@link com.cloudant.client.api.views.Key.Type}supplied to the
 *            {@link ViewRequestBuilder}
 * @param <V> the type of value emitted by the view,specified when supplied to the
 *            {@link ViewRequestBuilder}
 * @since 2.0.0
 */
public interface MultipleRequestBuilder<K, V> extends RequestBuilder<MultipleRequestBuilder<K, V>>,
        SettableViewParameters.Unpaginated<K, MultipleRequestBuilder<K, V>>,
        SettableViewParameters.Reduceable<K, MultipleRequestBuilder<K, V>> {

    /**
     * Adds a query request to this MultipleRequestBuilder.
     *
     * @return the request builder ready to compose the next request
     * @since 2.0.0
     */
    MultipleRequestBuilder<K, V> add();

    /**
     * Called after composing the multiple requests to build the ViewMultipleRequest.
     * <P>
     * It is only valid to call build() after adding one or more requests. If parameters have
     * been set but the request has not been added then an IllegalStateException will be thrown.
     * </P>
     *
     * @return the built ViewMultipleRequest that can perform the request
     * @throws IllegalStateException if add() was not the last call before build
     * @since 2.0.0
     */
    ViewMultipleRequest<K, V> build();
}
