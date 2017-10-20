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

import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.query.Sort;
import com.cloudant.client.api.query.TextIndex;
import com.cloudant.tests.util.MockedServerTest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.concurrent.TimeUnit;

public class IndexCreationTests extends MockedServerTest {

    private static MockResponse CREATED = new MockResponse().setBody("{\"result\": \"created\"}");

    // Strings for creating partial indexes
    private String selectorContent = "{year: {$gt: 2010}}";
    private String selectorPair = "partial_filter_selector: " + selectorContent;
    private String selector = "{" + selectorPair + "}";

    @Test
    public void createJsonIndex() throws Exception {
        createIndexTest(new JsonIndex.Builder()
                        .fields(new JsonIndex.Field("a"))
                        .definition(),
                "{type: \"json\", index: {fields: [\"a\"]}}");
    }

    @Test
    public void createJsonIndexSpecifyFieldOrder() throws Exception {
        createIndexTest(new JsonIndex.Builder()
                        .fields(
                                new JsonIndex.Field("a", Sort.Order.ASC),
                                new JsonIndex.Field("d", Sort.Order.DESC))
                        .definition(),
                "{type: \"json\", index: {fields: [{\"a\":\"asc\"},{\"d\":\"desc\"}]}}");
    }

    @Test
    public void createNamedJsonIndex() throws Exception {
        createIndexTest(new JsonIndex.Builder()
                        .name("testindex")
                        .fields(new JsonIndex.Field("a"))
                        .definition(),
                "{type: \"json\", name: \"testindex\", index: {fields: [\"a\"]}}");
    }

    @Test
    public void createJsonIndexInDesignDoc() throws Exception {
        createIndexTest(new JsonIndex.Builder()
                        .designDocument("testddoc")
                        .fields(new JsonIndex.Field("a"))
                        .definition(),
                "{type: \"json\", ddoc: \"testddoc\", index: {fields: [\"a\"]}}");
    }

    @Test
    public void createJsonIndexAllOptions() throws Exception {
        createIndexTest(new JsonIndex.Builder()
                        .designDocument("testddoc")
                        .name("testindex")
                        .fields(new JsonIndex.Field("a", Sort.Order.ASC),
                                new JsonIndex.Field("d", Sort.Order.DESC))
                        .definition(),
                "{type: \"json\", ddoc: \"testddoc\", name: \"testindex\", " +
                        "index: {fields: [{\"a\":\"asc\"},{\"d\":\"desc\"}]}}");
    }

    @Test
    public void createJsonIndexPartialSelectorOnly() throws Exception {
        createIndexTest(new JsonIndex.Builder()
                        .fields(new JsonIndex.Field("a"))
                        .partialFilterSelector(selectorContent)
                        .definition(),
                "{type: \"json\", index: {" + selectorPair + ", fields: [\"a\"]}}");
    }

    @Test
    public void createJsonIndexPartialSelectorPair() throws Exception {
        createIndexTest(new JsonIndex.Builder()
                        .fields(new JsonIndex.Field("a"))
                        .partialFilterSelector(selectorPair)
                        .definition(),
                "{type: \"json\", index: {" + selectorPair + ", fields: [\"a\"]}}");
    }

    @Test
    public void createJsonIndexPartialSelectorObject() throws Exception {
        createIndexTest(new JsonIndex.Builder()
                        .fields(new JsonIndex.Field("a"))
                        .partialFilterSelector(selector)
                        .definition(),
                "{type: \"json\", index: {" + selectorPair + ", fields: [\"a\"]}}");
    }

    @Test
    public void createTextIndex() throws Exception {
        createIndexTest(new TextIndex.Builder()
                        .definition(),
                "{type: \"text\", index: {}}");
    }

    @Test
    public void createNamedTextIndex() throws Exception {
        createIndexTest(new TextIndex.Builder()
                        .name("testindex")
                        .definition(),
                "{type: \"text\", name: \"testindex\", index: {}}");
    }

    @Test
    public void createTextIndexInDesignDoc() throws Exception {
        createIndexTest(new TextIndex.Builder()
                .designDocument("testddoc")
                        .definition(),
                "{type: \"text\", ddoc: \"testddoc\", index: {}}");
    }

