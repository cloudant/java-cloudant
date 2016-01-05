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

package com.cloudant.tests.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.tests.Foo;
import com.cloudant.tests.ViewsTest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.List;

/**
 * Helper class for use with multi-value key view tests.
 */
public class CheckPagination {

    public enum Type {
        SINGLE,
        COMPLEX
    }

    public static CheckPagination newTest() {
        return CheckPagination.newTest(Type.SINGLE);
    }

    public static CheckPagination newTest(Type type) {
        switch (type) {
            case COMPLEX:
                return new CheckPaginationWithMultiValueKey();
            case SINGLE:
            default:
                return new CheckPagination();
        }
    }

    //default view is example/foo
    protected String[] testViews = new String[]{"example/foo"};
    protected Key.Type<?> viewKeyType = Key.Type.STRING;

    /**
     * True if the view is in descending order, and false otherwise.
     */
    private boolean descending;

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

    private boolean stateless;

    public CheckPagination descending(boolean descending) {
        this.descending = descending;
        return this;
    }

    public CheckPagination docCount(int docCount) {
        this.docCount = docCount;
        return this;
    }

    public CheckPagination docsPerPage(int docsPerPage) {
        this.docsPerPage = docsPerPage;
        return this;
    }

    public CheckPagination pageToPages(int... pageToPages) {
        this.pageToPages = pageToPages;
        return this;
    }

    public CheckPagination stateless(boolean stateless) {
        this.stateless = stateless;
        return this;
    }

    /**
     * Check all the pages going forwards until we reach the last page. This assumes the given
     * {@code page} is the first page of results.
     *
     * @param currentPage   the page number of the {@code page}.
     * @param numberOfPages the number of pages to page forwards.
     * @param docCount      the total number of documents in the view.
     * @param docsPerPage   the maximum number of documents per page.
     * @return the last page in the view.
     */
    private void checkPagesForward(int currentPage,
                                           int numberOfPages,
                                           int docCount,
                                           int docsPerPage) throws IOException {
        for (int i = 0; i < numberOfPages; ++i) {
            nextPage();
            checkPage(page, docCount, docsPerPage, currentPage + i + 1, descending);
        }
    }

    /**
     * Check each page going backwards for the specified number of pages.
     *
     * @param currentPage   the page number of the {@code page}.
     * @param numberOfPages the number of pages to page backwards.
     * @param docCount      the total number of documents in the view.
     * @param docsPerPage   the maximum number of documents per page.
     * @return the first page in the view
     */
    private void checkPagesBackward(int currentPage,
                                            int numberOfPages,
                                            int docCount,
                                            int docsPerPage) throws IOException {
        for (int i = 0; i < numberOfPages; ++i) {
            previousPage();
            checkPage(page, docCount, docsPerPage, currentPage - i - 1, descending);
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
    private void checkPage(ViewResponse page, int docCount, int docsPerPage, int pageNumber,
                           boolean descending) {
        if (pageNumber == 1) {
            assertFalse(page.hasPreviousPage());
        } else {
            assertTrue(page.hasPreviousPage());
        }

        double numberOfPages = docCount / (double) docsPerPage;
        if (pageNumber >= numberOfPages) {
            assertFalse(page.hasNextPage());
        } else {
            assertTrue(page.hasNextPage());
        }

        int startIndex = (pageNumber - 1) * docsPerPage + 1;
        assertThat(page.getFirstRowCount().intValue(), is(startIndex));
        assertThat(page.getLastRowCount().intValue(), is(Math.min(startIndex + docsPerPage - 1,
                docCount)));
        assertThat(page.getPageNumber().intValue(), is(pageNumber));
        if (page.hasNextPage() || docCount % docsPerPage == 0) {
            assertThat(page.getRows().size(), is(docsPerPage));
        } else {
            assertThat(page.getRows().size(), is(docCount % docsPerPage));
        }
        checkDocumentTitles(page, docCount, docsPerPage, pageNumber, descending);
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
    private void checkDocumentTitles(ViewResponse page, int docCount, int docsPerPage, int
            pageNumber, boolean descending) {
        int offset = (pageNumber - 1) * docsPerPage + 1;
        if (descending) {
            offset = docCount + 1 - offset;
        }
        List<Foo> resultList = page.getDocsAs(Foo.class);
        for (int i = 0; i < resultList.size(); ++i) {
            assertEquals("Document titles do not match", ViewsTest.docTitle(descending ? offset-- :
                    offset++), resultList.get(i).getTitle());
        }
    }

    protected void initFoo(Foo foo, int i) {
        //no-op for default Foo
    }

    public void initTest(Database db) throws Exception {
        for (int i = 0; i < docCount; ++i) {
            Foo foo = new Foo(Utils.generateUUID(), ViewsTest.docTitle(i + 1));
            initFoo(foo, i);
            db.save(foo);
        }
    }

    private ViewRequest view;
    private ViewResponse page;

    public void runTest(Database db) throws Exception {

        //initTest
        initTest(db);

        // Run all views
        for (int viewCount = 0; viewCount < testViews.length; viewCount++) {
            view = db.getViewRequestBuilder(testViews[viewCount].split("/")[0],
                    testViews[viewCount].split("/")[1]).newPaginatedRequest(viewKeyType,
                    Object.class).rowsPerPage(docsPerPage).reduce(false).descending
                    (descending).includeDocs(true).build();

            // Get the first page of results.
            page = view.getResponse();
            checkPage(page, docCount, docsPerPage, 1, descending);

            int currentPage = 1;
            for (int i = 0; i < pageToPages.length; ++i) {
                if (pageToPages[i] > currentPage) {
                    checkPagesForward(currentPage, pageToPages[i] - currentPage, docCount,
                            docsPerPage);
                } else {
                    checkPagesBackward(currentPage, currentPage - pageToPages[i],
                            docCount, docsPerPage);
                }
                currentPage = pageToPages[i];
            }
        }
    }

    private void nextPage() throws IOException {
        if (stateless) {
            page = view.getResponse(page.getNextPageToken());
        } else {
            page = page.nextPage();
        }
    }

    private void previousPage() throws IOException {
        if (stateless) {
            page = view.getResponse(page.getPreviousPageToken());
        } else {
            page = page.previousPage();
        }
    }

    public static class CheckPaginationWithMultiValueKey extends CheckPagination {

        CheckPaginationWithMultiValueKey() {
            testViews = new String[]{"example/doc_title", "example/creator_created",
                    "example/creator_boolean_total", "example/created_boolean_creator"};
            viewKeyType = Key.Type.COMPLEX;
        }

        /**
         * Complex key tests have some content in the foo
         *
         * @param foo to add content to
         * @param i   doc count for data
         */
        @Override
        protected void initFoo(Foo foo, int i) {

            //JSON object for multi value key array tests
            JsonObject jsonObject = ViewsTest.multiValueKeyInit(foo, i);

            JsonArray jsonArray = new JsonArray();
            jsonArray.add(jsonObject);

            if (foo != null) {
                foo.setContentArray(jsonArray);
            }
        }
    }
}
