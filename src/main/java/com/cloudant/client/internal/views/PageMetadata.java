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

/**
 * <P>
 * Object for serializing metadata about a page.
 * </P>
 * <P>
 * This class is used to persist the parameter values for a page. A {@link ViewResponseImpl}
 * calculates the parameters required to get the pages preceding and following it and generates
 * instances of this class to pass that data to a request when nextPage() or previousPage() is
 * called.
 * </P>
 */
final class PageMetadata<K, V> {

    /**
     * The parameters needed to request the page described by this metadata
     */
    ViewQueryParameters<K, V> pageRequestParameters;

    /**
     * The page number of the page to retrieve
     */
    long pageNumber;
    /**
     * Indicate the paging direction for this metadata
     */
    PagingDirection direction;

    enum PagingDirection {
        FORWARD,
        BACKWARD
    }

    PageMetadata(PagingDirection direction) {
        this.direction = direction;
    }
}
