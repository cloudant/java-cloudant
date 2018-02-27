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

package com.cloudant.api.query;

import static com.cloudant.client.api.query.Expression.gt;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.query.Selector;
import com.cloudant.client.api.query.TextIndex;
import com.cloudant.client.internal.query.Helpers;
import com.cloudant.tests.base.TestWithMockedServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.concurrent.TimeUnit;

public class IndexCreationTests extends TestWithMockedServer {

    private static MockResponse CREATED = new MockResponse().setBody("{\"result\": \"created\"}");

    // Selector for creating partial indexes
    private Selector selectorContent = gt("year", 2010);
    // Keyed selector pair
    private String selectorPair = Helpers.withKey(Helpers.PARTIAL_FILTER_SELECTOR,
            selectorContent);

    @Test
    public void createJsonIndex() throws Exception {
        createIndexTest(JsonIndex.builder()
                        .asc("a")
                        .definition(),
                "{type: \"json\", index: {fields: [{\"a\":\"asc\"}]}}");
    }

    /**
     * Note mixed indexes are currently unsupported on the server, but we should still construct a
     * valid object even if the server will reject it.
     *
     * @throws Exception
     */
    @Test
    public void createJsonIndexSpecifyFieldOrder() throws Exception {
        createIndexTest(JsonIndex.builder()
                        .asc("a")
                        .desc("d")
                        .definition(),
                "{type: \"json\", index: {fields: [{\"a\":\"asc\"},{\"d\":\"desc\"}]}}");
    }

    @Test
    public void createNamedJsonIndex() throws Exception {
        createIndexTest(JsonIndex.builder()
                        .name("testindex")
                        .asc("a")
                        .definition(),
                "{type: \"json\", name: \"testindex\", index: {fields: [{\"a\":\"asc\"}]}}");
    }

    @Test
    public void createJsonIndexInDesignDoc() throws Exception {
        createIndexTest(JsonIndex.builder()
                        .designDocument("testddoc")
                        .asc("a")
                        .definition(),
                "{type: \"json\", ddoc: \"testddoc\", index: {fields: [{\"a\":\"asc\"}]}}");
    }

    @Test
    public void createJsonIndexAllOptions() throws Exception {
        createIndexTest(JsonIndex.builder()
                        .designDocument("testddoc")
                        .name("testindex")
                        .asc("a")
                        .desc("d")
                        .definition(),
                "{type: \"json\", ddoc: \"testddoc\", name: \"testindex\", " +
                        "index: {fields: [{\"a\":\"asc\"},{\"d\":\"desc\"}]}}");
    }

    @Test
    public void createJsonIndexPartialSelectorOnly() throws Exception {
        createIndexTest(JsonIndex.builder()
                        .asc("a")
                        .partialFilterSelector(selectorContent)
                        .definition(),
                "{type: \"json\", index: {" + selectorPair + ", fields: [{\"a\":\"asc\"}]}}");
    }

    @Test
    public void createTextIndex() throws Exception {
        createIndexTest(TextIndex.builder()
                        .definition(),
                "{type: \"text\", index: {}}");
    }

    @Test
    public void createNamedTextIndex() throws Exception {
        createIndexTest(TextIndex.builder()
                        .name("testindex")
                        .definition(),
                "{type: \"text\", name: \"testindex\", index: {}}");
    }

    @Test
    public void createTextIndexInDesignDoc() throws Exception {
        createIndexTest(TextIndex.builder()
                        .designDocument("testddoc")
                        .definition(),
                "{type: \"text\", ddoc: \"testddoc\", index: {}}");
    }

    @Test
    public void createTextIndexWithFields() throws Exception {
        createIndexTest(TextIndex.builder()
                        .string("s")
                        .bool("b")
                        .number("n")
                        .definition(),
                "{type: \"text\", index: {fields: [{name: \"s\", type:\"string\"}," +
                        "{name: \"b\", type:\"boolean\"},{name: \"n\", type:\"number\"}]}}");
    }

    @Test
    public void createTextIndexWithDefaultField() throws Exception {
        createIndexTest(TextIndex.builder()
                        .defaultField(true, "german")
                        .definition(),
                "{type: \"text\", index: { default_field: {enabled: true, analyzer: \"german\"}}}");
    }

    @Test
    public void createTextIndexWithStringAnalyzer() throws Exception {
        createIndexTest(TextIndex.builder()
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
        createIndexTest(TextIndex.builder()
                        .analyzer(a)
                        .definition(),
                "{type: \"text\", index: {analyzer: " + a + "}}");
    }

    @Test
    public void createTextIndexPartialSelectorOnly() throws Exception {
        createIndexTest(TextIndex.builder()
                        .partialFilterSelector(selectorContent)
                        .definition(),
                "{type: \"text\", index: {" + selectorPair + "}}");
    }

    @Test
    public void createTextIndexWithIndexArrayLengths() throws Exception {
        createIndexTest(TextIndex.builder()
                        .indexArrayLengths(false)
                        .definition(),
                "{type: \"text\", index: {index_array_lengths: false}}");
    }

    @Test
    public void createTextIndexWithAllOptions() throws Exception {
        createIndexTest(TextIndex.builder()
                        .name("testindex")
                        .designDocument("testddoc")
                        .string("s")
                        .bool("b")
                        .number("n")
                        .defaultField(true, "german")
                        .analyzer("keyword")
                        .partialFilterSelector(selectorContent)
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
                        selectorPair + "," +
                        "index_array_lengths: false" +
                        "}}");
    }

    private void createIndexTest(String definition, String expected) throws Exception {
        JsonObject exp = new Gson().fromJson(expected, JsonObject.class);
        server.enqueue(CREATED);
        db.createIndex(definition);
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        JsonObject actual = new Gson().fromJson(request.getBody().readUtf8(), JsonObject.class);
        assertEquals(exp, actual, "The request body should match the expected");
    }
}
