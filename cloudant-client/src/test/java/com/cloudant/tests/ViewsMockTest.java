/*
 * Copyright (c) 2018 IBM Corp. All rights reserved.
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

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.SettableViewParameters;
import com.cloudant.client.api.views.UnpaginatedRequestBuilder;
import com.cloudant.tests.base.TestWithMockedServer;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ViewsMockTest extends TestWithMockedServer {


    /**
     * We can't test the server behaviour of stale, but we can test the URL values are what we
     * expect. This test uses the various values and checks the stale parameter in the URL, it makes
     * a request using getSingleValue() as it is easier to mock the responses.
     *
     * @throws Exception
     */
    @Test
    public void staleParameterValues() throws Exception {
        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(server)
            .build();
        Database database = client.database("notarealdb", false);
        for (String s : new String[]{
                SettableViewParameters.STALE_NO,
                SettableViewParameters.STALE_OK,
                SettableViewParameters.STALE_UPDATE_AFTER}) {
            UnpaginatedRequestBuilder<String, Integer> viewBuilder = database.getViewRequestBuilder
                    ("testDDoc", "testView").newRequest(Key.Type.STRING, Integer.class);
            MockResponse mockResponse = new MockResponse().setResponseCode(200).setBody
                    ("{\"rows\":[{\"key\":null,\"value\":10}]}");
            server.enqueue(mockResponse);
            viewBuilder.stale(s).build().getSingleValue();
            HttpUrl url = server.takeRequest(1, TimeUnit.SECONDS).getRequestUrl();
            Assert.assertEquals(s, url.queryParameter("stale"));
            Assert.assertEquals("/notarealdb/_design/testDDoc/_view/testView", url.encodedPath());
        }
    }

    /**
     * Similar to staleParameterValues, but for `stable` parameter
     * @throws Exception
     */
    @Test
    public void stableParameterValues() throws Exception {
        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .build();
        Database database = client.database("notarealdb", false);
        for (Boolean b : new Boolean[]{
                false,
                true}) {
            UnpaginatedRequestBuilder<String, Integer> viewBuilder = database.getViewRequestBuilder
                    ("testDDoc", "testView").newRequest(Key.Type.STRING, Integer.class);
            MockResponse mockResponse = new MockResponse().setResponseCode(200).setBody
                    ("{\"rows\":[{\"key\":null,\"value\":10}]}");
            server.enqueue(mockResponse);
            viewBuilder.stable(b).build().getSingleValue();
            HttpUrl url = server.takeRequest(1, TimeUnit.SECONDS).getRequestUrl();
            Assert.assertEquals(Boolean.toString(b), url.queryParameter("stable"));
            Assert.assertEquals("/notarealdb/_design/testDDoc/_view/testView", url.encodedPath());
        }
    }

    /**
     * Similar to staleParameterValues, but for `update` parameter
     * @throws Exception
     */
    @Test
    public void updateParameterValues() throws Exception {
        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .build();
        Database database = client.database("notarealdb", false);
        for (String s : new String[]{
                SettableViewParameters.UPDATE_FALSE,
                SettableViewParameters.UPDATE_TRUE,
                SettableViewParameters.UPDATE_LAZY}) {
            UnpaginatedRequestBuilder<String, Integer> viewBuilder = database.getViewRequestBuilder
                    ("testDDoc", "testView").newRequest(Key.Type.STRING, Integer.class);
            MockResponse mockResponse = new MockResponse().setResponseCode(200).setBody
                    ("{\"rows\":[{\"key\":null,\"value\":10}]}");
            server.enqueue(mockResponse);
            viewBuilder.update(s).build().getSingleValue();
            HttpUrl url = server.takeRequest(1, TimeUnit.SECONDS).getRequestUrl();
            Assert.assertEquals(s, url.queryParameter("update"));
            Assert.assertEquals("/notarealdb/_design/testDDoc/_view/testView", url.encodedPath());
        }
    }

    /**
     * <p>
     * Test added for https://github.com/cloudant/java-cloudant/issues/411
     * </p>
     * <p>
     * When _all_docs is used with specified keys deleted documents are also returned. The value of
     * total_rows may represent only the un-deleted documents meaning more rows are returned than
     * total_rows. This total_rows variance doesn't always manifest so we reproduce it using a mock.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void getIdsAndRevsForDeletedIDsWithAllDocs() throws Exception {

        Map<String, String> idsAndRevs = new HashMap<String, String>(4);
        idsAndRevs.put("docid0", "1-a00e6463d52d7f167c8ac5c834836c1b");
        idsAndRevs.put("docid1", "1-a00e6463d52d7f167c8ac5c834836c1b");
        idsAndRevs.put("docid2", "2-acbb972b187ec952eae1ca74cfef16a9");
        idsAndRevs.put("docid3", "2-acbb972b187ec952eae1ca74cfef16a9");

        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .build();
        Database database = client.database("deletedidsalldocskeysdb", false);

        // _all_docs?keys=["docid0", "docid1", "docid2", "docid3"]
        MockResponse mockResponse = new MockResponse().setResponseCode(200).setBody
                ("{\"total_rows\":2,\"offset\":0,\"rows\":[\n" +
                        "{\"id\":\"docid0\",\"key\":\"docid0\"," +
                        "\"value\":{\"rev\":\"1-a00e6463d52d7f167c8ac5c834836c1b\"}},\n" +
                        "{\"id\":\"docid1\",\"key\":\"docid1\"," +
                        "\"value\":{\"rev\":\"1-a00e6463d52d7f167c8ac5c834836c1b\"}},\n" +
                        "{\"id\":\"docid2\",\"key\":\"docid2\"," +
                        "\"value\":{\"rev\":\"2-acbb972b187ec952eae1ca74cfef16a9\"," +
                        "\"deleted\":true}},\n" +
                        "{\"id\":\"docid3\",\"key\":\"docid3\"," +
                        "\"value\":{\"rev\":\"2-acbb972b187ec952eae1ca74cfef16a9\"," +
                        "\"deleted\":true}}\n" +
                        "]}");
        server.enqueue(mockResponse);

        // Do an _all_docs request using the 4 _ids of the generated docs.
        Map<String, String> allDocsIdsAndRevs = database.getAllDocsRequestBuilder().keys(idsAndRevs
                .keySet().toArray(new String[4])).build().getResponse().getIdsAndRevs();
        assertEquals(idsAndRevs, allDocsIdsAndRevs, "The ids and revs should be equal");
    }

}
