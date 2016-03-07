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
 * Interface for building an unpaginated {@link ViewRequest}.
 * <P>
 * Example usage:
 * </P>
 * <pre>
 * {@code
 *
 * ViewRequest<String, String> viewRequest =
 * //get a builder for the "foo" view of the "example" design doc
 * db.getViewRequestBuilder("example","foo")
 *
 *      //create a new request expecting String keys and values
 *      .newRequest(Key.Type.STRING, String.class)
 *
 *      //set any other required parameters
 *      .startKey("bar")
 *
 *      //build the request
 *      .build();
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
public interface UnpaginatedRequestBuilder<K, V> extends
        SingleRequestBuilder<K, V, UnpaginatedRequestBuilder<K, V>>,
        SettableViewParameters.Unpaginated<K, UnpaginatedRequestBuilder<K, V>>,
        SettableViewParameters.Reduceable<K, UnpaginatedRequestBuilder<K, V>> {
}
