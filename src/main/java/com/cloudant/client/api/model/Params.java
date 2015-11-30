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

import java.util.List;


/**
 * Query parameters to append to find requests.
 * <p>Example:
 * <pre>
 * Database.find(Foo.class, "doc-id", new Params().readQuorum(3));
 * </pre>
 *
 * @author Mario Briggs
 * @see Database#find(Class, String, Params)
 * @since 0.0.1
 */
public class Params {

    private com.cloudant.client.org.lightcouch.Params params = new com.cloudant.client.org.lightcouch.Params();

    public Params readQuorum(int quorum) {
        params.addParam("r", new Integer(quorum).toString());
        return this;
    }

    public Params attachments() {
        params = params.attachments();
        return this;
    }

    public Params revisions() {
        params = params.revisions();
        return this;
    }

    public Params conflicts() {
        params = params.conflicts();
        return this;
    }

    public Params addParam(String name, String value) {
        params = params.addParam(name, value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Params params1 = (Params) o;

        return params.equals(params1.params);

    }

    @Override
    public int hashCode() {
        return params.hashCode();
    }

    public List<String> getParams() {
        return params.getParams();
    }

    public Params revsInfo() {
        params = params.revsInfo();
        return this;
    }

    public Params rev(String rev) {
        params = params.rev(rev);
        return this;
    }

    public Params localSeq() {
        params = params.localSeq();
        return this;
    }

    public String toString() {
        return params.toString();
    }

    public com.cloudant.client.org.lightcouch.Params getInternalParams() {
        return params;
    }


}
