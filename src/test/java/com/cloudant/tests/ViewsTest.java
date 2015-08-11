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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.lightcouch.NoDocumentException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Category(RequiresDB.class)
public class ViewsTest {


    private static Database db;
    private CloudantClient account;
    private static String[] testViews = {"example/doc_title", "example/creator_created",
            "example/creator_boolean_total"};

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
     * in query and queryView will produce a result list of
     * all false docs.
     */

    @Test
    public void byStartAndEndBooleanKey() {
        init();

        View query = db.view("example/boolean")
                .startKey(false)
                .endKey(false);

        List<Object> result = query.query(Object.class);

        assertThat(result.size(), is(3));

        Page page = null;
        try {
            page = query.queryPage(30, null, Object.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertThat(page.getResultList().size(), is(3));
    }

    /**
     * Assert that passing a string key with double quotes
     * or spaces in query and queryView will produce
     * a result list without exception.
     */
    @Test
    public void byStartAndEndStringKey() {
        init();

        //Query for testing string with spaces
        View query = db.view("example/spaces_created")
                .startKey(" spaces ", 1)
                .endKey(" spaces 2", 2000);

        List<Object> result = query.query(Object.class);

        assertThat(result.size(), is(3));

        Page page = null;
        try {
            page = query.queryPage(30, null, Object.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertThat(page.getResultList().size(), is(3));

        //Query for testing string with quotes
        query = db.view("example/quotes_created")
                .startKey("\"quotes\" ", 1)
                .endKey("\"quotes\" 3", 2000);

        result = query.query(Object.class);

        assertThat(result.size(), is(3));

        page = null;
        try {
            page = query.queryPage(30, null, Object.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertThat(page.getResultList().size(), is(3));
    }

    /**
     * Assert that passing a single integer value key expression
     * in query and queryView will produce a result list of all docs.
     *
     * Run the same assert above but with a key that is an object array
     * containing an integer.
     */
    @Test
    public void byStartAndEndIntKeyArray() {
        init();

        View query = db.view("example/created")
                .startKey(1)
                .endKey(2000);

        List<Object> result = query.query(Object.class);

        assertThat(result.size(), is(3));

        Page page = null;
        try {
            page = query.queryPage(30, null, Object.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertThat(page.getResultList().size(), is(3));

        query = db.view("example/created")
                .startKey(new Object[]{1})
                .endKey(new Object[]{2000});


        result = query.query(Object.class);

        assertThat(result.size(), is(3));
    }

    /**
     * Assert that passing a key with two values in query
     * and queryView will produce a result list of all docs.
     *
     * Run the same assert above but with a key that is an
     * object array containing only integers.
     */
    @Test
    public void byStartAndEndTwoKeyArray() {
        init();

        View query = db.view("example/creator_created")
                .startKey("uuid", 1)
                .endKey("uuid", 2000);

        List<Object> result = query.query(Object.class);

        assertThat(result.size(), is(3));

        Page page = null;
        try {
            page = query.queryPage(30, null, Object.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertThat(page.getResultList().size(), is(3));


        query = db.view("example/creator_created")
                .startKey(new Object[]{"uuid", 1})
                .endKey(new Object[]{"uuid", 2000});


        result = query.query(Object.class);

        assertThat(result.size(), is(3));

        //Key array with only integers
        query = db.view("example/created_total")
                .startKey(1, 12.00)
                .endKey(2000, 15.00);


        result = query.query(Object.class);

        assertThat(result.size(), is(3));

        page = null;
        try {
            page = query.queryPage(30, null, Object.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertThat(page.getResultList().size(), is(3));
    }

    /**
     * Assert that passing a key with three different values in
     * query and queryView will produce a result list of all docs.
     *
     * Run the same assert above but with a key that is an object array
     * and should only return a result with 'true' as the first value.
     */
    @Test
    public void byStartAndEndThreeKeyArray() {
        init();

        View query = db.view("example/boolean_creator_created")
                .startKey(false, "uuid", 1)
                .endKey(false, "uuid", 2000);

        List<Object> result = query.query(Object.class);

        assertThat(result.size(), is(3));

        query = db.view("example/created_boolean_creator")
                .startKey(1000, false, "uuid")
                .endKey(1002, false, "uuid");

        result = query.query(Object.class);

        assertThat(result.size(), is(3));

        Page page = null;
        try {
            page = query.queryPage(30, null, Object.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertThat(page.getResultList().size(), is(3));


        query = db.view("example/boolean_creator_created")
                .startKey(new Object[]{false, "uuid", 1})
                .endKey(new Object[]{true, "uuid", 2000});

        result = query.query(Object.class);

        assertThat(result.size(), is(3));
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
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationAscendingReusingViewAllTheWayInEachDirectionTwice() {
        View view = db.view("example/foo")
                .reduce(false);
        checkPagination(false, 6, 2, view, 3, 1, 3, 1);
    }

    @Test
    public void paginationAscendingReusingMVKeyViewAllTheWayInEachDirectionTwice() {
        checkPaginationWithMultiValueKey(false, 6, 2, 3, 1, 3, 1);
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationDescendingReusingViewAllTheWayInEachDirectionTwice() {
        View view = db.view("example/foo")
                .reduce(false)
                .descending(true);
        checkPagination(true, 6, 2, view, 3, 1, 3, 1);
    }

    @Test
    public void paginationDescendingReusingMVKeyViewAllTheWayInEachDirectionTwice() {
        checkPaginationWithMultiValueKey(true, 6, 2, 3, 1, 3, 1);
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationAscendingReusingViewPartialLastPageAllTheWayInEachDirectionTwice() {
        View view = db.view("example/foo")
                .reduce(false);
        checkPagination(false, 5, 2, view, 3, 1, 3, 1);
    }

    @Test
    public void paginationAscendingReusingMVKeyViewPartialLastPageAllTheWayInEachDirectionTwice() {
        checkPaginationWithMultiValueKey(false, 5, 2, 3, 1, 3, 1);
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

    @Test
    public void paginationDescendingReusingMVKeyViewPartialLastPageAllTheWayInEachDirectionTwice() {
        checkPaginationWithMultiValueKey(true, 5, 2, 3, 1, 3, 1);
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationAscendingNewViewAllTheWayInEachDirectionTwice() {
        checkPagination(false, 6, 2, null, 3, 1, 3, 1);
    }

    @Test
    public void paginationAscendingNewMVKeyViewAllTheWayInEachDirectionTwice() {
        checkPaginationWithMultiValueKey(false, true, 6, 2, 3, 1, 3, 1);
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationDescendingNewViewAllTheWayInEachDirectionTwice() {
        checkPagination(true, 6, 2, null, 3, 1, 3, 1);
    }

    @Test
    public void paginationDescendingNewMVKeyViewAllTheWayInEachDirectionTwice() {
        checkPaginationWithMultiValueKey(true, true, 6, 2, 3, 1, 3, 1);
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationAscendingNewViewPartialLastPageAllTheWayInEachDirectionTwice() {
        checkPagination(false, 5, 2, null, 3, 1, 3, 1);
    }

    @Test
    public void paginationAscendingNewMVKeyViewPartialLastPageAllTheWayInEachDirectionTwice() {
        checkPaginationWithMultiValueKey(false, 5, 2, 3, 1, 3, 1);
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a newfuture {@link View} instance for each page query.
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationDescendingNewViewPartialLastPageAllTheWayInEachDirectionTwice() {
        checkPagination(true, 5, 2, null, 3, 1, 3, 1);
    }

    @Test
    public void paginationDescendingNewMVKeyViewPartialLastPageAllTheWayInEachDirectionTwice() {
        checkPaginationWithMultiValueKey(true, 5, 2, 3, 1, 3, 1);
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationAscendingReusingViewPartWayInEachDirection() {
        View view = db.view("example/foo")
                .reduce(false);
        checkPagination(false, 30, 5, view, 4, 2, 5, 3, 4, 2, 6);
    }

    @Test
    public void paginationAscendingReusingMVKeyViewPartWayInEachDirection() {
        checkPaginationWithMultiValueKey(false, 30, 5, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationDescendingReusingViewPartWayInEachDirection() {
        View view = db.view("example/foo")
                .reduce(false)
                .descending(true);
        checkPagination(true, 30, 5, view, 4, 2, 5, 3, 4, 2, 6);
    }

    @Test
    public void paginationDescendingReusingMVKeyViewPartWayInEachDirection() {
        checkPaginationWithMultiValueKey(true, 30, 5, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationAscendingReusingViewPartialLastPagePartWayInEachDirection() {
        View view = db.view("example/foo")
                .reduce(false);
        checkPagination(false, 28, 5, view, 4, 2, 5, 3, 4, 2, 6);
    }

    @Test
    public void paginationAscendingReusingMVKeyViewPartialLastPagePartWayInEachDirection() {
        checkPaginationWithMultiValueKey(false, 28, 5, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses the same {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationDescendingReusingViewPartialLastPagePartWayInEachDirection() {
        View view = db.view("example/foo")
                .reduce(false)
                .descending(true);
        checkPagination(true, 28, 5, view, 4, 2, 5, 3, 4, 2, 6);
    }

    @Test
    public void paginationDescendingReusingMVKeyViewPartialLastPagePartWayInEachDirection() {
        checkPaginationWithMultiValueKey(true, 28, 5, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationAscendingNewViewPartWayInEachDirection() {
        checkPagination(false, 30, 5, null, 4, 2, 5, 3, 4, 2, 6);
    }

    @Test
    public void paginationAscendingNewMVKeyViewPartWayInEachDirection() {
        checkPaginationWithMultiValueKey(false, 30, 5, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationDescendingNewViewPartWayInEachDirection() {
        checkPagination(true, 30, 5, null, 4, 2, 5, 3, 4, 2, 6);
    }

    @Test
    public void paginationDescendingNewMVKeyViewPartWayInEachDirection() {
        checkPaginationWithMultiValueKey(true, 30, 5, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Check that we can page through a view in ascending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a new {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationAscendingNewViewPartialLastPagePartWayInEachDirection() {
        checkPagination(false, 28, 5, null, 4, 2, 5, 3, 4, 2, 6);
    }

    @Test
    public void paginationAscendingNewMVKeyViewPartialLastPagePartWayInEachDirection() {
        checkPaginationWithMultiValueKey(false, 28, 5, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * Check that we can page through a view in descending order where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect. This test uses a newfuture {@link View} instance for each page query.
     * Page part way forward, and part way back a few times before paging to the last page.
     *
     * Run the same check above on an array of multi-value key views.
     */
    @Test
    public void paginationDescendingNewViewPartialLastPagePartWayInEachDirection() {
        checkPagination(true, 28, 5, null, 4, 2, 5, 3, 4, 2, 6);
    }

    @Test
    public void paginationDescendingNewMVKeyViewPartialLastPagePartWayInEachDirection() {
        checkPaginationWithMultiValueKey(true, 28, 5, 4, 2, 5, 3, 4, 2, 6);
    }

    /**
     * This helper function uses the given {@code view} to query for a page of
     * results. The {@code param} indicates whether we should be getting a next or previous
     * page. If {@code view} is null, a new {@link View} is created to perform the query,
     * otherwise the given {@code view} is used.
     *
     * @param expectedPageNumber the page number of the page we expect to be returned.
     * @param param              the request parameter to use to query a page, or {@code null} to
     *                              return the first page.
     * @param descending         true if the view should be created in descending order, and false
     *                              otherwise.
     * @param docCount           the total number of documents in the view.
     * @param docsPerPage        the maximum number of documents per page in the view.
     * @param view               the {@link View} object to use to perform the query, or {@code
     * null} to create a new {@link View}
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
     * For use with multi-value key view tests.
     *
     * This helper function uses the given {@code view} to query for a page of
     * results. The {@code param} indicates whether we should be getting a next or previous
     * page. If {@code view} is null, a new {@link View} is created to perform the query,
     * otherwise the given {@code view} is used.
     *
     * @param expectedPageNumber the page number of the page we expect to be returned.
     * @param param              the request parameter to use to query a page, or {@code null} to
     *                              return the first page.
     * @param descending         true if the view should be created in descending order, and false
     *                              otherwise.
     * @param docCount           the total number of documents in the view.
     * @param docsPerPage        the maximum number of documents per page in the view.
     * @param view               the {@link View} object to use to perform the query, or {@code
     *                              null} to create a new {@link View}
     * @param viewCount          the current view in the array of multi-value key views.
     *                              This value is used when view parameter is null.
     * @return the page of results.
     */
    private static Page queryAndCheckPageWithMultiValueKey(int expectedPageNumber, String param, boolean
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
     *                       new
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
     * For use with multi-value key view tests.
     *
     * Helper to test paging the view with multiple changes of direction. Uses a pre-defined
     * array of queries for multi-value key testing.
     *
     * @param descending  true if the view is in descending order, and false otherwise.
     * @param docCount    the total number of documents in the view.
     * @param docsPerPage the maximum number of documents per page in the view.
     * @param useNewView  true for creating a new view for the query, and false otherwise.
     * @param pageToPages the list of page numbers to page to.
     */
    private void checkPaginationWithMultiValueKey(boolean descending, boolean useNewView,
                                                  int docCount, int docsPerPage,
                                                  int... pageToPages) {
        for (int i = 0; i < docCount; ++i) {
            Foo foo = new Foo(generateUUID(), docTitle(i + 1));
            multiValueKeyInit(foo, i);
            db.save(foo);
        }

        // Run all views
        for(int viewCount = 0; viewCount < testViews.length; viewCount++) {
            View view = null;
            if(!useNewView) {
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

    private void checkPaginationWithMultiValueKey(boolean descending, int docCount, int docsPerPage,
                                                  int... pageToPages) {

        checkPaginationWithMultiValueKey(descending, false, docCount, docsPerPage, pageToPages);
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
     *                         a new
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
     * @param view          the {@link View} object to use to perform the query (or null to create
     *                         a new
     *                      view for the query).
     * @param viewCount     the current view in the array of multi-value key views. This value is
     *                      used when view parameter is null.
     * @return the last page in the view.
     */
    private Page checkPagesForwardMultiValueKey(int currentPage, int numberOfPages, Page page, boolean
            descending, int docCount, int docsPerPage, View view, int viewCount) {
        for (int i = 0; i < numberOfPages; ++i) {
            page = queryAndCheckPageWithMultiValueKey(currentPage + i + 1, page.getNextParam(),
                    descending,
                    docCount, docsPerPage, view, viewCount);
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
     *                         a new
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
     * @param view          the {@link View} object to use to perform the query (or null to create
     *                         a new
     *                      view for the query).
     * @param viewCount     the current view in the array of multi-value key views. This value is
     *                      used when view parameter is null.
     * @return the first page in the view
     */
    private Page checkPagesBackwardMultiValueKey(int currentPage, int numberOfPages, Page page, boolean
            descending, int docCount, int docsPerPage, View view, int viewCount) {
        for (int i = 0; i < numberOfPages; ++i) {
            page = queryAndCheckPageWithMultiValueKey(currentPage - i - 1, page.getPreviousParam(), descending,
                    docCount, docsPerPage, view, viewCount);
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

    private static void multiValueKeyInit(Foo foo, int i) {
        //JSON object for multi value key array tests
        JsonObject json = new JsonObject();
        json.addProperty("creator", "uuid");
        json.addProperty("created", 1000 + i);
        json.addProperty("boolean", false);
        json.addProperty("total", 10.01 + (i / 100));
        json.addProperty("quotes", "\"quotes\" " + String.valueOf(i));
        json.addProperty("spaces", " spaces " + String.valueOf(i));
        json.addProperty("letters", (char) ('a' + i) + "bc");

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(json);

        foo.setContentArray(jsonArray);
    }

    private static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
