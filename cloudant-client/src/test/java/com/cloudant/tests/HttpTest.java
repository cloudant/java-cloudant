package com.cloudant.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.TooManyRequestsException;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;
import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.http.interceptors.CookieInterceptor;
import com.cloudant.http.interceptors.Replay429Interceptor;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.MockWebServerResources;
import com.cloudant.tests.util.TestTimer;
import com.cloudant.tests.util.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

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
    public MockWebServer mockWebServer = new MockWebServer();

    /*
     * Basic test that we can write a document body by POSTing to a known database
     */
    @Test
    public void testWriteToServerOk() throws Exception {
        HttpConnection conn = new HttpConnection("POST", new URL(dbResource.getDbURIWithUserInfo()),
                "application/json");
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());

        // nothing read from stream
        assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        conn.execute();

        // stream was read to end
        assertEquals(0, bis.available());
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
        assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        try {
            conn.responseAsString();
            fail("IOException not thrown as expected");
        } catch (IOException ioe) {
            ; // "Attempted to read response from server before calling execute()"
        }

        // stream was not read because execute() was not called
        assertEquals(data.getBytes().length, bis.available());
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
        assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        conn.execute();

        // stream was read to end
        assertEquals(0, bis.available());
        assertEquals(2, conn.getConnection().getResponseCode() / 100);

        //check the json
        Gson gson = new Gson();
        JsonObject response = gson.fromJson(new InputStreamReader(conn.getConnection()
                .getInputStream()), JsonObject.class);

        assertTrue(response.has("ok"));
        assertTrue(response.get("ok").getAsBoolean());
        assertTrue(response.has("id"));
        assertTrue(response.has("rev"));
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
        assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        conn.execute();

        // stream was read to end
        assertEquals(0, bis.available());
        assertEquals(2, conn.getConnection().getResponseCode() / 100);

        //check the json
        Gson gson = new Gson();
        JsonObject response = gson.fromJson(new InputStreamReader(conn.getConnection()
                .getInputStream()), JsonObject.class);

        assertTrue(response.has("ok"));
        assertTrue(response.get("ok").getAsBoolean());
        assertTrue(response.has("id"));
        assertTrue(response.has("rev"));
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
        server.enqueue(MockWebServerResources.OK_COOKIE);
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
            assertEquals("The username URL decoded username should match", mockUser,
                    username);
            String password = URLDecoder.decode(Utils.splitAndAssert(parts[1], "=", 1)[1], "UTF-8");
            assertEquals("The username URL decoded password should match", mockPass,
                    password);
        } finally {
            server.shutdown();
        }
    }

    /**
     * This test check that the cookie is renewed if the server presents a Set-Cookie header
     * after the cookie authentication.
     *
     * @throws Exception
     */
    @Test
    public void cookieRenewal() throws Exception {

        final String renewalCookieValue =
                "AuthSession=\"RenewCookie_a2ltc3RlYmVsOjUxMzRBQTUzOtiY2_IDUIdsTJEVNEjObAbyhrgz\";";
        // Request sequence
        // _session request to get Cookie
        // GET request -> 200 with a Set-Cookie
        // _session for new cookie
        // GET replay -> 200
        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
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
        assertEquals
                ("AuthSession=\"RenewCookie_a2ltc3RlYmVsOjUxMzRBQTUzOtiY2_IDUIdsTJEVNEjObAbyhrgz" +
                        "\"", headerValue);
    }

    /**
     * This test checks that the cookie is successfully renewed if a 403 with an error of
     * "credentials_expired" is returned.
     *
     * @throws Exception
     */
    @Test
    public void cookie403Renewal() throws Exception {

        // Test for a 403 with expired credentials, should result in 4 requests
        basic403Test("credentials_expired", "Session expired", 4);
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

        // Test for a non-expiry 403, expect 2 requests
        basic403Test("403_not_expired_test", "example reason", 2);
    }

    /**
     * Same as {@link #handleNonExpiry403()} but with no reason property in the JSON.
     *
     * @throws Exception
     */
    @Test
    public void handleNonExpiry403NoReason() throws Exception {

        // Test for a non-expiry 403, expect 2 requests
        basic403Test("403_not_expired_test", null, 2);
    }

    /**
     * * Same as {@link #handleNonExpiry403()} but with a {@code null} reason property in the JSON.
     *
     * @throws Exception
     */
    @Test
    public void handleNonExpiry403NullReason() throws Exception {

        // Test for a non-expiry 403, expect 2 requests
        basic403Test("403_not_expired_test", "null", 2);
    }

    /**
     * Method that performs a basic test for a 403 response. The sequence of requests is:
     * <OL>
     * <LI>_session request to get Cookie</LI>
     * <LI>GET request -> a 403 response</LI>
     * <LI>_session for new cookie*</LI>
     * <LI>GET replay -> a 200 response*</LI>
     * </OL>
     * The requests annotated * should only happen in the credentials_expired 403 case
     *
     * @param error  the response JSON error content for the 403
     * @param reason the response JSON reason content for the 403
     */
    private void basic403Test(String error, String reason, int expectedRequests) throws
            Exception {
        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
        JsonObject responseBody = new JsonObject();
        responseBody.add("error", new JsonPrimitive(error));
        JsonElement jsonReason;
        if (reason != null) {
            if ("null".equals(reason)) {
                jsonReason = JsonNull.INSTANCE;
                reason = null; // For the assertion we need a real null, not a JsonNull
            } else {
                jsonReason = new JsonPrimitive(reason);
            }
            responseBody.add("reason", jsonReason);
        }
        mockWebServer.enqueue(new MockResponse().setResponseCode(403).setBody(responseBody
                .toString()));
        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse());

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .username("a")
                .password("b")
                .build();

        //the GET request will try to get a session, then perform the GET
        //the GET will result in a 403, which in a renewal case should mean another request to
        // _session followed by a replay of GET
        try {
            c.executeRequest(Http.GET(c.getBaseUri()));
            if (!error.equals("credentials_expired")) {
                fail("A 403 not due to cookie expiry should result in a CouchDbException");
            }
        } catch (CouchDbException e) {
            assertEquals("The exception error should be the expected message", error, e.getError());
            assertEquals("The exception reason should be the expected message", reason, e
                    .getReason());
        }

        // also assert that there were the correct number of calls
        assertEquals("The server should receive the expected number of requests",
                expectedRequests, mockWebServer
                        .getRequestCount());
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

        TestInputStreamGenerator(byte[] content) {
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

        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(403).setBody
                ("{\"error\":\"credentials_expired\", \"reason\":\"Session expired\"}\r\n"));
        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
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

    @Test
    public void testCustomHeader() throws Exception {
        mockWebServer.enqueue(new MockResponse());
        final String headerName = "Test-Header";
        final String headerValue = "testHeader";
        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .interceptors(new HttpConnectionRequestInterceptor() {

                    @Override
                    public HttpConnectionInterceptorContext interceptRequest
                            (HttpConnectionInterceptorContext context) {
                        context.connection.requestProperties.put(headerName, headerValue);
                        return context;
                    }
                }).build();
        client.getAllDbs();
        assertEquals("There should have been 1 request", 1, mockWebServer.getRequestCount());
        RecordedRequest request = mockWebServer.takeRequest();
        assertNotNull("The recorded request should not be null", request);
        assertNotNull("The custom header should have been present", request.getHeader(headerName));
        assertEquals("The custom header should have the specified value", headerValue, request
                .getHeader(headerName));
    }

    /**
     * Test that chunking is used when input stream length is not known.
     *
     * @throws Exception
     */
    @Test
    public void testChunking() throws Exception {
        mockWebServer.enqueue(new MockResponse());
        final int chunkSize = 1024 * 8;
        final int chunks = 50 * 4;
        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .build();
        // POST some large random data
        client.executeRequest(Http.POST(mockWebServer.url("/").url(), "text/plain")
                .setRequestBody(new RandomInputStreamGenerator(chunks * chunkSize)));
        assertEquals("There should have been 1 request", 1, mockWebServer
                .getRequestCount());
        RecordedRequest request = mockWebServer.takeRequest();
        assertNotNull("The recorded request should not be null", request);
        assertNull("There should be no Content-Length header", request.getHeader("Content-Length"));
        assertEquals("The Transfer-Encoding should be chunked", "chunked", request.getHeader
                ("Transfer-Encoding"));

        // It would be nice to assert that we got the chunk sizes we were expecting, but sadly the
        // HttpURLConnection and ChunkedOutputStream only use the chunkSize as a suggestion and seem
        // to use the buffer size instead. The best assertion we can make is that we did receive
        // multiple chunks.
        assertTrue("There should have been at least 2 chunks", request.getChunkSizes().size() > 1);
    }

    private static final class RandomInputStreamGenerator implements HttpConnection
            .InputStreamGenerator {
        final byte[] content;

        RandomInputStreamGenerator(int sizeOfRandomContent) {
            // Construct a byte array of random data
            content = new byte[sizeOfRandomContent];
            new java.util.Random().nextBytes(content);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }
    }

    /**
     * Test that a request is replayed in response to a 429.
     *
     * @throws Exception
     */
    @Test
    public void test429Backoff() throws Exception {
        mockWebServer.enqueue(MockWebServerResources.get429());
        mockWebServer.enqueue(new MockResponse());

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .interceptors(Replay429Interceptor.WITH_DEFAULTS)
                .build();
        c.executeRequest(Http.GET(c.getBaseUri()));

        assertEquals("There should be 2 requests", 2, mockWebServer.getRequestCount());
    }

    /**
     * Test that the default maximum number of retries is reached and the backoff is of at least the
     * expected duration.
     *
     * @throws Exception
     */
    @Test
    public void test429BackoffMaxDefault() throws Exception {

        // Always respond 429 for this test
        mockWebServer.setDispatcher(MockWebServerResources.ALL_429);

        TestTimer t = TestTimer.startTimer();
        try {
            CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                    .interceptors(Replay429Interceptor.WITH_DEFAULTS)
                    .build();
            c.executeRequest(Http.GET(c.getBaseUri()));
            fail("There should be a TooManyRequestsException");
        } catch (TooManyRequestsException e) {
            long duration = t.stopTimer(TimeUnit.MILLISECONDS);
            // 3 backoff periods for 4 attempts: 250 + 500 + 1000 = 1750 ms
            assertTrue("The duration should be at least 1750 ms, but was " + duration, duration >=
                    1750);
            assertEquals("There should be 4 request attempts", 4, mockWebServer
                    .getRequestCount());
        }
    }

    /**
     * Test that the configured maximum number of retries is reached and the backoff is of at least
     * the expected duration.
     *
     * @throws Exception
     */
    @Test
    public void test429BackoffMaxConfigured() throws Exception {

        // Always respond 429 for this test
        mockWebServer.setDispatcher(MockWebServerResources.ALL_429);


        TestTimer t = TestTimer.startTimer();
        try {
            CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                    .interceptors(new Replay429Interceptor(10, 1, true))
                    .build();
            c.executeRequest(Http.GET(c.getBaseUri()));
            fail("There should be a TooManyRequestsException");
        } catch (TooManyRequestsException e) {
            long duration = t.stopTimer(TimeUnit.MILLISECONDS);
            // 9 backoff periods for 10 attempts: 1 + 2 + 4 + 8 + 16 + 32 + 64 + 128 + 256 = 511 ms
            assertTrue("The duration should be at least 511 ms, but was " + duration, duration >=
                    511);
            assertEquals("There should be 10 request attempts", 10, mockWebServer
                    .getRequestCount());
        }
    }

    /**
     * Test that the outer number of configured retries takes precedence.
     *
     * @throws Exception
     */
    @Test
    public void test429BackoffMaxMoreThanRetriesAllowed() throws Exception {

        // Always respond 429 for this test
        mockWebServer.setDispatcher(MockWebServerResources.ALL_429);

        try {
            CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                    .interceptors(new Replay429Interceptor(10, 1, true))
                    .build();
            c.executeRequest(Http.GET(c.getBaseUri()).setNumberOfRetries(3));
            fail("There should be a TooManyRequestsException");
        } catch (TooManyRequestsException e) {
            assertEquals("There should be 3 request attempts", 3, mockWebServer
                    .getRequestCount());
        }

    }

    /**
     * Test that an integer number of seconds delay specified by a Retry-After header is honoured.
     *
     * @throws Exception
     */
    @Test
    public void test429BackoffRetryAfter() throws Exception {

        mockWebServer.enqueue(MockWebServerResources.get429().addHeader("Retry-After", "1"));
        mockWebServer.enqueue(new MockResponse());

        TestTimer t = TestTimer.startTimer();
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .interceptors(Replay429Interceptor.WITH_DEFAULTS)
                .build();
        c.executeRequest(Http.GET(c.getBaseUri()));

        long duration = t.stopTimer(TimeUnit.MILLISECONDS);
        assertTrue("The duration should be at least 1000 ms, but was " + duration, duration >=
                1000);
        assertEquals("There should be 2 request attempts", 2, mockWebServer
                .getRequestCount());
    }

    @Test
    public void test429IgnoreRetryAfter() throws Exception {
        mockWebServer.enqueue(MockWebServerResources.get429().addHeader("Retry-After", "1"));
        mockWebServer.enqueue(new MockResponse());

        TestTimer t = TestTimer.startTimer();
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .interceptors(new Replay429Interceptor(1, 1, false))
                .build();

        c.executeRequest(Http.GET(c.getBaseUri()));

        long duration = t.stopTimer(TimeUnit.MILLISECONDS);
        assertTrue("The duration should be less than 1000 ms, but was " + duration, duration <
                1000);
        assertEquals("There should be 2 request attempts", 2, mockWebServer
                .getRequestCount());
    }

    /**
     * Test the global number of retries
     * @throws Exception
     */
    @Test
    public void testHttpConnectionRetries() throws Exception {
        // Just return 200 OK
        mockWebServer.setDispatcher(new MockWebServerResources.ConstantResponseDispatcher(200));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .interceptors(new HttpConnectionResponseInterceptor() {
                    @Override
                    public HttpConnectionInterceptorContext interceptResponse
                            (HttpConnectionInterceptorContext context) {
                        // At least do something with the connection, otherwise we risk breaking it
                        try{
                            context.connection.getConnection().getResponseCode();
                        } catch(IOException e ) {
                            fail("IOException getting response code");
                        }
                        // Set to always replay
                        context.replayRequest = true;
                        return context;
                    }
                })
                .build();

        c.executeRequest(Http.GET(c.getBaseUri()).setNumberOfRetries(5));

        assertEquals("There should be 5 request attempts", 5, mockWebServer
                .getRequestCount());
    }
}
