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

package com.cloudant.client.api.model;

import com.cloudant.client.api.Database;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Options to set on findByIndex() request.
 * <p>Example:
 * <pre>
 * database.findByIndex(
 * 	   " \"selector\": { \"Movie_year\": {\"$gt\": 1960}, \"Person_name\": \"Alec Guinness\" }"
 * 		Movie.class,
 * 		new FindByIndexOptions()
 * .sort(new IndexField("Movie_year", SortOrder.desc))
 * .fields("Movie_name").fields("Movie_year")
 * .limit(1)
 * .skip(1)
 * .readQuorum(2));
 * </pre>
 *
 * @author Mario Briggs
 * @see Database#findByIndex(String, Class, FindByIndexOptions)
 * @since 0.0.1
 */
public class FindByIndexOptions {

    // search fields
    private Integer limit;
    private Integer skip;
    private List<IndexField> sort = new ArrayList<IndexField>();
    private List<String> fields = new ArrayList<String>();
    private Integer readQuorum;
    private JsonArray useIndex = new JsonArray();

    /**
     * @param limit limit the number of results return
     * @return this to set additional options
     */
    public FindByIndexOptions limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    /**
     * @param skip Skips <i>n</i> number of results.
     * @return this to set additional options
     */
    public FindByIndexOptions skip(Integer skip) {
        this.skip = skip;
        return this;
    }

    /**
     * @param readQuorum set the readQuorum
     * @return this to set additional options
     */
    public FindByIndexOptions readQuorum(Integer readQuorum) {
        this.readQuorum = readQuorum;
        return this;
    }

    /**
     * Can be called multiple times to set the sort syntax
     *
     * @param sort add a sort syntax field
     * @return this to set additional options
     */
    public FindByIndexOptions sort(IndexField sort) {
        this.sort.add(sort);
        return this;
    }


    /**
     * Can be called multiple times to set the list of return fields
     *
     * @param field set the return fields
     * @return this to set additional options
     */
    public FindByIndexOptions fields(String field) {
        this.fields.add(field);
        return this;
    }

    /**
     * Specify a specific index to run the query against
     *
     * @param designDocument set the design document to use
     * @return this to set additional options
     */
    public FindByIndexOptions useIndex(String designDocument) {
        JsonPrimitive jsonDesign = new JsonPrimitive(designDocument);
        this.useIndex.add(jsonDesign);
        return this;
    }

    /**
     * Specify a specific index to run the query against
     *
     * @param designDocument set the design document to use
     * @param indexName set the index name to use
     * @return this to set additional options
     */
    public FindByIndexOptions useIndex(String designDocument, String indexName) {
        JsonPrimitive jsonDesign = new JsonPrimitive(designDocument);
        JsonPrimitive jsonIndex = new JsonPrimitive(indexName);
        this.useIndex.add(jsonDesign);
        this.useIndex.add(jsonIndex);
        return this;
    }

    public List<String> getFields() {
        return fields;
    }

    public List<IndexField> getSort() {
        return sort;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getSkip() {
        return skip;
    }

    public Integer getReadQuorum() {
        return readQuorum;
    }

    public String getUseIndex() {
        return useIndex.toString();
    }
}
