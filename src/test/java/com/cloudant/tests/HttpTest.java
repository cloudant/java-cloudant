package com.cloudant.tests;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.http.interceptors.CookieInterceptor;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.SimpleHttpServer;
import com.cloudant.tests.util.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpTest {

    private String data = "{\"hello\":\"world\"}";

    @ClassRule
    public static CloudantClientResource clientResource = new CloudantClientResource();
    @Rule
    public final DatabaseResource dbResource = new DatabaseResource(clientResource);

    private final CloudantClient account = clientResource.get();

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
        Assert.assertEquals(bis.available(), data.getBytes().length);

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
            Assert.assertEquals(bis.available(), data.getBytes().length);
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
        Assert.assertEquals(bis.available(), data.getBytes().length);

        conn.setRequestBody(bis);
        conn.execute();

        // stream was read to end
        Assert.assertEquals(bis.available(), 0);
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
        Assert.assertEquals(bis.available(), data.getBytes().length);

        conn.setRequestBody(bis);
        try {
            conn.responseAsString();
            Assert.fail("IOException not thrown as expected");
        } catch (IOException ioe) {
            ; // "Attempted to read response from server before calling execute()"
        }

        // stream was not read because execute() was not called
        Assert.assertEquals(bis.available(), data.getBytes().length);
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
        Assert.assertEquals(bis.available(), data.getBytes().length);

        conn.setRequestBody(bis);
        conn.execute();

        // stream was read to end
        Assert.assertEquals(bis.available(), 0);
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
        Assert.assertEquals(bis.available(), data.getBytes().length);

        conn.setRequestBody(bis);
        conn.execute();

        // stream was read to end
        Assert.assertEquals(bis.available(), 0);
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
    @Test
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
            CloudantClient c = new CloudantClient(server.getUrl());
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

        final AtomicReference<String> readBody = new AtomicReference<String>();

        final List<List<String>> requests = new ArrayList<List<String>>();
        SimpleHttpServer server = new SimpleHttpServer() {
            int count = 0;

            @Override
            protected void serverAction(InputStream is, OutputStream os) throws Exception {
                count++;
                if (count == 1) {
                    //read the headers
                    super.readInputLines(is);
                    //work out how long the body is
                    int contentLength = 0;
                    for (String header : getLastInputRequestLines()) {
                        Matcher m = Pattern.compile("Content-Length\\s*:\\s*(\\d+)", Pattern
                                .CASE_INSENSITIVE).matcher(header);
                        if (m.matches() && m.groupCount() == 1) {
                            contentLength = Integer.valueOf(m.group(1));
                        }
                    }
                    //write the continue to allow the body to be sent faster
                    OutputStreamWriter osw = new OutputStreamWriter(new BufferedOutputStream(os));
                    osw.write("HTTP/1.1 100 Continue\r\n\r\n");
                    osw.flush();
                    //now read the body
                    BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    //wait for the client to be ready with the body
                    while (!r.ready()) {
                        Thread.sleep(100);
                    }
                    StringWriter w = new StringWriter();
                    int nRead = 0;
                    while (nRead < contentLength) {
                        w.write(r.read());
                        nRead++;
                    }
                    readBody.set(w.toString());
                    //send back a mock cookie
                    osw.write("HTTP/1.1 200 OK\r\n");
                    osw.write("Set-Cookie: " +
                            "AuthSession=\"a2ltc3RlYmVsOjUxMzRBQTUzOtiY2_IDUIdsTJEVNEjObAbyhrgz" +
                            "\";\r\n\r\n");
                    osw.write(("{\"ok\":true,\"name\":\"" + mockUser + "\",\"roles\":[]}\r\n"));
                    osw.flush();
                } else {
                    super.serverAction(is, os);
                }
            }
        };

        server.start();
        server.await();

        try {
            CloudantClient c = new CloudantClient(server.getUrl(), mockUser, mockPass);
            //the GET request will try to get a session, then perform the GET
            c.executeRequest(Http.GET(c.getBaseUri()));

            String sessionRequestContent = readBody.get();
            Assert.assertNotNull("The _session request should have non-null content",
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
            server.stop();
        }
    }
}
