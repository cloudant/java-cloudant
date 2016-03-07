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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.model.ReplicationResult;
import com.cloudant.client.api.model.ReplicationResult.ReplicationHistory;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.Utils;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category(RequiresDB.class)
public class ReplicationTest extends ReplicateBaseTest {

    @Test
    public void replication() {
        ReplicationResult result = account.replication()
                .createTarget(true)
                .source(db1URI)
                .target(db2URI)
                .trigger();

        assertTrue("The replication should complete ok", result.isOk());

        List<ReplicationHistory> histories = result.getHistories();
        assertThat(histories.size(), not(0));
    }

    @Test
    public void replication_filteredWithQueryParams() {
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("somekey1", "value 1");

        ReplicationResult result = account.replication()
                .createTarget(true)
                .source(db1URI)
                .target(db2URI)
                .filter("example/example_filter")
                .queryParams(queryParams)
                .trigger();

        assertTrue("The replication should complete ok", result.isOk());
    }

    @Test
    public void replicateDatabaseUsingReplicationTrigger() throws Exception {

        String docId = Utils.generateUUID();
        Foo fooOnDb1 = new Foo(docId);
        db1.save(fooOnDb1);

        // trigger a replication
        ReplicationResult result = account.replication().source(db1URI)
                .target(db2URI).createTarget(true).trigger();

        assertTrue("The replication should complete ok", result.isOk());

        Foo fooOnDb2 = Utils.findDocumentWithRetries(db2, docId, Foo.class, 20);
        assertNotNull("The document should have been replicated", fooOnDb2);
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
        ReplicationResult result = account.replication().source(db1URI)
                .target(db2URI).trigger();

        assertTrue("The replication should complete ok", result.isOk());

        //we replicated with a doc with the same ID but different content in each DB, we should get
        //a conflict
        assertConflictsNotZero(db2);
    }
}
