/*
 * Copyright (C) 2011 lightcouch.org
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

package com.cloudant.client.org.lightcouch;

import com.cloudant.client.internal.HierarchicalUriComponents;
import com.cloudant.client.internal.HierarchicalUriComponents.Type;

import java.io.UnsupportedEncodingException;

public class Param {

    private String key;
    private Object value;

    public Param(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String toURLEncodedString() {
        try {
            return String.format("%s=%s",
                    HierarchicalUriComponents.encodeUriComponent(getKey(), "UTF-8",
                            Type.QUERY_PARAM),
                    HierarchicalUriComponents.encodeUriComponent(getValue().toString(), "UTF-8",
                            Type.QUERY_PARAM));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Couldn't encode query parameter ", e);
        }
    }
}
