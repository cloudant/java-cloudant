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

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.google.gson.JsonObject;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import java.util.ArrayList;
import java.util.List;

@Category(RequiresDB.class)
public class BulkDocumentTest {

    public static CloudantClientResource clientResource = new CloudantClientResource();
    public static DatabaseResource dbResource = new DatabaseResource(clientResource);
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);


    private static Database db;

    @BeforeClass
    public static void setUp() {
        db = dbResource.get();
    }

    @Test
    public void bulkModifyDocs() {
        List<Object> newDocs = new ArrayList<Object>();
        newDocs.add(new Foo());
        newDocs.add(new JsonObject());

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
