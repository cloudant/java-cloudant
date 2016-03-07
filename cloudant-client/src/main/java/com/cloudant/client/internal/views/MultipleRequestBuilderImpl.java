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

import com.cloudant.client.api.views.MultipleRequestBuilder;
import com.cloudant.client.api.views.ViewMultipleRequest;

public class MultipleRequestBuilderImpl<K, V> extends
        CommonViewRequestBuilder<K, V, MultipleRequestBuilder<K, V>> implements
        MultipleRequestBuilder<K, V> {

    private boolean isBuildable = false;

    private final ViewMultipleRequester<K, V> multiViewRequester;

    public MultipleRequestBuilderImpl(ViewQueryParameters<K, V> params) {
        this(params, null);
    }

    private MultipleRequestBuilderImpl(ViewQueryParameters<K, V> params, ViewMultipleRequester<K, V>
            multiViewRequester) {
        super(params);
        this.multiViewRequester = (multiViewRequester == null) ? new ViewMultipleRequester<K, V>
                () : multiViewRequester;
        //one at least one request has been added, then it is buildable
        isBuildable = true;
    }

    @Override
    public MultipleRequestBuilder<K, V> returnThis() {
        //parameters have been set, not buildable until add() is called
        isBuildable = false;
        return this;
    }

    public MultipleRequestBuilder<K, V> add() {
        multiViewRequester.add(this.viewQueryParameters);
        return new MultipleRequestBuilderImpl<K, V>(new
                ViewQueryParameters<K, V>(viewQueryParameters),
                multiViewRequester);
    }

    public ViewMultipleRequest<K, V> build() {
        if (isBuildable) {
            return multiViewRequester;
        } else {
            throw new IllegalStateException("Request is not buildable, call add() before build()");
        }
    }

}
