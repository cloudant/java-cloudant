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
package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewMultipleRequest;
import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewRequestBuilder;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.ContextCollectingInterceptor;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Category(RequiresDB.class)
public class ViewsTest {

    @ClassRule
    public static CloudantClientResource clientResource = new CloudantClientResource();

    @Rule
    public DatabaseResource dbResource = new DatabaseResource(clientResource);


    private static ContextCollectingInterceptor cci = new ContextCollectingInterceptor();
    public static CloudantClientResource interceptedClient = new CloudantClientResource
            (CloudantClientHelper.getClientBuilder().interceptors(cci));
    public static DatabaseResource interceptedDB = new DatabaseResource(interceptedClient);
    @ClassRule
    public static RuleChain intercepted = RuleChain.outerRule(interceptedClient).around
            (interceptedDB);
    private Database db;

    @Before
    public void setUp() throws Exception {
        db = dbResource.get();
        Utils.putDesignDocs(db);
    }

    @Test
    public void queryView() throws Exception {
        init();
        List<Foo> foos = db.getViewRequestBuilder("example", "foo").newRequest(Key.Type.STRING,
                Object.class).includeDocs(true).build().getResponse().getDocsAs(Foo.class);
        assertThat(foos.size(), not(0));
    }

    @Test
    public void byKey() throws Exception {
        init();
        List<Foo> foos = db.getViewRequestBuilder("example", "foo").newRequest(Key.Type.STRING,
                Object.class).includeDocs(true).keys("key-1").build().getResponse().getDocsAs(Foo
                .class);
        assertThat(foos.size(), is(1));
    }

    @Test
    public void byKeys() throws Exception {
        init();
        List<Foo> foos = db.getViewRequestBuilder("example", "foo").newRequest(Key.Type.STRING,
                Object.class).includeDocs(true).keys("key-1", "key-2").build().getResponse()
                .getDocsAs(Foo.class);
        assertThat(foos.size(), is(2));
    }

    @Test
    public void byStartAndEndKey() throws Exception {
        init();
        List<Foo> foos = db.getViewRequestBuilder("example", "foo").newRequest(Key.Type.STRING,
                Object.class).includeDocs(true).startKey("key-1").endKey("key-2").build()
                .getResponse().getDocsAs(Foo.class);
        assertThat(foos.size(), is(2));
    }

    /**
     * Assert that passing a boolean key with value 'false'
     * in query will produce a result list of
     * all false docs.
     */
    @Test
    public void queryWithStartAndEndBooleanKey() throws Exception {
        init();
        List<Object> result = db.getViewRequestBuilder("example", "boolean").newRequest(Key.Type
                .BOOLEAN, Object.class).startKey(false).endKey(false).build()
                .getResponse().getValues();
        assertThat(result.size(), is(1));
    }

    /**
     * Assert that passing a boolean key with value 'true' in
     * queryView will produce a result list of a true doc.
     */
    @Test
    public void queryPageWithStartAndEndBooleanKey() throws Exception {
        init();
        ViewResponse<Boolean, Object> result = db.getViewRequestBuilder("example", "boolean")
                .newPaginatedRequest(Key.Type.BOOLEAN, Object.class).startKey(true).endKey(true)
                .rowsPerPage(2).build()
                .getResponse();

        List<Object> resultList = result.getValues();

        assertThat(resultList.size(), is(2));
    }

    /**
     * Assert that passing a string key with double quotes
     * or spaces in query and queryView will produce
     * a result list without exception.
     */
    @Test
    public void queryWithStartAndEndStringIntKeyArray() throws Exception {
        init();

        List<Object> result = db.getViewRequestBuilder("example", "spaces_created").newRequest
                (Key.Type.COMPLEX, Object.class)
                .startKey(Key.complex(" spaces ").add(1))
                .endKey(Key.complex(" spaces 1").add(2000))
                .build().getResponse().getValues();

        assertThat(result.size(), is(2));
    }

    /**
     * Assert that passing a string key with spaces
     * in queryPage will produce a result list
     * without exception.
     */
    @Test
    public void queryPageWithStartAndEndStringIntKeyArray() throws Exception {
        init();

        List<Object> result = db.getViewRequestBuilder("example", "spaces_created")
                .newPaginatedRequest(Key.Type.COMPLEX, Object.class)
                .startKey(Key.complex(" spaces ").add(1))
                .endKey(Key.complex(" spaces 0").add(2000))
                .rowsPerPage(30).build().getResponse().getValues();

        assertThat(result.size(), is(1));
    }

