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

import java.net.URI;
import java.net.URISyntaxException;

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
        return String.format("%s=%s", encodeQueryParameter(getKey()), encodeQueryParameter
                (getValue().toString()));
    }

    private static String encodeQueryParameter(String in) {
        // As this is to escape individual parameters, we need to
        // escape &, = and +.
        try {
            URI uri = new URI(
                    null, // scheme
                    null, // authority
                    null, // path
                    in,   // query
                    null  // fragment
            );
            return uri.toASCIIString()
                    .substring(1) // remove leading ?
                    .replace("&", "%26") // encode qs separators
                    .replace("=", "%3D")
                    .replace("+", "%2B");
        } catch (URISyntaxException e) {
            throw new RuntimeException(
                    "Couldn't encode query parameter " + in,
                    e);
        }
    }
}
