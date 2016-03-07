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

/**
 * Enables retrieving view responses.
 *
 * @param <K> the type of key emitted by the view
 * @param <V> the type of value emitted by the view
 * @since 2.0.0
 */
public interface ViewRequest<K, V> {

    /**
     * Performs the request and returns the response.
     * <P>
     * Example usage:
     * </P>
     * <pre>
     * {@code
     * ViewResponse<String, String> response = db.getViewRequestBuilder("designDoc","viewName")
     *                   .newRequest(Key.Type.STRING, String.class)
     *                   .build()
     *                   .getResponse();
     * }
     * </pre>
     *
     * @return the response object
     * @throws IOException if there is an error communicating with the server
     * @since 2.0.0
     */
    ViewResponse<K, V> getResponse() throws IOException;

    /**
     * Performs view request for the page represented by a pagination token obtained from a
     * previous request.
     * <P>
     * Example usage for stateless pagination:
     * </P>
     * <pre>
     * {@code
     * ViewResponse<String, String> responsePage1 = db.getViewRequestBuilder("designDoc","viewName")
     *                   .newRequest(Key.Type.STRING, String.class)
     *                   .build()
     *                   .getResponse();
     * String tokenForPage2 = responsePage1.getNextPageToken();
     *
     * // To request the next page:
     * // 1. Recreate the view request by supplying the same parameters to the builder.
     * // 2. Execute the request using the pagination token retrieved above.
     * ViewResponse<String, String> responsePage2 = db.getViewRequestBuilder("designDoc","viewName")
     *                   .newRequest(Key.Type.STRING, String.class)
     *                   .build()
     *                   .getResponse(tokenForPage2);
     * }
     * </pre>
     *
     * @param paginationToken obtained from a previous ViewResponse, {@code null} is equivalent to
     *                        {@link #getResponse()}
     * @return the response object
     * @throws IOException if there is an error communicating with the server
     * @see ViewResponse#getNextPageToken()
     * @see ViewResponse#getPreviousPageToken()
     * @since 2.1.0
     */
    ViewResponse<K, V> getResponse(String paginationToken) throws IOException;

    /**
     * Performs the request and returns a single value.
     * <P>
     * This is primarily intended for retrieving a single result (e.g. count) from a reduced view.
     * </P>
     * <P>
     * Example usage for a view with a "_count" reduce function:
     * </P>
     * <pre>
     * {@code
     * Integer count = db.getViewRequestBuilder("designDoc","viewWithCount")
     *                   .newRequest(Key.Type.STRING, Integer.class)
     *                   .reduce(true) //this is the default, but shown for clarity
     *                   .build()
     *                   .getSingleValue();
     * }
     * </pre>
     *
     * @return value from the first result row or {@code null} if there were no rows
     * @throws IOException if there is an error communicating with the server
     * @since 2.0.0
     */
    V getSingleValue() throws IOException;

}
