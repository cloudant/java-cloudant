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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.Membership;
import com.cloudant.client.api.model.Task;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.library.Version;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.TestLog;
import com.cloudant.tests.util.Utils;
import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;

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
            "\\[Java [^;]*;[^;]*;" +
            "[^;]* \\([^;]*;[^;]*;[^;]*\\)\\]";

    /**
     * Assert that the User-Agent header is of the expected form.
     */
    @Test
    public void testUserAgentHeaderString() {
        assertTrue("The value of the User-Agent header should match the format " +
                "\"java-cloudant/version [Java jvm.vendor; jvm" +
                ".version; jvm.runtime.version (os.arch; os.name; os.version)]\"", new Version().getUserAgentString().matches
                (userAgentRegex));
    }

    /**
     * Assert that requests have the User-Agent header added. This test runs a local HTTP server
     * process that can handle a single request to receive the request and validate the header.
     */
    @Test
    public void testUserAgentHeaderIsAddedToRequest() throws Exception {

        MockWebServer server = new MockWebServer();
        server.start();
        //send back an OK 200
        server.enqueue(new MockResponse());
        try {

            //instantiating the client performs a single post request
            CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(server)
                    .build();
            client.executeRequest(createPost(client.getBaseUri(), null, "application/json"));

            //assert that the request had the expected header
            String userAgentHeader = server.takeRequest(10, TimeUnit.SECONDS)
                    .getHeader("User-Agent");
            assertNotNull("The User-Agent header should be present on the request",
                    userAgentHeader);
            assertTrue("The value of the User-Agent header value on the request should match the " +
                    "format " +
                    "\"java-cloudant/version [Java jvm" +
                    ".vendor; jvm" +
                    ".version; jvm.runtime.version (os.arch; os.name; os.version)]\"",
                    userAgentHeader.matches(userAgentRegex));
        } finally {
            server.shutdown();
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
        String id = Utils.generateUUID();
        String dbName = "existing" + id;
        try {
            //create a DB for this test
            account.createDB(dbName);

            //do a get with create true for the already existing DB
            account.database(dbName, true);
        } finally {
            //clean up the DB created by this test
            account.deleteDB(dbName);
        }
    }

    @Test
    public void testDefaultPorts() throws Exception {
        CloudantClient c = null;

        c = CloudantClientHelper.newTestAddressClient().build();

        assertEquals("The http port should be 80", 80, c.getBaseUri().getPort());


        c = CloudantClientHelper.newHttpsTestAddressClient().build();

        assertEquals("The http port should be 443", 443, c.getBaseUri().getPort());
    }

    /**
     * Check that the connection timeout throws a SocketTimeoutException when it can't connect
     * within the timeout.
     */
    @Test(expected = SocketTimeoutException.class)
    public void connectionTimeout() throws Throwable {

        ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(0, 1);

        //block the single connection to our server
        Socket socket = new Socket();
        socket.connect(serverSocket.getLocalSocketAddress());

        //now try to connect, but should timeout because there is no connection available
        try {
            CloudantClient c = ClientBuilder.url(new URL("http://127.0.0.1:" + serverSocket
                    .getLocalPort()))
                    .connectTimeout(100, TimeUnit.MILLISECONDS).build();

            // Make a request
            c.getAllDbs();
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
     * server thread sleeps for twice the duration of the read timeout. If things are working
     * correctly then the client should see a SocketTimeoutException for the read.
     */
    @Test(expected = SocketTimeoutException.class)
    public void readTimeout() throws Throwable {

        final Long READ_TIMEOUT = 250l;

        //start a simple http server
        MockWebServer server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                Thread.sleep(READ_TIMEOUT * 2);
                return new MockResponse();
            }
        });

        try {
            server.start();

            try {
                CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server)
                        .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS).build();

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
            server.shutdown();
        }
    }

    /**
     * This tests that a CouchDbException is thrown if the user is null, but the password is
     * supplied.
     */
    @Test(expected = CouchDbException.class)
    public void nullUser() throws Exception {
        CloudantClientHelper.newTestAddressClient()
                .password(":0-myPassword")
                .build();

    }

    /**
     * This tests that a CouchDbException is thrown if the user is supplied, but the password is
     * null.
     */
    @Test(expected = CouchDbException.class)
    public void nullPassword() throws Exception {
        CloudantClientHelper.newTestAddressClient()
                .username("user")
                .build();
    }

    /**
     * Test that user info provided in a url is correctly removed and made into user name and
     * password fields.
     */
    @Test
    public void testUserInfoInUrl() throws Exception {
        ClientBuilder b = ClientBuilder.url(new URL("https://user:password@192.0.2.0"));

        //reflectively check (not nice, but better than having a bug)
        Field user = b.getClass().getDeclaredField("username");
        user.setAccessible(true);
        assertEquals("The username should match the one provided in the URL", "user", user.get(b));
        Field pass = b.getClass().getDeclaredField("password");
        pass.setAccessible(true);
        assertEquals("The password should match the one provided in the URL", "password", pass
                .get(b));

        CloudantClient c = b.build();

        assertFalse("The URL should not contain the username", c.getBaseUri().toString().contains
                ("user"));
        assertFalse("The URL should not contain the password", c.getBaseUri().toString().contains
                ("password"));

        //ensure that building a URL from it does not throw any exceptions
        new URL(c.getBaseUri().toString());
    }

    @Test
    public void sessionDeleteOnShutdown() throws Exception {
        MockWebServer server = new MockWebServer();
        // Mock a 200 OK for the _session DELETE request
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":\"true\"}"));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server).build();
        c.shutdown();

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertEquals("The request method should be DELETE", "DELETE", request.getMethod());
        assertEquals("The request should be to the _session path", "/_session", request.getPath());
    }

    /**
     * Test that adding the Basic Authentication interceptor to CloudantClient works.
     */
    @Test
    @Category(RequiresCloudant.class)
    public void testBasicAuth() throws IOException {
        BasicAuthInterceptor interceptor =
                new BasicAuthInterceptor(CloudantClientHelper.COUCH_USERNAME
                        + ":" + CloudantClientHelper.COUCH_PASSWORD);

        CloudantClient client = ClientBuilder.account(CloudantClientHelper.COUCH_USERNAME)
                .interceptors(interceptor).build();

        // Test passes if there are no exceptions
        client.getAllDbs();
    }
}
