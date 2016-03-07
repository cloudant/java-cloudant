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

/**
 * A request builder for a single request on a view.
 *
 * @param <K>  the type of key emitted by the view
 * @param <V>  the type of value emitted by the view
 * @param <RB> the implementing request builder type
 * @since 2.0.0
 */
public interface SingleRequestBuilder<K, V, RB> extends RequestBuilder<RB> {

    /**
     * @return the built ViewRequest
     * @since 2.0.0
     */
    ViewRequest<K, V> build();
}
