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

import com.cloudant.client.api.model.Document;
import com.cloudant.client.api.views.AllDocsRequest;
import com.cloudant.client.api.views.AllDocsResponse;
import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class wraps a ViewRequest and ViewResponse for easier processing of _all_docs requests and
 * responses.
 */
public class AllDocsRequestResponse implements AllDocsRequest, AllDocsResponse {

    private final ViewRequest<String, Revision> request;
    private ViewResponse<String, Revision> response = null;

    AllDocsRequestResponse(ViewQueryParameters<String, Revision> parameters) {
        request = new ViewRequestImpl<String, Revision>(parameters);
    }


    @Override
    public AllDocsResponse getResponse() throws IOException {
        response = request.getResponse();
        return this;
    }

    @Override
    public List<Document> getDocs() {
        return response.getDocs();
    }

    @Override
    public Map<String, String> getIdsAndRevs() {
        Map<String, String> docIdsAndRevs = new HashMap<String, String>();
        for (ViewResponse.Row<String, Revision> row : response.getRows()) {
            docIdsAndRevs.put(row.getKey(), row.getValue().get());
        }
        return docIdsAndRevs;
    }

    @Override
    public <D> List<D> getDocsAs(Class<D> docType) {
        return response.getDocsAs(docType);
    }

    @Override
    public List<String> getDocIds() {
        return response.getKeys();
    }

    /*
         * Object representation of rev field from a JSON object.
         * <P>
         * A convenience to allow deserialization of _all_docs JSON revision object data.
         * </P>
         */
    public static final class Revision {

        private String rev;

        /**
         * @return the value of the document rev field
         */
        public String get() {
            return rev;
        }
    }
}
