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


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.DesignDocumentManager;
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.cloudant.tests.util.Utils;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RequiresDB
public class DesignDocumentsTest extends TestWithDbPerClass {

    @RegisterExtension
    public static MockWebServerExtension mockWebServerExt = new MockWebServerExtension();

    private static MockWebServer mockWebServer;

    private static File rootDesignDir;
    private static DesignDocument designDocExample;
    private static DesignDocumentManager designManager;

    @BeforeEach
    public void beforeEach() {
        mockWebServer = mockWebServerExt.get();
    }

    @BeforeAll
    public static void beforeAll() throws Exception {
        rootDesignDir = new File(System.getProperty("user.dir")
                + "/src/test/resources/design-files");
        designManager = db.getDesignDocumentManager();
        designDocExample = fileToDesignDocument("example");
    }

    /**
     * Test helper that converts a test resource js file (located in the {@link #rootDesignDir}
     * to a DesignDocument.
     *
     * @param name parts of the file name before _design_doc.js (e.g. "conflicts", "example", or
     *             "views101")
     * @return the DesignDocument object generated from the file.
     * @throws Exception
     */
    private static DesignDocument fileToDesignDocument(String name) throws Exception {
        File testDesignDocFile = new File(String.format("%s/%s_design_doc.js", rootDesignDir,
                name));
        return designManager.fromFile(testDesignDocFile);
    }

    @Test
    public void designDocSync() throws Exception {
        Utils.assertOKResponse(designManager.put(designDocExample));
    }

    @Test
    public void designDocCompare() throws Exception {
        DesignDocument exampleDoc = fileToDesignDocument("example");
        Response response = designManager.put(exampleDoc);
        // Assign the revision to our local DesignDocument object (needed for equality)
        exampleDoc.setRevision(response.getRev());

        DesignDocument designDoc11 = db.getDesignDocumentManager().get("_design/example");

        assertEquals(exampleDoc, designDoc11, "The design document retrieved should equal ");
    }

    @Test
    public void updateDesignDocIndex() throws Exception {
        DesignDocument designDoc1 = DesignDocumentManager.fromFile(
                new File(String.format("%s/views101_design_doc.js", rootDesignDir)));

        designDoc1.setId("_design/MyAmazingDdoc");
        JsonObject indexes = designDoc1.getIndexes();
        designDoc1.setIndexes(null);

        Response response = designManager.put(designDoc1);
        designDoc1.setRevision(response.getRev());


        designDoc1.setIndexes(indexes);
        response = designManager.put(designDoc1);
        Utils.assertOKResponse(response);
    }

    @Test
    public void designDocs() throws Exception {
        List<DesignDocument> designDocs = DesignDocumentManager.fromDirectory(rootDesignDir);
        DesignDocument[] docArray = designDocs.toArray(new DesignDocument[designDocs.size()]);
        designManager.put(docArray);

        assertThat(designDocs.size(), not(0));
    }

    @Test
    public void updateDesignDocs() throws Exception {
        List<DesignDocument> designDocs = DesignDocumentManager.fromDirectory(rootDesignDir);
        assertThat(designDocs.size(), not(0));

        DesignDocument[] docArray = designDocs.toArray(new DesignDocument[designDocs.size()]);
        designManager.put(docArray);

        for (String id : new String[]{"_design/conflicts", "_design/example", "_design/views101"}) {
            assertNotNull(designManager.get(id), "");
        }
    }

    /**
     * Validate that a design document can be retrieved without using the "_design" prefix.
     *
     * @throws Exception
     */
    @Test
    public void designDocGetNoPrefix() throws Exception {
        // Write a doc with a _design prefix
        designManager.put(designDocExample);

        // Retrieve it without a prefix
        assertNotNull(designManager.get("example"), "The design doc should be retrieved without a" +
                " _design prefix");
    }

    /**
     * Validate that a design document can be retrieved without using the "_design" prefix when a
     * revision is supplied
     *
     * @throws Exception
     */
    @Test
    public void designDocGetNoPrefixWithRevision() throws Exception {
        // Write a doc with a _design prefix
        Response r = designManager.put(designDocExample);

        // Retrieve it without a prefix
        assertNotNull(designManager.get("example", r.getRev()), "The design doc should be " +
                "retrieved without a _design prefix");
    }

    /**
     * Validate that a design document can be retrieved without using the "_design" prefix.
     *
     * @throws Exception
     */
    @Test
    public void designDocRemoveNoPrefix() throws Exception {
        // Write a doc with a _design prefix
        designManager.put(designDocExample);

        // Remove it without a prefix
        Utils.assertOKResponse(designManager.remove("example"));
    }

