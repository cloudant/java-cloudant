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
import com.cloudant.tests.util.SingleRequestHttpServer;
import com.cloudant.tests.util.TestLog;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ServerSocketFactory;

public class CloudantClientTests {

    @ClassRule
    public static final TestLog TEST_LOG = new TestLog();

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
    public void testUserAgentHeaderIsAddedToRequest() throws Exception {

        int serverPort = 54321;
        SingleRequestHttpServer server = SingleRequestHttpServer.startServer(serverPort);
        //wait for the server to be ready
        server.waitForServer();

        //instantiating the client performs a single post request
        CloudantClient client = new CloudantClient("http://localhost:" + serverPort, null,
                (String) null);
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

        server.waitForShutdown();
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

    /**
     * Check that the connection timeout throws a SocketTimeoutException when it can't connect
     * within the timeout.
     */
    @Test(expected=SocketTimeoutException.class)
    public void connectionTimeout() throws Throwable {

        //create a socket on port 54321 with only 1 backlog
        ServerSocket serverSocket = new ServerSocket(54321, 1);
        //block the socket
        Socket socket = new Socket();
        socket.connect(serverSocket.getLocalSocketAddress());

        try {
            CloudantClient c = new CloudantClient("http://localhost:54321", null, (String) null, new
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
            IOUtils.closeQuietly(serverSocket);
            IOUtils.closeQuietly(socket);
        }
    }

    /**
     * Checks that the read timeout works. The test sets a read timeout of 0.25 s and the mock
     * server waits 0.5 s before continuing to send data down the stream so the timeout should be
     * triggered.
     */
    @Test(expected=SocketTimeoutException.class)
    public void readTimeout() throws Throwable {

        final CountDownLatch startupLatch = new CountDownLatch(1);
        final CountDownLatch cleanupLatch = new CountDownLatch(1);

        Thread server = new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket serverSocket = null;
                Socket socket = null;
                BufferedWriter w = null;
                try {
                    //create a socket on port 54321
                    serverSocket = ServerSocketFactory.getDefault()
                            .createServerSocket(54321);
                    startupLatch.countDown();
                    //wait for a connection
                    socket = serverSocket.accept();

                    // Just send a simple success response.
                    w = new BufferedWriter(new OutputStreamWriter(socket
                            .getOutputStream()));
                    w.write("HTTP/1.0 200 OK");
                    w.flush();
                    //now sleep for longer than the timeout
                    Thread.sleep(500);
                } catch (Throwable e) {
                    TEST_LOG.logger.log(Level.SEVERE, "Exception in readTimeout test server", e);
                } finally {
                    //make sure we count down to free up the test if something went wrong
                    startupLatch.countDown();
                    //close resources
                    IOUtils.closeQuietly(w);
                    IOUtils.closeQuietly(socket);
                    IOUtils.closeQuietly(serverSocket);
                    cleanupLatch.countDown();
                }
            }
        });
        //start the server
        server.start();
        //wait for the server to be listening for connections
        startupLatch.await();

        try {
            CloudantClient c = new CloudantClient("http://localhost:54321", null, (String) null, new
                    ConnectOptions().setReadTimeout(new TimeoutCustomizationInterceptor
                    .TimeoutOption(250, TimeUnit.MILLISECONDS)));
            //do a call that expects a response
            c.getAllDbs();
        } catch (CouchDbException e) {
            //unwrap the CouchDbException
            if (e.getCause() != null) {
                throw e.getCause();
            } else {
                throw e;
            }
        } finally {
            //wait for the other thread to cleanup before moving to the next test
            cleanupLatch.await();
        }
    }
}
