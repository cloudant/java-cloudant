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

package com.cloudant.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudant.client.internal.HierarchicalUriComponents;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;

public class HierarchicalUriComponentsTest {

    @Test
    public void encodePathComponentContainingPlusSign() throws UnsupportedEncodingException {
        String in = "/foo+bar/foo+bar/";
        String out = HierarchicalUriComponents.encodeUriComponent(
                in, "UTF-8", HierarchicalUriComponents.Type.PATH);

        // Regard plus sign ("+") as reserved character,
        // see https://issues.apache.org/jira/browse/COUCHDB-1580.
        assertEquals("/foo%2Bbar/foo%2Bbar/", out);
    }

    @Test
    public void encodePathSegmentComponentContainingPlusSign() throws UnsupportedEncodingException {
        String in = "foo+bar";
        String out = HierarchicalUriComponents.encodeUriComponent(
                in, "UTF-8", HierarchicalUriComponents.Type.PATH_SEGMENT);

        // Regard plus sign ("+") as reserved character,
        // see https://issues.apache.org/jira/browse/COUCHDB-1580.
        assertEquals("foo%2Bbar", out);
    }
}