    /**
     * Assert that passing a string key with quotes
     * in query will produce a result list
     * without exception.
     */
    @Test
    public void queryWithStartAndEndStringWithQuotesAndIntKeyArray() throws Exception {
        init();

        List<Object> result = db.getViewRequestBuilder("example", "quotes_created").newRequest
                (Key.Type.COMPLEX, Object.class)
                .startKey(Key.complex("\"quotes\" ").add(1))
                .endKey(Key.complex("\"quotes\" 0").add(2000))
                .build().getResponse().getValues();

        assertThat(result.size(), is(1));
    }

    /**
     * Assert that passing a string key with quotes
     * in query will produce a result list
     * without exception.
     */
    @Test
    public void queryPageWithStartAndEndStringWithQuotesAndIntKeyArray() throws Exception {
        init();

        List<Object> result = db.getViewRequestBuilder("example", "quotes_created")
                .newPaginatedRequest(Key.Type.COMPLEX, Object.class)
                .startKey(Key.complex("\"quotes\" ").add(1))
                .endKey(Key.complex("\"quotes\" 1").add(2000))
                .rowsPerPage(30).build().getResponse().getValues();

        assertThat(result.size(), is(2));
    }

    /**
     * Assert that passing an array of integers in query
     * will produce a result list of all docs.
     */
    @Test
    public void queryWithStartAndEndIntKeyArray() throws Exception {
        init();

        List<Key.ComplexKey> result = db.getViewRequestBuilder("example", "created").newRequest
                (Key.Type.COMPLEX, Object.class).startKey(Key.complex(1, 10)).endKey(Key.complex
                (2000, 5000)).build().getResponse().getKeys();

        assertThat(result.size(), is(3));
    }

    /**
     * Assert that passing an array of integers in queryPage
     * will produce a result list of two docs.
     */
    @Test
    public void queryPageWithStartAndEndIntKeyArray() throws Exception {
        init();

        List<Object> result = db.getViewRequestBuilder("example", "created").newPaginatedRequest
                (Key.Type.COMPLEX, Object.class).startKey(Key.complex(1, 10)).endKey(Key.complex
                (1001, 2000)).rowsPerPage(30).build().getResponse().getValues();

        assertThat(result.size(), is(2));
    }

    /**
     * Assert that passing a key in query with an explicit object array of a
     * string and integer value will produce a result list with three docs.
     */
    @Test
    public void queryWithStartAndEndObjectKeyArray() throws Exception {
        init();

        List<Object> result = db.getViewRequestBuilder("example", "creator_created").newRequest
                (Key.Type.COMPLEX, Object.class).startKey(Key.complex("uuid").add(1)).endKey(Key
                .complex("uuid").add(1010)).build().getResponse()
                .getValues();

        assertThat(result.size(), is(3));
    }

    /**
     * Assert that passing a key in queryPage with an explicit object array of
     * a string and integer value will produce a result list with three docs.
     */
    @Test
    public void queryPageWithStartAndEndObjectKeyArray() throws Exception {
        init();

        List<Key.ComplexKey> result = db.getViewRequestBuilder("example", "creator_created")
                .newPaginatedRequest(Key.Type.COMPLEX, Object.class).startKey(Key.complex("uuid")
                        .add(1)).endKey(Key.complex("uuid").add(1010)
                ).rowsPerPage(30).build().getResponse().getKeys();

        assertThat(result.size(), is(3));
    }

    /**
     * Assert that passing a key in query with an array of number
     * values will produce a result list with two docs.
     */
    @Test
    public void queryWithStartAndEndNumbersKeyArray() throws Exception {
        init();

        List<Key.ComplexKey> result = db.getViewRequestBuilder("example", "created_total")
                .newRequest
                        (Key.Type.COMPLEX, Object.class).startKey(Key.complex(1000, 12.00))
                .endKey(Key
                        .complex(1002, 15.00)).build().getResponse().getKeys();

        assertThat(result.size(), is(2));
    }

