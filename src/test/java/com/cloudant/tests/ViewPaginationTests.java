/*
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

import static org.junit.Assert.assertEquals;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.tests.util.CheckPagination;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.Utils;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class ViewPaginationTests {

    /**
     * Parameters for these tests so we run each test multiple times.
     * We run with a single key or a complex key and both ascending and descending.
     */
    @Parameterized.Parameters(name = "Key:{0},Descending:{1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {CheckPagination.Type.SINGLE, true},
                {CheckPagination.Type.SINGLE, false},
                {CheckPagination.Type.COMPLEX, false},
                {CheckPagination.Type.COMPLEX, true},
        });
    }

    @Parameterized.Parameter
    public CheckPagination.Type type;

    @Parameterized.Parameter(value = 1)
    public boolean descending;


    @ClassRule
    public static CloudantClientResource clientResource = new CloudantClientResource();
    @Rule
    public DatabaseResource dbResource = new DatabaseResource(clientResource);

    private Database db;

    @Before
    public void setUp() throws Exception {
        db = dbResource.get();
        Utils.putDesignDocs(db);
    }

    /**
     * Check that we can page through a view where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect.
     *
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     */
    @Test
    public void allTheWayInEachDirectionTwice() throws Exception {
        CheckPagination.newTest(type)
                .descending(descending)
                .docCount(6)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .runTest(db);
    }

    /**
     * Check that we can page through a view where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect.
     *
     * Page forward to the last page, back to the first page, forward to the last page and back to
     * the first page.
     */
    @Test
    public void partialLastPageAllTheWayInEachDirectionTwice() throws Exception {
        CheckPagination.newTest(type)
                .descending(descending)
                .docCount(5)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .runTest(db);
    }

    /**
     * Check that we can page through a view where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect.
     *
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @Test
    public void partWayInEachDirection() throws Exception {
        CheckPagination.newTest(type)
                .descending(descending)
                .docCount(30)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .runTest(db);
    }

    /**
     * Check that we can page through a view where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect.
     *
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @Test
    public void partialLastPagePartWayInEachDirection() throws Exception {
        CheckPagination.newTest(type)
                .descending(descending)
                .docCount(28)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .runTest(db);
    }

    /**
     * Check that we can page through a view where we use start and end keys.
     * Assert that we don't exceed the limits of those keys.
     */
    @Test
    public void startAndEndKeyLimits() throws Exception {

        //use the CheckPagination to set-up for this test, but we need to do running differently
        //since this page is not just simple paging
        CheckPagination.newTest(type)
                .descending(descending)
                .docCount(20)
                .docsPerPage(4)
                .initTest(db);

        // set up the start and end keys based on the test type
        // if we are descending then the start and end keys need to be reversed
        // then run the test with the correct request and limits
        if (CheckPagination.Type.SINGLE.equals(type)) {
            String startKey = (descending) ? "011" : "003";
            String endKey = (descending) ? "003" : "011";
            ViewResponse<String, String> page = db.getViewRequestBuilder("example", "foo")
                    .newPaginatedRequest(Key.Type.STRING, String.class).reduce(false).descending
                            (descending).rowsPerPage(4).startKey(startKey).endKey(endKey).build()
                    .getResponse();
            runStartAndEndKeyLimits(page, startKey, endKey);
        } else {
            //for multi we will use the creator_created view
            Key.ComplexKey startKey = (descending) ? Key.complex("uuid").add(1011) : Key.complex
                    ("uuid").add(1003);
            Key.ComplexKey endKey = (descending) ? Key.complex("uuid").add(1003) : Key.complex
                    ("uuid").add(1011);
            ViewResponse<Key.ComplexKey, String> page = db.getViewRequestBuilder("example",
                    "creator_created")
                    .newPaginatedRequest(Key.Type.COMPLEX, String.class).reduce(false).descending
                            (descending).rowsPerPage(4).startKey(startKey).endKey(endKey).build()
                    .getResponse();
            runStartAndEndKeyLimits(page, startKey, endKey);
        }
    }

    private <T> void runStartAndEndKeyLimits(ViewResponse<T, String> page, T startKey, T endKey)
            throws Exception {

        //check the start key is as expected
        assertEquals("The start key should be " + startKey, startKey, page.getKeys().get(0));

        //get the last page
        while (page.hasNextPage()) {
            page = page.nextPage();
        }
        //check the end key is as expected
        assertEquals("The end key should be " + endKey, endKey, page.getKeys().get(page.getKeys()
                .size()
                - 1));

        //now page backwards and ensure the last key we get is the start key
        while (page.hasPreviousPage()) {
            page = page.previousPage();
        }
        //check the start key is as expected
        assertEquals("The start key should be " + startKey, startKey, page.getKeys().get(0));
    }
}
