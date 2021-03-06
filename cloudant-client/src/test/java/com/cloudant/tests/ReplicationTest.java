/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright © 2015, 2021 IBM Corp. All rights reserved.
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.model.ReplicationResult;
import com.cloudant.client.api.model.ReplicationResult.ReplicationHistory;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithReplication;
import com.cloudant.tests.util.Utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresDB
public class ReplicationTest extends TestWithReplication {

    @Test
    public void replication() {
        ReplicationResult result = db1Resource.appendReplicationAuth(account.replication()
                .createTarget(true)
                .source(db1URI)
                .target(db2URI)
        )
                .trigger();

        assertTrue(result.isOk(), "The replication should complete ok");

        List<ReplicationHistory> histories = result.getHistories();
        assertThat(histories.size(), not(0));
    }

    @Test
    public void replication_filteredWithQueryParams() {
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("somekey1", "value 1");

        ReplicationResult result = db1Resource.appendReplicationAuth(account.replication()
                .createTarget(true)
                .source(db1URI)
                .target(db2URI)
                .filter("example/example_filter")
                .queryParams(queryParams)
        )
                .trigger();

        assertTrue(result.isOk(), "The replication should complete ok");
    }

    @Test
    public void replicateDatabaseUsingReplicationTrigger() throws Exception {

        String docId = Utils.generateUUID();
        Foo fooOnDb1 = new Foo(docId);
        db1.save(fooOnDb1);

        // trigger a replication
        ReplicationResult result =
                db1Resource.appendReplicationAuth(account.replication().source(db1URI)
                .target(db2URI).createTarget(true)).trigger();

        assertTrue(result.isOk(), "The replication should complete ok");

        Foo fooOnDb2 = Utils.findDocumentWithRetries(db2, docId, Foo.class, 20);
        assertNotNull(fooOnDb2, "The document should have been replicated");
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
        ReplicationResult result =
                db1Resource.appendReplicationAuth(account.replication().source(db1URI)
                .target(db2URI)).trigger();

        assertTrue(result.isOk(), "The replication should complete ok");

        //we replicated with a doc with the same ID but different content in each DB, we should get
        //a conflict
        assertConflictsNotZero(db2);
    }

    @Test
    public void replication_since_seq() throws Exception {
        String seq = db1.changes().getChanges().getResults().get(2).getSeq();
        ReplicationResult result = db1Resource.appendReplicationAuth(account.replication()
            .createTarget(true)
            .source(db1URI)
            .target(db2URI)
            .sinceSeq(seq)
        )
            .trigger();

        assertTrue(result.isOk(), "The replication should complete ok");

        List<ReplicationHistory> histories = result.getHistories();
        assertThat(histories.size(), not(0));
    }

    @Test
    public void replication_last_seq() throws Exception {
        String lastSeq = db1Resource.get().changes().getChanges().getLastSeq();

        ReplicationResult result = db1Resource.appendReplicationAuth(account.replication()
            .createTarget(true)
            .source(db1URI)
            .target(db2URI)
            .sinceSeq(lastSeq)
        )
            .trigger();

        assertTrue(result.isOk(), "The replication should complete ok");

        List<ReplicationHistory> histories = result.getHistories();
        assertThat(histories.size(), not(0));
    }
}
