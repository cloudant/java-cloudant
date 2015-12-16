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

package com.cloudant.client.internal.views;

import com.google.gson.annotations.SerializedName;

/**
 * <P>
 * This class encapsulates metadata about a page: its number, direction and the query parameters
 * needed to request it. A {@link ViewResponseImpl} generates instances of this class to pass that
 * data to a request when nextPage() or previousPage() is called.
 * </P>
 * <P>
 * Provides static methods for generating page query parameters for forward or backward pages based
 * on specified start keys and paging directions.
 * </P>
 */
final class PageMetadata<K, V> {

    /**
     * The parameters needed to request the page described by this metadata
     */
    final ViewQueryParameters<K, V> pageRequestParameters;

    /**
     * The page number of the page to retrieve
     */
    final long pageNumber;
    /**
     * Indicate the paging direction for this metadata
     */
    final PagingDirection direction;

    enum PagingDirection {
        @SerializedName("F")
        FORWARD,
        @SerializedName("B")
        BACKWARD
    }

    /**
     * Construct metadata that encapsulates the request query for an additional page.
     *
     * @param direction             the direction of the page
     * @param pageNumber            the number of the page
     * @param pageRequestParameters the query parameters to request the page
     */
    PageMetadata(PagingDirection direction, long pageNumber, ViewQueryParameters<K, V>
            pageRequestParameters) {
        // Set the direction and page number.
        this.direction = direction;
        this.pageNumber = pageNumber;
        this.pageRequestParameters = pageRequestParameters;
    }

    /**
     * Generate query parameters for a forward page with the specified start key.
     *
     * @param initialQueryParameters page 1 query parameters
     * @param startkey               the startkey for the forward page
     * @param startkey_docid         the doc id for the startkey (in case of duplicate keys)
     * @param <K>                    the view key type
     * @param <V>                    the view value type
     * @return the query parameters for the forward page
     */
    static <K, V> ViewQueryParameters<K, V> forwardPaginationQueryParameters
    (ViewQueryParameters<K, V> initialQueryParameters, K startkey, String startkey_docid) {

        // Copy the initial query parameters
        ViewQueryParameters<K, V> pageParameters = initialQueryParameters.copy();

        // Now override with the start keys provided
        pageParameters.setStartKey(startkey);
        pageParameters.setStartKeyDocId(startkey_docid);

        return pageParameters;
    }

    /**
     * Generate query parameters for the backward page with the specified startkey.
     * <P>
     * Pages only have a reference to the start key, when paging backwards this is the
     * startkey of the following page (i.e. the last element of the previous page) so to
     * correctly present page results when paging backwards requires some parameters to
     * be reversed for the previous page request
     * </P>
     *
     * @param initialQueryParameters page 1 query parameters
     * @param startkey               the startkey for the backward page
     * @param startkey_docid         the doc id for the start key (in case of duplicate keys)
     * @param <K>                    the view key type
     * @param <V>                    the view value type
     * @return the query parameters for the backward page.
     */
    static <K, V> ViewQueryParameters<K, V> reversePaginationQueryParameters
    (ViewQueryParameters<K, V> initialQueryParameters, K startkey, String startkey_docid) {
        // Get a copy of the parameters, using the forward pagination method
        ViewQueryParameters<K, V> reversedParameters = forwardPaginationQueryParameters
                (initialQueryParameters, startkey,
                        startkey_docid);

        // Now reverse some of the parameters to page backwards.
        // Paging backward is descending from the original direction.
        reversedParameters.setDescending(!initialQueryParameters.getDescending());

        // We must always include our start key if paging backwards so inclusive end is true
        reversedParameters.setInclusiveEnd(true);

        // Any initial startkey is now the end key because we are reversed from original direction
        if (startkey != null) {
            reversedParameters.endkey = initialQueryParameters.startkey;
        }
        if (startkey_docid != null) {
            reversedParameters.setEndKeyDocId(initialQueryParameters.startkey_docid);
        }

        return reversedParameters;
    }

}
