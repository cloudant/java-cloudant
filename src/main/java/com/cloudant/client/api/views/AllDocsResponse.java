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
     * @return a list of Document objects from the _all_docs request
     * @throws IllegalStateException if include_docs was {@code false}
     * @since 2.0.0
     */
    List<Document> getDocs();

    /**
     * Gets a map of the document id and revision for each result in the _all_docs request.
     *
     * @return a map with an entry for each document, key of _id and value of _rev
     * @since 2.0.0
     */
    Map<String, String> getIdsAndRevs();

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
     * @return a list of the document ids
     */
    List<String> getDocIds();

}
