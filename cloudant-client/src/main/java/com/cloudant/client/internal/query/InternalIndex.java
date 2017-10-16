/*
 * Copyright © 2017 IBM Corp. All rights reserved.
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

package com.cloudant.client.internal.query;

import com.cloudant.client.api.query.Field;
import com.cloudant.client.api.query.Index;

import java.util.List;

/**
 * Abstract class for common index configuration properties such as the name, design document, type
 * and list of fields.
 *
 * @param <D> index definition type
 */
public class InternalIndex<D extends Definition<F>, F extends Field> implements Index<F> {

    protected String ddoc;
    protected String name;
    protected D def;

    private String type;

    /**
     * Constructor for sub-classes to instantiate
     *
     * @param type the type of the index
     */
    protected InternalIndex(String type) {
        this.type = type;
    }

    /**
     * @return the design document ID
     */
    @Override
    public String getDesignDocumentID() {
        return this.ddoc;
    }

    /**
     * @return the name of the index
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * @return the type of the index
     */
    @Override
    public String getType() {
        return this.type;
    }

    /**
     * Get the JSON string representation of the selector configured for this index.
     *
     * @return selector JSON as string
     */
    @Override
    public String getPartialFilterSelector() {
        return (def.selector != null) ? def.selector.toString() : null;
    }

    /**
     * @return the list of fields in the index
     */
    @Override
    public List<F> getFields() {
        return this.def.fields;
    }
}
