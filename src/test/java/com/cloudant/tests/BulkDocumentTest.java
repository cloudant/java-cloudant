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
import static org.junit.Assert.assertThat;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.test.main.RequiresDB;
import com.google.gson.JsonObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

@Category(RequiresDB.class)
public class BulkDocumentTest {

    private static final Log log = LogFactory.getLog(BulkDocumentTest.class);
    private static Database db;
    private CloudantClient account;

    @Before

    public void setUp() {
        account = CloudantClientHelper.getClient();
        db = account.database("lightcouch-db-test", true);
    }

    @After
    public void tearDown() {
        account.deleteDB("lightcouch-db-test");
    }

    @Test
    public void bulkModifyDocs() {
        List<Object> newDocs = new ArrayList<Object>();
        newDocs.add(new Foo());
        newDocs.add(new JsonObject());

        //	boolean allOrNothing = true;

        // allorNothing is not supported in cloudant
        //	List<Response> responses = db.bulk(newDocs, allOrNothing);
        List<Response> responses = db.bulk(newDocs);

        assertThat(responses.size(), is(2));
    }

    @Test
    public void bulkDocsRetrieve() throws Exception {
        Response r1 = db.save(new Foo());
        Response r2 = db.save(new Foo());

        List<Foo> docs = db.getAllDocsRequestBuilder().includeDocs(true).keys(r1.getId(), r2
                .getId()).build().getResponse().getDocsAs(Foo.class);

        assertThat(docs.size(), is(2));
    }

}