    /**
     * Assert that passing a key in queryPage with an array of number
     * values will produce a result list with one doc.
     */
    @Test
    public void queryPageWithStartAndEndNumbersKeyArray() throws Exception {
        init();

        List<Key.ComplexKey> result = db.getViewRequestBuilder("example", "total_created")
                .newPaginatedRequest(Key.Type.COMPLEX, Object.class).startKey(Key.complex(10.00,
                        1)).endKey(Key.complex(11.00, 2000)).rowsPerPage(30).build().getResponse
                        ().getKeys();

        assertThat(result.size(), is(1));
    }

    /**
     * Assert that passing a key in query with an array of a boolean, string,
     * and integer value will produce a result list with three docs.
     */
    @Test
    public void queryWithStartAndEndBooleanStringIntegerKeyArray() throws Exception {
        init();

        List<Object> result = db.getViewRequestBuilder("example", "boolean_creator_created")
                .newRequest(Key.Type.COMPLEX, Object.class)
                .startKey(Key.complex(false).add("uuid").add(1))
                .endKey(Key.complex(true).add("uuid").add(2000))
                .build().getResponse().getValues();

        assertThat(result.size(), is(3));
    }

    /**
     * Assert that passing a key in queryPage with an array of a boolean, string,
     * and integer value will produce a result list with one doc.
     */
    @Test
    public void queryPageWithStartAndEndBooleanStringIntegerKeyArray() throws Exception {
        init();

        List<Object> result = db.getViewRequestBuilder("example", "boolean_creator_created")
                .newPaginatedRequest(Key.Type.COMPLEX, Object.class)
                .startKey(Key.complex(false).add("uuid").add(1))
                .endKey(Key.complex(false).add("uuid").add(2000))
                .rowsPerPage(30).build().getResponse().getValues();

        assertThat(result.size(), is(1));
    }

    /**
     * Assert that passing a key in query with an array of a integer, boolean,
     * and string value will produce a result list with one doc.
     */
    @Test
    public void queryWithStartAndEndIntegerBooleanStringKeyArray() throws Exception {
        init();

        List<Object> result = db.getViewRequestBuilder("example", "created_boolean_creator")
                .newRequest(Key.Type.COMPLEX, Object.class)
                .startKey(Key.complex(1000).add(false).add("uuid"))
                .endKey(Key.complex(1000).add(false).add("uuid"))
                .build().getResponse().getValues();

        assertThat(result.size(), is(1));
    }

    /**
     * Assert that passing a key in queryPage with an array of a integer, boolean,
     * and string value will produce a result list with two docs.
     */
    @Test
    public void queryPageWithStartAndEndIntegerBooleanStringKeyArray() throws Exception {
        init();

        List<Object> result = db.getViewRequestBuilder("example", "created_boolean_creator")
                .newPaginatedRequest
                        (Key.Type.COMPLEX, Object.class)
                .startKey(Key.complex(1000).add(false).add("uuid"))
                .endKey(Key.complex(1002).add(false).add("uuid"))
                .rowsPerPage(30).build().getResponse().getValues();

        assertThat(result.size(), is(2));
    }

    /**
     * Assert that passing a complex key with integer, string, and
     * boolean objects in query will produce a result with
     * JSON object values.
     * Assert that the keys and values in the query result's
     * JSON objects are the same as the expected objects.
     */
    @Test
    public void queryWithComplexStartEndKeyAndJsonObjectValue() throws Exception {
        init();

        List<JsonObject> result = db.getViewRequestBuilder("example", "boolean_creator_created")
                .newRequest
                        (Key.Type.COMPLEX, JsonObject.class)
                .startKey(Key.complex(true).add("uuid").add(1))
                .endKey(Key.complex(true).add("uuid").add(2000))
                .build().getResponse().getValues();

        assertThat(result.size(), is(2));

        Gson gson = new Gson();
        ArrayList<JsonObject> expectedJsonObject = new ArrayList<JsonObject>(),
                actualJsonObject = new ArrayList<JsonObject>();

        for (int i = 0; i < result.size(); i++) {
            expectedJsonObject.add(multiValueKeyInit(null, i + 1));
            //Build a list from the query's results of the JSON objects from 'contentArray'
            JsonObject jsonValueObject = result.get(i);
            JsonObject actualJsonContentObject = jsonValueObject.get("contentArray")
                    .getAsJsonArray().get(0).getAsJsonObject();
            actualJsonObject.add(actualJsonContentObject);
        }

        assertJsonObjectKeysAndValues(expectedJsonObject, actualJsonObject);
    }

