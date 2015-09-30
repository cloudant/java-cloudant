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

import com.cloudant.client.api.model.ReplicatorDocument;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.Utils;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category(RequiresDB.class)
public class ReplicatorTest extends ReplicateBaseTest {

    //TODO Enable in next PR with Rich's 52593-replicate-tests branch
    @Test
    public void replication() throws Exception {
        Response response = account.replicator()
                .createTarget(true)
                .source(db1URI)
                .target(db2URI)
                .save();

        // find and remove replicator doc
        Utils.waitForReplicatorToComplete(account, response.getId());
        Utils.removeReplicatorTestDoc(account, response.getId());
    }

    @Test
    public void replication_filteredWithQueryParams() throws Exception {
        Map<String, Object> queryParams = new HashMap<String, Object>();
        queryParams.put("somekey1", "value 1");

        Response response = account.replicator()
                .createTarget(true)
                .source(db1URI)
                .target(db2URI)
                .filter("example/example_filter")
                .queryParams(queryParams)
                .save();

        // find and remove replicator doc
        Utils.waitForReplicatorToComplete(account, response.getId());
        Utils.removeReplicatorTestDoc(account, response.getId());
    }

    @Test
    public void replicatorDB() throws Exception {
        String version = account.serverVersion();
        if (version.startsWith("0") || version.startsWith("1.0")) {
            return;
        }

        // trigger a replication
        Response response = account.replicator()
                .source(db1URI)
                .target(db2URI).continuous(true)
                .createTarget(true)
                .save();

        // we need the replication to finish before continuing
        Utils.waitForReplicatorToStart(account, response.getId());

        // find all replicator docs
        List<ReplicatorDocument> replicatorDocs = account.replicator()
                .findAll();
        assertThat(replicatorDocs.size(), is(not(0)));

        // find and remove replicator doc
        Utils.removeReplicatorTestDoc(account, response.getId());
    }

    @Test
    public void replication_conflict() throws Exception {
        String docId = Utils.generateUUID();
        Foo foodb1 = new Foo(docId, "title");
        Foo foodb2 = null;

        foodb1 = new Foo(docId, "titleX");

        db1.save(foodb1);

        Response response = account.replicator().source(db1URI)
                .target(db2URI).replicatorDocId(docId)
                .save();

        // we need the replication to finish before continuing
        Utils.waitForReplicatorToComplete(account, response.getId());

        foodb2 = Utils.findDocumentWithRetries(db2, docId, Foo.class, 20);
        foodb2.setTitle("titleY");
        db2.update(foodb2);

        foodb1 = Utils.findDocumentWithRetries(db1, docId, Foo.class, 20);
        foodb1.setTitle("titleZ");
        db1.update(foodb1);

        Response secondResponse = account.replicator().source(db1URI)
                .target(db2URI).save();

        // we need the replication to finish before continuing
        Utils.waitForReplicatorToComplete(account, secondResponse.getId());

        ViewResponse<Key.ComplexKey, String> conflicts = db2.getViewRequestBuilder
                ("conflicts", "conflict").newRequest(Key.Type.COMPLEX, String.class).build()
                .getResponse();

        assertThat(conflicts.getRows().size(), is(not(0)));

        // find and remove replicator doc
        Utils.removeReplicatorTestDoc(account, response.getId());
        Utils.removeReplicatorTestDoc(account, secondResponse.getId());
    }
}
