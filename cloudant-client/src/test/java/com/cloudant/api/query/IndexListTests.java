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

package com.cloudant.api.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.cloudant.client.api.query.Field;
import com.cloudant.client.api.query.Index;
import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.query.Sort;
import com.cloudant.client.api.query.TextIndex;
import com.cloudant.tests.util.MockedServerTest;

import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;

import java.util.Arrays;
import java.util.List;

public class IndexListTests extends MockedServerTest {

    private static String SELECTOR_STRING = "{\"year\":{\"$gt\":2010}}";

    // _all_docs
    private static String ALL_DOCS = "{\"ddoc\":null,\"name\":\"_all_docs\",\"type\":\"special\"," +
            "\"def\":{\"fields\":[{\"_id\":\"asc\"}]}}";
    private static String JSON_SIMPLE = "{\"ddoc\":\"_design/testindexddoc\"," +
            "\"name\":\"simplejson\",\"type\":\"json\"," +
            "\"def\":{\"fields\":[{\"Person_dob\":\"asc\"}],\"partial_filter_selector\":{}}}";
    private static String JSON_COMPLEX = "{\"ddoc\":\"_design/testindexddoc\"," +
            "\"name\":\"complexjson\",\"type\":\"json\",\"def\":{\"partial_filter_selector\":" +
            SELECTOR_STRING + ",\"fields\":[{\"Person_name\":\"asc\"},{\"Movie_year\":\"desc\"}]}}";
    private static String TEXT_SIMPLE = "{\"ddoc\":\"_design/testindexddoc\"," +
            "\"name\":\"simpletext\",\"type\":\"text\",\"def\":{\"default_analyzer\":\"keyword\"," +
            "\"default_field\":{},\"partial_filter_selector\":{}," +
            "\"fields\":[{\"Movie_name\":\"string\"}]," +
            "\"index_array_lengths\":true}}";
    private static String TEXT_COMPLEX = "{\"ddoc\":\"_design/testindexddoc\"," +
            "\"name\":\"complextext\",\"type\":\"text\",\"def\":{\"default_analyzer\": " +
            "{\"name\":\"perfield\",\"default\":\"english\",\"fields\":{\"spanish\":\"spanish\"," +
            "\"german\":\"german\"}},\"default_field\":{\"enabled\": true, \"analyzer\": " +
            "\"spanish\"},\"partial_filter_selector\":" + SELECTOR_STRING + "," +
            "\"fields\":[{\"Movie_name\":\"string\"}, {\"Movie_runtime\": \"number\"}, " +
            "{\"Movie_wonaward\": \"boolean\"}],\"index_array_lengths\":true}}";
    private static String TEXT_ALL_FIELDS = "{\"ddoc\":\"_design/testindexddoc\"," +
            "\"name\":\"textallfields\",\"type\":\"text\"," +
            "\"def\":{\"default_analyzer\":\"keyword\",\"default_field\":{\"enabled\": false}," +
            "\"partial_filter_selector\":{},\"fields\":[],\"index_array_lengths\":false}}";


    private void enqueueList(String... indexes) {
        MockResponse response = new MockResponse();
        StringBuilder responseContent = new StringBuilder("{\"indexes\": [");
        // Always add the special index
        responseContent.append(ALL_DOCS);
        // Add the rest of the indexes
        for (String index : indexes) {
            responseContent.append(",");
            responseContent.append(index);
        }
        responseContent.append("], \"total_rows\": " + (indexes.length + 1) + "}");
        response.setBody(responseContent.toString());
        server.enqueue(response);
    }

    private void assertIndex(Index index, String name, String type, String selector) throws
            Exception {
        if (selector == null) {
            selector = "{}";
        }
        assertIndex(index, name, "_design/testindexddoc", type, selector);
    }

    private void assertIndex(Index index, String name, String ddoc, String type, String selector)
            throws Exception {
        assertEquals("The index should have the correct name", name, index.getName());
        assertEquals("The index should have the correct ddoc", ddoc, index
                .getDesignDocumentID());
        assertEquals("The index should have the correct type", type, index.getType());
        assertEquals("The index should have the correct selector", selector, index
                .getPartialFilterSelector());
    }

    private void assertJsonIndex(JsonIndex index, String name, String selector, JsonIndex
            .Field... fields) throws Exception {
        assertIndex(index, name, "json", selector);
        // Assert the fields
        assertEquals("The fields should be correct", Arrays.asList(fields), index.getFields());
    }

    private void assertTextIndex(TextIndex index, String name, String selector, String analyzer,
                                 String defaultField, TextIndex.Field... fields) throws Exception {
        assertIndex(index, name, "text", selector);
        assertEquals("The analyzer should be correct", analyzer, index.getAnalyzer());
        assertEquals("The default field should be correct", defaultField, index.getDefaultField());
        // Assert the fields
        assertEquals("The fields should be correct", Arrays.asList(fields), index.getFields());
    }

    private void assertSimpleJson(JsonIndex index) throws Exception {
        assertJsonIndex(index, "simplejson", null, new JsonIndex.Field("Person_dob", Sort.Order
                .ASC));
    }

