/*
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

import static com.cloudant.tests.CloudantClientHelper.getReplicationSourceUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.Permissions;
import com.cloudant.client.api.model.Shard;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresCloudantLocal;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.test.main.RequiresCouch;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.cloudant.tests.util.MockWebServerResources;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.net.MalformedURLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresDB
public class DatabaseTest extends TestWithDbPerClass {

    @RegisterExtension
    public static MockWebServerExtension mockWebServerExt = new MockWebServerExtension();

    private static MockWebServer mockWebServer;

    @BeforeEach
    public void beforeEach() {
        mockWebServer = mockWebServerExt.get();
    }

    @BeforeAll
    public static void beforeAll() throws Exception {
        //replicate animaldb for tests
        com.cloudant.client.api.Replication r = account.replication();
        r.source(getReplicationSourceUrl("animaldb"));
        r.createTarget(true);
        r.target(dbResource.getDbURIWithUserInfo());
        r.trigger();
    }

    @Test
    @RequiresCloudantService
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
    @RequiresCouch
    @RequiresCloudantLocal
    public void testPermissionsException() {
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                Map<String, EnumSet<Permissions>> userPerms = db.getPermissions();
            }
        });
    }

    @Test
    public void permissionsParsing() throws Exception {
        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .build();
        Database db = client.database("notarealdb", false);

        // Mock up a request of all permissions
        mockWebServer.enqueue(MockWebServerResources.PERMISSIONS); // for GET _security
        mockWebServer.enqueue(MockWebServerResources.JSON_OK); // for PUT _security
        db.setPermissions("testUsername", EnumSet.allOf(Permissions.class));

        // Mock up a failing request
        String testError = "test error";
        String testReason = "test reason";
        mockWebServer.enqueue(MockWebServerResources.PERMISSIONS); // for GET _security
        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("{\"reason\":\"" +
                testReason + "\", \"error\":\"" + testError + "\"}"));
        try {
            db.setPermissions("testUsername", EnumSet.allOf(Permissions.class));
        } catch (CouchDbException e) {
            assertEquals(testError, e.getError());
            assertEquals(testReason, e.getReason());
        }
    }

    @Test
    @RequiresCloudant
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
    @RequiresCloudant
    public void shard() {
        Shard s = db.getShard("snipe");
        assertNotNull(s);
        assertNotNull(s.getRange());
        assertNotNull(s.getNodes());
        assert (s.getNodes().hasNext());
    }


    @Test
    @RequiresCloudant
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

        CloudantClient account = CloudantClientHelper.getClientBuilder()
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
