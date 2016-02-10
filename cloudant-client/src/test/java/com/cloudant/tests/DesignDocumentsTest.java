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

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.DesignDocumentManager;
import com.cloudant.client.api.Replication;
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.views.AllDocsRequest;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.MockWebServerResources;
import com.cloudant.tests.util.Utils;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Category(RequiresDB.class)
public class DesignDocumentsTest {

    @ClassRule
    public static CloudantClientResource clientResource = new CloudantClientResource();
    @Rule
    public DatabaseResource dbResource = new DatabaseResource(clientResource);
    @ClassRule
    public static MockWebServer mockWebServer = new MockWebServer();

    private Database db;
    private CloudantClient account;
    private File rootDesignDir;
    private DesignDocument designDocExample;
    private DesignDocumentManager designManager;

    @Before
    public void setUp() throws Exception {
        account = clientResource.get();
        db = dbResource.get();
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
    private DesignDocument fileToDesignDocument(String name) throws Exception {
        File testDesignDocFile = new File(String.format("%s/%s_design_doc.js", rootDesignDir, name));
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

        assertEquals("The design document retrieved should equal ", exampleDoc, designDoc11);
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
            assertNotNull("", designManager.get(id));
        }
    }

    /**
     * Tests that it is possible to add the dbcopy option to a view.
     * <OL>
     * <LI>Replicates animaldb.</LI>
     * <LI>Modifies the diet_count view to have the dbcopy option set.</LI>
     * <LI>Queries the diet_count view to ensure index creation.</LI>
     * <LI>Asserts that the dbcopy database was created.</LI>
     * <LI>Asserts that the dbcopy database contained 3 docs.</LI>
     * </OL>
     */
    @Test
    @Category(RequiresCloudant.class)
    public void dbCopy() throws Exception {
        //replicate animaldb into our test database
        Replication r = account.replication();
        r.source("https://clientlibs-test.cloudant.com/animaldb");
        r.target(dbResource.getDbURIWithUserInfo());
        r.trigger();

        String copiedDbName = "reducedanimaldb" + Utils.generateUUID();

        //find the diet_count map reduce view and set the dbcopy value
        DesignDocument dd = db.getDesignDocumentManager().get("_design/views101");
        for (Map.Entry<String, DesignDocument.MapReduce> view
                : dd.getViews().entrySet()) {
            if (view.getKey().equals("diet_count")) {
                DesignDocument.MapReduce dietCount = view
                        .getValue();
                dietCount.setDbCopy(copiedDbName);
            }
        }

        //put the new version back in the database
        Response response = db.getDesignDocumentManager().put(dd);
        assertNotNull("The design document should have been saved", response);

        try {

            //query the diet_count view to ensure the indexes are built
            int count = db.getViewRequestBuilder("views101", "diet_count").newRequest(Key.Type
                    .STRING, Integer.class).build().getSingleValue();

            assertEquals("There should be five records", 5, count);

            //assert that the db copied into does exist
            Database copied = account.database(copiedDbName, false);
            assertNotNull("The copied database should not be null", copied);

            //check the number of documents in the copied database is as expected
            AllDocsRequest docsRequest = copied.getAllDocsRequestBuilder().build();
            List<String> reducedDocIds;
            //The copied database documents are subject to eventual consistency across nodes so we
            // need an awkward retry loop to try and prevent the test intermittently failing when
            // the internal replication between nodes has not completed. Allow up to 2 minutes and
            // try every second.
            long timeout = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2);
            do {
                Thread.sleep(1000);
                reducedDocIds = docsRequest.getResponse().getDocIds();
            }
            while (System.currentTimeMillis() < timeout && (reducedDocIds == null ||
                    reducedDocIds.size() < 3));
            assertNotNull("The list of docs should not be null", reducedDocIds);
            assertEquals("There should be 3 documents (herbivore, carnivore, omnivore)", 3,
                    reducedDocIds.size());

        } finally {
            //clean up the copied db (the original db will be cleaned up in the dbResource clean up)
            account.deleteDB(copiedDbName);
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
        assertNotNull("The design doc should be retrieved without a _design prefix",
                designManager.get("example"));
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
        assertNotNull("The design doc should be retrieved without a _design prefix",
                designManager.get("example", r.getRev()));
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
        assertNotNull("The design doc should be retrievable with a _design prefix",
                designManager.get("_design/example"));
    }

    /**
     * Test that a design document with an index can be deleted.
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
    @Test(expected = CouchDbException.class)
    public void couchDbExceptionIfIOExceptionDuringDDocRemove() throws Exception {
        CloudantClient mockClient = CloudantClientHelper.newMockWebServerClientBuilder
                (mockWebServer).readTimeout(50, TimeUnit.MILLISECONDS).build();
        // Cause a read timeout to generate an IOException
        mockWebServer.setDispatcher(new MockWebServerResources.SleepingDispatcher(100, TimeUnit
                .MILLISECONDS) {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if ("HEAD".equals(request.getMethod())) {
                    return super.dispatch(request);
                } else {
                    return new MockResponse();
                }
            }
        });
        Database database = mockClient.database(dbResource.getDatabaseName(), false);
        // Try to remove a design document by id only, generates a HEAD request for revision info
        database.getDesignDocumentManager().remove("example");
    }

    /**
     * Test that a CouchDbException is thrown
     *
     * @throws Exception
     */
    @Test(expected = CouchDbException.class)
    public void couchDbExceptionIfNoETagOnDDocRemove() throws Exception {
        CloudantClient mockClient = CloudantClientHelper.newMockWebServerClientBuilder
                (mockWebServer).build();
        Database database = mockClient.database(dbResource.getDatabaseName(), false);
        // Queue a mock response with no "ETag" header
        mockWebServer.enqueue(new MockResponse());
        database.getDesignDocumentManager().remove("example");
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

        assertEquals("The retrieved list of design documents should match the expected list",
                designDocs, designManager.list());
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
            assertFalse("The map function should not start with \"", mrView.getMap().startsWith
                    ("\""));
            assertFalse("The map function should not end with \"", mrView.getMap().endsWith("\""));
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
        assertNotNull("There should be a retrieved design doc", retrievedDDoc);
        Map<String, DesignDocument.MapReduce> retrievedViews = retrievedDDoc.getViews();
        assertNotNull("There should be views defined on the design doc", retrievedViews);
        DesignDocument.MapReduce mrView = retrievedViews.get("testView");
        assertNotNull("There should be a testView in the retrieved design doc", mrView);
        assertEquals("The map function string should be the expected string",
                mapFunction, mrView.getMap());
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
        assertEquals("There should be one view", 1, views.size());
        for (DesignDocument.MapReduce mrView : views.values()) {
            assertTrue("The map function should be a javascript function in a JSON form, " +
                    "so start with {", mrView.getMap().startsWith("{"));
            assertTrue("The map function should be a javascript function in a JSON form, " +
                    "so end with }", mrView.getMap().endsWith("}"));
            assertEquals("The map function string should be an object form",
                    "{\"fields\":{\"Person_dob\":\"asc\"}}", mrView.getMap());
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
        assertEquals("There should be one view", 1, views.size());
        for (DesignDocument.MapReduce mrView : views.values()) {
            assertTrue("The map function should be a javascript function in a JSON form, " +
                    "so start with {", mrView.getMap().startsWith("{"));
            assertTrue("The map function should be a javascript function in a JSON form, " +
                    "so end with }", mrView.getMap().endsWith("}"));
            assertEquals("The map function string should be an object form",
                    "{\"fields\":{\"Person_dob\":\"asc\"}}", mrView.getMap());
        }
    }
}