    /**
     * Assert that passing a complex key with integer, string, and
     * boolean objects in queryView will produce a result with
     * JSON object values.
     * Assert that the keys and values in the page result's
     * JSON objects are the same as the expected objects.
     */
    @Test
    public void queryPageWithComplexStartEndKeyAndJsonObjectValue() throws Exception {
        init();

        List<JsonObject> result = db.getViewRequestBuilder("example", "boolean_creator_created")
                .newPaginatedRequest
                        (Key.Type.COMPLEX, JsonObject.class).startKey(Key.complex(true)
                        .add("uuid").add(1))
                .endKey(Key.complex(true).add("uuid").add(2000)).rowsPerPage(30)
                .build().getResponse()
                .getValues();

        assertThat(result.size(), is(2));

        Gson gson = new Gson();
        ArrayList<JsonObject> expectedJsonObject = new ArrayList<JsonObject>(),
                actualJsonObject = new ArrayList<JsonObject>();

        for (int i = 0; i < result.size(); i++) {
            expectedJsonObject.add(multiValueKeyInit(null, i + 1));
            //Build a list from the query's results of the JSON objects from 'contentArray'
            JsonObject actualJsonContentObject = (gson.toJsonTree(result.get(i)))
                    .getAsJsonObject().get("contentArray")
                    .getAsJsonArray().get(0).getAsJsonObject();
            actualJsonObject.add(actualJsonContentObject);
        }

        assertJsonObjectKeysAndValues(expectedJsonObject, actualJsonObject);
    }

    /**
     * Assert that passing a complex key with integer, string, and
     * boolean objects in query will produce a result with
     * JSON array values.
     * Assert that the keys and values in the query result's
     * JSON arrays are the same as the expected arrays.
     */
    @Test
    public void queryWithComplexStartEndKeyAndJsonArrayValue() throws Exception {
        init();

        List<JsonArray> result = db.getViewRequestBuilder("example", "creator_boolean_total")
                .newRequest
                        (Key.Type.COMPLEX, JsonArray.class).startKey(Key.complex("uuid")
                        .add(true).add(1))
                .endKey(Key.complex("uuid").add(true).add(2000)).build()
                .getResponse()
                .getValues();

        assertThat(result.size(), is(2));

        Gson gson = new Gson();
        ArrayList<JsonArray> expectedJsonArray = new ArrayList<JsonArray>(),
                actualJsonArray = new ArrayList<JsonArray>();


        for (int i = 0; i < result.size(); i++) {
            JsonArray expectedArray = new JsonArray();
            expectedArray.add(gson.toJsonTree("key-" + (i + 2)));
            expectedArray.add(gson.toJsonTree(10.009999999999999787 + i + 1));
            expectedJsonArray.add(expectedArray);
            //Build a list from the query's results of the JSON value array
            JsonArray actualJsonArrayValue = result.get(i);
            actualJsonArray.add(actualJsonArrayValue);
        }

        assertJsonArrayKeysAndValues(expectedJsonArray, actualJsonArray);
    }

    /**
     * Assert that passing a complex key with integer, string, and
     * boolean objects in queryView will produce a result with
     * JSON array values.
     * Assert that the keys and values in the page result's
     * JSON arrays are the same as the expected arrays.
     */
    @Test
    public void queryPageWithComplexStartEndKeyAndJsonArrayValue() throws Exception {
        init();

        List<JsonArray> result = db.getViewRequestBuilder("example", "creator_boolean_total")
                .newPaginatedRequest
                        (Key.Type.COMPLEX, JsonArray.class).startKey(Key.complex("uuid")
                        .add(true).add(1))
                .endKey(Key.complex("uuid").add(true).add(2000)).rowsPerPage(30)
                .build().getResponse()
                .getValues();

        assertThat(result.size(), is(2));

        Gson gson = new Gson();
        ArrayList<JsonArray> expectedJsonArray = new ArrayList<JsonArray>(),
                actualJsonArray = new ArrayList<JsonArray>();

        for (int i = 0; i < result.size(); i++) {
            JsonArray expectedArray = new JsonArray();
            expectedArray.add(gson.toJsonTree("key-" + (i + 2)));
            expectedArray.add(gson.toJsonTree(10.009999999999999787 + i + 1));
            expectedJsonArray.add(expectedArray);
            //Build a list from the query's results of the JSON value array
            JsonArray actualJsonArrayValue = result.get(i);
            actualJsonArray.add(actualJsonArrayValue);
        }

        assertJsonArrayKeysAndValues(expectedJsonArray, actualJsonArray);
    }

