/*
 * Copyright © 2018 IBM Corp. All rights reserved.
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

public class AllDocsResponseImpl extends ViewResponseImpl<String, AllDocsRequestResponse
        .AllDocsValue> {

    AllDocsResponseImpl(ViewQueryParameters<String, AllDocsRequestResponse.AllDocsValue>
                                initialQueryParameters, JsonObject response, PageMetadata<String,
            AllDocsRequestResponse.AllDocsValue> pageMetadata) {
        super(initialQueryParameters, response, pageMetadata);
    }

    @Override
    protected Row fromJson(JsonElement row) {
        return new AllDocsRowImpl(initialQueryParameters, row);
    }

    static class AllDocsRowImpl extends RowImpl<String, AllDocsRequestResponse.AllDocsValue> {

        AllDocsRowImpl(ViewQueryParameters<String, AllDocsRequestResponse.AllDocsValue>
                               parameters, JsonElement row) {
            super(parameters, row);
        }

        @Override
        public <D> D getDocumentAsType(Class<D> docType) {
            D doc = super.getDocumentAsType(docType);
            if (doc == null) {
                // The doc could be deleted, or include docs might be false, we can try to generate
                // a sparse doc for these cases.
                AllDocsRequestResponse.AllDocsValue v = getValue();
                if (v != null) {
                    JsonObject sparse = new JsonObject();
                    sparse.addProperty("_id", getId());
                    sparse.addProperty("_rev", v.getRev());
                    sparse.addProperty("_deleted", v.isDeleted());
                    doc = gson.fromJson(sparse, docType);
                }
            }
            return doc;
        }
    }

    @Override
    public List<Document> getDocs() {
        // This overrides the super to bypass the includeDocs check which is not needed for the all
        // docs case.
        return internalGetDocs();
    }

}
