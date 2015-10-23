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

import static org.junit.Assert.assertNotNull;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.FindByIndexOptions;
import com.cloudant.client.api.model.Index;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.IndexField.SortOrder;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.google.gson.GsonBuilder;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class IndexTests {

    private static CloudantClientResource clientResource = new CloudantClientResource();
    private static DatabaseResource dbResource = new DatabaseResource(clientResource);
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);

    private static Database db;
    private static CloudantClient account;

    @BeforeClass
    public static void setUp() throws Exception {
        account = clientResource.get();
        db = dbResource.get();

        // create the movies-demo db for our index tests
        com.cloudant.client.api.Replication r = account.replication();
        r.source("https://clientlibs-test.cloudant.com/movies-demo");
        r.createTarget(true);
        r.target(dbResource.getDbURIWithUserInfo());
        r.trigger();
    }

    @Test
    @Category(RequiresCloudant.class)
    public void indexTestAll() {

        db.createIndex("Person_name", "Person_name", null,
                new IndexField[]{
                        new IndexField("Person_name", SortOrder.asc),
                        new IndexField("Movie_year", SortOrder.asc)});
        db.createIndex("Movie_year", "Movie_year", null,
                new IndexField[]{new IndexField("Movie_year", SortOrder.asc)});

        List<Index> indices = db.listIndices();
        assertNotNull(indices);
        assert (indices.size() > 0);
        for (Index i : indices) {
            assertNotNull(i.getName());
            assertNotNull(i.getFields());
            Iterator<IndexField> flds = i.getFields();
            assert (flds.hasNext());
            while (flds.hasNext()) {
                IndexField fld = flds.next();
                assertNotNull(fld.getName());
                assertNotNull(fld.getOrder());
            }

        }

        List<Movie> movies = db.findByIndex("\"selector\": { \"Movie_year\": {\"$gt\": 1960}, " +
                        "\"Person_name\": \"Alec Guinness\" }",
                Movie.class,
                new FindByIndexOptions()
                        .sort(new IndexField("Movie_year", SortOrder.desc))
                        .fields("Movie_name").fields("Movie_year"));
        assertNotNull(movies);
        assert (movies.size() > 0);
        for (Movie m : movies) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }

        movies = db.findByIndex("\"selector\": { \"Movie_year\": {\"$gt\": 1960}, " +
                        "\"Person_name\": \"Alec Guinness\" }",
                Movie.class,
                new FindByIndexOptions()
                        .sort(new IndexField("Movie_year", SortOrder.desc))
                        .fields("Movie_name").fields("Movie_year")
                        .limit(1)
                        .skip(1)
                        .readQuorum(2));
        assertNotNull(movies);
        assert (movies.size() == 1);
        for (Movie m : movies) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }

        // selectorJson as a proper json object
        Map<String, Object> year = new HashMap<String, Object>();
        year.put("$gt", new Integer(1960));
        Map<String, Object> selector = new HashMap<String, Object>();
        selector.put("Movie_year", year);
        selector.put("Person_name", "Alec Guinness");
        movies = db.findByIndex(new GsonBuilder().create().toJson(selector),
                Movie.class,
                new FindByIndexOptions()
                        .sort(new IndexField("Movie_year", SortOrder.desc))
                        .fields("Movie_name").fields("Movie_year")
                        .limit(1)
                        .skip(1)
                        .readQuorum(2));
        assertNotNull(movies);
        assert (movies.size() == 1);
        for (Movie m : movies) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }

        //check find by using index design doc
        db.findByIndex(new GsonBuilder().create().toJson(selector), Movie.class, new
                FindByIndexOptions().sort(new IndexField("Movie_year", SortOrder.desc))
                .fields("Movie_name").fields("Movie_year")
                .limit(1)
                .skip(1)
                .readQuorum(2).useIndex("Movie_year"));
        assertNotNull(movies);
        assert (movies.size() == 1);
        for (Movie m : movies) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }

        //check find by using index design doc and index name
        db.findByIndex(new GsonBuilder().create().toJson(selector), Movie.class, new
                FindByIndexOptions().sort(new IndexField("Movie_year", SortOrder.desc))
                .fields("Movie_name").fields("Movie_year")
                .limit(1)
                .skip(1)
                .readQuorum(2).useIndex("Movie_year", "Movie_year"));
        assertNotNull(movies);
        assert (movies.size() == 1);
        for (Movie m : movies) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }


        db.deleteIndex("Person_name", "Person_name");
        db.deleteIndex("Movie_year", "Movie_year");
    }


}