    private void assertComplexJson(JsonIndex index) throws Exception {
        assertJsonIndex(index, "complexjson", SELECTOR_STRING, new JsonIndex.Field("Person_name",
                Sort.Order.ASC), new JsonIndex.Field("Movie_year", Sort.Order.DESC));
    }

    private void assertSimpleText(TextIndex index) throws Exception {
        assertTextIndex(index, "simpletext", null, "\"keyword\"", "{}", new TextIndex.Field
                ("Movie_name", TextIndex.Field.Type.STRING));
    }

    private void assertComplexText(TextIndex index) throws Exception {
        assertTextIndex(index, "complextext", SELECTOR_STRING, "{\"name\":\"perfield\"," +
                "\"default\":\"english\",\"fields\":{\"spanish\":\"spanish\"," +
                "\"german\":\"german\"}}", "{\"enabled\":true,\"analyzer\":\"spanish\"}", new
                TextIndex.Field("Movie_name", TextIndex.Field.Type.STRING), new TextIndex.Field
                ("Movie_runtime", TextIndex.Field.Type.NUMBER), new TextIndex.Field
                ("Movie_wonaward", TextIndex.Field.Type.BOOLEAN));
    }

    @Test
    public void listSimpleJsonIndex() throws Exception {
        enqueueList(JSON_SIMPLE);
        List<JsonIndex> indexes = db.listIndexes().jsonIndexes();
        assertEquals("There should be 1 JSON index", 1, indexes.size());
        JsonIndex simple = indexes.get(0);
        assertSimpleJson(simple);
    }

    @Test
    public void listComplexJsonIndex() throws Exception {
        enqueueList(JSON_COMPLEX);
        List<JsonIndex> indexes = db.listIndexes().jsonIndexes();
        assertEquals("There should be 1 JSON index", 1, indexes.size());
        JsonIndex complex = indexes.get(0);
        assertComplexJson(complex);

    }

    @Test
    public void listMultipleJsonIndexes() throws Exception {
        enqueueList(JSON_SIMPLE, JSON_COMPLEX);
        List<JsonIndex> indexes = db.listIndexes().jsonIndexes();
        assertEquals("There should be 2 JSON indexes", 2, indexes.size());
        JsonIndex simple = indexes.get(0);
        assertSimpleJson(simple);
        JsonIndex complex = indexes.get(1);
        assertComplexJson(complex);
    }

    @Test
    public void listSimpleTextIndex() throws Exception {
        enqueueList(TEXT_SIMPLE);
        List<TextIndex> indexes = db.listIndexes().textIndexes();
        assertEquals("There should be 1 text index", 1, indexes.size());
        TextIndex simple = indexes.get(0);
        assertSimpleText(simple);
    }

    @Test
    public void listComplexTextIndex() throws Exception {
        enqueueList(TEXT_COMPLEX);
        List<TextIndex> indexes = db.listIndexes().textIndexes();
        assertEquals("There should be 1 text index", 1, indexes.size());
        TextIndex complex = indexes.get(0);
        assertComplexText(complex);
    }

    @Test
    public void listMultipleTextIndexes() throws Exception {
        enqueueList(TEXT_SIMPLE, TEXT_COMPLEX, TEXT_ALL_FIELDS);
        List<TextIndex> indexes = db.listIndexes().textIndexes();
        assertEquals("There should be 3 text indexes", 3, indexes.size());
        TextIndex simple = indexes.get(0);
        assertSimpleText(simple);
        TextIndex complex = indexes.get(1);
        assertComplexText(complex);
        // Assert the all text index
        TextIndex all = indexes.get(2);
        assertTextIndex(all, "textallfields", null, "\"keyword\"", "{\"enabled\":false}");
    }

    @Test
    public void listAllIndexes() throws Exception {
        enqueueList(JSON_SIMPLE, JSON_COMPLEX, TEXT_SIMPLE, TEXT_COMPLEX, TEXT_ALL_FIELDS);
        List<Index<Field>> indexes = db.listIndexes().allIndexes();
        // Note 5 listed here, plus the special index that is always included
        assertEquals("There should be 6 indexes", 6, indexes.size());
        for (int i = 0; i < indexes.size(); i++) {
            String name;
            String type;
            String selector;
            switch (i) {
                case 0:
                    Index<Field> a = indexes.get(i);
                    assertIndex(a, "_all_docs", null, "special", null);
                    assertEquals("There should be 1 field", 1, a.getFields().size());
                    assertEquals("There field should be called _id", "_id", a.getFields().get(0).getName());
                    return;
                case 1:
                    name = "simplejson";
                    type = "json";
                    selector = null;
                    break;
                case 2:
                    name = "complexjson";
                    type = "json";
                    selector = SELECTOR_STRING;
                    break;
                case 3:
                    name = "simpletext";
                    type = "text";
                    selector = null;
                    break;
                case 4:
                    name = "complextext";
                    type = "text";
                    selector = SELECTOR_STRING;
                    break;
                case 5:
                    name = "textallfields";
                    type = "text";
                    selector = null;
                    break;
                default:
                    fail("Unknown index");
                    return;
            }
            assertIndex(indexes.get(i), name, type, selector);
        }
    }
}
