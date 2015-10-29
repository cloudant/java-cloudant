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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.cloudant.client.api.Changes;
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

import java.util.List;

@Category(RequiresDB.class)
public class ChangeNotificationsTest {

    @ClassRule
    public static CloudantClientResource clientResource = new CloudantClientResource();

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
}
