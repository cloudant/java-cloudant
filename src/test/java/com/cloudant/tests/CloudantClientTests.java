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
import com.cloudant.client.api.model.ConnectOptions;
import com.cloudant.client.api.model.Membership;
import com.cloudant.client.api.model.Task;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.http.AgentHelper;
import com.cloudant.http.interceptors.TimeoutCustomizationInterceptor;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.SimpleHttpServer;
import com.cloudant.tests.util.TestLog;

import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CloudantClientTests {

    @ClassRule
    public static final TestLog TEST_LOG = new TestLog();

    @ClassRule
    public static CloudantClientResource clientResource = new CloudantClientResource();
    private CloudantClient account = clientResource.get();

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

        Membership membership = account.getMembership();
        assertNotNull(membership);
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
    public void testUserAgentHeaderIsAddedToRequest() throws Exception {

        SimpleHttpServer server = new SimpleHttpServer();
        try {
            server.start();
            //wait for the server to be ready
            server.await();

            //instantiating the client performs a single post request
            CloudantClient client = new CloudantClient(server.getUrl(), null, (String) null);
            client.executeRequest(createPost(client.getBaseUri(), null, "application/json"));
            //assert that the request had the expected header
            boolean foundUserAgentHeaderOnRequest = false;
            boolean userAgentHeaderMatchedExpectedForm = false;
            for (String line : server.getLastInputRequestLines()) {
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
        } finally {
            server.stop();
        }
    }

    /**
     * Test a NoDocumentException is thrown when trying an operation on a DB that doesn't exist
     */
    @Test(expected = NoDocumentException.class)
    @Category(RequiresDB.class)
    public void nonExistentDatabaseException() {
        //try and get a DB that doesn't exist
        Database db = account.database("not_really_there", false);
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
            account.createDB("existing");

            //do a get with create true for the already existing DB
            account.database("existing", true);
        } finally {
            //clean up the DB created by this test
            account.deleteDB("existing");
        }
    }

    @Test
    public void testDefaultPorts() {
        CloudantClient c = new CloudantClient("http://192.0.2.0", null, (String) null);
        assertEquals("The http port should be 80", 80, c.getBaseUri().getPort());

        c = new CloudantClient("https://192.0.2.0", null, (String) null);
        assertEquals("The http port should be 443", 443, c.getBaseUri().getPort());
    }

    /**
     * Check that the connection timeout throws a SocketTimeoutException when it can't connect
     * within the timeout.
     */
    @Test(expected = SocketTimeoutException.class)
    public void connectionTimeout() throws Throwable {

        //start a simple http server
        SimpleHttpServer server = new SimpleHttpServer() {

            @Override
            public void start() throws Exception {
                //we don't actually want this server to loop, just create a socket
                //so set finished to true
                finished.set(true);
                super.start();
            }

        };
        server.start();
        server.await();

        //block the single connection to our server
        Socket socket = new Socket();
        socket.connect(server.getSocketAddress());

        //now try to connect, but should timeout because there is no connection available
        try {
            CloudantClient c = new CloudantClient(server.getUrl(), null, (String) null, new
                    ConnectOptions().setConnectionTimeout(new TimeoutCustomizationInterceptor
                    .TimeoutOption(100, TimeUnit.MILLISECONDS)));

            c.createDB("test");
        } catch (CouchDbException e) {
            //unwrap the CouchDbException
            if (e.getCause() != null) {
                //whilst it would be really nice to actually assert that this was a connect
                //exception and not some other SocketTimeoutException there are JVM differences in
                //this respect (i.e. OpenJDK does not appear to distinguish between read/connect)
                //in its exception messages
                throw e.getCause();
            } else {
                throw e;
            }
        } finally {
            //make sure we close the sockets
            IOUtils.closeQuietly(socket);
            server.stop();
        }
    }

    /**
     * Checks that the read timeout works. The test sets a read timeout of 0.25 s and the mock
     * server thread sleeps for twice the duration of the read timeout. If things are working
     * correctly then the client should see a SocketTimeoutException for the read.
     */
    @Test(expected = SocketTimeoutException.class)
    public void readTimeout() throws Throwable {

        final Long READ_TIMEOUT = 250l;

        //start a simple http server
        SimpleHttpServer server = new SimpleHttpServer() {
            @Override
            protected void serverAction(InputStream is, OutputStream os) throws Exception {
                //sleep for longer than the read timeout
                Thread.sleep(READ_TIMEOUT * 2);
            }
        };

        try {
            server.start();
            server.await();

            try {
                CloudantClient c = new CloudantClient(server.getUrl(), null, (String) null, new
                        ConnectOptions().setReadTimeout(new TimeoutCustomizationInterceptor
                        .TimeoutOption(READ_TIMEOUT, TimeUnit.MILLISECONDS)));
                //do a call that expects a response
                c.getAllDbs();
            } catch (CouchDbException e) {
                //unwrap the CouchDbException
                if (e.getCause() != null) {
                    throw e.getCause();
                } else {
                    throw e;
                }
            }
        } finally {
            server.stop();
        }
    }

    /**
     * This tests that a CouchDbException is thrown if the user is null, but the password is
     * supplied.
     */
    @Test(expected = CouchDbException.class)
    public void nullUser() {
        new CloudantClient("http://192.0.2.0", null, ":0-myPassword");
    }

    /**
     * This tests that a CouchDbException is thrown if the user is supplied, but the password is
     * null.
     */
    @Test(expected = CouchDbException.class)
    public void nullPassword() {
        new CloudantClient("http://192.0.2.0", "user", null);
    }
}
