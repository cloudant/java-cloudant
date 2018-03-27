/*
 * Copyright © 2017, 2018 IBM Corp. All rights reserved.
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

import static com.cloudant.tests.util.MockWebServerResources.EXPECTED_OK_COOKIE;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.TooManyRequestsException;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;
import com.cloudant.http.interceptors.BasicAuthInterceptor;
import com.cloudant.http.interceptors.Replay429Interceptor;
import com.cloudant.http.internal.interceptors.CookieInterceptor;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.extensions.CloudantClientExtension;
import com.cloudant.tests.extensions.DatabaseExtension;
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.cloudant.tests.extensions.MultiExtension;
import com.cloudant.tests.util.HttpFactoryParameterizedTest;
import com.cloudant.tests.util.MockWebServerResources;
import com.cloudant.tests.util.TestTimer;
import com.cloudant.tests.util.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.api.function.Executable;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ExtendWith(HttpTest.ParameterProvider.class)
public class HttpTest extends HttpFactoryParameterizedTest {

    static class ParameterProvider implements TestTemplateInvocationContextProvider {
        @Override
        public boolean supportsTestTemplate(ExtensionContext context) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts
                (ExtensionContext context) {
            return Stream.of(invocationContext(false),
                    invocationContext(true));
        }

        public static TestTemplateInvocationContext invocationContext(final boolean okUsable) {
            return new TestTemplateInvocationContext() {
                @Override
                public String getDisplayName(int invocationIndex) {
                    return String.format("okUsable:%s", okUsable);
                }

                @Override
                public List<Extension> getAdditionalExtensions() {
                    return Collections.<Extension>singletonList(new ParameterResolver() {
                        @Override
                        public boolean supportsParameter(ParameterContext parameterContext,
                                                         ExtensionContext extensionContext) {
                            switch (parameterContext.getIndex()) {
                                case 0:
                                    return parameterContext.getParameter().getType().equals
                                            (boolean.class);
                            }
                            return false;
                        }

                        @Override
                        public Object resolveParameter(ParameterContext parameterContext,
                                                       ExtensionContext extensionContext) {
                            switch (parameterContext.getIndex()) {
                                case 0:
                                    return okUsable;
                            }
                            return null;
                        }
                    });
                }
            };
        }
    }

    private String data = "{\"hello\":\"world\"}";

    public static CloudantClientExtension clientResource = new CloudantClientExtension();
    public static DatabaseExtension.PerClass dbResource = new DatabaseExtension.PerClass
            (clientResource);
    public static MockWebServerExtension mockWebServerExt = new MockWebServerExtension();

    @RegisterExtension
    public static MultiExtension extensions = new MultiExtension(clientResource, dbResource,
            mockWebServerExt);

    public MockWebServer mockWebServer;

    @BeforeEach
    public void beforeEach() {
        mockWebServer = mockWebServerExt.get();
    }

    /*
     * Basic test that we can write a document body by POSTing to a known database
     */
    @TestTemplate
    public void testWriteToServerOk() throws Exception {
        HttpConnection conn = new HttpConnection("POST", new URL(dbResource.getDbURIWithUserInfo()),
                "application/json");
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());

        // nothing read from stream
        assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        HttpConnection response = conn.execute();
        // Consume response stream
        String responseStr = response.responseAsString();
        String okPattern = ".*\"ok\"\\s*:\\s*true.*";
        assertTrue(Pattern.compile(okPattern,
                Pattern.DOTALL).matcher(responseStr).matches(), "There should be an ok response: " +
                "" + responseStr);

        // stream was read to end
        assertEquals(0, bis.available());

        assertEquals(2, response.getConnection().getResponseCode
                () / 100, "Should be a 2XX response code");
    }

    /*
     * Basic test to check that an IOException is thrown when we attempt to get the response
     * without first calling execute()
     */
    @TestTemplate
    public void testReadBeforeExecute() throws Exception {
        HttpConnection conn = new HttpConnection("POST", new URL(dbResource.getDbURIWithUserInfo()),
                "application/json");
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());

        // nothing read from stream
        assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        try {
            String response = conn.responseAsString();
            fail("IOException not thrown as expected instead had response " + response);
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
    @TestTemplate
    @RequiresCloudant
    public void testCookieAuthWithoutRetry() throws IOException {
        CookieInterceptor interceptor = new CookieInterceptor(CloudantClientHelper.COUCH_USERNAME,
                CloudantClientHelper.COUCH_PASSWORD, clientResource.get().getBaseUri().toString());

        HttpConnection conn = new HttpConnection("POST", dbResource.get().getDBUri().toURL(),
                "application/json");
        conn.responseInterceptors.add(interceptor);
        conn.requestInterceptors.add(interceptor);
        ByteArrayInputStream bis = new ByteArrayInputStream(data.getBytes());

        // nothing read from stream
        assertEquals(data.getBytes().length, bis.available());

        conn.setRequestBody(bis);
        HttpConnection responseConn = conn.execute();

        // stream was read to end
        assertEquals(0, bis.available());
        assertEquals(2, responseConn.getConnection().getResponseCode() / 100);

        //check the json
        Gson gson = new Gson();
        InputStream is = responseConn.responseAsInputStream();
        try {
            JsonObject response = gson.fromJson(new InputStreamReader(is), JsonObject.class);
            assertTrue(response.has("ok"));
            assertTrue(response.get("ok").getAsBoolean());
            assertTrue(response.has("id"));
            assertTrue(response.has("rev"));
        } finally {
            is.close();
        }
    }

    @TestTemplate
    public void testCookieAuthWithPath() throws Exception {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
        mockWebServer.enqueue(MockWebServerResources.JSON_OK);
        CloudantClient client = ClientBuilder.url(mockWebServer.url("/pathex").url())
                .username("user")
                .password("password")
                .build();
        //send single request
        client.database("test", true);
        assertThat("_session is the second element of the path",
                mockWebServer.takeRequest().getPath(),
                is("/pathex/_session"));
    }

    /**
     * Test that adding the Basic Authentication interceptor to HttpConnection
     * will complete with a response code of 200.  The response input stream
     * is expected to hold the newly created document's id and rev.
     */
    @TestTemplate
    @RequiresCloudant
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
        HttpConnection responseConn = conn.execute();

        // stream was read to end
        assertEquals(0, bis.available());
        assertEquals(2, responseConn.getConnection().getResponseCode() / 100);

        //check the json
        Gson gson = new Gson();
        InputStream is = responseConn.responseAsInputStream();
        try {
            JsonObject response = gson.fromJson(new InputStreamReader(is), JsonObject.class);
            assertTrue(response.has("ok"));
            assertTrue(response.get("ok").getAsBoolean());
            assertTrue(response.has("id"));
            assertTrue(response.has("rev"));
        } finally {
            is.close();
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
    @TestTemplate
    public void cookieInterceptorURLEncoding() throws Exception {
        final String mockUser = "myStrangeUsername=&?";
        String mockPass = "?&=NotAsStrangeInAPassword";

        //expect a cookie request then a GET
        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse());

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .username(mockUser)
                .password(mockPass)
                .build();
        //the GET request will try to get a session, then perform the GET
        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertTrue(response.isEmpty(), "There should be no response body on the mock response");

        RecordedRequest r = MockWebServerResources.takeRequestWithTimeout(mockWebServer);
        String sessionRequestContent = r.getBody().readString(Charset.forName("UTF-8"));
        assertNotNull(sessionRequestContent, "The _session request should have non-null content");
        //expecting name=...&password=...
        String[] parts = Utils.splitAndAssert(sessionRequestContent, "&", 1);
        String username = URLDecoder.decode(Utils.splitAndAssert(parts[0], "=", 1)[1], "UTF-8");
        assertEquals(mockUser, username, "The username URL decoded username should match");
        String password = URLDecoder.decode(Utils.splitAndAssert(parts[1], "=", 1)[1], "UTF-8");
        assertEquals(mockPass, password, "The username URL decoded password should match");
    }

    /**
     * This test check that the cookie is renewed if the server presents a Set-Cookie header
     * after the cookie authentication.
     *
     * @throws Exception
     */
    @TestTemplate
    public void cookieRenewal() throws Exception {
        final String hello = "{\"hello\":\"world\"}\r\n";
        final String renewalCookieToken =
                "RenewCookie_a2ltc3RlYmVsOjUxMzRBQTUzOtiY2_IDUIdsTJEVNEjObAbyhrgz";

        // Request sequence
        // _session request to get Cookie
        // GET request -> 200 with a Set-Cookie
        // GET replay -> 200
        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).addHeader("Set-Cookie",
                MockWebServerResources.authSessionCookie(renewalCookieToken, null))
                .setBody(hello));
        mockWebServer.enqueue(new MockResponse());

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .username("a")
                .password("b")
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response, "The expected response should be received");

        // assert that there were 2 calls
        assertEquals(2, mockWebServer
                .getRequestCount(), "The server should have received 2 requests");

        assertEquals("/_session", MockWebServerResources.takeRequestWithTimeout(mockWebServer)
                .getPath(), "The request should have been for /_session");
        assertEquals("/", MockWebServerResources.takeRequestWithTimeout(mockWebServer).getPath(),
                "The request should have been for /");

        String secondResponse = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertTrue(
                secondResponse.isEmpty(), "There should be no response body on the mock response"
                        + secondResponse);

        // also assert that there were 3 calls
        assertEquals(3, mockWebServer
                .getRequestCount(), "The server should have received 3 requests");

        // this is the request that should have the new cookie.
        RecordedRequest request = MockWebServerResources.takeRequestWithTimeout(mockWebServer);
        assertEquals("/", request.getPath(), "The request should have been for path /");
        String headerValue = request.getHeader("Cookie");
        // The cookie may or may not have the session id quoted, so check both
        assertThat("The Cookie header should contain the expected session value", headerValue,
                anyOf(containsString(MockWebServerResources.authSession(renewalCookieToken)),
                        containsString(MockWebServerResources.authSessionUnquoted
                                (renewalCookieToken))));
    }

    /**
     * This test checks that the cookie is successfully renewed if a 403 with an error of
     * "credentials_expired" is returned.
     *
     * @throws Exception
     */
    @TestTemplate
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
    @TestTemplate
    public void handleNonExpiry403() throws Exception {

        // Test for a non-expiry 403, expect 2 requests
        basic403Test("403_not_expired_test", "example reason", 2);
    }

    /**
     * Same as {@link #handleNonExpiry403()} but with no reason property in the JSON.
     *
     * @throws Exception
     */
    @TestTemplate
    public void handleNonExpiry403NoReason() throws Exception {

        // Test for a non-expiry 403, expect 2 requests
        basic403Test("403_not_expired_test", null, 2);
    }

    /**
     * * Same as {@link #handleNonExpiry403()} but with a {@code null} reason property in the JSON.
     *
     * @throws Exception
     */
    @TestTemplate
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
            String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
            assertTrue(response.isEmpty(), "There should be no response body on the mock response");
            if (!error.equals("credentials_expired")) {
                fail("A 403 not due to cookie expiry should result in a CouchDbException");
            }
        } catch (CouchDbException e) {
            assertEquals(error, e.getError(), "The exception error should be the expected message");
            assertEquals(reason, e
                    .getReason(), "The exception reason should be the expected message");
        }

        // also assert that there were the correct number of calls
        assertEquals(
                expectedRequests, mockWebServer
                        .getRequestCount(), "The server should receive the expected number of " +
                        "requests");
    }

    @TestTemplate
    public void inputStreamRetryString() throws Exception {
        HttpConnection request = Http.POST(mockWebServer.url("/").url(), "application/json");
        String content = "abcde";
        request.setRequestBody(content);
        testInputStreamRetry(request, content.getBytes("UTF-8"));
    }

    @TestTemplate
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

    @TestTemplate
    public void inputStreamRetry() throws Exception {
        HttpConnection request = Http.POST(mockWebServer.url("/").url(), "application/json");
        final byte[] content = "abcde".getBytes("UTF-8");
        // Mock up an input stream that doesn't support marking
        request.setRequestBody(new UnmarkableInputStream(content));
        testInputStreamRetry(request, content);
    }

    @TestTemplate
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

    @TestTemplate
    public void inputStreamRetryGenerator() throws Exception {
        HttpConnection request = Http.POST(mockWebServer.url("/").url(), "application/json");
        byte[] content = "abcde".getBytes("UTF-8");
        request.setRequestBody(new TestInputStreamGenerator(content));
        testInputStreamRetry(request, content);
    }

    @TestTemplate
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
                        // Close the error stream
                        InputStream errors = context.connection.getConnection().getErrorStream();
                        if (errors != null) {
                            IOUtils.closeQuietly(errors);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    fail("IOException getting response code in test interceptor");
                }
                return context;
            }
        }).build().executeRequest(request);

        String responseStr = response.responseAsString();
        assertTrue(responseStr.isEmpty(), "There should be no response body on the mock response");
        assertEquals(200, response.getConnection()
                .getResponseCode(), "The final response code should be 200");

        // We want the second request
        assertEquals(2, mockWebServer.getRequestCount(), "There should have been two requests");
        MockWebServerResources.takeRequestWithTimeout(mockWebServer);
        RecordedRequest rr = MockWebServerResources.takeRequestWithTimeout(mockWebServer);
        assertNotNull(rr, "The request should have been recorded");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream((int) rr
                .getBodySize());
        rr.getBody().copyTo(byteArrayOutputStream);
        assertArrayEquals(expectedContent,
                byteArrayOutputStream.toByteArray(), "The body bytes should have matched after a " +
                        "retry");
    }

    @TestTemplate
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
        String responseStr = response.responseAsString();
        assertTrue(responseStr.isEmpty(), "There should be no response body on the mock response");
        response.getConnection().getResponseCode();
    }

    @TestTemplate
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
        assertEquals(1, mockWebServer.getRequestCount(), "There should have been 1 request");
        RecordedRequest request = MockWebServerResources.takeRequestWithTimeout(mockWebServer);
        assertNotNull(request, "The recorded request should not be null");
        assertNotNull(request.getHeader(headerName), "The custom header should have been present");
        assertEquals(headerValue, request
                .getHeader(headerName), "The custom header should have the specified value");
    }

    /**
     * Test that chunking is used when input stream length is not known.
     *
     * @throws Exception
     */
    @TestTemplate
    public void testChunking() throws Exception {
        mockWebServer.enqueue(new MockResponse());
        final int chunkSize = 1024 * 8;
        final int chunks = 50 * 4;
        CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .build();
        // POST some large random data
        String response = client.executeRequest(Http.POST(mockWebServer.url("/").url(),
                "text/plain")
                .setRequestBody(new RandomInputStreamGenerator(chunks * chunkSize)))
                .responseAsString();
        assertTrue(response.isEmpty(), "There should be no response body on the mock response");
        assertEquals(1, mockWebServer
                .getRequestCount(), "There should have been 1 request");
        RecordedRequest request = MockWebServerResources.takeRequestWithTimeout(mockWebServer);
        assertNotNull(request, "The recorded request should not be null");
        assertNull(request.getHeader("Content-Length"), "There should be no Content-Length header");
        assertEquals("chunked", request.getHeader
                ("Transfer-Encoding"), "The Transfer-Encoding should be chunked");

        // It would be nice to assert that we got the chunk sizes we were expecting, but sadly the
        // HttpURLConnection and ChunkedOutputStream only use the chunkSize as a suggestion and seem
        // to use the buffer size instead. The best assertion we can make is that we did receive
        // multiple chunks.
        assertTrue(request.getChunkSizes().size() > 1, "There should have been at least 2 chunks");
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
    @TestTemplate
    public void test429Backoff() throws Exception {
        mockWebServer.enqueue(MockWebServerResources.get429());
        mockWebServer.enqueue(new MockResponse());

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .interceptors(Replay429Interceptor.WITH_DEFAULTS)
                .build();
        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertTrue(response.isEmpty(), "There should be no response body on the mock response");

        assertEquals(2, mockWebServer.getRequestCount(), "There should be 2 requests");
    }

    /**
     * Test that the default maximum number of retries is reached and the backoff is of at least the
     * expected duration.
     *
     * @throws Exception
     */
    @TestTemplate
    public void test429BackoffMaxDefault() throws Exception {

        // Always respond 429 for this test
        mockWebServer.setDispatcher(MockWebServerResources.ALL_429);

        TestTimer t = TestTimer.startTimer();
        try {
            CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                    .interceptors(Replay429Interceptor.WITH_DEFAULTS)
                    .build();
            String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
            fail("There should be a TooManyRequestsException instead had response " + response);
        } catch (TooManyRequestsException e) {
            long duration = t.stopTimer(TimeUnit.MILLISECONDS);
            // 3 backoff periods for 4 attempts: 250 + 500 + 1000 = 1750 ms
            assertTrue(duration >=
                    1750, "The duration should be at least 1750 ms, but was " + duration);
            assertEquals(4, mockWebServer
                    .getRequestCount(), "There should be 4 request attempts");
        }
    }

    /**
     * Test that the configured maximum number of retries is reached and the backoff is of at least
     * the expected duration.
     *
     * @throws Exception
     */
    @TestTemplate
    public void test429BackoffMaxConfigured() throws Exception {

        // Always respond 429 for this test
        mockWebServer.setDispatcher(MockWebServerResources.ALL_429);


        TestTimer t = TestTimer.startTimer();
        try {
            CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                    .interceptors(new Replay429Interceptor(10, 1, true))
                    .build();
            String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
            fail("There should be a TooManyRequestsException instead had response " + response);
        } catch (TooManyRequestsException e) {
            long duration = t.stopTimer(TimeUnit.MILLISECONDS);
            // 9 backoff periods for 10 attempts: 1 + 2 + 4 + 8 + 16 + 32 + 64 + 128 + 256 = 511 ms
            assertTrue(duration >=
                    511, "The duration should be at least 511 ms, but was " + duration);
            assertEquals(10, mockWebServer
                    .getRequestCount(), "There should be 10 request attempts");
        }
    }

    /**
     * Test that the outer number of configured retries takes precedence.
     *
     * @throws Exception
     */
    @TestTemplate
    public void test429BackoffMaxMoreThanRetriesAllowed() throws Exception {

        // Always respond 429 for this test
        mockWebServer.setDispatcher(MockWebServerResources.ALL_429);

        try {
            CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                    .interceptors(new Replay429Interceptor(10, 1, true))
                    .build();
            String response = c.executeRequest(Http.GET(c.getBaseUri()).setNumberOfRetries(3))
                    .responseAsString();
            fail("There should be a TooManyRequestsException instead had response " + response);
        } catch (TooManyRequestsException e) {
            assertEquals(3, mockWebServer
                    .getRequestCount(), "There should be 3 request attempts");
        }

    }

    /**
     * Test that an integer number of seconds delay specified by a Retry-After header is honoured.
     *
     * @throws Exception
     */
    @TestTemplate
    public void test429BackoffRetryAfter() throws Exception {

        mockWebServer.enqueue(MockWebServerResources.get429().addHeader("Retry-After", "1"));
        mockWebServer.enqueue(new MockResponse());

        TestTimer t = TestTimer.startTimer();
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .interceptors(Replay429Interceptor.WITH_DEFAULTS)
                .build();
        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertTrue(response.isEmpty(), "There should be no response body on the mock response");

        long duration = t.stopTimer(TimeUnit.MILLISECONDS);
        assertTrue(duration >=
                1000, "The duration should be at least 1000 ms, but was " + duration);
        assertEquals(2, mockWebServer
                .getRequestCount(), "There should be 2 request attempts");
    }

    @TestTemplate
    public void test429IgnoreRetryAfter() throws Exception {
        mockWebServer.enqueue(MockWebServerResources.get429().addHeader("Retry-After", "1"));
        mockWebServer.enqueue(new MockResponse());

        TestTimer t = TestTimer.startTimer();
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .interceptors(new Replay429Interceptor(1, 1, false))
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertTrue(response.isEmpty(), "There should be no response body on the mock response");

        long duration = t.stopTimer(TimeUnit.MILLISECONDS);
        assertTrue(duration <
                1000, "The duration should be less than 1000 ms, but was " + duration);
        assertEquals(2, mockWebServer
                .getRequestCount(), "There should be 2 request attempts");
    }

    /**
     * Test the global number of retries
     *
     * @throws Exception
     */
    @TestTemplate
    public void testHttpConnectionRetries() throws Exception {
        // Just return 200 OK
        mockWebServer.setDispatcher(new MockWebServerResources.ConstantResponseDispatcher(200));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .interceptors(new HttpConnectionResponseInterceptor() {
                    @Override
                    public HttpConnectionInterceptorContext interceptResponse
                            (HttpConnectionInterceptorContext context) {
                        // At least do something with the connection, otherwise we risk breaking it
                        try {
                            context.connection.getConnection().getResponseCode();
                        } catch (IOException e) {
                            fail("IOException getting response code");
                        }
                        // Set to always replay
                        context.replayRequest = true;
                        return context;
                    }
                })
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri()).setNumberOfRetries(5))
                .responseAsString();
        assertTrue(response.isEmpty(), "There should be no response body on the mock response");

        assertEquals(5, mockWebServer
                .getRequestCount(), "There should be 5 request attempts");
    }

    /**
     * Test that an IllegalArgumentException is thrown if a https proxy address is used. SSL
     * proxies are not supported. HTTP proxies can tunnel SSL connections to HTTPS servers.
     *
     * @throws Exception
     */
    @TestTemplate
    public void httpsProxyIllegalArgumentException() throws Exception {

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {

                // Get a client pointing to an https proxy
                CloudantClient client = CloudantClientHelper.newMockWebServerClientBuilder
                        (mockWebServer)
                        .proxyURL(new URL("https://192.0.2.0")).build();

                String response = client.executeRequest(Http.GET(client.getBaseUri()))
                        .responseAsString();
                fail("There should be an IllegalStateException for an https proxy.");
            }
        });
    }

    /**
     * Test that the stored cookie is applied to requests for different URLs. Most of the other
     * tests just check a single URL.
     *
     * @throws Exception
     */
    @TestTemplate
    public void cookieAppliedToDifferentURL() throws Exception {
        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
        mockWebServer.enqueue(new MockResponse().setBody("first"));
        mockWebServer.enqueue(new MockResponse().setBody("second"));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .username("a")
                .password("b")
                .build();


        URI baseURI = c.getBaseUri();
        URL first = new URL(baseURI.getScheme(), baseURI.getHost(), baseURI.getPort(), "/testdb");
        String response = c.executeRequest(Http.GET(first)).responseAsString();
        assertEquals("first", response, "The correct response body should be present");

        // There should be a request for a cookie followed by a the real request
        assertEquals(2, mockWebServer.getRequestCount(), "There should be 2 requests");

        assertEquals("/_session", MockWebServerResources.takeRequestWithTimeout(mockWebServer)
                .getPath(), "The first request should have been for a cookie");

        RecordedRequest request = MockWebServerResources.takeRequestWithTimeout(mockWebServer);
        assertEquals("/testdb",
                request.getPath(), "The second request should have been for /testdb");
        assertNotNull(request.getHeader("Cookie"), "There should be a cookie on the request");

        // Now make a request to another URL
        URL second = new URL(baseURI.getScheme(), baseURI.getHost(), baseURI.getPort(),
                "/_all_dbs");

        response = c.executeRequest(Http.GET(second)).responseAsString();
        assertEquals("second", response, "The correct response body should be present");

        // There should now be an additional request
        assertEquals(3, mockWebServer.getRequestCount(), "There should be 3 requests");

        request = MockWebServerResources.takeRequestWithTimeout(mockWebServer);
        assertEquals("/_all_dbs", request.getPath(), "The second request should have been for " +
                "/_all_dbs");
        String cookieHeader = request.getHeader("Cookie");
        assertNotNull(cookieHeader, "There should be a cookie on the request");
        assertTrue(
                request.getHeader("Cookie").contains(EXPECTED_OK_COOKIE), "The cookie header " +
                        cookieHeader + " should contain the expected value.");
    }

    /**
     * Test that cookie authentication is stopped if the credentials were bad.
     *
     * @throws Exception
     */
    @TestTemplate
    public void badCredsDisablesCookie() throws Exception {
        mockWebServer.setDispatcher(new Dispatcher() {
            private int counter = 0;

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                counter++;
                // Return a 401 for the first _session request, after that return 200 OKs
                if (counter == 1) {
                    return new MockResponse().setResponseCode(401);
                } else {
                    return new MockResponse().setBody("TEST");
                }
            }
        });

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .username("bad")
                .password("worse")
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals("TEST", response, "The expected response body should be received");

        // There should only be two requests: an initial auth failure followed by an ok response.
        // If the cookie interceptor keeps trying then there will be more _session requests.
        assertEquals(2, mockWebServer.getRequestCount(), "There should be 2 requests");

        assertEquals("/_session",
                MockWebServerResources.takeRequestWithTimeout(mockWebServer).getPath(), "The " +
                        "first request should have been for a cookie");
        assertEquals("/",
                MockWebServerResources.takeRequestWithTimeout(mockWebServer).getPath(), "The " +
                        "second request should have been for /");

        response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals("TEST", response, "The expected response body should be received");

        // Make another request, the cookie interceptor should not try again so there should only be
        // one more request.
        assertEquals(3, mockWebServer.getRequestCount(), "There should be 3 requests");
        assertEquals("/", MockWebServerResources.takeRequestWithTimeout(mockWebServer).getPath(),
                "The third request should have been for /");
    }

    /**
     * Test that having no body and hence no error stream on a 403 response correctly results in the
     * 403 causing a CouchDbException with no NPEs.
     *
     * @throws Exception
     */
    @TestTemplate
    public void noErrorStream403() throws Exception {

        assertThrows(CouchDbException.class, new Executable() {
            @Override
            public void execute() throws Throwable {

                // Respond with a cookie init to the first request to _session
                mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
                // Respond to the executeRequest GET of / with a 403 with no body
                mockWebServer.enqueue(new MockResponse().setResponseCode(403));
                CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                        .username("a")
                        .password("b")
                        .build();

                String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
                fail("There should be an exception, but received response " + response);
            }
        });
    }

    /**
     * Test that having no body and hence no error stream on a 401 response correctly results in a
     * 401 cookie renewal without a NPE.
     *
     * @throws Exception
     */
    @TestTemplate
    public void noErrorStream401() throws Exception {

        // Respond with a cookie init to the first request to _session
        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
        // Respond to the executeRequest GET of / with a 401 with no body
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));
        // 401 triggers a renewal so respond with a new cookie for renewal request to _session
        mockWebServer.enqueue(MockWebServerResources.OK_COOKIE);
        // Finally respond 200 OK with body of "TEST" to the replay of GET to /
        mockWebServer.enqueue(new MockResponse().setBody("TEST"));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .username("a")
                .password("b")
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals("TEST", response, "The expected response body should be received");
    }

    /**
     * Tests that loading the OkHelper will not try to use any classes from outside the
     * cloudant-http built classes. Specifically it won't cause any okhttp classes to try to be
     * loaded outside of the controlled check for the presence of okhttp.
     *
     * @throws ClassNotFoundException if the load tries to use any classes it shouldn't
     * @throws Exception              if there is another issue in the test
     */
    @TestTemplate
    public void okUsableClassLoad() throws ClassNotFoundException, Exception {
        // Point to the built classes, it's a bit awkward but we need to load the class cleanly
        File f = new File("../cloudant-http/build/classes/java/main/");
        ClassLoader loader = new CloudantHttpIsolationClassLoader(new URL[]{f.toURI().toURL()});
        Class<?> okHelperClass = Class.forName("com.cloudant.http.internal.ok.OkHelper"
                , true, loader);
        assertEquals(isOkUsable, okHelperClass.getMethod
                ("isOkUsable").invoke(null), "The isOkUsable value should be correct");
    }

    /**
     * A simple classloader that has a null parent classloader when testing okUsable false. This
     * isolates classes loaded with this classloader from the test classpath and hence any
     * attempt to load a class from the okhttp library will result in a NoClassDefFound.
     */
    public class CloudantHttpIsolationClassLoader extends URLClassLoader {

        public CloudantHttpIsolationClassLoader(URL[] urls) {
            // If we are testing okhttp then allow the parent classloader, otherwise use null
            // to isolate okhttp classes from the test load
            super(urls, isOkUsable ? CloudantHttpIsolationClassLoader.class.getClassLoader() :
                    null);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }
    }

    // helper - assert that _n_ requests were made on the mock server and return them in an array
    public static RecordedRequest[] takeN(MockWebServer server, int n) throws Exception {
        assertEquals(n,
                server.getRequestCount(), String.format(Locale.ENGLISH, "The server should have " +
                        "%d received requests", n));
        RecordedRequest[] recordedRequests = new RecordedRequest[n];
        for (int i = 0; i < n; i++) {
            recordedRequests[i] = MockWebServerResources.takeRequestWithTimeout(server);
        }
        return recordedRequests;
    }


}
