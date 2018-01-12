/*
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

import com.cloudant.client.api.model.IndexField.SortOrder;
import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.query.TextIndex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates a Cloudant Index definition
 *
 * @author Mario Briggs
 * @since 0.0.1
 * @see JsonIndex
 * @see TextIndex
 */
@Deprecated
public class Index {

    private String ddoc;
    private String name;
    private String type;
    private List<IndexField> def = new ArrayList<IndexField>();

    /**
     * @return the designDocId
     */
    public String getDesignDocId() {
        return ddoc;
    }


    /**
     * @return the index name
     */
    public String getName() {
        return name;
    }


    /**
     * @return the index type e.g. json
     */
    public String getType() {
        return type;
    }


    /**
     * @return the IndexFields
     */
    public Iterator<IndexField> getFields() {
        return def.iterator();
    }

    public String toString() {
        StringBuilder index = new StringBuilder("ddoc: " + ddoc + ", name: " + name + ", type: "
                + type + ", fields: [");
        Iterator<IndexField> flds = getFields();
        while (flds.hasNext()) {
            index.append(flds.next().toString());
            index.append(",");
        }
        index.append("]");
        return index.toString();
    }


    public Index(String designDocId, String name, String type) {
        this.ddoc = designDocId;
        this.name = name;
        this.type = type;
    }

    public void addIndexField(String fieldName, SortOrder order) {
        def.add(new IndexField(fieldName, order));
    }
}