    @Test
    public void byComplexKey() throws Exception {
        init();

        List<Foo> foos = db.getViewRequestBuilder("example", "by_date").newRequest(Key.Type.COMPLEX,
                Object.class).includeDocs(true).keys(Key.complex(2011, 10, 15)).reduce(false)
                .build().getResponse().getDocsAs(Foo
                        .class);

        assertThat(foos.size(), is(2));
    }

    @Test
    public void byComplexKeys() throws Exception {
        init();
        Key.ComplexKey complexKey1 = Key.complex(new Integer[]{2011, 10, 15});
        Key.ComplexKey complexKey2 = Key.complex(new Integer[]{2013, 12, 17});

        List<Foo> foos = db.getViewRequestBuilder("example", "by_date").newRequest(Key.Type.COMPLEX,
                Object.class).includeDocs(true).keys(complexKey1, complexKey2).reduce(false)
                .build().getResponse().getDocsAs(Foo
                        .class);

        assertThat(foos.size(), is(3));
    }

    @Test
    public void viewResultEntries() throws Exception {
        init();

        Collection<ViewResponse.Row<Key.ComplexKey, String>> rows = db.getViewRequestBuilder
                ("example",
                        "by_date").newRequest(Key.Type.COMPLEX,
                String.class).reduce(false).build().getResponse().getRows();

        assertThat(rows.size(), is(3));
    }

    @Test
    public void scalarValues() throws Exception {
        init();

        ViewRequestBuilder builder = db.getViewRequestBuilder("example", "by_tag");

        int allTags = builder.newRequest(Key.Type.STRING, int.class).build().getSingleValue();
        assertThat(allTags, is(4));

        long couchDbTags = builder.newRequest(Key.Type.STRING, long.class).keys("couchdb").build()
                .getSingleValue();
        assertThat(couchDbTags, is(2L));

        String javaTags = builder.newRequest(Key.Type.STRING, String.class).keys("java").build()
                .getSingleValue();
        assertThat(javaTags, is("1"));
    }

    @Test
    public void viewWithNoResult_emptyList() throws IOException {
        init();
        assertEquals("The results list should be of length 0", 0, db.getViewRequestBuilder
                ("example", "by_tag").newRequest(Key.Type.STRING, Object.class).keys
                ("javax").build().getResponse().getKeys().size());

    }

    @Test
    public void viewWithNoResult_nullSingleResult() throws IOException {
        init();
        assertNull("The single result should be null", db.getViewRequestBuilder("example",
                "by_tag").newRequest(Key.Type.STRING,
                Object.class).keys
                ("javax").build().getSingleValue());

    }

    @Test
    public void groupLevel() throws Exception {
        init();
        List<Integer> result = db.getViewRequestBuilder("example", "by_date").newRequest(Key.Type
                .COMPLEX, Integer.class).groupLevel(2).build().getResponse().getValues();
        assertThat(result.size(), is(2));
    }

    @Test
    public void allDocs() throws Exception {
        init();
        db.save(new Foo());
        List<String> allDocIds = db.getAllDocsRequestBuilder().build().getResponse().getDocIds();
        assertThat(allDocIds.size(), not(0));
        Map<String, String> idsAndRevs = db.getAllDocsRequestBuilder().build().getResponse()
                .getIdsAndRevs();
        assertThat(idsAndRevs.size(), not(0));
        for (Map.Entry<String, String> doc : idsAndRevs.entrySet()) {
            assertNotNull("The document _rev value should not be null", doc.getValue());
        }
    }

    @Test
    public void allDocsWithKeys() throws Exception {
        init();
        String id1 = db.save(new Foo()).getId();
        String id2 = db.save(new Foo()).getId();
        //create 3 and 4, but we don't care about the IDs
        db.save(new Foo()).getId();
        db.save(new Foo()).getId();

        List<String> allDocIds = db.getAllDocsRequestBuilder().keys(id1, id2).build().getResponse()
                .getDocIds();
        assertThat(allDocIds.size(), is(2));
    }

    /**
     * @param index the index to encode.
     * @return a three character string representing the given {@code index}.
     */
    public static String docTitle(int index) {
        return String.format("%03d", index);
    }

