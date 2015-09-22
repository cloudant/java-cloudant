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
import com.cloudant.client.api.views.AllDocsRequestBuilder;
import com.cloudant.client.api.views.ViewRequest;

public class AllDocsRequestBuilderImpl extends CommonViewRequestBuilder<String, Document.Revision,
        AllDocsRequestBuilder> implements AllDocsRequestBuilder {

    public AllDocsRequestBuilderImpl(ViewQueryParameters<String, Document.Revision> parameters) {
        super(parameters);
    }

    @Override
    public AllDocsRequestBuilder returnThis() {
        return this;
    }

    @Override
    public ViewRequest<String, Document.Revision> build() {
        return new ViewRequestImpl<String, Document.Revision>(viewQueryParameters);
    }
}
