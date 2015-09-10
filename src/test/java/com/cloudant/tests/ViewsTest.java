/*
 * Copyright (C) 2011 lightcouch.org
 *
 * Modifications for this distribution by IBM Cloudant, Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.View;
import com.cloudant.client.api.model.Page;
import com.cloudant.client.api.model.ViewResult;
import com.cloudant.test.main.RequiresDB;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.cloudant.client.org.lightcouch.NoDocumentException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Category(RequiresDB.class)
public class ViewsTest {


    private static Database db;
    private CloudantClient account;
    private static String[] testViews = {"example/doc_title", "example/creator_created",
            "example/creator_boolean_total", "example/created_boolean_creator"};

    @Before
    public void setUp() {
        account = CloudantClientHelper.getClient();

        db = account.database("lightcouch-db-test", true);

        db.syncDesignDocsWithDb();
    }

    @After
    public void tearDown() {
        account.deleteDB("lightcouch-db-test");
        account.shutdown();
    }

    @Test
    public void queryView() {
        init();
        List<Foo> foos = db.view("example/foo")
                .includeDocs(true)
                .query(Foo.class);
        assertThat(foos.size(), not(0));
    }

    @Test
    public void byKey() {
        init();
        List<Foo> foos = db.view("example/foo")
                .includeDocs(true)
                .key("key-1")
                .query(Foo.class);
        assertThat(foos.size(), is(1));
    }

    @Test
    public void byKeys() {
        init();
        List<Foo> foos = db.view("example/foo")
                .includeDocs(true)
                .keys(new Object[]{"key-1", "key-2"})
                .query(Foo.class);
        assertThat(foos.size(), is(2));
    }

    @Test
    public void byStartAndEndKey() {
        init();
        List<Foo> foos = db.view("example/foo")
                .startKey("key-1")
                .endKey("key-2")
                .includeDocs(true)
                .query(Foo.class);
        assertThat(foos.size(), is(2));
    }

    /**
     * Assert that passing a boolean key with value 'false'
     * in query will produce a result list of
     * all false docs.
     */
    @Test
    public void queryWithStartAndEndBooleanKey() {
        init();

        View view = db.view("example/boolean")
                .startKey(false)
                .endKey(false);

        List<Object> result = view.query(Object.class);

        assertThat(result.size(), is(1));
    }

    /**
     * Assert that passing a boolean key with value 'true' in
     * queryView will produce a result list of a true doc.
     */
    @Test
    public void queryPageWithStartAndEndBooleanKey() {
        init();

        View view = db.view("example/boolean")
                .startKey(true)
                .endKey(true);

        Page page = view.queryPage(2, null, Object.class);

        assertThat(page.getResultList().size(), is(2));
    }

    /**
     * Assert that passing a string key with double quotes
     * or spaces in query and queryView will produce
     * a result list without exception.
     */
    @Test
    public void queryWithStartAndEndStringIntKeyArray() {
        init();

        //Query for testing string with spaces
        View view = db.view("example/spaces_created")
                .startKey(" spaces ", 1)
                .endKey(" spaces 1", 2000);

        List<Object> result = view.query(Object.class);

        assertThat(result.size(), is(2));
    }

    /**
     * Assert that passing a string key with spaces
     * in queryPage will produce a result list
     * without exception.
     */
    @Test
    public void queryPageWithStartAndEndStringIntKeyArray() {
        init();

        //Query for testing string with spaces
        View view = db.view("example/spaces_created")
                .startKey(" spaces ", 1)
                .endKey(" spaces 0", 2000);

        Page page = view.queryPage(30, null, Object.class);

        assertThat(page.getResultList().size(), is(1));
    }

    /**
     * Assert that passing a string key with quotes
     * in query will produce a result list
     * without exception.
     */
    @Test
    public void queryWithStartAndEndStringWithQuotesAndIntKeyArray() {
        init();

        View view = db.view("example/quotes_created")
                .startKey("\"quotes\" ", 1)
                .endKey("\"quotes\" 0", 2000);

        List<Object> result = view.query(Object.class);

        assertThat(result.size(), is(1));
    }

    /**
     * Assert that passing a string key with quotes
     * in query will produce a result list
     * without exception.
     */
    @Test
    public void queryPageWithStartAndEndStringWithQuotesAndIntKeyArray() {
        init();

        View view = db.view("example/quotes_created")
                .startKey("\"quotes\" ", 1)
                .endKey("\"quotes\" 1", 2000);

        Page page = view.queryPage(30, null, Object.class);

        assertThat(page.getResultList().size(), is(2));
    }

    /**
     * Assert that passing an array of integers in query
     * will produce a result list of all docs.
     */
    @Test
    public void queryWithStartAndEndIntKeyArray() {
        init();

        View view = db.view("example/created")
                .startKey(1, 10)
                .endKey(2000, 5000);

        List<Object> result = view.query(Object.class);

        assertThat(result.size(), is(3));
    }

    /**
     * Assert that passing an array of integers in queryPage
     * will produce a result list of two docs.
     */
    @Test
    public void queryPageWithStartAndEndIntKeyArray() {
        init();

        View view = db.view("example/created")
                .startKey(1, 10)
                .endKey(1001, 2000);

        Page page = view.queryPage(30, null, Object.class);

        assertThat(page.getResultList().size(), is(2));
    }

    /**
     * Assert that passing a key in query with an explicit object array of a
     * string and integer value will produce a result list with three docs.
     */
    @Test
    public void queryWithStartAndEndObjectKeyArray() {
        init();

        View view = db.view("example/creator_created")
                .startKey(new Object[]{"uuid", 1})
                .endKey(new Object[]{"uuid", 1010});


        List<Object> result = view.query(Object.class);

        assertThat(result.size(), is(3));
    }

    /**
     * Assert that passing a key in queryPage with an explicit object array of
     * a string and integer value will produce a result list with three docs.
     */
    @Test
    public void queryPageWithStartAndEndObjectKeyArray() {
        init();

        View view = db.view("example/creator_created")
                .startKey(new Object[]{"uuid", 1})
                .endKey(new Object[]{"uuid", 1010});

        Page page = view.queryPage(30, null, Object.class);

        assertThat(page.getResultList().size(), is(3));
    }

    /**
     * Assert that passing a key in query with an array of number
     * values will produce a result list with two docs.
     */
    @Test
    public void queryWithStartAndEndNumbersKeyArray() {
        init();

        View view = db.view("example/created_total")
                .startKey(1000, 12.00)
                .endKey(1002, 15.00);

        List<Object> result = view.query(Object.class);

        assertThat(result.size(), is(2));
    }

    /**
     * Assert that passing a key in queryPage with an array of number
     * values will produce a result list with one doc.
     */
    @Test
    public void queryPageWithStartAndEndNumbersKeyArray() {
        init();

        View view = db.view("example/total_created")
                .startKey(10.00, 1)
                .endKey(11.00, 2000);

        Page page = view.queryPage(30, null, Object.class);

        assertThat(page.getResultList().size(), is(1));
    }

    /**
     * Assert that passing a key in query with an array of a boolean, string,
     * and integer value will produce a result list with three docs.
     */
    @Test
    public void queryWithStartAndEndBooleanStringIntegerKeyArray() {
        init();

        View view = db.view("example/boolean_creator_created")
                .startKey(false, "uuid", 1)
                .endKey(true, "uuid", 2000);

        List<Object> result = view.query(Object.class);

        assertThat(result.size(), is(3));
    }

    /**
     * Assert that passing a key in queryPage with an array of a boolean, string,
     * and integer value will produce a result list with one doc.
     */
    @Test
    public void queryPageWithStartAndEndBooleanStringIntegerKeyArray() {
        init();

        View view = db.view("example/boolean_creator_created")
                .startKey(false, "uuid", 1)
                .endKey(false, "uuid", 2000);

        Page page = view.queryPage(30, null, Object.class);

        assertThat(page.getResultList().size(), is(1));
    }

    /**
     * Assert that passing a key in query with an array of a integer, boolean,
     * and string value will produce a result list with one doc.
     */
    @Test
    public void queryWithStartAndEndIntegerBooleanStringKeyArray() {
        init();

        View view = db.view("example/created_boolean_creator")
                .startKey(1000, false, "uuid")
                .endKey(1000, false, "uuid");

        List<Object> result = view.query(Object.class);

        assertThat(result.size(), is(1));
    }

    /**
     * Assert that passing a key in query with an explicit object array will produce the same
     * result list as the queryWithStartAndEndIntegerBooleanStringKeyArray test.
     */
    @Test
    public void queryWithStartAndEndKeyUsingNewObjectWithIntegerBooleanStringArray() {
        init();

        View view = db.view("example/created_boolean_creator")
                .startKey(new Object[]{1000, false, "uuid"})
                .endKey(new Object[]{1000, false, "uuid"});

        List<Object> result = view.query(Object.class);

        assertThat(result.size(), is(1));
    }

    /**
     * Assert that passing a key in queryPage with an array of a integer, boolean,
     * and string value will produce a result list with two docs.
     */
    @Test
    public void queryPageWithStartAndEndIntegerBooleanStringKeyArray() {
        init();

        View view = db.view("example/created_boolean_creator")
                .startKey(1000, false, "uuid")
                .endKey(1002, false, "uuid");

        Page page = view.queryPage(30, null, Object.class);

        assertThat(page.getResultList().size(), is(2));
    }

    /**
     * Assert that passing a key in queryPage with an explicit object array will produce the same
     * result list as the queryPageWithStartAndEndIntegerBooleanStringKeyArray test.
     */
    @Test
    public void queryPageWithStartAndEndKeyUsingNewObjectWithIntegerBooleanStringArray() {
        init();

        View view = db.view("example/created_boolean_creator")
                .startKey(new Object[]{1000, false, "uuid"})
                .endKey(new Object[]{1002, false, "uuid"});

        Page page = view.queryPage(30, null, Object.class);

        assertThat(page.getResultList().size(), is(2));
    }

    /**
     * Assert that passing a complex key with integer, string, and
     * boolean objects in query will produce a result with
     * JSON object values.
     * Assert that the keys and values in the query result's
     * JSON objects are the same as the expected objects.
     */
    @Test
    public void queryWithComplexStartEndKeyAndJsonObjectValue() {
        init();

        View view = db.view("example/boolean_creator_created")
                .startKey(true ,"uuid",1)
                .endKey(true ,"uuid", 2000);

        List<Object> result = view.query(Object.class);

        assertThat(result.size(), is(2));

        Gson gson = new Gson();
        ArrayList<JsonObject> expectedJsonObject = new ArrayList<JsonObject>(),
                              actualJsonObject = new ArrayList<JsonObject>();

        for(int i = 0; i < result.size(); i++) {
            expectedJsonObject.add(multiValueKeyInit(null, i + 1));
            //Build a list from the query's results of the JSON objects from 'contentArray'
            JsonObject jsonValueObject = (gson.toJsonTree(result.get(i)))
                    .getAsJsonObject().get("value").getAsJsonObject();
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
    public void queryPageWithComplexStartEndKeyAndJsonObjectValue() {
        init();

        View view = db.view("example/boolean_creator_created")
                .startKey(true ,"uuid",1)
                .endKey(true ,"uuid", 2000);

        Page page = view.queryPage(30, null, Object.class);

        assertThat(page.getResultList().size(), is(2));

        Gson gson = new Gson();
        ArrayList<JsonObject> expectedJsonObject = new ArrayList<JsonObject>(),
                actualJsonObject = new ArrayList<JsonObject>();

        for(int i = 0; i < page.getResultList().size(); i++) {
            expectedJsonObject.add(multiValueKeyInit(null, i + 1));
            //Build a list from the query's results of the JSON objects from 'contentArray'
            JsonObject actualJsonContentObject = (gson.toJsonTree(page.getResultList().get(i)))
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
    public void queryWithComplexStartEndKeyAndJsonArrayValue() {
        init();

        View query = db.view("example/creator_boolean_total")
                .startKey("uuid" ,true, 1)
                .endKey("uuid" ,true, 2000);

        List<Object> result = query.query(Object.class);

        assertThat(result.size(), is(2));

        Gson gson = new Gson();
        ArrayList<JsonArray> expectedJsonArray = new ArrayList<JsonArray>(),
                actualJsonArray = new ArrayList<JsonArray>();


        for(int i = 0; i < result.size(); i++) {
            JsonArray expectedArray = new JsonArray();
            expectedArray.add(gson.toJsonTree("key-" + (i + 2)));
            expectedArray.add(gson.toJsonTree(10.009999999999999787 + i + 1));
            expectedJsonArray.add(expectedArray);
            //Build a list from the query's results of the JSON value array
            JsonArray jsonValueArray = (gson.toJsonTree(result.get(i)))
                    .getAsJsonObject().get("value").getAsJsonArray();
            actualJsonArray.add(jsonValueArray);
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
    public void queryPageWithComplexStartEndKeyAndJsonArrayValue() {
        init();

        View query = db.view("example/creator_boolean_total")
                .startKey("uuid" ,true, 1)
                .endKey("uuid" ,true, 2000);

        Page page = query.queryPage(30, null, Object.class);

        assertThat(page.getResultList().size(), is(2));

        Gson gson = new Gson();
        ArrayList<JsonArray> expectedJsonArray = new ArrayList<JsonArray>(),
                actualJsonArray = new ArrayList<JsonArray>();

        for(int i = 0; i < page.getResultList().size(); i++) {
            JsonArray expectedArray = new JsonArray();
            expectedArray.add(gson.toJsonTree("key-" + (i + 2)));
            expectedArray.add(gson.toJsonTree(10.009999999999999787 + i + 1));
            expectedJsonArray.add(expectedArray);
            //Build a list from the query's results of the JSON value array
            JsonArray actualArray = new JsonArray();
            JsonElement jsonTitleValue = (gson.toJsonTree(page.getResultList().get(i)))
                    .getAsJsonObject().get("title");
            actualArray.add(jsonTitleValue);
            JsonElement jsonTotalValue = (gson.toJsonTree(page.getResultList().get(i)))
                    .getAsJsonObject().get("contentArray").getAsJsonArray().get(0)
                    .getAsJsonObject().get("total");
            actualArray.add(jsonTotalValue);
            actualJsonArray.add(actualArray);
        }

        assertJsonArrayKeysAndValues(expectedJsonArray, actualJsonArray);
    }

    @Test
    public void byComplexKey() {
        init();
        List<Foo> foos = db.view("example/by_date")
                .key(2011, 10, 15)
                .includeDocs(true)
                .reduce(false)
                .query(Foo.class);
        assertThat(foos.size(), is(2));
    }

    @Test
    public void byComplexKeys() {
        init();
        int[] complexKey1 = new int[]{2011, 10, 15};
        int[] complexKey2 = new int[]{2013, 12, 17};
        List<Foo> foos = db.view("example/by_date")
                .keys(new Object[]{complexKey1, complexKey2})
                .includeDocs(true)
                .reduce(false)
                .query(Foo.class);
        assertThat(foos.size(), is(3));
    }

    @Test
    public void viewResultEntries() {
        init();
        ViewResult<int[], String, Foo> viewResult = db.view("example/by_date")
                .reduce(false)
                .queryView(int[].class, String.class, Foo.class);
        assertThat(viewResult.getRows().size(), is(3));
    }

    @Test
    public void scalarValues() {
        init();
        int allTags = db.view("example/by_tag").queryForInt();
        assertThat(allTags, is(4));

        long couchDbTags = db.view("example/by_tag")
                .key("couchdb")
                .queryForLong();
        assertThat(couchDbTags, is(2L));

        String javaTags = db.view("example/by_tag")
                .key("java")
                .queryForString();
        assertThat(javaTags, is("1"));
    }

    @Test(expected = NoDocumentException.class)
    public void viewWithNoResult_throwsNoDocumentException() {
        init();
        db.view("example/by_tag")
                .key("javax")
                .queryForInt();
    }

    @Test
    public void groupLevel() {
        init();
        ViewResult<int[], Integer, Foo> viewResult = db
                .view("example/by_date")
                .groupLevel(2)
                .queryView(int[].class, Integer.class, Foo.class);
        assertThat(viewResult.getRows().size(), is(2));
    }

    @Test
    public void allDocs() {
        init();
        db.save(new Foo());
        List<JsonObject> allDocs = db.view("_all_docs")
                .query(JsonObject.class);
        assertThat(allDocs.size(), not(0));
    }

    /**
     * @param index the index to encode.
     * @return a three character string representing the given {@code index}.
     */
    private static String docTitle(int index) {
        return String.format("%03d", index);
    }

    /**
     * Checks the document titles are as expected on the given {@code page}.
     *
     * @param page        the page to check.
     * @param docCount    the total number of documents in the view.
     * @param docsPerPage the number of documents per page.
     * @param pageNumber  the page number of the {@code page}.
     * @param descending  whether the view is descending or not.
     */
    private static void checkDocumentTitles(Page page, int docCount, int docsPerPage, int
            pageNumber, boolean descending) {
        int offset = (pageNumber - 1) * docsPerPage + 1;
        if (descending) {
            offset = docCount + 1 - offset;
        }
        List<Foo> resultList = page.getResultList();
        for (int i = 0; i < resultList.size(); ++i) {
            assertEquals("Document titles do not match", docTitle(descending ? offset-- :
                    offset++), resultList.get(i).getTitle());
        }
    }

    /**
     * Checks various aspects of the given {@code page} are as expected.
     *
     * @param page        the page to check.
     * @param docCount    the total number of documents in the view.
     * @param docsPerPage the number of documents per page.
     * @param pageNumber  the page number of the {@code page}.
     * @param descending  whether the view is descending or not.
     */
    private static void checkPage(Page page, int docCount, int docsPerPage, int pageNumber,
                                  boolean descending) {
        if (pageNumber == 1) {
            assertFalse(page.isHasPrevious());
        } else {
            assertTrue(page.isHasPrevious());
        }

        double numberOfPages = docCount / (double) docsPerPage;
        if (pageNumber >= numberOfPages) {
            assertFalse(page.isHasNext());
        } else {
            assertTrue(page.isHasNext());
        }

        int startIndex = (pageNumber - 1) * docsPerPage + 1;
        assertThat(page.getResultFrom(), is(startIndex));
        assertThat(page.getResultTo(), is(Math.min(startIndex + docsPerPage - 1, docCount)));
        assertThat(page.getPageNumber(), is(pageNumber));
        if (page.isHasNext() || docCount % docsPerPage == 0) {
            assertThat(page.getResultList().size(), is(docsPerPage));
        } else {
            assertThat(page.getResultList().size(), is(docCount % docsPerPage));
        }
        checkDocumentTitles(page, docCount, docsPerPage, pageNumber, descending);
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     */
    @Test
    public void paginationAscendingReusingViewAllTheWayInEachDirectionTwice() {
        View view = db.view("example/foo")
                .reduce(false);
        checkPagination(false, 6, 2, view, 3, 1, 3, 1);
    }

    /**
     * Run the same check as paginationAscendingReusingViewAllTheWayInEachDirectionTwice
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationAscendingReusingMVKeyViewAllTheWayInEachDirectionTwice() {
        new CheckPaginationWithMultiValueKey()
                .descending(false)
                .docCount(6)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .runTest();
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     */
    @Test
    public void paginationDescendingReusingViewAllTheWayInEachDirectionTwice() {
        View view = db.view("example/foo")
                .reduce(false)
                .descending(true);
        checkPagination(true, 6, 2, view, 3, 1, 3, 1);
    }

    /**
     * Run the same check as paginationDescendingReusingViewAllTheWayInEachDirectionTwice
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationDescendingReusingMVKeyViewAllTheWayInEachDirectionTwice() {
        new CheckPaginationWithMultiValueKey()
                .descending(true)
                .docCount(6)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .runTest();
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     */
    @Test
    public void paginationAscendingReusingViewPartialLastPageAllTheWayInEachDirectionTwice() {
        View view = db.view("example/foo")
                .reduce(false);
        checkPagination(false, 5, 2, view, 3, 1, 3, 1);
    }

    /**
     * Run the same check as paginationAscendingReusingViewPartialLastPageAllTheWayInEachDirection
     * Twice test on views using an array of multi-value keys.
     */
    @Test
    public void paginationAscendingReusingMVKeyViewPartialLastPageAllTheWayInEachDirectionTwice() {
        new CheckPaginationWithMultiValueKey()
                .descending(false)
                .docCount(5)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .runTest();
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationDescendingReusingViewPartialLastPageAllTheWayInEachDirectionTwice() {
        View view = db.view("example/foo")
                .reduce(false)
                .descending(true);
        checkPagination(true, 5, 2, view, 3, 1, 3, 1);
    }

    /**
     * Run the same check as paginationDescendingReusingViewPartialLastPageAllTheWayInEachDirection
     * Twice test on views using an array of multi-value keys.
     */
    @Test
    public void paginationDescendingReusingMVKeyViewPartialLastPageAllTheWayInEachDirectionTwice() {
        new CheckPaginationWithMultiValueKey()
                .descending(true)
                .docCount(5)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .runTest();
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     */
    @Test
    public void paginationAscendingNewViewAllTheWayInEachDirectionTwice() {
        checkPagination(false, 6, 2, null, 3, 1, 3, 1);
    }

    /**
     * Run the same check as paginationAscendingNewViewAllTheWayInEachDirectionTwice
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationAscendingNewMVKeyViewAllTheWayInEachDirectionTwice() {
        new CheckPaginationWithMultiValueKey()
                .descending(false)
                .useNewView(true)
                .docCount(6)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .runTest();
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     */
    @Test
    public void paginationDescendingNewViewAllTheWayInEachDirectionTwice() {
        checkPagination(true, 6, 2, null, 3, 1, 3, 1);
    }

    /**
     * Run the same check as paginationDescendingNewViewAllTheWayInEachDirectionTwice
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationDescendingNewMVKeyViewAllTheWayInEachDirectionTwice() {
        new CheckPaginationWithMultiValueKey()
                .descending(true)
                .useNewView(true)
                .docCount(6)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .runTest();
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     */
    @Test
    public void paginationAscendingNewViewPartialLastPageAllTheWayInEachDirectionTwice() {
        checkPagination(false, 5, 2, null, 3, 1, 3, 1);
    }

    /**
     * Run the same check as paginationAscendingNewViewPartialLastPageAllTheWayInEachDirectionTwice
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationAscendingNewMVKeyViewPartialLastPageAllTheWayInEachDirectionTwice() {
        new CheckPaginationWithMultiValueKey()
                .descending(false)
                .useNewView(true)
                .docCount(5)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .runTest();
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a newfuture {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     */
    @Test
    public void paginationDescendingNewViewPartialLastPageAllTheWayInEachDirectionTwice() {
        checkPagination(true, 5, 2, null, 3, 1, 3, 1);
    }

    /**
     * Run the same check as paginationDescendingNewViewPartialLastPageAllTheWayInEachDirectionTwice
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationDescendingNewMVKeyViewPartialLastPageAllTheWayInEachDirectionTwice() {
        //checkPaginationWithMultiValueKey(true, 5, 2, 3, 1, 3, 1);
        new CheckPaginationWithMultiValueKey()
                .descending(true)
                .useNewView(true)
                .docCount(5)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .runTest();
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @Test
    public void paginationAscendingReusingViewPartWayInEachDirection() {
        View view = db.view("example/foo")
                .reduce(false);
        checkPagination(false, 30, 5, view, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Run the same check as paginationAscendingReusingViewPartWayInEachDirection
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationAscendingReusingMVKeyViewPartWayInEachDirection() {
        new CheckPaginationWithMultiValueKey()
                .descending(false)
                .docCount(30)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .runTest();
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @Test
    public void paginationDescendingReusingViewPartWayInEachDirection() {
        View view = db.view("example/foo")
                .reduce(false)
                .descending(true);
        checkPagination(true, 30, 5, view, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Run the same check as paginationDescendingReusingViewPartWayInEachDirection
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationDescendingReusingMVKeyViewPartWayInEachDirection() {
        new CheckPaginationWithMultiValueKey()
                .descending(true)
                .docCount(30)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .runTest();
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @Test
    public void paginationAscendingReusingViewPartialLastPagePartWayInEachDirection() {
        View view = db.view("example/foo")
                .reduce(false);
        checkPagination(false, 28, 5, view, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Run the same check as paginationAscendingReusingViewPartialLastPagePartWayInEachDirection
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationAscendingReusingMVKeyViewPartialLastPagePartWayInEachDirection() {
        new CheckPaginationWithMultiValueKey()
                .descending(false)
                .docCount(28)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .runTest();
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @Test
    public void paginationDescendingReusingViewPartialLastPagePartWayInEachDirection() {
        View view = db.view("example/foo")
                .reduce(false)
                .descending(true);
        checkPagination(true, 28, 5, view, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Run the same check as paginationDescendingReusingViewPartialLastPagePartWayInEachDirection
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationDescendingReusingMVKeyViewPartialLastPagePartWayInEachDirection() {
        new CheckPaginationWithMultiValueKey()
                .descending(true)
                .docCount(28)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .runTest();
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @Test
    public void paginationAscendingNewViewPartWayInEachDirection() {
        checkPagination(false, 30, 5, null, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Run the same check as paginationAscendingNewViewPartWayInEachDirection
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationAscendingNewMVKeyViewPartWayInEachDirection() {
        new CheckPaginationWithMultiValueKey()
                .descending(false)
                .useNewView(true)
                .docCount(30)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .runTest();
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @Test
    public void paginationDescendingNewViewPartWayInEachDirection() {
        checkPagination(true, 30, 5, null, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Run the same check as paginationDescendingNewViewPartWayInEachDirection
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationDescendingNewMVKeyViewPartWayInEachDirection() {
        new CheckPaginationWithMultiValueKey()
                .descending(true)
                .useNewView(true)
                .docCount(30)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .runTest();
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @Test
    public void paginationAscendingNewViewPartialLastPagePartWayInEachDirection() {
        checkPagination(false, 28, 5, null, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Run the same check as paginationAscendingNewViewPartialLastPagePartWayInEachDirection
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationAscendingNewMVKeyViewPartialLastPagePartWayInEachDirection() {
        new CheckPaginationWithMultiValueKey()
                .descending(false)
                .useNewView(true)
                .docCount(28)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .runTest();
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a newfuture {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @Test
    public void paginationDescendingNewViewPartialLastPagePartWayInEachDirection() {
        checkPagination(true, 28, 5, null, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Run the same check as paginationDescendingNewViewPartialLastPagePartWayInEachDirection
     * test on views using an array of multi-value keys.
     */
    @Test
    public void paginationDescendingNewMVKeyViewPartialLastPagePartWayInEachDirection() {
        new CheckPaginationWithMultiValueKey()
                .descending(true)
                .useNewView(true)
                .docCount(28)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .runTest();
    }

    /**
     * This helper function uses the given {@code view} to query for a page of
     * results. The {@code param} indicates whether we should be getting a next or previous
     * page. If {@code view} is null, a new {@link View} is created to perform the query,
     * otherwise the given {@code view} is used.
     *
     * @param expectedPageNumber the page number of the page we expect to be returned.
     * @param param              the request parameter to use to query a page, or {@code null} to
     *                           return the first page.
     * @param descending         true if the view should be created in descending order, and false
     *                           otherwise.
     * @param docCount           the total number of documents in the view.
     * @param docsPerPage        the maximum number of documents per page in the view.
     * @param view               the {@link View} object to use to perform the query, or {@code
     *                           null} to create a new {@link View}
     * @return the page of results.
     */
    private static Page queryAndCheckPage(int expectedPageNumber, String param, boolean
            descending, int docCount, int docsPerPage, View view) {
        View queryView = view;
        if (queryView == null) {
            queryView = db.view("example/foo")
                    .reduce(false)
                    .descending(descending);
        }
        Page page = queryView.queryPage(docsPerPage, param, Foo.class);
        checkPage(page, docCount, docsPerPage, expectedPageNumber, descending);
        return page;
    }

    /**
     * Helper to test paging the view with multiple changes of direction.
     * <p/>
     * Starting from page 1, page through to each page identified by the page numbers in
     * {@code pageToPages}, checking all the intervening pages as well as the pages specified
     * in {@code pageToPages} are as expected.
     * <p/>
     * For example, if {@code pageToPages} contains {@code 4, 2, 5}, this will check pages in the
     * following order: 1, 2, 3, 4, 3, 2, 3, 4, 5.
     *
     * @param descending  true if the view is in descending order, and false otherwise.
     * @param docCount    the total number of documents in the view.
     * @param docsPerPage the maximum number of documents per page in the view.
     * @param view        the {@link View} object to use to perform the query (or null to create a
     *                    new
     *                    view for the query).
     * @param pageToPages the list of page numbers to page to.
     */
    private void checkPagination(boolean descending, int docCount, int docsPerPage, View view,
                                 int... pageToPages) {

        for (int i = 0; i < docCount; ++i) {
            Foo foo = new Foo(generateUUID(), docTitle(i + 1));
            db.save(foo);
        }

        // Get the first page of results.
        Page page = queryAndCheckPage(1, null, descending, docCount, docsPerPage, view);

        int currentPage = 1;
        for (int i = 0; i < pageToPages.length; ++i) {
            if (pageToPages[i] > currentPage) {
                page = checkPagesForward(currentPage, pageToPages[i] - currentPage, page,
                        descending, docCount, docsPerPage, view);
            } else {
                page = checkPagesBackward(currentPage, currentPage - pageToPages[i], page,
                        descending, docCount, docsPerPage, view);
            }
            currentPage = pageToPages[i];
        }
    }

    /**
     * Check all the pages going forwards until we reach the last page. This assumes the given
     * {@code page} is the first page of results.
     *
     * @param currentPage   the page number of the {@code page}.
     * @param numberOfPages the number of pages to page forwards.
     * @param page          the page from which to start paging forwards.
     * @param descending    true if the view is in descending order, and false otherwise.
     * @param docCount      the total number of documents in the view.
     * @param docsPerPage   the maximum number of documents per page.
     * @param view          the {@link View} object to use to perform the query (or null to create
     *                      a new
     *                      view for the query).
     * @return the last page in the view.
     */
    private Page checkPagesForward(int currentPage, int numberOfPages, Page page, boolean
            descending, int docCount, int docsPerPage, View view) {
        for (int i = 0; i < numberOfPages; ++i) {
            page = queryAndCheckPage(currentPage + i + 1, page.getNextParam(), descending,
                    docCount, docsPerPage, view);
        }
        return page;
    }

    /**
     * Check all the pages going backwards until we reach the first page. This assumes the
     * given {@code page} is the last page of results.
     *
     * @param currentPage   the page number of the {@code page}.
     * @param numberOfPages the number of pages to page backwards.
     * @param page          the page from which to start paging backwards.
     * @param descending    true if the view is in descending order, and false otherwise.
     * @param docCount      the total number of documents in the view.
     * @param docsPerPage   the maximum number of documents per page.
     * @param view          the {@link View} object to use to perform the query (or null to create
     *                      a new
     *                      view for the query).
     * @return the first page in the view
     */
    private Page checkPagesBackward(int currentPage, int numberOfPages, Page page, boolean
            descending, int docCount, int docsPerPage, View view) {
        for (int i = 0; i < numberOfPages; ++i) {
            page = queryAndCheckPage(currentPage - i - 1, page.getPreviousParam(), descending,
                    docCount, docsPerPage, view);
        }
        return page;
    }

    private static void init() {
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

    private static JsonObject multiValueKeyInit(Foo foo, int i) {
        //JSON object for multi value key array tests
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("creator", "uuid");
        jsonObject.addProperty("created", 1000 + i);
        jsonObject.addProperty("boolean", (i != 0));
        jsonObject.addProperty("total", 10.01 + i);
        jsonObject.addProperty("quotes", "\"quotes\" " + String.valueOf(i));
        jsonObject.addProperty("spaces", " spaces " + String.valueOf(i));
        jsonObject.addProperty("letters", (char) ('a' + i) + "bc");

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(jsonObject);

        if(foo != null) {
            foo.setContentArray(jsonArray);
        }

        return jsonObject;
    }

    private static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Helper method to assert that keys and values in two JSON objects are equal.
     * @param expectedJson the expected JSON element(s)
     * @param actualJson the actual JSON element(s) from test result list
     */
    private void assertJsonObjectKeysAndValues(ArrayList<JsonObject> expectedJson,
                                         ArrayList<JsonObject> actualJson) {

        assertEquals(expectedJson.size(),
                actualJson.size());

        for(int i = 0; i < actualJson.size(); i++) {
            JsonObject expectedJsonObject = expectedJson.get(i).getAsJsonObject();
            JsonObject actualJsonObject = actualJson.get(i).getAsJsonObject();

            Iterator<Map.Entry<String, JsonElement>> actualJsonIter =
                    actualJsonObject.entrySet().iterator();

            for (Map.Entry<String, JsonElement> expectedJsonMap :
                    expectedJsonObject.entrySet()) {
                String expectedJsonKey = expectedJsonMap.getKey();
                JsonElement expectedJsonValue = expectedJsonMap.getValue();

                if(actualJsonIter.hasNext()) {
                    Map.Entry<String, JsonElement> actualJsonMap = actualJsonIter.next();
                    assertEquals(expectedJsonKey, actualJsonMap.getKey());
                    assertEquals(expectedJsonValue, actualJsonMap.getValue());
                }
            }
        }
    }

    /**
     * Helper method to assert that keys and values in two JSON arrays are equal.
     * @param expectedJson the expected JSON element(s)
     * @param actualJson the actual JSON element(s) from test result list
     */
    private void assertJsonArrayKeysAndValues(ArrayList<JsonArray> expectedJson,
                                               ArrayList<JsonArray> actualJson) {
        assertEquals(expectedJson.size(), actualJson.size());

        for(int i = 0; i < actualJson.size(); i++) {
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
     * Helper class for use with multi-value key view tests.
     */
    private class CheckPaginationWithMultiValueKey {
        /**
         * True if the view is in descending order, and false otherwise.
         */
        private boolean descending;
        /**
         * True for creating a new view for the query, and false otherwise.
         */
        private boolean useNewView;
        /**
         * The total number of documents in the view.
         */
        private int docCount;
        /**
         * The maximum number of documents per page.
         */
        private int docsPerPage;
        /**
         * The list of page numbers to page to.
         */
        private int[] pageToPages;

        public CheckPaginationWithMultiValueKey descending(boolean descending) {
            this.descending = descending;
            return this;
        }

        public CheckPaginationWithMultiValueKey useNewView(boolean useNewView) {
            this.useNewView = useNewView;
            return this;
        }

        public CheckPaginationWithMultiValueKey docCount(int docCount) {
            this.docCount = docCount;
            return this;
        }

        public CheckPaginationWithMultiValueKey docsPerPage(int docsPerPage) {
            this.docsPerPage = docsPerPage;
            return this;
        }

        public CheckPaginationWithMultiValueKey pageToPages(int... pageToPages) {
            this.pageToPages = pageToPages;
            return this;
        }

        /**
         * For use with multi-value key view tests.
         *
         * This helper function uses the given {@code view} to query for a page of
         * results. The {@code param} indicates whether we should be getting a next or previous
         * page. If {@code view} is null, a new {@link View} is created to perform the query,
         * otherwise the given {@code view} is used.
         *
         * @param expectedPageNumber the page number of the page we expect to be returned.
         * @param param              the request parameter to use to query a page, or {@code
         * null} to
         *                           return the first page.
         * @param descending         true if the view should be created in descending order, and
         *                           false
         *                           otherwise.
         * @param docCount           the total number of documents in the view.
         * @param docsPerPage        the maximum number of documents per page in the view.
         * @param view               the {@link View} object to use to perform the query, or {@code
         *                           null} to create a new {@link View}
         * @param viewCount          the current view in the array of multi-value key views.
         *                           This value is used when view parameter is null.
         * @return the page of results.
         */
        private Page queryAndCheckPageWithMultiValueKey(int expectedPageNumber, String param,
                                                        boolean
                descending, int docCount, int docsPerPage, View view, int viewCount) {
            View queryView = view;
            if (queryView == null) {
                queryView = db.view(testViews[viewCount])
                        .reduce(false)
                        .descending(descending);
            }
            Page page = queryView.queryPage(docsPerPage, param, Foo.class);
            checkPage(page, docCount, docsPerPage, expectedPageNumber, descending);
            return page;
        }

        /**
         * For use with multi-value key view tests.
         *
         * Check all the pages going forwards until we reach the last page. This assumes the given
         * {@code page} is the first page of results.
         *
         * @param currentPage   the page number of the {@code page}.
         * @param numberOfPages the number of pages to page forwards.
         * @param page          the page from which to start paging forwards.
         * @param descending    true if the view is in descending order, and false otherwise.
         * @param docCount      the total number of documents in the view.
         * @param docsPerPage   the maximum number of documents per page.
         * @param view          the {@link View} object to use to perform the query (or null to
         *                      create
         *                      a new
         *                      view for the query).
         * @param viewCount     the current view in the array of multi-value key views. This
         *                      value is
         *                      used when view parameter is null.
         * @return the last page in the view.
         */
        private Page checkPagesForwardMultiValueKey(int currentPage, int numberOfPages, Page
                page, boolean
                descending, int docCount, int docsPerPage, View view, int viewCount) {
            for (int i = 0; i < numberOfPages; ++i) {
                page = queryAndCheckPageWithMultiValueKey(currentPage + i + 1, page.getNextParam(),
                        descending,
                        docCount, docsPerPage, view, viewCount);
            }
            return page;
        }

        /**
         * For use with multi-value key view tests.
         *
         * Check all the pages going backwards until we reach the first page. This assumes the
         * given {@code page} is the last page of results.
         *
         * @param currentPage   the page number of the {@code page}.
         * @param numberOfPages the number of pages to page backwards.
         * @param page          the page from which to start paging backwards.
         * @param descending    true if the view is in descending order, and false otherwise.
         * @param docCount      the total number of documents in the view.
         * @param docsPerPage   the maximum number of documents per page.
         * @param view          the {@link View} object to use to perform the query (or null to
         *                      create
         *                      a new
         *                      view for the query).
         * @param viewCount     the current view in the array of multi-value key views. This
         *                      value is
         *                      used when view parameter is null.
         * @return the first page in the view
         */
        private Page checkPagesBackwardMultiValueKey(int currentPage, int numberOfPages, Page
                page, boolean
                descending, int docCount, int docsPerPage, View view, int viewCount) {
            for (int i = 0; i < numberOfPages; ++i) {
                page = queryAndCheckPageWithMultiValueKey(currentPage - i - 1, page
                                .getPreviousParam(), descending,
                        docCount, docsPerPage, view, viewCount);
            }
            return page;
        }


        /**
         * Run tests for paging the view with multiple changes of direction. Uses a pre-defined
         * array of queries for multi-value key testing.
         *
         * <p/>
         * Starting from page 1, page through to each page identified by the page numbers in
         * {@code pageToPages}, checking all the intervening pages as well as the pages specified
         * in {@code pageToPages} are as expected.
         * <p/>
         * For example, if {@code pageToPages} contains {@code 4, 2, 5}, this will check pages in
         * the
         * following order: 1, 2, 3, 4, 3, 2, 3, 4, 5.
         */
        public void runTest() {
            for (int i = 0; i < docCount; ++i) {
                Foo foo = new Foo(generateUUID(), docTitle(i + 1));
                multiValueKeyInit(foo, i);
                db.save(foo);
            }

            // Run all views
            for (int viewCount = 0; viewCount < testViews.length; viewCount++) {
                View view = null;
                if (!useNewView) {
                    view = db.view(testViews[viewCount])
                            .reduce(false)
                            .descending(descending);
                }

                // Get the first page of results.
                Page page = queryAndCheckPageWithMultiValueKey(1, null, descending, docCount,
                        docsPerPage, view, viewCount);

                int currentPage = 1;
                for (int i = 0; i < pageToPages.length; ++i) {
                    if (pageToPages[i] > currentPage) {
                        page = checkPagesForwardMultiValueKey(currentPage, pageToPages[i] -
                                        currentPage, page,
                                descending, docCount, docsPerPage, view, viewCount);
                    } else {
                        page = checkPagesBackwardMultiValueKey(currentPage, currentPage -
                                        pageToPages[i], page,
                                descending, docCount, docsPerPage, view, viewCount);
                    }
                    currentPage = pageToPages[i];
                }
            }
        }
    }
}