    private void init() {
        Foo foo = null;

        foo = new Foo("id-1", "key-1");
        foo.setTags(Arrays.asList(new String[]{"couchdb", "views"}));
        foo.setComplexDate(new int[]{2011, 10, 15});
        multiValueKeyInit(foo, 0);
        db.save(foo);

        foo = new Foo("id-2", "key-2");
        foo.setTags(Arrays.asList(new String[]{"java", "couchdb"}));
        foo.setComplexDate(new int[]{2011, 10, 15});
        multiValueKeyInit(foo, 1);
        db.save(foo);

        foo = new Foo("id-3", "key-3");
        foo.setComplexDate(new int[]{2013, 12, 17});
        multiValueKeyInit(foo, 2);
        db.save(foo);
    }

    public static JsonObject multiValueKeyInit(Foo foo, int i) {
        //JSON object for multi value key array tests
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("creator", "uuid");
        jsonObject.addProperty("created", 1000 + i);
        jsonObject.addProperty("boolean", (i != 0));
        jsonObject.addProperty("total", 10.01 + i);
        jsonObject.addProperty("quotes", "\"quotes\" " + String.valueOf(i));
        jsonObject.addProperty("spaces", " spaces " + String.valueOf(i));
        jsonObject.addProperty("letters", (char) ('a' + i) + "bc");
        jsonObject.addProperty("one", 1);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject);

        if (foo != null) {
            foo.setContentArray(jsonArray);
        }

