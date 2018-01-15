/*
 * Copyright © 2017, 2018 IBM Corp. All rights reserved.
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

import com.cloudant.client.api.Database;
import com.cloudant.client.api.query.Expression;
import com.cloudant.client.api.query.Field;
import com.cloudant.client.api.query.Operation;
import com.cloudant.client.api.query.Selector;
import com.cloudant.client.api.query.TextIndex;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract index definition builder to be extended by specific index type builders.
 *
 * @param <I> the typed index class
 * @param <B> the typed index class builder
 */
public abstract class Builder<I extends InternalIndex<D, F>, D extends Definition<F>, B
        extends Builder, F extends Field> {

    /**
     * The new instance of the typed index
     */
    protected I instance = newInstance();

    /**
     * Configure the name of the index, if not set a name will be generated.
     *
     * @param indexName name of the index
     * @return the builder for chaining
     */
    public B name(String indexName) {
        instance.name = indexName;
        return returnThis();
    }

    /**
     * Configure the design document name, if not set a new design document will be created with
     * a generated name.
     *
     * @param designDocumentId design document ID (the _design prefix is added if not supplied)
     * @return the builder for chaining
     */
    public B designDocument(String designDocumentId) {
        instance.ddoc = designDocumentId;
        return returnThis();
    }

    /**
     * Configure a selector to choose documents that should be added to the index.
     */
    public B partialFilterSelector(Selector selector) {
        instance.def.selector = Helpers.getJsonObjectFromSelector(selector);
        return returnThis();
    }

    /**
     * Add fields to the text index configuration.
     *
     * @param fields the {@link TextIndex.Field} configurations to add
     * @return the builder for chaining
     */
    protected B fields(List<F> fields) {
        if (instance.def.fields == null) {
            instance.def.fields = new ArrayList<F>(fields.size());
        }
        instance.def.fields.addAll(fields);
        return returnThis();
    }

    /**
     * @return the string form of the JSON object encapsulating the index definition
     * @see Database#createIndex(String)
     */
    public String definition() {
        JsonObject indexAsJson = new Gson().toJsonTree(instance).getAsJsonObject();
        // Merge the def fields into the index object to cope with create/list asymmetry
        JsonObject definition = indexAsJson.getAsJsonObject("def");
        indexAsJson.remove("def");
        indexAsJson.add("index", definition);
        return indexAsJson.toString();
    }

    /**
     * @return the instance of the typed index builder
     */
    protected abstract B returnThis();

    /**
     * @return a new instance of the typed index
     */
    protected abstract I newInstance();
}
