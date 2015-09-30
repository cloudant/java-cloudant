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

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.createPost;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.Membership;
import com.cloudant.client.api.model.Task;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.http.AgentHelper;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.SingleRequestHttpServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class CloudantClientTests {

    private static final Log log = LogFactory.getLog(CloudantClientTests.class);

    public static CloudantClient cookieBasedClient;
    private CloudantClient account;

    @Before
    public void setUp() {
        account = CloudantClientHelper.getClient();

        //TODO review in next PR with cookie interceptor changes
        if (CloudantClientHelper.COUCH_PASSWORD == null) {
            cookieBasedClient = account;
        } else {
            cookieBasedClient = new CloudantClient(account.getBaseUri().toString(),
                    CloudantClientHelper.COUCH_USERNAME, CloudantClientHelper.COUCH_PASSWORD);
        }

    }


    @After
    public void tearDown() {
        account.shutdown();
        cookieBasedClient.shutdown();
    }

    @Test
    @Category(RequiresCloudantService.class)
    public void apiKey() {
        ApiKey key = account.generateApiKey();
        assertNotNull(key);
        assertNotNull(key.getKey());
        assertNotNull(key.getPassword());
    }

    @Test
    @Category(RequiresCloudant.class)
    public void activeTasks() {
        List<Task> tasks = account.getActiveTasks();
        assertNotNull(tasks);
    }

    @Test
    @Category(RequiresCloudant.class)
    public void membership() {
        Membership mship = account.getMembership();
        assertNotNull(mship);
        assertNotNull(mship.getClusterNodes());
        assertNotNull(mship.getClusterNodes().hasNext());
        assertNotNull(mship.getAllNodes());
        assertNotNull(mship.getAllNodes().hasNext());
    }

    @Test
    @Category(RequiresCloudant.class)
    public void cookieTest() {

        Membership membership = cookieBasedClient.getMembership();
        assertNotNull(membership);
    }

    //TODO Enable in next PR with cookie interceptor changes
    @Ignore
    @Test
    @Category(RequiresDB.class)
    public void cookieNegativeTest() {
        String cookie = "";//account.getCookie() + "XXX";
        boolean exceptionRaised = true;
        try {
            new CloudantClient(
                    CloudantClientHelper.SERVER_URI.toString(), cookie).getAllDbs();
            exceptionRaised = false;
        } catch (CouchDbException e) {
            if (e.getMessage().contains("Forbidden")) {
                exceptionRaised = true;
            }
        }
        if (exceptionRaised == false) {
            Assert.fail("could connect to cloudant with random AuthSession cookie");
        }
    }

    private final String userAgentRegex = "java-cloudant/[^\\s]+ " +
            "\\[Java \\([^;]*;[^;]*;[^;]*\\) [^;]*;[^;]*;" +
            "[^;]*\\]";

    /**
     * Assert that the User-Agent header is of the expected form.
     */
    @Test
    public void testUserAgentHeaderString() {
        assertTrue("The value of the User-Agent header should match the format " +
                "\"java-cloudant/version [Java (os.arch; os.name; os.version) jvm.vendor; jvm" +
                ".version; jvm.runtime.version]\"", AgentHelper.USER_AGENT.matches
                (userAgentRegex));
    }

    /**
     * Assert that requests have the User-Agent header added. This test runs a local HTTP server
     * process that can handle a single request to receive the request and validate the header.
     */
    @Test
    public void testUserAgentHeaderIsAddedToRequest() {

        int serverPort = 54321;
        SingleRequestHttpServer server = SingleRequestHttpServer.startServer(serverPort);
        //wait for the server to be ready
        server.waitForServer();

        //instantiating the client performs a single post request
        CloudantClient client = new CloudantClient("http://localhost:" + serverPort, "", "");
        client.executeRequest(createPost(client.getBaseUri(), null, "application/json"));
        //assert that the request had the expected header
        boolean foundUserAgentHeaderOnRequest = false;
        boolean userAgentHeaderMatchedExpectedForm = false;
        for (String line : server.getRequestInput()) {
            if (line.contains("User-Agent")) {
                foundUserAgentHeaderOnRequest = true;
                if (line.matches(".*" + userAgentRegex)) {
                    userAgentHeaderMatchedExpectedForm = true;
                }
            }
        }
        assertTrue("The User-Agent header should be present on the request",
                foundUserAgentHeaderOnRequest);
        assertTrue("The value of the User-Agent header value on the request should match the " +
                        "format " +
                        "\"java-cloudant/version [Java (os.arch; os.name; os.version) jvm" +
                        ".vendor; jvm" +
                        ".version; jvm.runtime.version]\"",
                userAgentHeaderMatchedExpectedForm);

    }

    /**
     * Test a NoDocumentException is thrown when trying an operation on a DB that doesn't exist
     */
    @Test(expected = NoDocumentException.class)
    @Category(RequiresDB.class)
    public void nonExistentDatabaseException() {
        //try and get a DB that doesn't exist
        Database db = cookieBasedClient.database("not_really_there", false);
        //try an operation against the non-existant DB
        db.info();
    }

    /**
     * Validate that no exception bubbles up when trying to create a DB that already exists
     */
    @Test
    @Category(RequiresDB.class)
    public void existingDatabaseCreateException() {
        try {
            //create a DB for this test
            cookieBasedClient.createDB("existing");

            //do a get with create true for the already existing DB
            cookieBasedClient.database("existing", true);
        } finally {
            //clean up the DB created by this test
            cookieBasedClient.deleteDB("existing");
        }
    }

    @Test
    public void testDefaultPorts() {
        CloudantClient c = new CloudantClient("http://192.0.2.0", null, (String) null);
        assertEquals("The http port should be 80", 80, c.getBaseUri().getPort());

        c = new CloudantClient("https://192.0.2.0", null, (String) null);
        assertEquals("The http port should be 443", 443, c.getBaseUri().getPort());
    }

}
