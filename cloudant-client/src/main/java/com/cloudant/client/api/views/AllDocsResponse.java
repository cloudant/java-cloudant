/*
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

import java.util.List;
import java.util.Map;

/**
 * Encapsulates a response from an _all_docs request.
 * <P>
 * Provides methods to facilitate processing of the response.
 * </P>
 *
 * @since 2.0.0
 */
public interface AllDocsResponse {

    /**
     * <P>
     * Get the document information from an _all_docs request.
     * </P>
     * <P>
     * Note that if requesting docs using {@link AllDocsRequestBuilder#keys(Object[])} the list of
     * documents may include <b>deleted</b> documents that have one of the specified ids.
     * </P>
     * <P>
     * Note if {@link AllDocsRequestBuilder#includeDocs(boolean)} is false then attachment metadata
     * will not be present.
     * </P>
     * @return a list of Document objects from the _all_docs request
     * @since 2.0.0
     */
    List<Document> getDocs();

    /**
     * <P>
     * Gets a map of the document id and revision for each result in the _all_docs request.
     * </P>
     * <P>
     * Note that if requesting docs using {@link AllDocsRequestBuilder#keys(Object[])} the ids and
     * revs may include <b>deleted</b> documents that have one of the specified ids.
     * </P>
     *
     * @return a map with an entry for each document, key of _id and value of _rev
     * @since 2.0.0
     */
    Map<String, String> getIdsAndRevs();

    /**
     * <P>
     * Deserializes the included full content of result documents to a list of the specified type.
     * </P>
     * <P>
     * Note that if requesting docs using {@link AllDocsRequestBuilder#keys(Object[])} the list of
     * documents may include <b>deleted</b> documents that have one of the specified ids. You may
     * want to ensure that your document type can support checking of the deleted flag.
     * </P>
     *
     * @param docType the class type to deserialize the JSON document to
     * @param <D>     the type of the document
     * @return the deserialized document
     * @throws IllegalStateException if include_docs was {@code false}
     * @since 2.0.0
     */
    <D> List<D> getDocsAs(Class<D> docType);

    /**
     * @return a list of the document ids
     */
    List<String> getDocIds();

    /**
     * Gets a map of the document id and error message if an error exists for any result
     * in the _all_docs request.
     * For example, if a doc id does not exist the error message will show "not_found".
     *
     * @return a map with an entry for key of the doc id and value of error
     * @since 2.10.0
     */
    Map<String, String> getErrors();

}