    /**
     * Validate that a design document can be retrieved without using the "_design" prefix when a
     * revision is supplied
     *
     * @throws Exception
     */
    @Test
    public void designDocRemoveNoPrefixWithRevision() throws Exception {
        // Write a doc with a _design prefix
        Response r = designManager.put(designDocExample);

        // Retrieve it without a prefix
        Utils.assertOKResponse(designManager.remove("example", r.getRev()));
    }

    /**
     * Validate that a design document can be removed without using the "_design" prefix when a
     * DesignDocument object is supplied
     *
     * @throws Exception
     */
    @Test
    public void designDocRemoveNoPrefixWithObject() throws Exception {
        // Write a doc with a _design prefix
        Response r = designManager.put(designDocExample);

        DesignDocument ddoc = new DesignDocument();
        ddoc.setId("example");
        ddoc.setRevision(r.getRev());

        // Retrieve it without a prefix
        Utils.assertOKResponse(designManager.remove(ddoc));
    }

    /**
     * Validate that a design document can be retrieved without using the "_design" prefix.
     *
     * @throws Exception
     */
    @Test
    public void designDocPutNoPrefix() throws Exception {
        // Write a doc without a _design prefix
        // Create an example without the _design prefix
        DesignDocument designDocExampleNoPrefix = fileToDesignDocument("example");
        designDocExampleNoPrefix.setId("example");
        Utils.assertOKResponse(designManager.put(designDocExampleNoPrefix));

        // Retrieve it with a prefix
        assertNotNull(designManager.get("_design/example"), "The design doc should be retrievable" +
                " with a _design prefix");
    }

    /**
     * Test that a design document with an index can be deleted.
     *
     * @throws Exception
     */
    @Test
    public void deleteDesignDocWithIndex() throws Exception {
        // Put a design document with indices
        DesignDocument ddocWithIndices = fileToDesignDocument("views101");
        designManager.put(ddocWithIndices);

        // Now delete the design doc with indices
        designManager.remove("_design/views101");
    }

