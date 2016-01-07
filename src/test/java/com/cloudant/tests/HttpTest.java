package com.cloudant.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.http.interceptors.CookieInterceptor;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.MockWebServerResource;
import com.cloudant.tests.util.SimpleHttpServer;
import com.cloudant.tests.util.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class HttpTest {

    private String data = "{\"hello\":\"world\"}";

    public static CloudantClientResource clientResource = new CloudantClientResource();
    public static DatabaseResource dbResource = new DatabaseResource(clientResource);
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);


    /*
     * Test "Expect: 100-Continue" header works as expected
     * See "8.2.3 Use of the 100 (Continue) Status" in http://tools.ietf.org/html/rfc2616
     * We expect the precondition of having a valid DB name to have failed, and therefore, the body
     * data will not have been written.
     *
     * NB this behaviour is only supported on certain JDKs - so we have to make a weaker set of
     * asserts. If it is supported, we expect execute() to throw an exception and then nothing will
     * have been read from the stream. If it is not supported, execute() will not throw and we
     * cannot make any assumptions about how much of the stream has been read (remote side may close
     * whilst we are still writing).
     */
    @Test
    public void testExpect100Continue() throws IOException {
        String no_such_database = clientResource.getBaseURIWithUserInfo() + "/no_such_database";
        HttpConnection conn = new HttpConnection("POST", new URL(no_such_database),
                "application/json");
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());

        // nothing read from stream
        Assert.assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        boolean thrown = false;
        try {
            conn.execute();
        } catch (ProtocolException pe) {
            // ProtocolException with message "Server rejected operation" on JDK 1.7
            thrown = true;
        }

        if (thrown) {
            // still nothing read from stream
            Assert.assertEquals(data.getBytes().length, bis.available());
        }
    }

    /*
     * Basic test that we can write a document body by POSTing to a known database
     */
    @Test
    public void testWriteToServerOk() throws Exception {
        HttpConnection conn = new HttpConnection("POST", new URL(dbResource.getDbURIWithUserInfo()),
                "application/json");
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());

        // nothing read from stream
        Assert.assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        conn.execute();

        // stream was read to end
        Assert.assertEquals(0, bis.available());
    }

    /*
     * Basic test to check that an IOException is thrown when we attempt to get the response
     * without first calling execute()
     */
    @Test
    public void testReadBeforeExecute() throws Exception {
        HttpConnection conn = new HttpConnection("POST", new URL(dbResource.getDbURIWithUserInfo()),
                "application/json");
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());

        // nothing read from stream
        Assert.assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        try {
            conn.responseAsString();
            fail("IOException not thrown as expected");
        } catch (IOException ioe) {
            ; // "Attempted to read response from server before calling execute()"
        }

        // stream was not read because execute() was not called
        Assert.assertEquals(data.getBytes().length, bis.available());
    }


    //NOTE: This test doesn't work with specified couch servers,
    // the URL will always include the creds specified for the test
    //
    // A couchdb server needs to be set and running with the correct
    // security settings, the database *must* not be public, it *must*
    // be named cookie_test
    //
    @Test
    @Category(RequiresCloudant.class)
    public void testCookieAuthWithoutRetry() throws IOException {
        CookieInterceptor interceptor = new CookieInterceptor(CloudantClientHelper.COUCH_USERNAME,
                CloudantClientHelper.COUCH_PASSWORD);

        HttpConnection conn = new HttpConnection("POST", dbResource.get().getDBUri().toURL(),
                "application/json");
        conn.responseInterceptors.add(interceptor);
        conn.requestInterceptors.add(interceptor);
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());

        // nothing read from stream
        Assert.assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        conn.execute();

        // stream was read to end
        Assert.assertEquals(0, bis.available());
        Assert.assertEquals(2, conn.getConnection().getResponseCode() / 100);

        //check the json
        Gson gson = new Gson();
        JsonObject response = gson.fromJson(new InputStreamReader(conn.getConnection()
                .getInputStream()), JsonObject.class);

        Assert.assertTrue(response.has("ok"));
        Assert.assertTrue(response.get("ok").getAsBoolean());
        Assert.assertTrue(response.has("id"));
        Assert.assertTrue(response.has("rev"));
    }

    /**
     * Test that adding the Basic Authentication interceptor to HttpConnection
     * will complete with a response code of 200.  The response input stream
     * is expected to hold the newly created document's id and rev.
     */
    @Test
    @Category(RequiresCloudant.class)
    public void testBasicAuth() throws IOException {
        BasicAuthInterceptor interceptor =
                new BasicAuthInterceptor(CloudantClientHelper.COUCH_USERNAME
                        + ":" + CloudantClientHelper.COUCH_PASSWORD);

        HttpConnection conn = new HttpConnection("POST", dbResource.get().getDBUri().toURL(),
                "application/json");
        conn.requestInterceptors.add(interceptor);
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());

        // nothing read from stream
        Assert.assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        conn.execute();

        // stream was read to end
        Assert.assertEquals(0, bis.available());
        Assert.assertEquals(2, conn.getConnection().getResponseCode() / 100);

        //check the json
        Gson gson = new Gson();
        JsonObject response = gson.fromJson(new InputStreamReader(conn.getConnection()
                .getInputStream()), JsonObject.class);

        Assert.assertTrue(response.has("ok"));
        Assert.assertTrue(response.get("ok").getAsBoolean());
        Assert.assertTrue(response.has("id"));
        Assert.assertTrue(response.has("rev"));
    }

    /**
     * This test validates that the client does not make body content available for reading until
     * after receiving the 100 continue. This is primarily a validation of the 100 continue
     * performance enhancement functioning correctly in whatever underlying http library is being
     * used to back our HttpConnection.
     *
     * @throws Exception
     */
    //TODO re-enable after OkHttp fixes:
    // https://github.com/square/okhttp/issues/675
    //https://github.com/square/okhttp/issues/1337
    @Ignore
    public void expectContinue() throws Exception {

        final AtomicBoolean foundExpectHeader = new AtomicBoolean();
        final AtomicBoolean readyBefore100 = new AtomicBoolean();
        final AtomicBoolean bodyAvailableBefore100 = new AtomicBoolean();
        final AtomicBoolean bodyReadAfter100 = new AtomicBoolean();

        //Mock server for validating the Expect: 100-continue behaviour
        SimpleHttpServer server = new SimpleHttpServer() {
            private int invocationCount = 0;

            @Override
            protected void serverAction(InputStream is, OutputStream os) throws Exception {

                invocationCount++;

                //we only want to do this once, otherwise this server is a no-op
                if (invocationCount == 1) {
                    //read headers from input stream i.e. up to the CRLF between headers and body
                    BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line;
                    while ((line = r.readLine()) != null && !line.isEmpty()) {
                        log.finest(line);
                        //check for the Expect header
                        if (!foundExpectHeader.get()) {
                            foundExpectHeader.set(Pattern.compile
                                    ("Expect\\s*:\\s*100-continue", Pattern.CASE_INSENSITIVE)
                                    .matcher(line).matches());
                        }
                        //if the body content was read before 100-continue was sent then set the
                        // fail flag
                        if (line.contains(data)) {
                            bodyAvailableBefore100.set(true);
                        }
                    }

                    readyBefore100.set(r.ready());

                    //write the 100 continue
                    log.fine("Writing 100 interim response");
                    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    w.write("HTTP/1.1 100 Continue\r\n\r\n");
                    w.flush();

                    log.fine("Reading body");
                    while ((line = r.readLine()) != null && !line.isEmpty()) {
                        log.finest(line);
                        //if the body content wasn't read here set the fail flag
                        if (line.contains(data)) {
                            bodyReadAfter100.set(true);
                        }
                    }
                    super.writeOK(os);
                } else {
                    log.fine("Server action invoked more than once, expected for server stop");
                }
            }
        };


        try {
            //start and wait for our simple server to be ready
            server.start();
            server.await();

            //create a client to connect to post to the simple server
            CloudantClient c = CloudantClientHelper.newSimpleHttpServerClient(server).build();
            HttpConnection conn = Http.POST(c.getBaseUri(), "application/json");
            //set the body content, add some new lines to make it easier for the simple server
            conn.setRequestBody(data + "\r\n\r\n");
            //do the request
            c.executeRequest(conn);

            //the simple server stores some booleans in the test assertions object
            // now we validate those assertions
            Assert.assertTrue("The Expect:100-continue header should be present",
                    foundExpectHeader.get());
            Assert.assertFalse("The body should not be readable before 100-continue",
                    bodyAvailableBefore100.get());
            Assert.assertFalse("The stream should not be ready before 100-continue",
                    readyBefore100.get());
            Assert.assertTrue("The body should have been read after 100-continue",
                    bodyReadAfter100.get());
        } finally {
            server.stop();
        }
    }

    /**
     * This test mocks up a server to receive the _session request and asserts that the request
     * body is correctly encoded (per application/x-www-form-urlencoded). Because it requires a
     * body this test also relies on Expect: 100-continue working in the client as that is enabled
     * by default.
     *
     * @throws Exception
     */
    @Test
    public void cookieInterceptorURLEncoding() throws Exception {
        final String mockUser = "myStrangeUsername=&?";
        String mockPass = "?&=NotAsStrangeInAPassword";

        MockWebServer server = new MockWebServer();
        //expect a cookie request then a GET
        server.enqueue(MockWebServerResource.OK_COOKIE);
        server.enqueue(new MockResponse());

        server.start();

        try {
            CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server)
                    .username(mockUser)
                    .password(mockPass)
                    .build();
            //the GET request will try to get a session, then perform the GET
            c.executeRequest(Http.GET(c.getBaseUri()));

            RecordedRequest r = server.takeRequest(10, TimeUnit.SECONDS);
            String sessionRequestContent = r.getBody().readString(Charset.forName("UTF-8"));
            assertNotNull("The _session request should have non-null content",
                    sessionRequestContent);
            //expecting name=...&password=...
            String[] parts = Utils.splitAndAssert(sessionRequestContent, "&", 1);
            String username = URLDecoder.decode(Utils.splitAndAssert(parts[0], "=", 1)[1], "UTF-8");
            Assert.assertEquals("The username URL decoded username should match", mockUser,
                    username);
            String password = URLDecoder.decode(Utils.splitAndAssert(parts[1], "=", 1)[1], "UTF-8");
            Assert.assertEquals("The username URL decoded password should match", mockPass,
                    password);
        } finally {
            server.shutdown();
        }
    }

    /**
     * This test checks that the cookie is successfully renewed if a 403 with an error of
     * "credentials_expired" is returned.
     *
     * @throws Exception
     */
    @Test
    public void cookie403Renewal() throws Exception {

        MockWebServer server = new MockWebServer();
        // Request sequence
        // _session request to get Cookie
        // GET request -> 403
        // _session for new cookie
        // GET replay -> 200
        server.enqueue(MockWebServerResource.OK_COOKIE);
        server.enqueue(new MockResponse().setResponseCode(403).setBody
                ("{\"error\":\"credentials_expired\", \"reason\":\"Session expired\"}\r\n"));
        server.enqueue(MockWebServerResource.OK_COOKIE);
        server.enqueue(new MockResponse());

        server.start();

        try {
            CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server)
                    .username("a")
                    .password("b")
                    .build();
            //the GET request will try to get a session, then perform the GET
            //the GET will result in a 403, which should mean another request to _session
            //followed by a replay of GET
            c.executeRequest(Http.GET(c.getBaseUri()));

            //if we don't handle the 403 correctly an exception will be thrown

            // also assert that there were 4 calls
            assertEquals("The server should have received 4 requests", 4, server.getRequestCount());
        } finally {
            server.shutdown();
        }
    }

    /**
     * This test checks that if we get a 403 that is not an error of "credentials_expired" then
     * the exception is correctly thrown and the error stream is deserialized. This is important
     * because the CookieInterceptor will have consumed 403 error streams to check for the expiry.
     *
     * @throws Exception
     */
    @Test
    public void handleNonExpiry403() throws Exception {

        MockWebServer server = new MockWebServer();
        // Request sequence
        // _session request to get Cookie
        // GET request -> 403 (CouchDbException)
        server.enqueue(MockWebServerResource.OK_COOKIE);
        server.enqueue(new MockResponse().setResponseCode(403).setBody
                ("{\"error\":\"403_not_expired_test\", \"reason\":\"example reason\"}\r\n"));

        server.start();

        try {
            CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(server)
                    .username("a")
                    .password("b")
                    .build();
            //the GET request will try to get a session, then perform the GET
            //the GET will result in a 403, which should result in a CouchDbException
            c.executeRequest(Http.GET(c.getBaseUri()));
            fail("A 403 not due to cookie expiry should result in a CouchDbException");
        } catch (CouchDbException e) {
            e.printStackTrace();
            assertNotNull("The error should not be null", e.getError());
            assertEquals("The error message should be the expected one", "403_not_expired_test", e
                    .getError());
        } finally {
            server.shutdown();
        }
    }
}
