/*
 * Copyright (C) 2011 lightcouch.org
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.Changes;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ChangesResult;
import com.cloudant.client.api.model.ChangesResult.Row;
import com.cloudant.client.api.model.DbInfo;
import com.cloudant.client.api.model.Response;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiresDB
public class ChangeNotificationsTest extends TestWithDbPerClass {

    @RegisterExtension
    public static MockWebServerExtension mockWebServerExt = new MockWebServerExtension();
    private static MockWebServer mockWebServer;

    @BeforeEach
    public void setup() {
        mockWebServer = mockWebServerExt.get();
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
        assertNotNull(request, "There should be a changes request");
        assertTrue(request.getPath()
                .contains("descending=true"), "There should be a descending parameter on the " +
                "request");
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
        assertNotNull(request, "There should be a changes request");
        assertTrue(request.getPath()
                .contains("filter=myFilter"), "There should be a filter parameter on the request");
        assertTrue(request.getPath()
                .contains("myParam=paramValue"), "There should be a custom parameter on the " +
                "request");
    }

    /**
     * Test that the continuous changes feed iterator correctly reads the last_seq line
     */
    @Test
    public void changesFeedLastSeq() throws Exception {

        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .build();
        Database db = client.database("notreal", false);

        // Mock up a set of changes with a last_seq
        String body = "{\"seq\":\"1" +
                "-g1AAAAETeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YiA6oWI9xakhyAZFI9SFcGcyJjLpDHbmxukmponkjYBKIdlscCJBkagBTQov0Y7jMmpPMARCfIZ1kA23taSg\",\"id\":\"llama\",\"changes\":[{\"rev\":\"6-77acefc448de2129bb6427ebdeb021df\"}]}\n" +
                "{\"seq\":\"2-g1AAAAFDeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YiA6oWI9xakhyAZFI9SFcGcyJjLpDHbmxukmponkjYBKIdlscCJBkagBTQov0Y7jMmpPMARCeyG41NTZPSTAmbkgUA-4RpJw\",\"id\":\"kookaburra\",\"changes\":[{\"rev\":\"4-6038cf35dfe1211f85484dec951142c7\"}]}\n" +
                "{\"seq\":\"3-g1AAAAFDeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YiA6oWI9xakhyAZFI9SFcGcyJjLpDHbmxukmponkjYBKIdlscCJBkagBTQov0Y7jMmpPMARCfYjUwQNxqbmialmRI2JQsA-7RpKA\",\"id\":\"panda\",\"changes\":[{\"rev\":\"2-f578490963b0bd266f6c5bbf92302977\"}]}\n" +
                "{\"seq\":\"4-g1AAAAFDeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YiA6oWI9xakhyAZFI9SFcGcyJjLpDHbmxukmponkjYBKIdlscCJBkagBTQov0Y7jMmpPMARCfYjcwQNxqbmialmRI2JQsA--RpKQ\",\"id\":\"snipe\",\"changes\":[{\"rev\":\"3-4b2fb3b7d6a226b13951612d6ca15a6b\"}]}\n" +
                "{\"seq\":\"5-g1AAAAFzeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJjLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqUWwxNjdJNTRPJGwC0R7JYwGSDA1ACmjR_qxEBlSdxoR0HoDoBLuRGeJGY1PTpDRTwqZkAQAv9XgS\",\"id\":\"badger\",\"changes\":[{\"rev\":\"4-51aa94e4b0ef37271082033bba52b850\"}]}\n" +
                "{\"seq\":\"6-g1AAAAGjeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJjLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqUWwxNjdJNTRPJGwC0R7JYwGSDA1ACmjRfoRNhubGZqaWlqT6x5iQTQcgNoH9xAzxk7GpaVKaKWFTsgBvlIad\",\"id\":\"870908b66ac0ed114512e6fb6d00260f\",\"changes\":[{\"rev\":\"2-eec205a9d413992850a6e32678485900\"}],\"deleted\":true}\n" +
                "{\"seq\":\"7-g1AAAAGjeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJTLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqobYwgm0xNjdJNTRPJGwC0R7JYwGSDA1ACmjRfoRNhubGZqaWlqT6x5iQTQcgNoH9xAzxk7GpaVKaKWFTsgBw_oae\",\"id\":\"_design/validation\",\"changes\":[{\"rev\":\"2-97e93126a6337d173f9b2810c0b9c0b6\"}],\"deleted\":true}\n" +
                "{\"seq\":\"8-g1AAAAGjeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJzLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqobYwgm0xNjdJNTRPJGwC0R7JYwGSDA1ACmjRfoRNhubGZqaWlqT6x5iQTQcgNiGFnLGxqWlSmilhU7IAcmiGnw\",\"id\":\"elephant\",\"changes\":[{\"rev\":\"3-f812228f45b5f4e496250556195372b2\"}]}\n" +
                "{\"seq\":\"9-g1AAAAGjeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJzLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqobYwgm0xNjdJNTRPJGwC0R7JYwGSDA1ACmjRfpBNTGCbDM2NzUwtLUn1jzEhmw5AbEIKOWNjU9OkNFPCpmQBAHMChqA\",\"id\":\"_design/views101\",\"changes\":[{\"rev\":\"1-a918dd4f11704143b535f0ab3af4bf75\"}]}\n" +
                "{\"seq\":\"10-g1AAAAGjeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJLLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqobYwgm0xNjdJNTRPJGwC0R7JYwGSDA1ACmjRfpBNTGCbDM2NzUwtLUn1jzEhmw5AbAL7iRniJ2NT06Q0U8KmZAEAdGyGoQ\",\"id\":\"lemur\",\"changes\":[{\"rev\":\"3-552d9dbf91fa914a07756e69b9ceaafa\"}]}\n" +
                "{\"seq\":\"11-g1AAAAGjeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJLLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqobYwgm0xNjdJNTRPJGwC0R7JYwGSDA1ACmjRfpBNzGCbDM2NzUwtLUn1jzEhmw5AbPqPsMnY2NQ0Kc2UsClZAHUGhqI\",\"id\":\"aardvark\",\"changes\":[{\"rev\":\"3-fe45a3e06244adbe7ba145e74e57aba5\"}]}\n" +
                "{\"seq\":\"12-g1AAAAGjeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJrLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqobYwgm0xNjdJNTRPJGwC0R7JYwGSDA1ACmjRfpBNzGCbDM2NzUwtLUn1jzEhmw5AbPqPsMnY2NQ0Kc2UsClZAHZwhqM\",\"id\":\"zebra\",\"changes\":[{\"rev\":\"3-750dac460a6cc41e6999f8943b8e603e\"}]}\n" +
                "{\"seq\":\"13-g1AAAAGjeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJrLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqobYwgm0xNjdJNTRPJGwC0R7JYwGSDA1ACmjRfpBNLGCbDM2NzUwtLUn1jzEhmw5AbAL7iRniJ2NT06Q0U8KmZAEAdwqGpA\",\"id\":\"cat\",\"changes\":[{\"rev\":\"2-eec205a9d413992850a6e32678485900\"}],\"deleted\":true}\n" +
                "{\"seq\":\"14-g1AAAAGjeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJrLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqobYwgm0xNjdJNTRPJGwC0R7JYwGSDA1ACmjRfoR_DM2NzUwtLUn1jzEhmw5AbAL7iRniJ2NT06Q0U8KmZAEAd6SGpQ\",\"id\":\"giraffe\",\"changes\":[{\"rev\":\"3-7665c3e66315ff40616cceef62886bd8\"}]}\n" +
                "{\"last_seq\":\"14-g1AAAAGjeJzLYWBgYMlgTmGQT0lKzi9KdUhJMtNLykxPyilN1UvOyS9NScwr0ctLLckBKmRKZEiy____f1YGcyJrLlCA3dwkLcXczISwdlQrjHBbkeQAJJPqobYwgm0xNjdJNTRPJGwC0R7JYwGSDA1ACmjRfoR_DM2NzUwtLUn1jzEhmw5AbAL7iRniJ2NT06Q0U8KmZAEAd6SGpQ\",\"pending\":0}";
        mockWebServer.enqueue(new MockResponse().setBody(body));
        Changes c = db.changes().continuousChanges();
        int nChanges = 0;
        // check against regression where hasNext() will hang
        while (c.hasNext()) {
            nChanges++;
            c.next();
        }
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request, "There should be a changes request");
        assertEquals(14, nChanges, "There should be 14 changes");
    }

}
