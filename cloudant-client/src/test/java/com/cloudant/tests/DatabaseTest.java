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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.Permissions;
import com.cloudant.client.api.model.Shard;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresCloudantLocal;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.test.main.RequiresCouch;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.google.gson.GsonBuilder;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import java.net.MalformedURLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Category(RequiresDB.class)
public class DatabaseTest {

    public static CloudantClientResource clientResource = new CloudantClientResource();
    public static DatabaseResource dbResource = new DatabaseResource(clientResource);

    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);

    private static Database db;
    private static CloudantClient account;

    @BeforeClass
    public static void setUp() throws Exception {
        account = clientResource.get();
        db = dbResource.get();

        //replicate animaldb for tests
        com.cloudant.client.api.Replication r = account.replication();
        r.source("http://clientlibs-test.cloudant.com/animaldb");
        r.createTarget(true);
        r.target(dbResource.getDbURIWithUserInfo());
        r.trigger();
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
        assertEquals(1, userPerms.size());
        assertEquals(p, userPerms.get(key.getKey()));

        p = EnumSet.noneOf(Permissions.class);
        db.setPermissions(key.getKey(), p);
        userPerms = db.getPermissions();
        assertNotNull(userPerms);
        assertEquals(1, userPerms.size());
        assertEquals(p, userPerms.get(key.getKey()));


    }

    /**
     * Test that when called against a DB that is not a Cloudant service
     * an UnsupportedOperationException is thrown
     */
    @Test(expected = UnsupportedOperationException.class)
    @Category({RequiresCouch.class, RequiresCloudantLocal.class})
    public void testPermissionsException() {
        Map<String, EnumSet<Permissions>> userPerms = db.getPermissions();
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
    public void customGsonDeserializerTest() throws MalformedURLException {
        GsonBuilder builder = new GsonBuilder();
        builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        CloudantClient account  = CloudantClientHelper.getClientBuilder()
                .gsonBuilder(builder)
                .build();

        Database db = account.database(dbResource.getDatabaseName(), false);

        Map<String, Object> h = new HashMap<String, Object>();
        h.put("_id", "serializertest");
        h.put("date", "2015-01-23T18:25:43.511Z");
        db.save(h);

        db.find(Foo.class, "serializertest"); // should not throw a JsonSyntaxException

    }
}
