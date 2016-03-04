package com.cloudant.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionResponseInterceptor;
import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.http.interceptors.CookieInterceptor;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.MockWebServerResource;
import com.cloudant.tests.util.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HttpTest {

    private String data = "{\"hello\":\"world\"}";

    public static CloudantClientResource clientResource = new CloudantClientResource();
    public static DatabaseResource dbResource = new DatabaseResource(clientResource);
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);
    @Rule
    public MockWebServerResource mwr = new MockWebServerResource();
    public MockWebServer mockWebServer = mwr.getServer();

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

        // Request sequence
        // _session request to get Cookie
        // GET request -> 403
        // _session for new cookie
        // GET replay -> 200
        mockWebServer.enqueue(MockWebServerResource.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(403).setBody
                ("{\"error\":\"credentials_expired\", \"reason\":\"Session expired\"}\r\n"));
        mockWebServer.enqueue(MockWebServerResource.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse());

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .username("a")
                .password("b")
                .build();
        //the GET request will try to get a session, then perform the GET
        //the GET will result in a 403, which should mean another request to _session
        //followed by a replay of GET
        c.executeRequest(Http.GET(c.getBaseUri()));

        //if we don't handle the 403 correctly an exception will be thrown

        // also assert that there were 4 calls
        assertEquals("The server should have received 4 requests", 4, mockWebServer
                .getRequestCount());

    }

    /**
     * This test check that the cookie is renewed if the server presents a Set-Cookie header
     * after the cookie authentication.
     * @throws Exception
     */
    @Test
    public void cookieRenewal() throws Exception {

        final String renewalCookieValue =
                "AuthSession=\"RenewCookie_a2ltc3RlYmVsOjUxMzRBQTUzOtiY2_IDUIdsTJEVNEjObAbyhrgz\";";
        // Request sequence
        // _session request to get Cookie
        // GET request -> 403
        // _session for new cookie
        // GET replay -> 200
        mockWebServer.enqueue(MockWebServerResource.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).addHeader("Set-Cookie",
                String.format(Locale.ENGLISH, "%s;", renewalCookieValue))
                .setBody("{\"hello\":\"world\"}\r\n"));
        mockWebServer.enqueue(new MockResponse());

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .username("a")
                .password("b")
                .build();


        c.executeRequest(Http.GET(c.getBaseUri()));
        c.executeRequest(Http.GET(c.getBaseUri()));


        // also assert that there were 3 calls
        assertEquals("The server should have received 3 requests", 3, mockWebServer
                .getRequestCount());

        mockWebServer.takeRequest(); // cookie
        mockWebServer.takeRequest(); // actual get

        // this is the request that should have the new cookie.
        RecordedRequest request = mockWebServer.takeRequest();
        String headerValue = request.getHeader("Cookie");
        Assert.assertEquals("AuthSession=\"RenewCookie_a2ltc3RlYmVsOjUxMzRBQTUzOtiY2_IDUIdsTJEVNEjObAbyhrgz\"", headerValue);
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

        // Request sequence
        // _session request to get Cookie
        // GET request -> 403 (CouchDbException)
        mockWebServer.enqueue(MockWebServerResource.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(403).setBody
                ("{\"error\":\"403_not_expired_test\", \"reason\":\"example reason\"}\r\n"));

        try {
            CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
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
        }
    }

    @Test
    public void inputStreamRetryString() throws Exception {
        HttpConnection request = Http.POST(mockWebServer.url("/").url(), "application/json");
        String content = "abcde";
        request.setRequestBody(content);
        testInputStreamRetry(request, content.getBytes("UTF-8"));
    }

    @Test
    public void inputStreamRetryBytes() throws Exception {
        HttpConnection request = Http.POST(mockWebServer.url("/").url(), "application/json");
        byte[] content = "abcde".getBytes("UTF-8");
        request.setRequestBody(content);
        testInputStreamRetry(request, content);
    }

    private static class UnmarkableInputStream extends InputStream {

        private final byte[] content;
        int read;
        int available;

        UnmarkableInputStream(byte[] content) {
            this.content = content;
            available = content.length;
            read = 0;
        }

        @Override
        public int read() throws IOException {
            if (available == 0) {
                return -1;
            } else {
                int i = content[read];
                read++;
                available--;
                return i;
            }
        }

        @Override
        public boolean markSupported() {
            return false;
        }
    }

    @Test
    public void inputStreamRetry() throws Exception {
        HttpConnection request = Http.POST(mockWebServer.url("/").url(), "application/json");
        final byte[] content = "abcde".getBytes("UTF-8");
        // Mock up an input stream that doesn't support marking
        request.setRequestBody(new UnmarkableInputStream(content));
        testInputStreamRetry(request, content);
    }

    @Test
    public void inputStreamRetryWithLength() throws Exception {
        HttpConnection request = Http.POST(mockWebServer.url("/").url(), "application/json");
        final byte[] content = "abcde".getBytes("UTF-8");
        // Mock up an input stream that doesn't support marking
        request.setRequestBody(new UnmarkableInputStream(content), content.length);
        testInputStreamRetry(request, content);
    }

    private static class TestInputStreamGenerator implements HttpConnection.InputStreamGenerator {

        private final byte[] content;
        TestInputStreamGenerator(byte[] content){
            this.content = content;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }
    }

    @Test
    public void inputStreamRetryGenerator() throws Exception {
        HttpConnection request = Http.POST(mockWebServer.url("/").url(), "application/json");
        byte[] content = "abcde".getBytes("UTF-8");
        request.setRequestBody(new TestInputStreamGenerator(content));
        testInputStreamRetry(request, content);
    }

    @Test
    public void inputStreamRetryGeneratorWithLength() throws Exception {
        HttpConnection request = Http.POST(mockWebServer.url("/").url(), "application/json");
        byte[] content = "abcde".getBytes("UTF-8");
        request.setRequestBody(new TestInputStreamGenerator(content), content.length);
        testInputStreamRetry(request, content);
    }

    private void testInputStreamRetry(HttpConnection request, byte[] expectedContent) throws
            Exception {
        final MockResponse retry = new MockResponse().setResponseCode(444);
        mockWebServer.enqueue(retry);
        mockWebServer.enqueue(new MockResponse());
        HttpConnection response = CloudantClientHelper.newMockWebServerClientBuilder
                (mockWebServer).interceptors(new HttpConnectionResponseInterceptor() {

            // This interceptor responds to our 444 request with a retry
            @Override
            public HttpConnectionInterceptorContext interceptResponse
            (HttpConnectionInterceptorContext context) {
                try {
                    if (444 == context.connection.getConnection().getResponseCode()) {
                        context.replayRequest = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    fail("IOException getting response code in test interceptor");
                }
                return context;
            }
        }).build().executeRequest(request);

        assertEquals("The final response code should be 200", 200, response.getConnection()
                .getResponseCode());

        // We want the second request
        assertEquals("There should have been two requests", 2, mockWebServer.getRequestCount());
        mockWebServer.takeRequest();
        RecordedRequest rr = mockWebServer.takeRequest();
        assertNotNull("The request should have been recorded", rr);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int) rr
                .getBodySize());
        rr.getBody().copyTo(byteArrayOutputStream);
        assertArrayEquals("The body bytes should have matched after a retry", expectedContent,
                byteArrayOutputStream.toByteArray());
    }

    @Test
    public void testCookieRenewOnPost() throws Exception {

        mockWebServer.enqueue(MockWebServerResource.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(403).setBody
                ("{\"error\":\"credentials_expired\", \"reason\":\"Session expired\"}\r\n"));
        mockWebServer.enqueue(MockWebServerResource.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse());
        
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .username("a")
                .password("b")
                .build();

        HttpConnection request = Http.POST(mockWebServer.url("/").url(), "application/json");
        request.setRequestBody("{\"some\": \"json\"}");
        HttpConnection response = c.executeRequest(request);
        response.getConnection().getResponseCode();
    }
}