    @Test
    public void createTextIndexWithFields() throws Exception {
        createIndexTest(new TextIndex.Builder()
                        .fields(new TextIndex.Field("s", TextIndex.Field.Type.STRING),
                                new TextIndex.Field("b", TextIndex.Field.Type.BOOLEAN),
                                new TextIndex.Field("n", TextIndex.Field.Type.NUMBER))
                        .definition(),
                "{type: \"text\", index: {fields: [{name: \"s\", type:\"string\"}," +
                        "{name: \"b\", type:\"boolean\"},{name: \"n\", type:\"number\"}]}}");
    }

    @Test
    public void createTextIndexWithDefaultField() throws Exception {
        createIndexTest(new TextIndex.Builder()
                        .defaultField(true, "german")
                        .definition(),
                "{type: \"text\", index: { default_field: {enabled: true, analyzer: \"german\"}}}");
    }

    @Test
    public void createTextIndexWithStringAnalyzer() throws Exception {
        createIndexTest(new TextIndex.Builder()
                        .analyzer("keyword")
                        .definition(),
                "{type: \"text\", index: {analyzer: \"keyword\"}}");
    }

    @Test
    public void createTextIndexWithObjectAnalyzer() throws Exception {
        String a = "{\"name\": \"perfield\"," +
                "\"default\": \"english\"," +
                "\"fields\": {" +
                "\"spanish\": \"spanish\"," +
                "\"german\": \"german\"}}";
        createIndexTest(new TextIndex.Builder()
                        .analyzer(a)
                        .definition(),
                "{type: \"text\", index: {analyzer: " + a + "}}");
    }

    @Test
    public void createTextIndexPartialSelectorOnly() throws Exception {
        createIndexTest(new TextIndex.Builder()
                        .partialFilterSelector(selectorContent)
                        .definition(),
                "{type: \"text\", index: " + selector + "}");
    }

    @Test
    public void createTextIndexPartialSelectorPair() throws Exception {
        createIndexTest(new TextIndex.Builder()
                        .partialFilterSelector(selectorPair)
                        .definition(),
                "{type: \"text\", index: " + selector + "}");
    }

    @Test
    public void createTextIndexPartialSelectorObject() throws Exception {
        createIndexTest(new TextIndex.Builder()
                        .partialFilterSelector(selector)
                        .definition(),
                "{type: \"text\", index: " + selector + "}");
    }

    @Test
    public void createTextIndexWithIndexArrayLengths() throws Exception {
        createIndexTest(new TextIndex.Builder()
                        .indexArrayLengths(false)
                        .definition(),
                "{type: \"text\", index: {index_array_lengths: false}}");
    }

    @Test
    public void createTextIndexWithAllOptions() throws Exception {
        createIndexTest(new TextIndex.Builder()
                        .name("testindex")
                        .designDocument("testddoc")
                        .fields(new TextIndex.Field("s", TextIndex.Field.Type.STRING),
                                new TextIndex.Field("b", TextIndex.Field.Type.BOOLEAN),
                                new TextIndex.Field("n", TextIndex.Field.Type.NUMBER))
                        .defaultField(true, "german")
                        .analyzer("keyword")
                        .partialFilterSelector(selector)
                        .indexArrayLengths(false)
                        .definition(),
                "{type: \"text\"," +
                        "name: \"testindex\"," +
                        "ddoc: \"testddoc\"," +
                        "index: {" +
                        "fields: [{name: \"s\", type:\"string\"}," +
                        "{name: \"b\", type:\"boolean\"}," +
                        "{name: \"n\", type:\"number\"}]," +
                        "default_field: {enabled: true, analyzer: \"german\"}," +
                        "analyzer: \"keyword\"," +
                        "partial_filter_selector: " + selectorContent + "," +
                        "index_array_lengths: false" +
                        "}}");
    }

    private void createIndexTest(String definition, String expected) throws Exception {
        JsonObject exp = new Gson().fromJson(expected, JsonObject.class);
        server.enqueue(CREATED);
        db.createIndex(definition);
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        JsonObject actual = new Gson().fromJson(request.getBody().readUtf8(), JsonObject.class);
        assertEquals("The request body should match the expected", exp, actual);
    }
}
