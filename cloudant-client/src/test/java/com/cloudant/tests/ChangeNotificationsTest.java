/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright © 2015, 2016 IBM Corp. All rights reserved.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.Changes;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ChangesResult;
import com.cloudant.client.api.model.ChangesResult.Row;
import com.cloudant.client.api.model.DbInfo;
import com.cloudant.client.api.model.Response;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Category(RequiresDB.class)
public class ChangeNotificationsTest {

    @ClassRule
    public static CloudantClientResource clientResource = new CloudantClientResource();
    @ClassRule
    public static MockWebServer mockWebServer = new MockWebServer();

    @Rule
    public DatabaseResource dbResource = new DatabaseResource(clientResource);

    private Database db;

    @Before
    public void setup() {
        db = dbResource.get();
    }

    @Test
    public void changes_normalFeed() {
        db.save(new Foo());

        ChangesResult changes = db.changes()
                .includeDocs(true)
                .limit(1)
                .getChanges();

        List<ChangesResult.Row> rows = changes.getResults();

        for (Row row : rows) {
            List<ChangesResult.Row.Rev> revs = row.getChanges();
            String docId = row.getId();
            JsonObject doc = row.getDoc();

            assertNotNull(revs);
            assertNotNull(docId);
            assertNotNull(doc);
        }

        assertThat(rows.size(), is(1));
    }

    @Test
    public void changes_normalFeed_withSeqInterval() {
        for(int i = 0; i < 100; i++) {
            db.save(new Foo());
        }

        ChangesResult changes = db.changes()
                .includeDocs(true)
                .sequenceInterval(10)
                .getChanges();

        List<ChangesResult.Row> rows = changes.getResults();

        int seqCount = 0;
        for (Row row : rows) {
            List<ChangesResult.Row.Rev> revs = row.getChanges();
            String docId = row.getId();
            JsonObject doc = row.getDoc();
            String seq = row.getSeq();
            if(!seq.equals("null")) {
                seqCount++;
            }

            assertNotNull(revs);
            assertNotNull(docId);
            assertNotNull(doc);
        }
        // first change always contains a seq
        assertNotNull(rows.get(0).getSeq());
        assertEquals(10, seqCount);


        assertThat(rows.size(), is(100));
    }

    @Test
    public void changes_continuousFeed() {
        db.save(new Foo());

        DbInfo dbInfo = db.info();
        String since = dbInfo.getUpdateSeq();

        Changes changes = db.changes()
                .includeDocs(true)
                .since(since)
                .heartBeat(30000)
                .continuousChanges();

        Response response = db.save(new Foo());

        while (changes.hasNext()) {
            ChangesResult.Row feed = changes.next();
            final JsonObject feedObject = feed.getDoc();
            final String docId = feed.getId();

            assertEquals(response.getId(), docId);
            assertNotNull(feedObject);

            changes.stop();
        }
    }

    @Test
    public void changes_continuousFeed_withSeqInterval() {
        //db.save(new Foo());

        DbInfo dbInfo = db.info();
        String since = dbInfo.getUpdateSeq();

        Changes changes = db.changes()
                .includeDocs(true)
                .since(since)
                .heartBeat(30000)
                .sequenceInterval(2)
                .continuousChanges();

        Response firstDocResp = db.save(new Foo());
        Response secondDocResp = db.save(new Foo());
        List<Response> responses = new ArrayList<Response>();
        responses.add(firstDocResp);
        responses.add(secondDocResp);

        int docCount = 0;
        while (changes.hasNext()) {
            ChangesResult.Row feed = changes.next();
            final JsonObject feedObject = feed.getDoc();
            final String docId = feed.getId();

            assertEquals(responses.get(docCount).getId(), docId);
            assertNotNull(feedObject);

            if(docCount == 1) {
                assertEquals("null", feed.getSeq());
                changes.stop();
            } else {
                assertNotNull(feed.getSeq());
            }
            docCount++;
        }
    }

    /**
     * Test that the descending true parameter is applied and the results are returned in reverse
     * order.
     */
    @Test
    public void changesDescending() throws Exception {

        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .build();
        Database db = client.database("notreal", false);

        // Mock up an empty set of changes
        mockWebServer.enqueue(new MockResponse().setBody("{\"results\": []}"));
        db.changes().descending(true).getChanges();
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("There should be a changes request", request);
        assertTrue("There should be a descending parameter on the request", request.getPath()
                .contains("descending=true"));
    }

    /**
     * Test that a custom parameter can be added to a changes request.
     */
    @Test
    public void changesCustomParameter() throws Exception {
        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .build();
        Database db = client.database("notreal", false);
        // Mock up an empty set of changes
        mockWebServer.enqueue(new MockResponse().setBody("{\"results\": []}"));
        db.changes().filter("myFilter").parameter("myParam", "paramValue").getChanges();
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull("There should be a changes request", request);
        assertTrue("There should be a filter parameter on the request", request.getPath()
                .contains("filter=myFilter"));
        assertTrue("There should be a custom parameter on the request", request.getPath()
                .contains("myParam=paramValue"));
    }
}
