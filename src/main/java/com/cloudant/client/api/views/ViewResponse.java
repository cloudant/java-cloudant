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

import com.cloudant.client.api.model.Document;

import java.io.IOException;
import java.util.List;

/**
 * Encapsulates a response from a view request.
 * <P>
 * Provides methods to facilitate processing of the response.
 * </P>
 *
 * @param <K> the type of key emitted by the view
 * @param <V> the type of value emitted by the view
 * @since 2.0.0
 */
public interface ViewResponse<K, V> extends Iterable<ViewResponse<K, V>> {

    /**
     * @return list of the result rows on this page
     * @since 2.0.0
     */
    List<Row<K, V>> getRows();

    /**
     * @return list of the keys from all of the result rows on this page
     * @since 2.0.0
     */
    List<K> getKeys();

    /**
     * @return the values from all of the result rows on this page
     * @since 2.0.0
     */
    List<V> getValues();

    /**
     * @return the documents from all of the result rows on this page
     * @throws IllegalStateException if include_docs was {@code false}
     * @since 2.0.0
     */
    List<Document> getDocs();

    /**
     * Deserializes the included full content of result documents to a list of the specified type.
     *
     * @param docType the class type to deserialize the JSON document to
     * @param <D>     the type of the document
     * @return the deserialized document
     * @throws IllegalStateException if include_docs was {@code false}
     * @since 2.0.0
     */
    <D> List<D> getDocsAs(Class<D> docType);

    /**
     * Encapsulates a single row from the response results array.
     *
     * @param <K> the type of key emitted by the view
     * @param <V> the type of value emitted by the view
     * @since 2.0.0
     */
    interface Row<K, V> {

        /**
         * @return the ID of the row
         * @since 2.0.0
         */
        String getId();

        /**
         * @return the key emitted by the map function for this row
         * @since 2.0.0
         */
        K getKey();

        /**
         * @return the value emitted by the map function for this row
         * @since 2.0.0
         */
        V getValue();

        /**
         * @return the document for this row
         * @throws IllegalStateException if there is no doc content for the row
         * @since 2.0.0
         */
        Document getDocument();

        /**
         * Deserializes the included full content of a result document to the specified type.
         *
         * @param docType the class type to deserialize the JSON document to
         * @param <D>     the type of the document
         * @return an instance of the document
         * @throws IllegalStateException if there is no doc content for the row
         * @since 2.0.0
         */
        <D> D getDocumentAsType(Class<D> docType);
    }

    /**
     * @return true if there is a next page
     * @since 2.0.0
     */
    boolean hasNextPage();

    /**
     * @return true if there is a previous page
     * @since 2.0.0
     */
    boolean hasPreviousPage();

    /**
     * @return the next page of results from this view query
     * @throws IOException if there is an error communicating with the server
     * @since 2.0.0
     */
    ViewResponse<K, V> nextPage() throws IOException;

    /**
     * @return the previous page of results from this view query
     * @throws IOException if there is an error communicating with the server
     * @since 2.0.0
     */
    ViewResponse<K, V> previousPage() throws IOException;

    /**
     * Returns an opaque pagination token for the next page of results, which can be used with
     * {@link ViewRequest#getResponse(String)} to retrieve the next page without keeping
     * ViewResponse state.
     *
     * @return opaque pagination token for the next page, or {@code null} if there is no next page
     * @see ViewRequest#getResponse(String)
     * @since 2.1.0
     */
    String getNextPageToken();

    /**
     * Returns an opaque pagination token for the previous page of results, which can be used with
     * {@link ViewRequest#getResponse(String)} to retrieve the previous page without keeping
     * ViewResponse state.
     *
     * @return opaque pagination token for the previous page, or {@code null} if there is no
     * previous page
     * @see ViewRequest#getResponse(String)
     * @since 2.1.0
     */
    String getPreviousPageToken();

    /**
     * <P>
     * Get the page number of this response.
     * </P>
     * <P>
     * Useful for displaying a page number when using
     * {@link ViewRequestBuilder#newPaginatedRequest(Key.Type, Class)}
     * </P>
     * <P>
     * Will always be 1 for an unpaginated request using
     * {@link ViewRequestBuilder#newRequest(Key.Type, Class)}
     * </P>
     *
     * @return the page number
     * @since 2.0.0
     */
    Long getPageNumber();

    /**
     * @return the count of the first row on this page
     * @since 2.0.0
     */
    Long getFirstRowCount();

    /**
     * @return the count of the last row on this page
     * @since 2.0.0
     */
    Long getLastRowCount();

    /**
     * @return the total number of rows in the view, before any filtering has taken place
     * @since 2.0.0
     */
    Long getTotalRowCount();

}
