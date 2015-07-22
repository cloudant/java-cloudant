package com.cloudant.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.Permissions;
import com.cloudant.client.api.model.Shard;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresCloudantService;
import com.google.gson.GsonBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseTest {
    private static final Log log = LogFactory.getLog(DatabaseTest.class);
    private static Database db;
    private CloudantClient account;

    @Before
    public void setUp() {
        account = CloudantClientHelper.getClient();
        // replciate the animals db for search tests
        com.cloudant.client.api.Replication r = account.replication();
        r.source("https://examples.cloudant.com/animaldb");
        r.createTarget(true);
        r.target(CloudantClientHelper.SERVER_URI.toString() + "/animaldb");
        r.trigger();
        db = account.database("animaldb", false);
    }

    @After
    public void tearDown() {
        account.deleteDB("animaldb");
        account.shutdown();
    }


    @Test
    @Category(RequiresCloudantService.class)
    public void permissions() {
        Map<String, EnumSet<Permissions>> userPerms = db.getPermissions();
        assertNotNull(userPerms);
        ApiKey key = account.generateApiKey();
        EnumSet<Permissions> p = EnumSet.<Permissions>of(Permissions._reader, Permissions._writer);
        db.setPermissions(key.getKey(), p);
        userPerms = db.getPermissions();
        assertNotNull(userPerms);
        assertEquals(userPerms.size(), 1);
        assertEquals(userPerms.get(key.getKey()), p);

        p = EnumSet.noneOf(Permissions.class);
        db.setPermissions(key.getKey(), p);
        userPerms = db.getPermissions();
        assertNotNull(userPerms);
        assertEquals(userPerms.size(), 1);
        assertEquals(userPerms.get(key.getKey()), p);


    }

    /**
     * Test that when called against a DB that is not a Cloudant service
     * an UnsupportedOperationException is thrown
     */
    @Test
    public void testPermissionsException() {
        try {
            Map<String, EnumSet<Permissions>> userPerms = db.getPermissions();
            //ignore the test in cases where we don't get an exception
            Assume.assumeTrue(false);
        }catch(UnsupportedOperationException e){
            //this is the exception we want; expected for non-Cloudant service
            //if another exception is thrown the test will fail
        }
    }

    @Test
    @Category(RequiresCloudant.class)
    public void shards() {
        List<Shard> shards = db.getShards();
        assert (shards.size() > 0);
        for (Shard s : shards) {
            assertNotNull(s.getRange());
            assertNotNull(s.getNodes());
            assertNotNull(s.getNodes().hasNext());
        }
    }

    @Test
    @Category(RequiresCloudant.class)
    public void shard() {
        Shard s = db.getShard("snipe");
        assertNotNull(s);
        assertNotNull(s.getRange());
        assertNotNull(s.getNodes());
        assert (s.getNodes().hasNext());
    }


    @Test
    @Category(RequiresCloudant.class)
    public void QuorumTests() {

        db.save(new Animal("human"), 2);
        Animal h = db.find(Animal.class, "human", new com.cloudant.client.api.model.Params()
                .readQuorum(2));
        assertNotNull(h);
        assertEquals("human", h.getId());

        db.update(h.setClass("inhuman"), 2);
        h = db.find(Animal.class, "human", new com.cloudant.client.api.model.Params().readQuorum
                (2));
        assertEquals("inhuman", h.getclass());

        db.post(new Animal("test"), 2);
        h = db.find(Animal.class, "test", new com.cloudant.client.api.model.Params().readQuorum(3));
        assertEquals("test", h.getId());


    }

    //Test case for issue #31
    @Test
    public void customGsonDeserializerTest() {
        Map<String, Object> h = new HashMap<String, Object>();
        h.put("_id", "serializertest");
        h.put("date", "2015-01-23T18:25:43.511Z");
        db.save(h);

        GsonBuilder builder = new GsonBuilder();
        builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        account.setGsonBuilder(builder);

        db.find(Foo.class, "serializertest"); // should not throw a JsonSyntaxException

    }
}
