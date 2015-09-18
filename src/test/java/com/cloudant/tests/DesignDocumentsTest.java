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
import com.cloudant.client.api.Replication;
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.views.Key;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresDB;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.Map;

@Category(RequiresDB.class)
public class DesignDocumentsTest {
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
        account.shutdown();
    }


    @Test
    public void designDocSync() {
        DesignDocument designDoc = db.design().getFromDesk("example");
        db.design().synchronizeWithDb(designDoc);
    }

    @Test
    public void designDocCompare() {
        DesignDocument designDoc1 = db.design().getFromDesk("example");
        db.design().synchronizeWithDb(designDoc1);

        DesignDocument designDoc11 = db.design().getFromDb("_design/example");

        assertEquals(designDoc1, designDoc11);
    }

    @Test
    public void designDocs() {
        List<DesignDocument> designDocs = db.design().getAllFromDesk();
        db.syncDesignDocsWithDb();

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
        r.target(CloudantClientHelper.SERVER_URI.toString() + "/lightcouch-db-test");
        r.trigger();

        String copiedDbName = "reducedanimaldb";

        //find the diet_count map reduce view and set the dbcopy value
        DesignDocument dd = db.design().getFromDb("_design/views101");
        for (Map.Entry<String, DesignDocument.MapReduce> view
                : dd.getViews().entrySet()) {
            if (view.getKey().equals("diet_count")) {
                DesignDocument.MapReduce dietCount = view
                        .getValue();
                dietCount.setDbCopy(copiedDbName);
            }
        }

        try {
            //put the new version back in the database
            db.design().synchronizeWithDb(dd);

            //query the diet_count view to ensure the indexes are built
            db.getViewRequestBuilder("views101", "diet_count").newRequest(Key.Type.STRING,
                    Integer.class).build().getSingleValue();

            //assert that the db copied into has a result in it
            Database copied = account.database(copiedDbName, false);
            assertNotNull("The copied database should not be null", copied);
            List<String> reducedDocIds = copied.getAllDocsRequestBuilder().build().getResponse()
                    .getDocIds();
            assertNotNull("The list of docs should not be null", reducedDocIds);
            assertEquals("There should be 3 documents (herbivore, carnivore, omnivore)", 3,
                    reducedDocIds.size());

        } finally {
            //clean up the copied db (lightcouch-db-test will be cleaned up in tearDown)
            account.deleteDB(copiedDbName);
        }
    }

}
