/*
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerTest;
import com.cloudant.tests.util.CheckPagination;
import com.cloudant.tests.util.Utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiresDB
@ExtendWith(ViewPaginationTests.ParameterProvider.class)
public class ViewPaginationTests extends TestWithDbPerTest {

    static class ParameterProvider implements TestTemplateInvocationContextProvider {
        @Override
        public boolean supportsTestTemplate(ExtensionContext context) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts
                (ExtensionContext context) {
            return StreamSupport.stream(data().spliterator(), false);
        }

        public static TestTemplateInvocationContext invocationContext(final CheckPagination.Type
                                                                              type,
                                                                      final boolean descending,
                                                                      final boolean stateless) {
            return new TestTemplateInvocationContext() {
                @Override
                public String getDisplayName(int invocationIndex) {
                    return String.format("Key:%s,Descending:%s,Stateless:%s", type,
                            descending, stateless);
                }

                @Override
                public List<Extension> getAdditionalExtensions() {
                    return Collections.<Extension>singletonList(new ParameterResolver() {
                        @Override
                        public boolean supportsParameter(ParameterContext parameterContext,
                                                         ExtensionContext extensionContext) {
                            switch (parameterContext.getIndex()) {
                                case 0:
                                    return parameterContext.getParameter().getType().equals
                                            (CheckPagination.Type.class);
                                case 1:
                                    return parameterContext.getParameter().getType().equals
                                            (boolean.class);
                                case 2:
                                    return parameterContext.getParameter().getType().equals
                                            (boolean.class);
                            }
                            return false;
                        }

                        @Override
                        public Object resolveParameter(ParameterContext parameterContext,
                                                       ExtensionContext extensionContext) {
                            switch (parameterContext.getIndex()) {
                                case 0:
                                    return type;
                                case 1:
                                    return descending;
                                case 2:
                                    return stateless;
                            }
                            return null;
                        }
                    });
                }
            };
        }
    }

    public static Iterable<TestTemplateInvocationContext> data() {

        List<TestTemplateInvocationContext> contexts = new
                ArrayList<TestTemplateInvocationContext>();
        boolean[] tf = new boolean[]{true, false};
        for (CheckPagination.Type type : CheckPagination.Type.values()) {
            for (boolean descending : tf) {
                for (boolean stateless : tf) {
                    contexts.add(ParameterProvider.invocationContext(type, descending, stateless));
                }
            }
        }
        return contexts;
    }

    @BeforeEach
    public void setUp() throws Exception {
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
    @TestTemplate
    public void allTheWayInEachDirectionTwice(CheckPagination.Type type,
                                              boolean descending,
                                              boolean stateless) throws Exception {
        CheckPagination.newTest(type)
                .descending(descending)
                .docCount(6)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .stateless(stateless)
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
    @TestTemplate
    public void partialLastPageAllTheWayInEachDirectionTwice(CheckPagination.Type type,
                                                             boolean descending,
                                                             boolean stateless) throws Exception {
        CheckPagination.newTest(type)
                .descending(descending)
                .docCount(5)
                .docsPerPage(2)
                .pageToPages(3, 1, 3, 1)
                .stateless(stateless)
                .runTest(db);
    }

    /**
     * Check that we can page through a view where the number of results
     * is an exact multiple of the number of pages. Check each page contains the documents
     * we expect.
     *
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @TestTemplate
    public void partWayInEachDirection(CheckPagination.Type type,
                                       boolean descending,
                                       boolean stateless) throws Exception {
        CheckPagination.newTest(type)
                .descending(descending)
                .docCount(30)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .stateless(stateless)
                .runTest(db);
    }

    /**
     * Check that we can page through a view where the number of results
     * is not an exact multiple of the number of pages. Check each page contains the documents
     * we expect.
     *
     * Page part way forward, and part way back a few times before paging to the last page.
     */
    @TestTemplate
    public void partialLastPagePartWayInEachDirection(CheckPagination.Type type,
                                                      boolean descending,
                                                      boolean stateless) throws Exception {
        CheckPagination.newTest(type)
                .descending(descending)
                .docCount(28)
                .docsPerPage(5)
                .pageToPages(4, 2, 5, 3, 4, 2, 6)
                .stateless(stateless)
                .runTest(db);
    }

    /**
     * Check that we can page through a view where we use start and end keys.
     * Assert that we don't exceed the limits of those keys.
     */
    @TestTemplate
    public void startAndEndKeyLimits(CheckPagination.Type type,
                                     boolean descending,
                                     boolean stateless) throws Exception {
        startAndEndKeyLimits(type, descending, stateless, true);
    }

    /**
     * Check that we can page through a view where we use start and end keys.
     * Assert that we don't exceed the limits of those keys.
     */
    @TestTemplate
    public void startAndEndKeyLimitsExclusiveEnd(CheckPagination.Type type,
                                                 boolean descending,
                                                 boolean stateless) throws Exception {
        startAndEndKeyLimits(type, descending, stateless, false);
    }

    private void startAndEndKeyLimits(CheckPagination.Type type,
                                      boolean descending,
                                      boolean stateless,
                                      boolean inclusiveEnd) throws Exception {
        //use the CheckPagination to set-up for this test, but we need to do running differently
        //since this page is not just simple paging
        CheckPagination cp = CheckPagination.newTest(type)
                .descending(descending)
                .docCount(20)
                .docsPerPage(4)
                .stateless(stateless);
        cp.initTest(db);

        // set up the start and end keys based on the test type
        // if we are descending then the start and end keys need to be reversed
        // if we are exclusive end then we need to change the expected end key
        // then run the test with the correct request and limits
        if (CheckPagination.Type.SINGLE.equals(type)) {
            String startKey = (descending) ? "011" : "003";
            String endKey = (descending) ? "003" : "011";
            String expectedEndKey = (inclusiveEnd) ? endKey : ((descending) ? "004" : "010");
            ViewRequest<String, String> request = db.getViewRequestBuilder("example", "foo")
                    .newPaginatedRequest(Key.Type.STRING, String.class).reduce(false).descending
                            (descending).inclusiveEnd(inclusiveEnd).rowsPerPage(4).startKey
                            (startKey).endKey(endKey).build();
            runStartAndEndKeyLimits(stateless, request, startKey, expectedEndKey);
        } else {
            //for multi we will use the creator_created view
            Key.ComplexKey startKey = (descending) ? Key.complex("uuid").add(1011) : Key.complex
                    ("uuid").add(1003);
            Key.ComplexKey endKey = (descending) ? Key.complex("uuid").add(1003) : Key.complex
                    ("uuid").add(1011);
            Key.ComplexKey expectedEndKey = (inclusiveEnd) ? endKey : ((descending) ? Key.complex
                    ("uuid").add(1004) : Key.complex("uuid").add(1010));
            ViewRequest<Key.ComplexKey, String> request = db.getViewRequestBuilder("example",
                    "creator_created")
                    .newPaginatedRequest(Key.Type.COMPLEX, String.class).reduce(false).descending
                            (descending).inclusiveEnd(inclusiveEnd).rowsPerPage(4).startKey
                            (startKey).endKey(endKey).build();
            runStartAndEndKeyLimits(stateless, request, startKey, expectedEndKey);
        }
    }

    private <T> void runStartAndEndKeyLimits(boolean stateless,
                                             ViewRequest<T, String> request,
                                             T expectedStartKey,
                                             T expectedEndKey)
            throws Exception {

        ViewResponse<T, String> page = request.getResponse();

        //check the start key is as expected
        assertEquals(expectedStartKey, page.getKeys().get(0), "The start key should be " +
                expectedStartKey);

        //get the last page
        while (page.hasNextPage()) {
            if (stateless) {
                page = request.getResponse(page.getNextPageToken());
            } else {
                page = page.nextPage();
            }
        }
        //check the end key is as expected
        assertEquals(expectedEndKey, page.getKeys().get(page.getKeys().size() - 1), "The end key " +
                "should be " + expectedEndKey);

        //now page backwards and ensure the last key we get is the start key
        while (page.hasPreviousPage()) {
            if (stateless) {
                page = request.getResponse(page.getPreviousPageToken());
            } else {
                page = page.previousPage();
            }
        }
        //check the start key is as expected
        assertEquals(expectedStartKey, page.getKeys().get(0), "The start key should be " +
                expectedStartKey);
    }
}