    /**
     * Test that a CouchDbException is thrown if an IOException is encountered when trying to get
     * revision information for a design document removal.
     *
     * @throws Exception
     */
    @Test
    public void couchDbExceptionIfIOExceptionDuringDDocRemove() throws Exception {
        assertThrows(CouchDbException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                CloudantClient mockClient = CloudantClientHelper.newMockWebServerClientBuilder
                        (mockWebServer).readTimeout(50, TimeUnit.MILLISECONDS).build();
                // Cause a read timeout to generate an IOException
                mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
                Database database = mockClient.database(dbResource.getDatabaseName(), false);
                // Try to remove a design document by id only, generates a HEAD request for
                // revision info
                database.getDesignDocumentManager().remove("example");
            }
        });
    }

    /**
     * Test that a CouchDbException is thrown
     *
     * @throws Exception
     */
    @Test
    public void couchDbExceptionIfNoETagOnDDocRemove() throws Exception {
        assertThrows(CouchDbException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                CloudantClient mockClient = CloudantClientHelper.newMockWebServerClientBuilder
                        (mockWebServer).build();
                Database database = mockClient.database(dbResource.getDatabaseName(), false);
                // Queue a mock response with no "ETag" header
                mockWebServer.enqueue(new MockResponse());
                database.getDesignDocumentManager().remove("example");
            }
        });
    }

    /**
     * Test the {@link DesignDocumentManager#list()} function. Assert that the returned list of
     * design documents matches that expected.
     *
     * @throws Exception
     */
    @Test
    public void listDesignDocuments() throws Exception {
        // Put all the design docs from the directory
        List<DesignDocument> designDocs = DesignDocumentManager.fromDirectory(rootDesignDir);

        // Sort the list lexicographically so that the order matches that returned by the list
        // function, as elements need to be in the same order for list.equals().
        Collections.sort(designDocs, new Comparator<DesignDocument>() {
            @Override
            public int compare(DesignDocument doc1, DesignDocument doc2) {
                return doc1.getId().compareTo(doc2.getId());
            }
        });

        for (DesignDocument doc : designDocs) {
            // Put each design document and set the revision for equality comparison later
            doc.setRevision(designManager.put(doc).getRev());
        }

        assertEquals(designDocs, designManager.list(), "The retrieved list of design documents " +
                "should match the expected list");
    }

    /**
     * Test that for javascript language MR view (i.e. the map function is a JSON string) that the
     * map function is correctly deserialized as a Java String.
     * The javascript string must be enclosed in \", for compatibility with JSON objects that might
     * be stored in maps we have to internally switch between the two, but the library did not
     * originally deal with both cases so for backwards compatibility must return the JSON strings
     * without the leading and trailing " when they are java strings.
     *
     * @throws Exception
     * @see #serializeJavascriptView()
     * @see #serializeQueryDesignDoc()
     * @see #deserializeQueryDesignDoc()
     */
    @Test
    public void deserializeJavascriptView() throws Exception {
        DesignDocument queryDDoc = fileToDesignDocument("example");
        Map<String, DesignDocument.MapReduce> views = queryDDoc.getViews();
        for (DesignDocument.MapReduce mrView : views.values()) {
            assertFalse(mrView.getMap().startsWith("\""), "The map function should not start with" +
                    " \"");
            assertFalse(mrView.getMap().endsWith("\""), "The map function should not end with \"");
        }
    }

    /**
     * Test that when setting a javascript map function it is correctly serialized and deserialized.
     *
     * @throws Exception
     * @see #deserializeJavascriptView()
     * @see #serializeQueryDesignDoc()
     * @see #deserializeQueryDesignDoc()
     */
    @Test
    public void serializeJavascriptView() throws Exception {
        // Create and write a design document with a javascript lang map function
        String testDDocName = "testJSMapFn";
        String mapFunction = "function(doc){emit([doc.contentArray[0].boolean,doc.contentArray[0]" +
                ".creator,doc.contentArray[0].created],doc);}";
        DesignDocument ddoc = new DesignDocument();
        ddoc.setId(testDDocName);
        Map<String, DesignDocument.MapReduce> views = new HashMap<String, DesignDocument
                .MapReduce>();
        DesignDocument.MapReduce mr = new DesignDocument.MapReduce();
        mr.setMap(mapFunction);
        mr.setReduce("_count");
        views.put("testView", mr);
        ddoc.setViews(views);
        Response r = designManager.put(ddoc);

        // Retrieve the doc and check that the javascript function is correct
        DesignDocument retrievedDDoc = designManager.get(testDDocName, r.getRev());
        assertNotNull(retrievedDDoc, "There should be a retrieved design doc");
        Map<String, DesignDocument.MapReduce> retrievedViews = retrievedDDoc.getViews();
        assertNotNull(retrievedViews, "There should be views defined on the design doc");
        DesignDocument.MapReduce mrView = retrievedViews.get("testView");
        assertNotNull(mrView, "There should be a testView in the retrieved design doc");
        assertEquals(mapFunction, mrView.getMap(), "The map function string should be the " +
                "expected string");
    }

    /**
     * Test that if a query language design document is serialized the map function is a string
     * form of the JSON object.
     *
     * @throws Exception
     * @see #serializeJavascriptView()
     * @see #deserializeJavascriptView()
     * @see #deserializeQueryDesignDoc()
     */
    @Test
    public void serializeQueryDesignDoc() throws Exception {
        DesignDocument queryDDoc = fileToDesignDocument("query");
        Map<String, DesignDocument.MapReduce> views = queryDDoc.getViews();
        assertEquals(1, views.size(), "There should be one view");
        for (DesignDocument.MapReduce mrView : views.values()) {
            assertTrue(mrView.getMap().startsWith("{"), "The map function should be a javascript " +
                    "function in a JSON form, " + "so start with {");
            assertTrue(mrView.getMap().endsWith("}"), "The map function should be a javascript " +
                    "function in a JSON form, " + "so end with }");
            assertEquals("{\"fields\":{\"Person_dob\":\"asc\"}}", mrView.getMap(), "The map " +
                    "function string should be an object form");
        }
    }

    /**
     * Test that deserializing a query language design document results in the correct form of the
     * map function.
     *
     * @throws Exception
     * @see #serializeJavascriptView()
     * @see #deserializeJavascriptView()
     * @see #serializeQueryDesignDoc()
     */
    @Test
    public void deserializeQueryDesignDoc() throws Exception {
        // Put the query design document
        designManager.put(fileToDesignDocument("query"));
        // Get the query design document
        DesignDocument queryDDoc = designManager.get("testQuery");
        Map<String, DesignDocument.MapReduce> views = queryDDoc.getViews();
        assertEquals(1, views.size(), "There should be one view");
        for (DesignDocument.MapReduce mrView : views.values()) {
            assertTrue(mrView.getMap().startsWith("{"), "The map function should be a javascript " +
                    "function in a JSON form, " + "so start with {");
            assertTrue(mrView.getMap().endsWith("}"), "The map function should be a javascript " +
                    "function in a JSON form, " + "so end with }");
            assertEquals("{\"fields\":{\"Person_dob\":\"asc\"}}", mrView.getMap(), "The map " +
                    "function string should be an object form");
        }
    }
}
