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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.model.ReplicatorDocument;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.PreconditionFailedException;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.Utils;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category(RequiresDB.class)
public class ReplicatorTest extends ReplicateBaseTest {

    private String repDocId;

    @BeforeClass
    public static void createReplicatorDB() {
        try {
            clientResource.get().createDB("_replicator");
        } catch (PreconditionFailedException e){
            // 412 - We can swallow this.
        }
    }

    @Before
    public void generateReplicatorDocId() {
        repDocId = Utils.generateUUID();
    }

    @After
    public void cleanUpReplicatorDoc() throws Exception {
        Utils.removeReplicatorTestDoc(account, repDocId);
    }

    @Test
    public void replication() throws Exception {
        Response response = account.replicator()
                .replicatorDocId(repDocId)
                .createTarget(true)
                .source(db1URI)
                .target(db2URI)
                .save();

        // find and remove replicator doc
        ReplicatorDocument repDoc = Utils.waitForReplicatorToComplete(account, response.getId());
        assertTrue("The replicator should reach completed state", "completed".equalsIgnoreCase
                (repDoc.getReplicationState()));
    }

    @Test
    public void replication_filteredWithQueryParams() throws Exception {
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("somekey1", "value 1");

        Response response = account.replicator()
                .createTarget(true)
                .replicatorDocId(repDocId)
                .source(db1URI)
                .target(db2URI)
                .filter("example/example_filter")
                .queryParams(queryParams)
                .save();

        // find and remove replicator doc
        ReplicatorDocument repDoc = Utils.waitForReplicatorToComplete(account, response.getId());
        assertTrue("The replicator should reach completed state", "completed".equalsIgnoreCase
                (repDoc.getReplicationState()));
    }

    @Test
    public void replicatorDB() throws Exception {

        // trigger a replication
        Response response = account.replicator()
                .replicatorDocId(repDocId)
                .source(db1URI)
                .target(db2URI).continuous(true)
                .createTarget(true)
                .save();

        // we need the replication to start before continuing
        Utils.waitForReplicatorToStart(account, response.getId());

        // find all replicator docs
        List<ReplicatorDocument> replicatorDocs = account.replicator()
                .findAll();
        assertThat(replicatorDocs.size(), is(not(0)));

    }

    @Test
    public void replication_conflict() throws Exception {
        String docId = Utils.generateUUID();
        Foo foodb1 = new Foo(docId, "titleX");
        Foo foodb2 = new Foo(docId, "titleY");

        //save Foo(X) in DB1
        db1.save(foodb1);
        //save Foo(Y) in DB2
        db2.save(foodb2);

        //replicate with DB1 with DB2
        Response response = account.replicator().source(db1URI)
                .target(db2URI).replicatorDocId(repDocId)
                .save();

        // we need the replication to finish before continuing
        Utils.waitForReplicatorToComplete(account, response.getId());

        //we replicated with a doc with the same ID but different content in each DB, we should get
        //a conflict
        assertConflictsNotZero(db2);
    }
}
