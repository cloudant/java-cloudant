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

import static com.cloudant.client.api.query.EmptyExpression.empty;
import static com.cloudant.client.api.query.Expression.eq;
import static com.cloudant.client.api.query.Expression.gt;
import static com.cloudant.client.api.query.Operation.and;
import static com.cloudant.tests.CloudantClientHelper.getReplicationSourceUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.FindByIndexOptions;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.api.query.Sort;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiresCloudant
public class IndexTests extends TestWithDbPerClass {

    @BeforeAll
    public static void setUp() throws Exception {

        // create the movies-demo db for our index tests
        com.cloudant.client.api.Replication r = account.replication();
        r.source(getReplicationSourceUrl("movies-demo"));
        r.createTarget(true);
        r.target(dbResource.getDbURIWithUserInfo());
        r.trigger();

        //Create indexes
        db.createIndex(JsonIndex.builder().name("Person_name")
                .designDocument("Person_name")
                .asc("Person_name", "Movie_year")
                .definition());
        db.createIndex(JsonIndex.builder().name("Movie_year")
                .designDocument("Movie_year")
                .asc("Movie_year")
                .definition());
    }

    /* Deprecated API tests */

    @Test
    public void testDeprecatedApiNotNullIndexMovieNameAndYear() {
        List<Movie> movies = db.findByIndex("\"selector\": { \"Movie_year\": {\"$gt\": 1960}, " +
                        "\"Person_name\": \"Alec Guinness\" }",
                Movie.class,
                new FindByIndexOptions()
                        .sort(new IndexField("Movie_year", IndexField.SortOrder.desc))
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
    public void testDeprecatedApiNotNullIndexMovieNameAndYearWithCompleteJsonObjectStringSelector
    () {
        List<Movie> movies = db.findByIndex("{\"selector\": { \"Movie_year\": {\"$gt\": 1960}, " +
                        "\"Person_name\": \"Alec Guinness\" } }",
                Movie.class,
                new FindByIndexOptions()
                        .sort(new IndexField("Movie_year", IndexField.SortOrder.desc))
                        .fields("Movie_name").fields("Movie_year"));
        assertNotNull(movies);
        assert (movies.size() > 0);
        for (Movie m : movies) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }

    }

    @Test
    public void invalidSelectorObjectThrowsJsonParseException() {
        assertThrows(JsonParseException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                db.findByIndex("\"selector\"invalid", Movie.class);
            }
        });
    }

    @Test
    public void invalidFieldThrowsJsonParseException() {
        assertThrows(JsonParseException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                FindByIndexOptions findByIndexOptions = new FindByIndexOptions();
                findByIndexOptions.fields("\"");
                db.findByIndex("{\"type\":\"subscription\"}", Movie.class, findByIndexOptions);
            }
        });
    }

    /* Current API tests */

    @Test
    public void testNotNullIndexMovieNameAndYear() {
        QueryResult<Movie> movies = db.query(new QueryBuilder(and(
                gt("Movie_year", 1960),
                eq("Person_name", "Alec Guinness"))).
                        sort(Sort.desc("Movie_year")).
                        fields("Movie_name", "Movie_year").
                        executionStats(true).
                        build(),
                Movie.class);

        assertNotNull(movies);
        assert (movies.getDocs().size() > 0);
        // TODO assert order
        for (Movie m : movies.getDocs()) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }
        assertTrue(movies.getExecutionStats().getExecutionTimeMs() > 0);
        assertTrue(movies.getExecutionStats().getResultsReturned() > 0);
        assertTrue(movies.getExecutionStats().getTotalDocsExamined() > 0);
    }

    @Test
    public void testNotNullIndexNamesAndFields() {
        List<JsonIndex> indices = db.listIndexes().jsonIndexes();
        assertNotNull(indices);
        assert (indices.size() > 0);
        for (JsonIndex i : indices) {
            assertNotNull(i.getName());
            assertNotNull(i.getFields());
            List<JsonIndex.Field> flds = i.getFields();
            assertTrue(flds.size() > 0, "The fields should not be empty");
            for (JsonIndex.Field field : flds) {
                assertNotNull(field.getName(), "The field name should not be null");
                assertNotNull(field.getOrder(), "The sort order should not be null");
            }
        }
    }

    // TODO should we support read quorum which this test previously supported
    @Test
    public void testIndexMovieNameAndYearWithLimitSkipOptions() {
        QueryResult<Movie> movies = db.query(new QueryBuilder(and(
                gt("Movie_year", 1960),
                eq("Person_name", "Alec Guinness"))).
                        sort(Sort.desc("Movie_year")).
                        fields("Movie_name", "Movie_year").
                        limit(1).
                        skip(1).
                        build(),
                Movie.class);
        assertNotNull(movies);
        assert (movies.getDocs().size() == 1);
        for (Movie m : movies.getDocs()) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }
    }


    @Test
    public void testIndexMovieFindByIndexDesignDoc() {
        QueryResult<Movie> movies = db.query(new QueryBuilder(and(
                gt("Movie_year", 1960),
                eq("Person_name", "Alec Guinness"))).
                        sort(Sort.desc("Movie_year")).
                        fields("Movie_name", "Movie_year").
                        limit(1).
                        skip(1).
                        useIndex("Movie_year").
                        build(),
                Movie.class);
        assertNotNull(movies);
        assert (movies.getDocs().size() == 1);
        for (Movie m : movies.getDocs()) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }
    }

    @Test
    public void testIndexMovieFindByIndexDesignDocAndName() {
        QueryResult<Movie> movies = db.query(new QueryBuilder(and(
                gt("Movie_year", 1960),
                eq("Person_name", "Alec Guinness"))).
                        sort(Sort.desc("Movie_year")).
                        fields("Movie_name", "Movie_year").
                        limit(1).
                        skip(1).
                        useIndex("Movie_year", "Movie_year").
                        build(),
                Movie.class);
        assertNotNull(movies);
        assert (movies.getDocs().size() == 1);
        for (Movie m : movies.getDocs()) {
            assertNotNull(m.getMovie_name());
            assertNotNull(m.getMovie_year());
        }
    }

    @Test
    public void testBookmarks() {
        QueryBuilder queryBuilder = new QueryBuilder(and(
                gt("Movie_year", 1960),
                eq("Person_name", "Alec Guinness"))).
                sort(Sort.desc("Movie_year")).
                fields("Movie_name", "Movie_year").
                limit(2);

        QueryResult<Movie> moviesPage;
        String bookmark = null;
        int pageCount = 0;
        QueryBuilder queryBuilderBookmarked = queryBuilder;
        do {
            if (bookmark != null) {
                queryBuilderBookmarked = queryBuilder.bookmark(bookmark);
            }
            moviesPage = db.query(queryBuilderBookmarked.build(), Movie.class);
            bookmark = moviesPage.getBookmark();
            if (!moviesPage.getDocs().isEmpty()) {
                pageCount++;
            }
        } while (!moviesPage.getDocs().isEmpty());
        assertEquals(3, pageCount);

    }

    /**
     * Test that a design document name is passed as a string.
     *
     * @throws Exception
     */
    @Test
    public void useIndexDesignDocJsonTypeIsString() throws Exception {
        JsonElement useIndex = getUseIndexFromRequest(new QueryBuilder(empty()).
                useIndex("Movie_year"));
        assertUseIndexString(useIndex);
    }

    private void assertUseIndexString(JsonElement useIndex) throws Exception {
        assertNotNull(useIndex, "The use_index property should not be null");
        assertTrue(useIndex.isJsonPrimitive(), "The use_index property should be a JsonPrimitive");
        JsonPrimitive useIndexPrimitive = useIndex.getAsJsonPrimitive();
        assertTrue(useIndexPrimitive.isString(), "The use_index property should be a string");
        String useIndexString = useIndexPrimitive.getAsString();
        assertEquals("Movie_year", useIndexString, "The use_index property should be Movie_year");
    }

    /**
     * Test that a design document and index name is passed as an array.
     *
     * @throws Exception
     */
    @Test
    public void useIndexDesignDocAndIndexNameJsonTypeIsArray() throws Exception {
        JsonElement useIndex = getUseIndexFromRequest(new QueryBuilder(empty()).
                useIndex("Movie_year", "Person_name"));
        assertNotNull(useIndex, "The use_index property should not be null");
        assertTrue(useIndex.isJsonArray(), "The use_index property should be a JsonArray");
        JsonArray useIndexArray = useIndex.getAsJsonArray();
        assertEquals(2, useIndexArray.size(), "The use_index array should have two elements");
        assertEquals("Movie_year", useIndexArray.get(0).getAsString(), "The use_index design " +
                "document should be Movie_year");
        assertEquals("Person_name", useIndexArray.get(1).getAsString(), "The use_index index name" +
                " should be Person_name");
    }

    /**
     * Test that use_index is not specified if no index is provided
     *
     * @throws Exception
     */
    @Test
    public void useIndexNotSpecified() throws Exception {
        JsonElement useIndex = getUseIndexFromRequest(new QueryBuilder(empty()));
        assertNull(useIndex, "The use_index property should be null (i.e. was not specified)");
    }

    /**
     * Test that use_index is replaced if called multiple times
     *
     * @throws Exception
     */
    @Test
    public void useIndexReplaced() throws Exception {
        QueryBuilder builder = new QueryBuilder(empty()).
                useIndex("Movie_year", "Person_name").
                useIndex("Movie_year");
        assertUseIndexString(getUseIndexFromRequest(builder));
    }

    /**
     * Uses a mock web server to record a _find request using the specified options
     *
     * @param builder query to make
     * @return the JsonElement from the use_index property of the JsonObject POSTed with the request
     * @throws Exception
     */
    private JsonElement getUseIndexFromRequest(QueryBuilder builder) throws Exception {
        JsonElement useIndexRequestProperty = null;
        MockWebServer mockWebServer = new MockWebServer();
        // Return 200 OK with empty array of docs (once for each request)
        mockWebServer.enqueue(new MockResponse().setBody("{ \"docs\" : []}"));
        mockWebServer.start();
        try {
            CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder
                    (mockWebServer).build();
            Database db = client.database("mock", false);
            db.query(builder.build(), Movie.class);
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            JsonObject body = new Gson().fromJson(request.getBody().readUtf8(), JsonObject.class);
            useIndexRequestProperty = body.get("use_index");
        } finally {
            mockWebServer.shutdown();
        }
        return useIndexRequestProperty;
    }
}
