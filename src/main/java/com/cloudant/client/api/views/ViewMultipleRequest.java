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

import java.io.IOException;
import java.util.List;

/**
 * <P>
 * A convenience class for performing multiple unpaginated query requests on a single view.
 * </P>
 * <P>The request is built by a {@link MultipleRequestBuilder}.
 * </P>
 * <P>
 * Example usage:
 * </P>
 * <pre>
 * {@code
 *
 * List<ViewResponse<String, String>> responses =
 *
 * //get a builder for the "alpha" view of the "directory" design doc
 * db.getViewRequestBuilder("directory","alpha")
 *
 *     //create a new multi request expecting String keys and values
 *     .newMultipleRequest(Key.Type.STRING, String.class)
 *
 *     //add three request queries
 *     .startKey("A").endKey("B").add() //add a request from A to B
 *     .startKey("H").endKey("I").add() //add a request from H to I
 *     .startKey("N").endKey("O").add() //add a request from N to O
 *
 *     //build the request; note the use of buildMulti() not build()
 *     .buildMulti()
 *
 *     //do the POST to get the responses
 *     .getViewResponses();
 *
 * //loop the responses (in order the requests were added)
 * for (ViewResponse<String, String> response : responses) {
 *     //first response is result from A to B
 *     //second response is result from H to I
 *     //third response is result from N to O
 * }
 *
 * }
 * </pre>
 * <a target="_blank"
 * href="https://docs.cloudant.com/creating_views.html#sending-several-queries-to-a-view">
 * Cloudant API reference
 * </a>
 *
 * @param <K> the type of key emitted by the view, fixed by the
 *            {@link com.cloudant.client.api.views.Key.Type} supplied to the
 *            {@link ViewRequestBuilder}
 * @param <V> the type of value emitted by the view, specified when supplied to the
 *            {@link ViewRequestBuilder}
 * @since 2.0.0
 */
public interface ViewMultipleRequest<K, V> {

    /**
     * Perform a single POST request to get the responses for the queries built into this request.
     *
     * @return a list of the responses, one for each query
     * @throws IOException if there is an error communicating with the server
     * @since 2.0.0
     */
    List<ViewResponse<K, V>> getViewResponses() throws IOException;
}
