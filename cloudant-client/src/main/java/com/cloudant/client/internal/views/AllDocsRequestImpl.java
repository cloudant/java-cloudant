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

import java.io.IOException;

public class AllDocsRequestImpl extends ViewRequestImpl<String, AllDocsRequestResponse
        .AllDocsValue> {

    AllDocsRequestImpl(ViewQueryParameters<String, AllDocsRequestResponse.AllDocsValue>
                               viewQueryParameters) {
        super(viewQueryParameters);
    }

    @Override
    protected ViewResponseImpl<String, AllDocsRequestResponse.AllDocsValue> makeResponse
            (PageMetadata<String, AllDocsRequestResponse.AllDocsValue> metadata) throws
            IOException {
        ViewQueryParameters<String, AllDocsRequestResponse.AllDocsValue> requestParameters =
                (metadata != null) ? metadata.pageRequestParameters : viewQueryParameters;
        return new AllDocsResponseImpl(viewQueryParameters, ViewRequester.getResponseAsJson
                (requestParameters), metadata);
    }

}
