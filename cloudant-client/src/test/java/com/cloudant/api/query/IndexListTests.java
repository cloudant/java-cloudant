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

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IndexListTests extends MockedServerTest {

    private static String SELECTOR_STRING = "{\"year\":{\"$gt\":2010}}";

    // _all_docs
    private static String ALL_DOCS = fromFile("index_all_docs");
    private static String JSON_SIMPLE = fromFile("index_json_simple");
    private static String JSON_COMPLEX = fromFile("index_json_complex");
    private static String TEXT_SIMPLE = fromFile("index_text_simple");
    private static String TEXT_COMPLEX = fromFile("index_text_complex");
    private static String TEXT_ALL_FIELDS = fromFile("index_text_all");
    private static String TEXT_SIMPLE_SELECTOR = fromFile("index_text_simple_selector");

    /**
     * Utility to convert a test resource file into a string.
     *
     * @param resourceFileName name of the file excluding path and extension
     * @return
     */
    private static String fromFile(String resourceFileName) {
        System.out.println(new File(".").getAbsolutePath());
        try {
            return IOUtils.toString(new BufferedInputStream(new FileInputStream("" +
                    "./src/test/resources/query-tests/" + resourceFileName + ".js")), "UTF-8");
        } catch (Exception e) {
            fail("Error reading test resource: " +  e.getMessage());
        }
        return null;
    }

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

    private void assertJsonIndex(JsonIndex index, String name, String selector, Map<String, Sort
            .Order>... expectedFields) throws Exception {
        assertIndex(index, name, "json", selector);
        // Assert the fields
        new FieldAssertHelper.Json(expectedFields).assertFields(index.getFields());
    }

    private void assertTextIndex(TextIndex index, String name, String selector, String analyzer,
                                 String defaultField, Map<String, TextIndex.Field.Type>...
                                         expectedFields) throws Exception {
        assertIndex(index, name, "text", selector);
        assertEquals("The analyzer should be correct", analyzer, index.getAnalyzer());
        assertEquals("The default field should be correct", defaultField, index.getDefaultField());
        // Assert the fields
        new FieldAssertHelper.Text(expectedFields).assertFields(index.getFields());
    }

    private void assertSimpleJson(JsonIndex index) throws Exception {
        assertJsonIndex(index, "simplejson", null, Collections.singletonMap("Person_dob", Sort.Order
                .ASC));
    }

    private void assertComplexJson(JsonIndex index) throws Exception {
        assertJsonIndex(index, "complexjson", SELECTOR_STRING, Collections.singletonMap
                ("Person_name", Sort.Order
                        .ASC), Collections.singletonMap("Movie_year", Sort.Order.DESC));
    }

    private void assertSimpleText(TextIndex index) throws Exception {
        assertTextIndex(index, "simpletext", null, "\"keyword\"", "{}", Collections.singletonMap
                ("Movie_name", TextIndex.Field.Type.STRING));
    }

    private void assertComplexText(TextIndex index) throws Exception {
        assertTextIndex(index, "complextext", SELECTOR_STRING, "{\"name\":\"perfield\"," +
                        "\"default\":\"english\",\"fields\":{\"spanish\":\"spanish\"," +
                        "\"german\":\"german\"}}", "{\"enabled\":true,\"analyzer\":\"spanish\"}",
                Collections.singletonMap("Movie_name", TextIndex.Field.Type.STRING), Collections
                        .singletonMap("Movie_runtime", TextIndex.Field.Type.NUMBER), Collections
                        .singletonMap("Movie_wonaward", TextIndex.Field.Type.BOOLEAN));
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

    /**
     * Note this checks deserialization of the old "selector" field instead of
     * partial_filter_selector
     *
     * @throws Exception
     */
    @Test
    public void listSimpleTextIndexWithSelector() throws Exception {
        enqueueList(TEXT_SIMPLE_SELECTOR);
        List<TextIndex> indexes = db.listIndexes().textIndexes();
        assertEquals("There should be 1 text index", 1, indexes.size());
        TextIndex simple = indexes.get(0);
        assertTextIndex(simple, "simpleselector", SELECTOR_STRING, "\"keyword\"", "{}",
                Collections.singletonMap
                        ("Movie_name", TextIndex.Field.Type.STRING));
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
                    assertEquals("There field should be called _id", "_id", a.getFields().get(0)
                            .getName());
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
