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

import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.RequestBuilder;
import com.cloudant.client.api.views.SettableViewParameters;

public abstract class CommonViewRequestBuilder<K, V, RB extends RequestBuilder<RB>> implements
        SettableViewParameters.Common<K, RB>,
        SettableViewParameters.Paginated<K, RB>,
        SettableViewParameters.Unpaginated<K, RB>,
        SettableViewParameters.Reduceable<K, RB> {

    protected final ViewQueryParameters<K, V> viewQueryParameters;

    protected CommonViewRequestBuilder(ViewQueryParameters<K, V> parameters) {
        this.viewQueryParameters = parameters;
    }

    public abstract RB returnThis();

    public void validateQuery() {
        if (viewQueryParameters.group_level != null && !Key.ComplexKey.class.isAssignableFrom
                (viewQueryParameters.getKeyType())) {
            throw new IllegalArgumentException("Group level is only valid when using a complex " +
                    "key");
        }
        if (viewQueryParameters.reduce != null && viewQueryParameters.getReduce() &&
                viewQueryParameters.getIncludeDocs()) {
            throw new IllegalArgumentException("Cannot include_docs with reduce=true");
        }
        if (!viewQueryParameters.getReduce()) {
            //reduce is set to false
            if (viewQueryParameters.getGroup() || viewQueryParameters.getGroupLevel() != null) {
                throw new IllegalArgumentException("Cannot use group or group_level with " +
                        "reduce=false");
            }
        }
    }

    @Override
    public RB descending(boolean descending) {
        viewQueryParameters.setDescending(descending);
        return returnThis();
    }

    @Override
    public RB endKey(K endkey) {
        viewQueryParameters.setEndKey(endkey);
        return returnThis();
    }

    @Override
    public RB endKeyDocId(String endkey_docid) {
        viewQueryParameters.setEndKeyDocId(endkey_docid);
        return returnThis();
    }

    @Override
    public RB group(boolean group) {
        viewQueryParameters.setGroup(group);
        return returnThis();
    }

    @Override
    public RB groupLevel(int group_level) {
        viewQueryParameters.setGroupLevel(group_level);
        return returnThis();
    }

    @Override
    public RB includeDocs(boolean include_docs) {
        viewQueryParameters.setIncludeDocs(include_docs);
        return returnThis();
    }

    @Override
    public RB inclusiveEnd(boolean inclusive_end) {
        viewQueryParameters.setInclusiveEnd(inclusive_end);
        return returnThis();
    }

    @Override
    public RB keys(K... keys) {
        viewQueryParameters.setKeys(keys);
        return returnThis();
    }

    @Override
    public RB reduce(boolean reduce) {
        viewQueryParameters.setReduce(reduce);
        return returnThis();
    }

    @Override
    public RB stale(String stale) {
        viewQueryParameters.setStale(stale);
        return returnThis();
    }

    @Override
    public RB startKey(K startkey) {
        viewQueryParameters.setStartKey(startkey);
        return returnThis();
    }

    @Override
    public RB startKeyDocId(String startkey_docid) {
        viewQueryParameters.setStartKeyDocId(startkey_docid);
        return returnThis();
    }

    @Override
    public RB limit(int limit) {
        viewQueryParameters.setLimit(limit);
        return returnThis();
    }

    @Override
    public RB skip(long skip) {
        viewQueryParameters.setSkip(skip);
        return returnThis();
    }

    @Override
    public RB rowsPerPage(int rowsPerPage) {
        viewQueryParameters.setRowsPerPage(rowsPerPage);
        return returnThis();
    }
}
