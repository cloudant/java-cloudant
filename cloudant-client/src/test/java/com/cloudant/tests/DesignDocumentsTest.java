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

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.DesignDocumentManager;
import com.cloudant.client.api.Replication;
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.views.AllDocsRequest;
import com.cloudant.client.api.views.Key;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.Utils;
import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Category(RequiresDB.class)
public class DesignDocumentsTest {

    private static CloudantClientResource clientResource = new CloudantClientResource();
    private static DatabaseResource dbResource = new DatabaseResource(clientResource);
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);

    private static Database db;
    private CloudantClient account;
    private File rootDesignDir;
    private File designDocExample;
    private DesignDocumentManager designManager;

    @Before
    public void setUp() {
        account = clientResource.get();
        db = dbResource.get();
        rootDesignDir = new File(System.getProperty("user.dir")
                + "/src/test/resources/design-files");
        designDocExample = new File(String.format("%s/example_design_doc.js", rootDesignDir));
        designManager = db.getDesignDocumentManager();
    }

    @Test
    public void designDocSync() throws Exception {
        DesignDocument designDoc = DesignDocumentManager.fromFile(designDocExample);

        db.getDesignDocumentManager().put(designDoc);
    }

    @Test
    public void designDocCompare() throws Exception {
        DesignDocument designDoc1 = DesignDocumentManager.fromFile(designDocExample);
        designManager.put(designDoc1);

        DesignDocument designDoc11 = db.getDesignDocumentManager().get("_design/example");

        assertEquals(designDoc1, designDoc11);
    }

    @Test
    public void updateDesignDocIndex() throws Exception {
        DesignDocument designDoc1 = DesignDocumentManager.fromFile(
                new File(String.format("%s/views101_design_doc.js", rootDesignDir)));

        designDoc1.setId("MyAmazingDdoc");
        JsonObject indexes = designDoc1.getIndexes();
        designDoc1.setIndexes(null);

        Response response = designManager.put(designDoc1);
        designDoc1.setRevision(response.getRev());


        designDoc1.setIndexes(indexes);
        response = designManager.put(designDoc1);

        Assert.assertEquals(2, response.getStatusCode() / 100);

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

        DesignDocument[] docArray = designDocs.toArray(new DesignDocument[designDocs.size()]);
        designManager.put(docArray);

        assertThat(designDocs.size(), not(0));
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

}
