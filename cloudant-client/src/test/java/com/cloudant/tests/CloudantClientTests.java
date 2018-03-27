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

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.createPost;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.Membership;
import com.cloudant.client.api.model.Task;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.client.org.lightcouch.PreconditionFailedException;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.http.internal.interceptors.UserAgentInterceptor;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.cloudant.tests.util.MockWebServerResources;
import com.cloudant.tests.util.Utils;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ServerSocketFactory;

/**
 * Note some tests in this class use Java 1.7 features
 */
public class CloudantClientTests extends TestWithDbPerClass {

    @RegisterExtension
    public MockWebServerExtension mockWebServerExt = new MockWebServerExtension();

    public MockWebServer server;

    @BeforeEach
    public void beforeEach() {
        server = mockWebServerExt.get();
    }

    @Test
    @RequiresCloudantService
    public void apiKey() {
        ApiKey key = account.generateApiKey();
        assertNotNull(key);
        assertNotNull(key.getKey());
        assertNotNull(key.getPassword());
    }

    @Test
    @RequiresCloudant
    public void activeTasks() {
        List<Task> tasks = account.getActiveTasks();
        assertNotNull(tasks);
    }

    @Test
    @RequiresCloudant
    public void membership() {
        Membership mship = account.getMembership();
        assertNotNull(mship);
        assertNotNull(mship.getClusterNodes());
        assertNotNull(mship.getClusterNodes().hasNext());
        assertNotNull(mship.getAllNodes());
        assertNotNull(mship.getAllNodes().hasNext());
    }

    @Test
    @RequiresCloudant
    public void cookieTest() {

        Membership membership = account.getMembership();
        assertNotNull(membership);
    }

    // java-cloudant/n.n.n or java-cloudant/unknown followed by 4 groups of /anything
    private final String userAgentRegex = "java-cloudant/(?:(?:\\d+.\\d+.\\d+))" +
            "(?:/{1}[^/]+){4}";

    private final String userAgentUnknownRegex = "cloudant-http/(?:(?:unknown))" +
            "(?:/{1}[^/]+){4}";


    private final String userAgentFormat = "java-cloudant/version/jvm.version/jvm.vendor/os" +
            ".name/os.arch";

    /**
     * Assert that the User-Agent header is of the expected form.
     */
    @Test
    public void testUserAgentHeaderString() throws Exception {

        // This doesn't read the a properties file, since the tests do not run from the published
        // jars.
        String userAgentHeader = new UserAgentInterceptor(UserAgentInterceptor.class
                .getClassLoader(),
                "META-INF/com.cloudant.client.properties").getUserAgent();
        assertTrue(userAgentHeader.matches(userAgentUnknownRegex), "The value of the User-Agent " +
                "header: " + userAgentHeader + " should match the " + "format: " + userAgentFormat);
    }

