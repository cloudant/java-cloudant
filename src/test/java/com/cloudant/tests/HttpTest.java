package com.cloudant.tests;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.http.CookieInterceptor;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.SimpleHttpServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ProtocolException;
import java.net.URL;
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
}
