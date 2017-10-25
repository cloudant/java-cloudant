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

    private boolean isBuildable;

    private final ViewMultipleRequester<K, V> multiViewRequester;

    public MultipleRequestBuilderImpl(ViewQueryParameters<K, V> params) {
        this(params, null);
    }

    private MultipleRequestBuilderImpl(ViewQueryParameters<K, V> params, ViewMultipleRequester<K, V>
            multiViewRequester) {
        super(params);
        if (multiViewRequester == null) {
            this.multiViewRequester = new ViewMultipleRequester<K, V>();
            // This is a new instance, isBuildable needs to be false until add is called.
            isBuildable = false;
        } else {
            this.multiViewRequester = multiViewRequester;
            // An existing multiViewRequester is only called from add()
            // so we can set isBuildable true
            isBuildable = true;
        }
    }

    @Override
    public MultipleRequestBuilder<K, V> returnThis() {
        // This method is called by the builder whenever parameters are set.
        // Sets isBuildable to false since parameters have been added, but add() has not yet been
        // called.
        this.isBuildable = false;
        return this;
    }

    @Override
    public MultipleRequestBuilder<K, V> add() {
        multiViewRequester.add(this.viewQueryParameters);
        return new MultipleRequestBuilderImpl<K, V>(new
                ViewQueryParameters<K, V>(viewQueryParameters), multiViewRequester);
    }

    @Override
    public ViewMultipleRequest<K, V> build() {
        if (isBuildable) {
            return multiViewRequester;
        } else {
            throw new IllegalStateException("Request is not buildable, call add() before build()");
        }
    }

}