    @Test
    public void testUserAgentHeaderStringFromFile() throws Exception {
        // This doesn't read the a properties file, since the tests do not run from the published
        // jars.
        // Point to the built classes, it's a bit awkward but we need to load the class cleanly
        File f = new File("../cloudant-http/build/classes/main/");
        String userAgentHeader = new UserAgentInterceptor(new URLClassLoader(new URL[]{f.toURI()
                .toURL()}) {

            @Override
            public InputStream getResourceAsStream(String name) {
                if (name.equals("META-INF/com.cloudant.client.properties")) {
                    try {
                        return new ByteArrayInputStream(("user.agent.name=java-cloudant\nuser" +
                                ".agent.version=1.6.1").getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
                return super.getResourceAsStream(name);
            }
        }, "META-INF/com.cloudant.client.properties").getUserAgent();
        assertTrue(userAgentHeader.matches(userAgentRegex), "The value of the User-Agent header: " +
                "" + userAgentHeader + " should match the " + "format: " + userAgentFormat);
    }

    /**
     * Assert that requests have the User-Agent header added. This test runs a local HTTP server
     * process that can handle a single request to receive the request and validate the header.
     */
    @Test
    public void testUserAgentHeaderIsAddedToRequest() throws Exception {

        //send back an OK 200
        server.enqueue(new MockResponse());

        //instantiating the client performs a single post request
        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .build();
        String response = client.executeRequest(createPost(client.getBaseUri(), null,
                "application/json")).responseAsString();
        assertTrue(response.isEmpty(), "There should be no response body on the mock response");

        //assert that the request had the expected header
        String userAgentHeader = server.takeRequest(10, TimeUnit.SECONDS)
                .getHeader("User-Agent");
        assertNotNull(userAgentHeader, "The User-Agent header should be present on the request");
        assertTrue(userAgentHeader.matches(userAgentUnknownRegex), "The value of the User-Agent " +
                "header " + userAgentHeader + " on the request" + " should match the format " +
                userAgentFormat);
    }

    /**
     * Test a NoDocumentException is thrown when trying an operation on a DB that doesn't exist
     */
    @Test
    @RequiresDB
    public void nonExistentDatabaseException() {
        assertThrows(NoDocumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                //try and get a DB that doesn't exist
                Database db = account.database("not_really_there", false);
                //try an operation against the non-existant DB
                db.info();
            }
        });
    }

    /**
     * Validate that no exception bubbles up when trying to create a DB that already exists
     */
    @Test
    @RequiresDB
    public void existingDatabaseCreateException() {
        String id = Utils.generateUUID();
        String dbName = "existing" + id;
        try {
            //create a DB for this test
            account.createDB(dbName);

            // Get a database instance using create true for the already existing DB
            account.database(dbName, true);
        } finally {
            //clean up the DB created by this test
            account.deleteDB(dbName);
        }
    }

    /**
     * Validate that a PreconditionFailedException is thrown when using the createDB method to
     * create a database that already exists.
     */
    @Test
    @RequiresDB
    public void existingDatabaseCreateDBException() {
        assertThrows(PreconditionFailedException.class, new Executable() {
            @Override
            public void execute() throws Throwable {

                String id = Utils.generateUUID();
                String dbName = "existing" + id;
                try {
                    //create a DB for this test
                    account.createDB(dbName);

                    //do a get with create true for the already existing DB
                    account.createDB(dbName);

                } finally {
                    //clean up the DB created by this test
                    account.deleteDB(dbName);
                }
            }
        });
    }

    @Test
    public void testDefaultPorts() throws Exception {
        CloudantClient c = null;

        c = CloudantClientHelper.newTestAddressClient().build();

        assertEquals(80, c.getBaseUri().getPort(), "The http port should be 80");


        c = CloudantClientHelper.newHttpsTestAddressClient().build();

        assertEquals(443, c.getBaseUri().getPort(), "The http port should be 443");
    }

    /**
     * Check that the connection timeout throws a SocketTimeoutException when it can't connect
     * within the timeout.
     */
    @Test
    public void connectionTimeout() throws Throwable {
        assertThrows(SocketTimeoutException.class, new Executable() {
            @Override
            public void execute() throws Throwable {

                // Do this test on the loopback
                InetAddress loopback = InetAddress.getLoopbackAddress();
                ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket
                        (0, 1,
                                loopback);

                int port = serverSocket.getLocalPort();
                //block the single connection to our server
                Socket socket = new Socket(loopback.getHostAddress(), port);

                //now try to connect, but should timeout because there is no connection available
                try {
                    CloudantClient c = ClientBuilder.url(new URL("http", loopback.getHostAddress
                            (), port,
                            "")).connectTimeout(100, TimeUnit.MILLISECONDS)
                            // Unfortunately openjdk doesn't honour the connect timeout so we set
                            // the read
                            // timeout as well so that the test doesn't take too long on that
                            // platform
                            .readTimeout(250, TimeUnit.MILLISECONDS)
                            .build();

                    // Make a request
                    c.getAllDbs();
                } catch (CouchDbException e) {
                    //unwrap the CouchDbException
                    if (e.getCause() != null) {
                        //whilst it would be really nice to actually assert that this was a connect
                        //exception and not some other SocketTimeoutException there are JVM
                        // differences in
                        //this respect (i.e. OpenJDK does not appear to distinguish between
                        // read/connect)
                        //in its exception messages
                        throw e.getCause();
                    } else {
                        throw e;
                    }
                } finally {
                    //make sure we close the sockets
                    IOUtils.closeQuietly(socket);
                    IOUtils.closeQuietly(serverSocket);
                }
            }
        });
    }

    /**
     * Checks that the read timeout works. The test sets a read timeout of 0.25 s and the mock
     * server thread never sends a response. If things are working
     * correctly then the client should see a SocketTimeoutException for the read.
     */
    @Test
    public void readTimeout() throws Throwable {
        assertThrows(SocketTimeoutException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                // Don't respond so the read will timeout
                server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
                try {
                    CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server)
                            .readTimeout(25, TimeUnit.MILLISECONDS).build();

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
            }
        });
    }

    /**
     * This tests that a CouchDbException is thrown if the user is null, but the password is
     * supplied.
     */
    @Test
    public void nullUser() throws Exception {
        assertThrows(CouchDbException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                CloudantClientHelper.newTestAddressClient()
                        .password(":0-myPassword")
                        .build();
            }
        });
    }

    /**
     * This tests that a CouchDbException is thrown if the user is supplied, but the password is
     * null.
     */
    @Test
    public void nullPassword() throws Exception {
        assertThrows(CouchDbException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                CloudantClientHelper.newTestAddressClient()
                        .username("user")
                        .build();
            }
        });
    }

    /**
     * Test that user info provided in a url is correctly removed and made into user name and
     * password fields.
     */
    @Test
    public void testUserInfoInUrl() throws Exception {
        urlCheck("user", "password");
    }

    // A String of all the URI reserved and "unsafe" characters, plus © and an emoji. Encodes to:
    // %21*%27%28%29%3B%3A%40%26%3D%2B%24%2C%2F%3F%23%5B%5D+%22%25
    // -.%3C%3E%5C%5E_%60%7B%7C%7D%7E%C2%A9%F0%9F%94%92
    private static String SPECIALS = "!*'();:@&=+$,/?#[] \"%-.<>\\^_`{|}~©\uD83D\uDD12";

    /**
     * Test that user info provided in a url is correctly removed and made into user name and
     * password fields when the user info includes URL encoded characters.
     */
    @Test
    public void testUserInfoInUrlWithSpecials() throws Exception {
        String user = URLEncoder.encode("user" + SPECIALS, "UTF-8");
        String pw = URLEncoder.encode("password" + SPECIALS, "UTF-8");
        urlCheck(user, pw);
    }

    /**
     * Test that user info provided via the username and password methods including URL special
     * characters is encoded correctly.
     */
    @Test
    public void testUserInfoWithSpecials() throws Exception {
        String user = "user" + SPECIALS;
        String pw = "password" + SPECIALS;
        credentialsCheck(CloudantClientHelper.newMockWebServerClientBuilder(server).username
                (user).password(pw), URLEncoder.encode(user, "UTF-8"), URLEncoder.encode(pw,
                "UTF-8"));
    }

    private static Pattern CREDENTIALS = Pattern.compile("name=([^&]+)&password=(.+)");

    private void urlCheck(String encodedUser, String encodedPassword) throws Exception {
        ClientBuilder b = ClientBuilder.url(server.url("").newBuilder().encodedUsername
                (encodedUser).encodedPassword(encodedPassword).build().url());
        credentialsCheck(b, encodedUser, encodedPassword);
    }

    private void credentialsCheck(ClientBuilder b, String encodedUser, String encodedPassword)
            throws Exception {
        CloudantClient c = b.build();

        server.enqueue(MockWebServerResources.OK_COOKIE);
        server.enqueue(MockWebServerResources.JSON_OK);

        HttpConnection conn = c.executeRequest(Http.GET(c.getBaseUri()));
        // Consume response stream and assert ok: true
        String responseStr = conn.responseAsString();
        assertNotNull(responseStr);

        // One request to _session then one to get info
        assertEquals(2, server.getRequestCount(), "There should be two requests");

        // Get the _session request
        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();
        // body should be of form:
        // name=YourUserName&password=YourPassword
        Matcher m = CREDENTIALS.matcher(body);
        assertTrue(m.matches(), "The _session request should match the regex");
        assertEquals(2, m.groupCount(), "There should be a username group and a password group in" +
                " the creds");
        assertEquals(encodedUser, m.group(1), "The username should match");
        assertEquals(encodedPassword, m.group(2), "The password should match");

        //ensure that building a URL from it does not throw any exceptions
        new URL(c.getBaseUri().toString());
    }

    @Test
    public void sessionDeleteOnShutdown() throws Exception {
        // Mock a 200 OK for the _session DELETE request
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":\"true\"}"));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server).build();
        c.shutdown();

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertEquals("DELETE", request.getMethod(), "The request method should be DELETE");
        assertEquals("/_session", request.getPath(), "The request should be to the _session path");
    }

    /**
     * Test that adding the Basic Authentication interceptor to CloudantClient works.
     */
    @Test
    @RequiresCloudant
    public void testBasicAuth() throws IOException {
        BasicAuthInterceptor interceptor =
                new BasicAuthInterceptor(CloudantClientHelper.COUCH_USERNAME
                        + ":" + CloudantClientHelper.COUCH_PASSWORD);

        CloudantClient client = ClientBuilder.account(CloudantClientHelper.COUCH_USERNAME)
                .interceptors(interceptor).build();

        // Test passes if there are no exceptions
        client.getAllDbs();
    }

    /**
     * Test that configuring the Basic Authentication interceptor from credentials and adding to
     * the CloudantClient works.
     */
    @Test
    public void testBasicAuthFromCredentials() throws Exception {
        BasicAuthInterceptor interceptor =
                BasicAuthInterceptor.createFromCredentials("username", "password");

        // send back a mock OK 200
        server.enqueue(new MockResponse());

        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(server)
                .interceptors(interceptor).build();

        client.getAllDbs();

        // expected 'username:password'
        assertEquals("Basic dXNlcm5hbWU6cGFzc3dvcmQ=", server.takeRequest().getHeader
                ("Authorization"));
    }

    @Test
    public void gatewayStyleURL() throws Exception {

        final String gatewayPath = "/gateway";

        // Set a dispatcher that returns 200 if the requests have the correct path /gateway/_all_dbs
        // Otherwise return 400.
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().equals(gatewayPath + "/_all_dbs")) {
                    return new MockResponse();
                } else {
                    return new MockResponse().setResponseCode(400);
                }
            }
        });

        // Build a client with a URL that includes a path
        CloudantClient c = ClientBuilder.url(new URL(server.url(gatewayPath).toString())).build();
        // If the request path is wrong this call will return 400 and throw an exception failing the
        // test.
        c.getAllDbs();

        // Build a client with a URL that includes a path with a trailing /
        c = ClientBuilder.url(new URL(server.url(gatewayPath + "/").toString())).build();
        // If the request path is wrong this call will return 400 and throw an exception failing the
        // test.
        c.getAllDbs();
    }

    /**
     * Assert that a {@code null} URL causes an IllegalArgumentException to be thrown.
     *
     * @throws Exception
     */
    @Test
    public void nullURLThrowsIAE() throws Exception {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                ClientBuilder.url(null);
            }
        });
    }
}
