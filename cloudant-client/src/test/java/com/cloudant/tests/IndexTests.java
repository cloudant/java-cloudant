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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.FindByIndexOptions;
import com.cloudant.client.api.model.Index;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.IndexField.SortOrder;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Category(RequiresCloudant.class)
public class IndexTests {

    private static CloudantClientResource clientResource = new CloudantClientResource();
    private static DatabaseResource dbResource = new DatabaseResource(clientResource);
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);

    private static Database db;
    private static CloudantClient account;
    private static Map<String, Object> selector;

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

        //Create indexes
        db.createIndex("Person_name", "Person_name", null,
                new IndexField[]{
                        new IndexField("Person_name", SortOrder.asc),
                        new IndexField("Movie_year", SortOrder.asc)});
        db.createIndex("Movie_year", "Movie_year", null,
                new IndexField[]{new IndexField("Movie_year", SortOrder.asc)});

        //Create selector object: {"Movie_year": { "$gt": 1960}, "Person_name": "Alec Guinness"}
        Map<String, Object> year = new HashMap<String, Object>();
        year.put("$gt", new Integer(1960));
        selector = new HashMap<String, Object>();
        selector.put("Movie_year", year);
        selector.put("Person_name", "Alec Guinness");
    }

    @Test
    public void testNotNullIndexNamesAndFields() {
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
    }

    @Test
    public void testNotNullIndexMovieNameAndYear() {
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
    }

    /**
     * Tests that a complete JSON object in String form e.g.
     * <pre>
     *     {@code
     *          {"selector" : {"Movie_year" : { "$gt" : 1960}, "Person_name" : "Alec Guiness"}}
     *     }
     * </pre>
     * can be passed to the findByIndex method. Note other tests do not use the surrounding { }.
     *
     * @see <a href="https://github.com/cloudant/java-cloudant/issues/137">Issue 137</a>
     */
    @Test
    public void testNotNullIndexMovieNameAndYearWithCompleteJsonObjectStringSelector() {
        List<Movie> movies = db.findByIndex("{\"selector\": { \"Movie_year\": {\"$gt\": 1960}, " +
                        "\"Person_name\": \"Alec Guinness\" } }",
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
    }

    /**
     * Tests that a complete JSON object selector, when converted to a String works for findByIndex.
     *
     * @see #testNotNullIndexMovieNameAndYearWithCompleteJsonObjectStringSelector()
     * @see <a href="https://github.com/cloudant/java-cloudant/issues/137">Issue 137</a>
     */
    @Test
    public void testNotNullIndexMovieNameAndYearWithCompleteJsonObjectSelectorAsString() {
        Map<String, Object> selectorObject = new HashMap<String, Object>();
        selectorObject.put("selector", selector);
        JsonObject selectorObj = new Gson().toJsonTree(selectorObject).getAsJsonObject();
        List<Movie> movies = db.findByIndex(selectorObj.toString(),
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
    }

    @Test
    public void testNotNullIndexMovieNameAndYearSelectorStringWithSpace() {
        List<Movie> movies = db.findByIndex("    \"selector\"   :      {    \"Movie_year\"    :  " +
                        "{\"$gt\": 1960}, \"Person_name\": \"Alec Guinness\" }     ", Movie.class,
                new FindByIndexOptions()
                        .sort(new IndexField("Movie_year", SortOrder.desc))
                        .fields("Movie_name").fields("Movie_year"));
        assertNotNull(movies);
        assert (movies.size() > 0);
        for (Movie m : movies) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }
    }

    @Test
    public void testIndexMovieNameAndYearWithLimitSkipOptions() {
        List<Movie> movies = db.findByIndex("\"selector\": { \"Movie_year\": {\"$gt\": 1960}, " +
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
    }

    @Test
    public void testIndexMovieNameAndYearWithJsonMapObject() {
        List<Movie> movies = db.findByIndex(new GsonBuilder().create().toJson(selector),
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
    }

    @Test
    public void testIndexMovieFindByIndexDesignDoc() {
        Map<String, Object> year = new HashMap<String, Object>();
        year.put("$gt", new Integer(1960));
        Map<String, Object> selector = new HashMap<String, Object>();
        selector.put("Movie_year", year);
        selector.put("Person_name", "Alec Guinness");
        //check find by using index design doc
        List<Movie> movies = db.findByIndex(new GsonBuilder().create().toJson(selector),
                Movie.class,
                new FindByIndexOptions()
                        .sort(new IndexField("Movie_year", SortOrder.desc))
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
    }

    @Test
    public void testIndexMovieFindByIndexDesignDocAndName() {
        Map<String, Object> year = new HashMap<String, Object>();
        year.put("$gt", new Integer(1960));
        Map<String, Object> selector = new HashMap<String, Object>();
        selector.put("Movie_year", year);
        selector.put("Person_name", "Alec Guinness");
        //check find by using index design doc and index name
        List<Movie> movies = db.findByIndex(new GsonBuilder().create().toJson(selector),
                Movie.class,
                new FindByIndexOptions().sort(new IndexField("Movie_year", SortOrder.desc))
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

    }

    @Test(expected = JsonParseException.class)
    public void invalidSelectorObjectThrowsJsonParseException() {
        db.findByIndex("\"selector\"invalid", Movie.class);
    }

    @Test(expected = JsonParseException.class)
    public void invalidFieldThrowsJsonParseException() {
        FindByIndexOptions findByIndexOptions = new FindByIndexOptions();
        findByIndexOptions.fields("\"");
        db.findByIndex("{\"type\":\"subscription\"}", Movie.class, findByIndexOptions);
    }

    /**
     * Test that a design document name is passed as a string.
     *
     * @throws Exception
     */
    @Test
    public void useIndexDesignDocJsonTypeIsString() throws Exception {
        JsonElement useIndex = getUseIndexFromRequest(new FindByIndexOptions().useIndex
                ("Movie_year"));
        assertUseIndexString(useIndex);
    }

    private void assertUseIndexString(JsonElement useIndex) throws Exception {
        assertNotNull("The use_index property should not be null", useIndex);
        assertTrue("The use_index property should be a JsonPrimitive", useIndex.isJsonPrimitive());
        JsonPrimitive useIndexPrimitive = useIndex.getAsJsonPrimitive();
        assertTrue("The use_index property should be a string", useIndexPrimitive.isString());
        String useIndexString = useIndexPrimitive.getAsString();
        assertEquals("The use_index property should be Movie_year", "Movie_year", useIndexString);
    }

    /**
     * Test that a design document and index name is passed as an array.
     *
     * @throws Exception
     */
    @Test
    public void useIndexDesignDocAndIndexNameJsonTypeIsArray() throws Exception {
        JsonElement useIndex = getUseIndexFromRequest(new FindByIndexOptions().useIndex
                ("Movie_year", "Person_name"));
        System.out.println(useIndex);
        assertNotNull("The use_index property should not be null", useIndex);
        assertTrue("The use_index property should be a JsonArray", useIndex.isJsonArray());
        JsonArray useIndexArray = useIndex.getAsJsonArray();
        assertEquals("The use_index array should have two elements", 2, useIndexArray.size());
        assertEquals("The use_index design document should be Movie_year", "Movie_year",
                useIndexArray.get(0).getAsString());
        assertEquals("The use_index index name should be Person_name", "Person_name",
                useIndexArray.get(1).getAsString());
    }

    /**
     * Test that use_index is not specified if no index is provided
     *
     * @throws Exception
     */
    @Test
    public void useIndexNotSpecified() throws Exception {
        JsonElement useIndex = getUseIndexFromRequest(null);
        assertNull("The use_index property should be null (i.e. was not specified)", useIndex);
    }

    /**
     * Test that use_index is replaced if called multiple times
     *
     * @throws Exception
     */
    @Test
    public void useIndexReplaced() throws Exception {
        FindByIndexOptions options = new FindByIndexOptions().useIndex("Movie_year",
                "Person_name").useIndex("Movie_year");
        assertUseIndexString(getUseIndexFromRequest(options));
    }

    /**
     * Uses a mock web server to record a _find request using the specified options
     *
     * @param options FindByIndexOptions to test
     * @return the JsonElement from the use_index property of the JsonObject POSTed with the request
     * @throws Exception
     */
    private JsonElement getUseIndexFromRequest(FindByIndexOptions options) throws Exception {
        JsonElement useIndexRequestProperty = null;
        MockWebServer mockWebServer = new MockWebServer();
        // Return 200 OK with empty array of docs (once for each request)
        mockWebServer.enqueue(new MockResponse().setBody("{ \"docs\" : []}"));
        mockWebServer.start();
        try {
            CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder
                    (mockWebServer).build();
            Database db = client.database("mock", false);
            if (options != null) {
                db.findByIndex("{}", Movie.class, options);
            } else {
                db.findByIndex("{}", Movie.class);
            }
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            JsonObject body = new Gson().fromJson(request.getBody().readUtf8(), JsonObject.class);
            useIndexRequestProperty = body.get("use_index");
        } finally {
            mockWebServer.shutdown();
        }
        return useIndexRequestProperty;
    }
}