        return jsonObject;
    }

    /**
     * Helper method to assert that keys and values in two JSON objects are equal.
     *
     * @param expectedJson the expected JSON element(s)
     * @param actualJson   the actual JSON element(s) from test result list
     */
    private void assertJsonObjectKeysAndValues(ArrayList<JsonObject> expectedJson,
                                               ArrayList<JsonObject> actualJson) {

        assertEquals(expectedJson.size(),
                actualJson.size());

        for (int i = 0; i < actualJson.size(); i++) {
            JsonObject expectedJsonObject = expectedJson.get(i).getAsJsonObject();
            JsonObject actualJsonObject = actualJson.get(i).getAsJsonObject();

            Iterator<Map.Entry<String, JsonElement>> actualJsonIter =
                    actualJsonObject.entrySet().iterator();

            for (Map.Entry<String, JsonElement> expectedJsonMap :
                    expectedJsonObject.entrySet()) {
                String expectedJsonKey = expectedJsonMap.getKey();
                JsonElement expectedJsonValue = expectedJsonMap.getValue();

                if (actualJsonIter.hasNext()) {
                    Map.Entry<String, JsonElement> actualJsonMap = actualJsonIter.next();
                    assertEquals(expectedJsonKey, actualJsonMap.getKey());
                    assertEquals(expectedJsonValue, actualJsonMap.getValue());
                }
            }
        }
    }

    /**
     * Helper method to assert that keys and values in two JSON arrays are equal.
     *
     * @param expectedJson the expected JSON element(s)
     * @param actualJson   the actual JSON element(s) from test result list
     */
    private void assertJsonArrayKeysAndValues(ArrayList<JsonArray> expectedJson,
                                              ArrayList<JsonArray> actualJson) {
        assertEquals(expectedJson.size(), actualJson.size());

        for (int i = 0; i < actualJson.size(); i++) {
            //Check key and values in the JSON array
            JsonArray expectedJsonArray = expectedJson.get(i).getAsJsonArray();
            JsonArray actualJsonArray = actualJson.get(i).getAsJsonArray();

            assertEquals(expectedJsonArray.size(),
                    actualJsonArray.size());

            Iterator<JsonElement> actualJsonIter =
                    actualJsonArray.iterator();

            Iterator<JsonElement> expectedJsonIter =
                    expectedJsonArray.iterator();

            while (expectedJsonIter.hasNext()) {
                JsonElement expectedJsonElement = expectedJsonIter.next();
                JsonElement actualJsonElement = actualJsonIter.next();

                assertEquals(expectedJsonElement, actualJsonElement);
            }
        }
    }

    /**
     * Validate that it is possible to POST multiple requests to a view query and that the
     * results of each request are as expected.
     *
     * @throws IOException
     */
    @Test
    @Category(RequiresCloudant.class)
    public void multiRequest() throws IOException {
        init();
        ViewMultipleRequest<String, Object> multi = db.getViewRequestBuilder("example", "foo")
                .newMultipleRequest(Key.Type.STRING, Object.class)
                .keys("key-1").add()
                .keys("key-2").add()
                .keys("key-3").add()
                .build();
        int i = 1;
        List<ViewResponse<String, Object>> responses = multi.getViewResponses();
        assertEquals("There should be 3 respones for 3 requests", 3, responses.size());
        for (ViewResponse<String, Object> response : responses) {
            assertEquals("There should be 1 row in each response", 1, response.getRows().size());
            assertEquals("The returned key should be key-" + i, "key-" + i, response.getKeys()
                    .get(0));
            i++;
        }
    }

    /**
     * Validate that an IllegalStateException is thrown if an attempt is made to build a multi
     * request without calling add() before build().
     *
     * @throws IOException
     */
    @Test(expected = IllegalStateException.class)
    public void multiRequestBuildOnlyAfterAdd() throws IOException {
        ViewMultipleRequest<String, Object> multi = db.getViewRequestBuilder("example", "foo")
                .newMultipleRequest(Key.Type.STRING, Object.class)
                .keys("key-1").add()
                .keys("key-2").build();
    }

    /**
     * Assert that no additional pages are available on an unpaginated request even if additional
     * results are available.
     *
     * @throws Exception
     */
    @Test
    public void assertNoPagesOnUnpaginated() throws Exception {
        init();
        ViewResponse<String, Object> response = db.getViewRequestBuilder("example", "foo")
                .newRequest(Key.Type.STRING,
                        Object.class).limit(2).build().getResponse();
        assertEquals("There should be 2 keys returned", 2, response.getKeys().size());
        assertFalse("There should be no additional pages", response.hasNextPage());
        assertNull("The next page should be null", response.nextPage());
    }

    /**
     * Validate that it is possible to loop through pages using an enhanced for loop. Verify that
     * the results are expected on each page.
     *
     * @throws Exception
     */
    @Test
    public void enhancedForPagination() throws Exception {
        init();
        ViewRequest<String, Object> paginatedQuery = db.getViewRequestBuilder("example", "foo")
                .newPaginatedRequest(Key.Type.STRING,
                        Object.class).rowsPerPage(1).build();
        int i = 1;
        for (ViewResponse<String, Object> page : paginatedQuery.getResponse()) {
            assertEquals("There should be one key on each page", 1, page.getKeys().size());
            assertEquals("The key should be key-" + i, "key-" + i, page.getKeys().get(0));
            i++;
        }
    }

    /**
     * Assert that an IllegalArgumentException is thrown when rowsPerPage exceeds MAX_INT-1.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void rowsPerPageValidationMax() throws Exception {
        ViewRequest<String, Object> paginatedQuery = db.getViewRequestBuilder("example", "foo")
                .newPaginatedRequest(Key.Type.STRING,
                        Object.class).rowsPerPage(Integer.MAX_VALUE).build();
    }

    /**
     * Assert that an IllegalArgumentException is thrown when rowsPerPage is zero.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void rowsPerPageValidationZero() throws Exception {
        ViewRequest<String, Object> paginatedQuery = db.getViewRequestBuilder("example", "foo")
                .newPaginatedRequest(Key.Type.STRING,
                        Object.class).rowsPerPage(0).build();
    }

    /**
     * Assert that an IllegalArgumentException is thrown when rowsPerPage is negative.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void rowsPerPageValidationMin() throws Exception {
        ViewRequest<String, Object> paginatedQuery = db.getViewRequestBuilder("example", "foo")
                .newPaginatedRequest(Key.Type.STRING,
                        Object.class).rowsPerPage(-25).build();
    }

    /**
     * Assert that an IllegalArgumentException is thrown when specifying both reduce=true and
     * include_docs=true
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void validationIncludeDocsReduceView() throws Exception {
        ViewRequest<String, Object> paginatedQuery = db.getViewRequestBuilder("example", "foo")
                .newRequest(Key.Type.STRING,
                        Object.class).includeDocs(true).reduce(true).build();
    }

    /**
     * Test that no validation exception is thrown when reduce is not specified, but include_docs
     * is true. This is because whilst reduce=true and include_docs=true are mutually exclusive
     * and even though reduce defaults to true, if the view does not actually have a reduce function
     * then include_docs is still valid on the server.
     *
     * @throws Exception
     */
    @Test
    public void noExceptionWhenReduceTrueByDefault() throws Exception {
        ViewRequest<String, Object> paginatedQuery = db.getViewRequestBuilder("example", "foo")
                .newRequest(Key.Type.STRING,
                        Object.class).includeDocs(true).build();
    }

    /**
     * Tests than an IllegalArgumentException is thrown when group_level is set without using a
     * complex key.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void validationGroupLevelWithSimpleKey() throws Exception {
        ViewRequest<String, Object> paginatedQuery = db.getViewRequestBuilder("example", "foo")
                .newRequest(Key.Type.STRING,
                        Object.class).groupLevel(1).build();
    }

    /**
     * Tests than an IllegalArgumentException is thrown when group_level is set without using a
     * reduce view.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void validationGroupLevelWithNonReduce() throws Exception {
        ViewRequest<String, Object> paginatedQuery = db.getViewRequestBuilder("example", "foo")
                .newRequest(Key.Type.STRING,
                        Object.class).reduce(false).groupLevel(1).build();
    }

    /**
     * Tests than an IllegalArgumentException is thrown when group is set without using a
     * reduce view.
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void validationGroupWithNonReduce() throws Exception {
        ViewRequest<String, Object> paginatedQuery = db.getViewRequestBuilder("example", "foo")
                .newRequest(Key.Type.STRING,
                        Object.class).reduce(false).group(true).build();
    }

    /**
     * Test written for https://github.com/cloudant/java-cloudant/issues/172 where complex key
     * integers were being changed to floats when using pagination tokens. For example ["uuid", 1]
     * would become ["uuid", 1.0] when trying to request the second page. 1.0 sorts before 1, so the
     * wrong documents would be returned on the second page.
     *
     * The test creates 10 documents that emit complex keys of e.g. ["uuid", 1000]
     *
     * We use 5 documents per page and use a start key of ["uuid", 1].
     *
     * The test gets the first page and then retrieves the second page using a token.
     * It captures the request URL and then asserts that the startkey was of the correct integer
     * form.
     *
     * @throws Exception
     */
    @Test
    public void testComplexKeyContainingIntTokenPagination() throws Exception {

        // Use the intercepted client as we want to check the query string in the connection
        db = interceptedDB.get();
        Utils.putDesignDocs(db);

        // Create 10 documents in the database
        int nDocs = 10;
        for (int i = 0; i < nDocs; i++) {
            Foo f = new Foo("" + i);
            f.setPosition(i);
            multiValueKeyInit(f, i);
            db.save(f);
        }

        // Use the creator_created view (complex keys [String, int])
        ViewRequest<Key.ComplexKey, Object> request = db.getViewRequestBuilder("example",
                "creator_created").newPaginatedRequest(Key.Type.COMPLEX, Object.class).startKey(Key
                .complex("uuid").add(1)).rowsPerPage(5).build();

        // Get the second page response by token
        ViewResponse<Key.ComplexKey, Object> response = request.getResponse();
        String token = response.getNextPageToken();
        request.getResponse(token);

        // We want the last context
        HttpConnectionInterceptorContext context = cci.contexts.get(cci.contexts.size() - 1);
        String query = context.connection.url.getQuery();
        assertTrue("The query startkey should match.", query.contains("startkey=%5B%22uuid%22," +
                "1005%5D"));
    }

    /**
     * Tests that reserved characters in a view parameter are encoded.
     * https://github.com/cloudant/java-cloudant/issues/202
     *
     * @throws Exception
     */
    @Test
    public void testUriReservedCharsInStartKey() throws Exception {
        char[] reservedChars = new char[]{
                // 3986 general delimeters
                ':', '/', '?', '#', '[', ']', '@',
                // 3986 sub delimeters
                '!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '='};
        for (char c : reservedChars) {
            testUriReservedCharInStartKey(c);
        }
    }

    private void testUriReservedCharInStartKey(char c) throws Exception {
        try {
            ViewRequest<String, Object> request = db.getViewRequestBuilder("example",
                    "foo").newPaginatedRequest(Key.Type.STRING, Object.class)
                    .startKey("a" + c + "b")
                    .rowsPerPage(5)
                    .build();
            request.getResponse();
            // We don't actually need to do anything with the response, just ensure the request does
            // not cause an exception.
        } catch (Exception e) {
            fail("The character " + c + " caused an exception to be thrown.");
        }
    }
}
