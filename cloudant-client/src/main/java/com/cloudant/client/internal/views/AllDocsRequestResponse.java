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

package com.cloudant.client.internal.views;

import com.cloudant.client.api.model.Document;
import com.cloudant.client.api.views.AllDocsRequest;
import com.cloudant.client.api.views.AllDocsResponse;
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

    private final AllDocsRequestImpl request;
    private ViewResponse<String, AllDocsValue> response = null;

    AllDocsRequestResponse(ViewQueryParameters<String, AllDocsValue> parameters) {
        request = new AllDocsRequestImpl(parameters);
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
        for (ViewResponse.Row<String, AllDocsValue> row : response.getRows()) {
            AllDocsValue rev = row.getValue();
            if (rev != null) {
                docIdsAndRevs.put(row.getKey(), rev.getRev());
            }
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

    @Override
    public Map<String, String> getErrors() {
        Map<String, String> errors = new HashMap<String, String>();
        for (ViewResponse.Row<String, AllDocsValue> row : response.getRows()) {
            if(row.getError() != null) {
                errors.put(row.getKey(), row.getError());
            }
        }
        return errors;
    }

    /*
         * Object representation of rev field from a JSON object.
         * <P>
         * A convenience to allow deserialization of _all_docs JSON revision object data.
         * </P>
         */
    public static final class AllDocsValue {

        private String rev = null;
        private boolean deleted = false;

        /**
         * @return the value of the document rev field
         */
        public String getRev() {
            return rev;
        }

        /**
         * @return true if deleted
         */
        public boolean isDeleted() {
            return deleted;
        }
    }
}
