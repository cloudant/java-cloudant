package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ReplicatorDocument;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.model.ViewResult;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.Utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Category(RequiresDB.class)
public class ReplicatorTest {

    private static Properties props;
    private static Database db1;

    private static Database db2;

    private static String db1URI;
    private static String db2URI;
    private CloudantClient account;

    @Before
    public void setUp() {
        account = CloudantClientHelper.getClient();
        db1 = account.database("lightcouch-db-test", true);
        db1URI = CloudantClientHelper.SERVER_URI.toString() + "/lightcouch-db-test";


        db2 = account.database("lightcouch-db-test-2", true);
        db1.syncDesignDocsWithDb();
        db2.syncDesignDocsWithDb();


        db2URI = CloudantClientHelper.SERVER_URI.toString() + "/lightcouch-db-test-2";

    }

    @After
    public void tearDown() {
        account.deleteDB("lightcouch-db-test");
        account.deleteDB("lightcouch-db-test-2");
        account.shutdown();
    }

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

        foodb2 = db2.find(Foo.class, docId);
        foodb2.setTitle("titleY");
        db2.update(foodb2);

        foodb1 = db1.find(Foo.class, docId);
        foodb1.setTitle("titleZ");
        db1.update(foodb1);

        Response secondResponse = account.replicator().source(db1URI)
                .target(db2URI).save();

        // we need the replication to finish before continuing
        Utils.waitForReplicatorToComplete(account, secondResponse.getId());

        ViewResult<String[], String, Foo> conflicts = db2.view("conflicts/conflict")
                .includeDocs(true).queryView(String[].class, String.class, Foo.class);

        assertThat(conflicts.getRows().size(), is(not(0)));

        // find and remove replicator doc
        Utils.removeReplicatorTestDoc(account, response.getId());
        Utils.removeReplicatorTestDoc(account, secondResponse.getId());
    }
}
