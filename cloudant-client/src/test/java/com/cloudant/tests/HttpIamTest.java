/*
 * Copyright © 2017, 2019 IBM Corp. All rights reserved.
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

import static com.cloudant.http.internal.interceptors.IamCookieInterceptor.IAM_TOKEN_SERVER_URL_PROPERTY_KEY;
import static com.cloudant.tests.HttpTest.takeN;
import static com.cloudant.tests.util.MockWebServerResources.EXPECTED_OK_COOKIE;
import static com.cloudant.tests.util.MockWebServerResources.EXPECTED_OK_COOKIE_2;
import static com.cloudant.tests.util.MockWebServerResources.IAM_API_KEY;
import static com.cloudant.tests.util.MockWebServerResources.IAM_TOKEN;
import static com.cloudant.tests.util.MockWebServerResources.IAM_TOKEN_2;
import static com.cloudant.tests.util.MockWebServerResources.OK_IAM_COOKIE;
import static com.cloudant.tests.util.MockWebServerResources.OK_IAM_COOKIE_2;
import static com.cloudant.tests.util.MockWebServerResources.assertMockIamCloudantRequests;
import static com.cloudant.tests.util.MockWebServerResources.assertMockIamServerRequests;
import static com.cloudant.tests.util.MockWebServerResources.hello;
import static com.cloudant.tests.util.MockWebServerResources.iamSession;
import static com.cloudant.tests.util.MockWebServerResources.iamSessionUnquoted;
import static com.cloudant.tests.util.MockWebServerResources.iamTokenEndpoint;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.http.Http;
import com.cloudant.http.interceptors.Replay429Interceptor;
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.cloudant.tests.util.MockWebServerResources;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.nio.charset.Charset;

/**
 * Created by tomblench on 30/06/2017.
 */

public class HttpIamTest {

    @RegisterExtension
    public MockWebServerExtension mockWebServerExt = new MockWebServerExtension();

    @RegisterExtension
    public MockWebServerExtension mockIamServerExt = new MockWebServerExtension();

    public MockWebServer mockWebServer;
    public MockWebServer mockIamServer;

    /**
     * Before each test set the value of the endpoint in the property mock
     */
    @BeforeEach
    public void setIAMMockEndpoint() {
        mockWebServer = mockWebServerExt.get();
        mockIamServer = mockIamServerExt.get();
        // Override the default IAM token server with our test mock server
        System.setProperty(IAM_TOKEN_SERVER_URL_PROPERTY_KEY, mockIamServer.url(iamTokenEndpoint)
                .toString());
    }

    @AfterEach
    public void clearIAMMock() {
        System.clearProperty(IAM_TOKEN_SERVER_URL_PROPERTY_KEY);
    }

    /**
     * Test IAM token and cookie flow:
     * - GET a resource on the cloudant server
     * - Cookie jar empty, so get IAM token followed by session cookie
     * - GET now proceeds as normal, expected cookie value is sent in header
     *
     * @throws Exception
     */
    @Test
    public void iamTokenAndCookieSuccessful() throws Exception {

        // Request sequence
        // _iam_session request to get Cookie
        // GET request -> 200 with a Set-Cookie
        mockWebServer.enqueue(OK_IAM_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));

        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .iamApiKey(IAM_API_KEY)
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response, "The expected response should be received");

        MockWebServerResources.assertMockIamRequests(mockWebServer, mockIamServer);
    }

    @Test
    public void iamTokenWithValidClientIdAndSecretAndCookieSuccessful() throws Exception {

        // Request sequence
        // _iam_session request to get Cookie
        // GET request -> 200 with a Set-Cookie
        mockWebServer.enqueue(OK_IAM_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(hello));
        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN));

        final String mockIAMUser = "iamServerUser";
        final String mockIAMPass = "iamServerPass";
        final String mockAuthHeader = "Basic aWFtU2VydmVyVXNlcjppYW1TZXJ2ZXJQYXNz";

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .iamApiKey(IAM_API_KEY, mockIAMUser, mockIAMPass)
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response, "The expected response should be received");

        assertMockIamCloudantRequests(takeN(mockWebServer, 2));

        RecordedRequest recordedIAMRequest = takeN(mockIamServer, 1)[0];
        assertMockIamServerRequests(recordedIAMRequest);
        assertEquals(recordedIAMRequest.getHeader("Authorization"), mockAuthHeader);
    }

    /**
     * Assert that the IAM API key is preferred to username/password if both are supplied.
     * As above test but with different builder arguments.
     *
     * @throws Exception
     */
    @Test
    public void iamApiKeyPreferredToUsernamePassword() throws Exception {

        // Request sequence
        // _iam_session request to get Cookie
        // GET request -> 200 with a Set-Cookie
        mockWebServer.enqueue(OK_IAM_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));

        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .username("username")
                .password("password")
                .iamApiKey(IAM_API_KEY)
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response, "The expected response should be received");

        MockWebServerResources.assertMockIamRequests(mockWebServer, mockIamServer);
    }

    /**
     * Test IAM token and cookie flow, where session expires and is successfully renewed:
     * - GET a resource on the cloudant server
     * - Cookie jar empty, so get IAM token followed by session cookie
     * - GET now proceeds as normal, expected cookie value is sent in header
     * - second GET on cloudant server, re-using session cookie
     * - third GET on cloudant server, cookie expired, get IAM token and session cookie and replay
     * request
     *
     * @throws Exception
     */
    @Test
    public void iamTokenAndCookieWithExpirySuccessful() throws Exception {

        // Request sequence
        mockWebServer.enqueue(OK_IAM_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));
        // cookie expired
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody
                ("{\"error\":\"credentials_expired\"}"));
        // response with new cookie
        mockWebServer.enqueue(OK_IAM_COOKIE_2);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));

        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN));
        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN_2));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .iamApiKey(IAM_API_KEY)
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response, "The expected response should be received");

        String response2 = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response2, "The expected response should be received");

        String response3 = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response3, "The expected response should be received");

        // cloudant mock server

        // assert that there were 6 calls
        RecordedRequest[] recordedRequests = takeN(mockWebServer, 6);

        assertEquals("/_iam_session",
                recordedRequests[0].getPath(), "The request should have been for /_iam_session");
        assertThat("The request body should contain the IAM token",
                recordedRequests[0].getBody().readString(Charset.forName("UTF-8")),
                containsString(IAM_TOKEN));
        // first request
        assertEquals("/",
                recordedRequests[1].getPath(), "The request should have been for /");
        // The cookie may or may not have the session id quoted, so check both
        assertThat("The Cookie header should contain the expected session value",
                recordedRequests[1].getHeader("Cookie"),
                anyOf(containsString(iamSession(EXPECTED_OK_COOKIE)),
                        containsString(iamSessionUnquoted(EXPECTED_OK_COOKIE))));
        // second request
        assertEquals("/",
                recordedRequests[2].getPath(), "The request should have been for /");
        // third request, will be rejected due to cookie expiry
        assertEquals("/",
                recordedRequests[3].getPath(), "The request should have been for /");
        // renew cookie after third request fails
        assertEquals("/_iam_session",
                recordedRequests[4].getPath(), "The request should have been for /_iam_session");
        assertThat("The request body should contain the IAM token",
                recordedRequests[4].getBody().readString(Charset.forName("UTF-8")),
                containsString(IAM_TOKEN_2));
        // replay of third request
        assertEquals("/",
                recordedRequests[5].getPath(), "The request should have been for /");
        // The (new) cookie may or may not have the session id quoted, so check both
        assertThat("The Cookie header should contain the expected session value",
                recordedRequests[5].getHeader("Cookie"),
                anyOf(containsString(iamSession(EXPECTED_OK_COOKIE_2)),
                        containsString(iamSessionUnquoted(EXPECTED_OK_COOKIE_2))));

        // iam mock server

        // assert that there were 2 calls
        RecordedRequest[] recordedIamRequests = takeN(mockIamServer, 2);
        // first time, automatically fetch because cookie jar is empty
        assertEquals(iamTokenEndpoint,
                recordedIamRequests[0].getPath(), "The request should have been for " +
                        "/identity/token");
        assertThat("The request body should contain the IAM API key",
                recordedIamRequests[0].getBody().readString(Charset.forName("UTF-8")),
                containsString("apikey=" + IAM_API_KEY));
        // second time, refresh because the cloudant session cookie has expired
        assertEquals(iamTokenEndpoint,
                recordedIamRequests[1].getPath(), "The request should have been for " +
                        "/identity/token");
    }

    /**
     * Test IAM token and cookie flow, where session expires and subsequent IAM token fails:
     * - GET a resource on the cloudant server
     * - Cookie jar empty, so get IAM token followed by session cookie
     * - GET now proceeds as normal, expected cookie value is sent in header
     * - second GET on cloudant server, re-using session cookie
     * - third GET on cloudant server, cookie expired, subsequent IAM token fails
     * - exception is thrown
     *
     * @throws Exception
     */
    @Test
    public void iamRenewalFailureOnIamToken() throws Exception {

        // Request sequence
        mockWebServer.enqueue(OK_IAM_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));
        // cookie expired
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody
                ("{\"error\":\"credentials_expired\"}"));

        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN));
        // mock IAM going down randomly
        mockIamServer.enqueue(new MockResponse().setResponseCode(500));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .iamApiKey(IAM_API_KEY)
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response, "The expected response should be received");

        String response2 = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response2, "The expected response should be received");

        // this never gets a response because the token failure stops the playback - this is
        // correct because the underlying stream has now been closed but the exception is a bit
        // unhelpful
        CouchDbException re =
                assertThrows(CouchDbException.class,
                        () -> c.executeRequest(Http.GET(c.getBaseUri())).responseAsString(),
                        "Failure to get a token should throw a CouchDbException.");
        assertTrue(re.getMessage().startsWith("HTTP response error getting session"), "The " +
                "exception should have been for a HTTP response error.");

        // cloudant mock server

        // assert that there were 4 calls
        RecordedRequest[] recordedRequests = takeN(mockWebServer, 4);
        assertEquals("/_iam_session",
                recordedRequests[0].getPath(), "The request should have been for /_iam_session");
        assertThat("The request body should contain the IAM token",
                recordedRequests[0].getBody().readString(Charset.forName("UTF-8")),
                containsString(IAM_TOKEN));
        // first request
        assertEquals("/",
                recordedRequests[1].getPath(), "The request should have been for /");
        // The cookie may or may not have the session id quoted, so check both
        assertThat("The Cookie header should contain the expected session value",
                recordedRequests[1].getHeader("Cookie"),
                anyOf(containsString(iamSession(EXPECTED_OK_COOKIE)),
                        containsString(iamSessionUnquoted(EXPECTED_OK_COOKIE))));
        // second request
        assertEquals("/",
                recordedRequests[2].getPath(), "The request should have been for /");
        // third request, will be rejected due to cookie expiry
        assertEquals("/",
                recordedRequests[3].getPath(), "The request should have been for /");

        // iam mock server

        // assert that there were 2 calls
        RecordedRequest[] recordedIamRequests = takeN(mockIamServer, 2);
        // first time, automatically fetch because cookie jar is empty
        assertEquals(iamTokenEndpoint,
                recordedIamRequests[0].getPath(), "The request should have been for " +
                        "/identity/token");
        assertThat("The request body should contain the IAM API key",
                recordedIamRequests[0].getBody().readString(Charset.forName("UTF-8")),
                containsString("apikey=" + IAM_API_KEY));
        // second time, refresh (but gets 500) because the cloudant session cookie has expired
        assertEquals(iamTokenEndpoint,
                recordedIamRequests[1].getPath(), "The request should have been for " +
                        "/identity/token");
    }

    /**
     * Test IAM token and cookie flow, where session expires and subsequent session cookie fails:
     * - GET a resource on the cloudant server
     * - Cookie jar empty, so get IAM token followed by session cookie
     * - GET now proceeds as normal, expected cookie value is sent in header
     * - second GET on cloudant server, re-using session cookie
     * - third GET on cloudant server, cookie expired, get IAM token, subsequent session cookie
     * request fails, CouchDbException
     *
     * @throws Exception
     */
    @Test
    public void iamRenewalFailureOnSessionCookie() throws Exception {

        // Request sequence
        mockWebServer.enqueue(OK_IAM_COOKIE);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));
        // cookie expired
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).
                setBody("{\"error\":\"credentials_expired\"}"));
        // mock 500 on cookie renewal
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN));
        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN_2));

        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .iamApiKey(IAM_API_KEY)
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response, "The expected response should be received");

        String response2 = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response2, "The expected response should be received");

        assertThrows(CouchDbException.class,
                () -> c.executeRequest(Http.GET(c.getBaseUri())).responseAsString(), "Should get " +
                        "a CouchDbException when _iam_session renewal fails.");

        // cloudant mock server

        // assert that there were 5 calls
        RecordedRequest[] recordedRequests = takeN(mockWebServer, 5);

        assertEquals("/_iam_session",
                recordedRequests[0].getPath(), "The request should have been for /_iam_session");
        assertThat("The request body should contain the IAM token",
                recordedRequests[0].getBody().readString(Charset.forName("UTF-8")),
                containsString(IAM_TOKEN));
        // first request
        assertEquals("/",
                recordedRequests[1].getPath(), "The request should have been for /");
        // The cookie may or may not have the session id quoted, so check both
        assertThat("The Cookie header should contain the expected session value",
                recordedRequests[1].getHeader("Cookie"),
                anyOf(containsString(iamSession(EXPECTED_OK_COOKIE)),
                        containsString(iamSessionUnquoted(EXPECTED_OK_COOKIE))));
        // second request
        assertEquals("/",
                recordedRequests[2].getPath(), "The request should have been for /");
        // third request, will be rejected due to cookie expiry
        assertEquals("/",
                recordedRequests[3].getPath(), "The request should have been for /");
        // try to renew cookie but get 500
        assertEquals("/_iam_session",
                recordedRequests[4].getPath(), "The request should have been for /_iam_session");
        assertThat("The request body should contain the IAM token",
                recordedRequests[4].getBody().readString(Charset.forName("UTF-8")),
                containsString(IAM_TOKEN_2));

        // iam mock server

        // assert that there were 2 calls
        RecordedRequest[] recordedIamRequests = takeN(mockIamServer, 2);
        // first time, automatically fetch because cookie jar is empty
        assertEquals(iamTokenEndpoint,
                recordedIamRequests[0].getPath(), "The request should have been for " +
                        "/identity/token");
        assertThat("The request body should contain the IAM API key",
                recordedIamRequests[0].getBody().readString(Charset.forName("UTF-8")),
                containsString("apikey=" + IAM_API_KEY));
        // second time, refresh because the cloudant session cookie has expired
        assertEquals(iamTokenEndpoint,
                recordedIamRequests[1].getPath(), "The request should have been for " +
                        "/identity/token");
    }

    @Test
    public void iamTokenServer429RetryAndSucceed() throws Exception {
        // Mock request sequence
        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN));
        mockWebServer.enqueue(OK_IAM_COOKIE);
        // First get request succeeds
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));

        // Second get request has a 401 cookie expired
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).
                setBody("{\"error\":\"credentials_expired\"}"));
        // IAM server 429 on token request
        mockIamServer.enqueue(new MockResponse().setStatus("HTTP/1.1 429 Too many requests"));
        // Success on retry
        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN_2));
        mockWebServer.enqueue(OK_IAM_COOKIE_2);
        // Second get request suceeds after renewal
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));

        // Request sequence
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .iamApiKey(IAM_API_KEY)
                .interceptors(Replay429Interceptor.WITH_DEFAULTS)
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response, "The expected response should be received");

        String response2 = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response2, "The expected response should be received");

        // iam mock server

        // assert that there were 3 calls
        RecordedRequest[] recordedIamRequests = takeN(mockIamServer, 3);
        // first time, automatically fetch because cookie jar is empty
        assertEquals(iamTokenEndpoint,
                recordedIamRequests[0].getPath(), "The request should have been for " +
                        "/identity/token");
        assertThat("The request body should contain the IAM API key",
                recordedIamRequests[0].getBody().readString(Charset.forName("UTF-8")),
                containsString("apikey=" + IAM_API_KEY));
        // second time, 429 response
        assertEquals(iamTokenEndpoint,
                recordedIamRequests[1].getPath(), "The request should have been for " +
                        "/identity/token");
        // third time, refresh because the cloudant session cookie has expired
        assertEquals(iamTokenEndpoint,
                recordedIamRequests[1].getPath(), "The request should have been for " +
                        "/identity/token");

        // cloudant mock server

        // assert that there were 5 calls
        RecordedRequest[] recordedRequests = takeN(mockWebServer, 5);

        assertEquals("/_iam_session",
                recordedRequests[0].getPath(), "The request should have been for /_iam_session");
        assertThat("The request body should contain the IAM token",
                recordedRequests[0].getBody().readString(Charset.forName("UTF-8")),
                containsString(IAM_TOKEN));
        // first request
        assertEquals("/",
                recordedRequests[1].getPath(), "The request should have been for /");
        // The cookie may or may not have the session id quoted, so check both
        assertThat("The Cookie header should contain the expected session value",
                recordedRequests[1].getHeader("Cookie"),
                anyOf(containsString(iamSession(EXPECTED_OK_COOKIE)),
                        containsString(iamSessionUnquoted(EXPECTED_OK_COOKIE))));
        // second request (rejected for cookie expiry)
        assertEquals("/",
                recordedRequests[2].getPath(), "The request should have been for /");
        // with new valid token get a new session
        assertEquals("/_iam_session",
                recordedRequests[3].getPath(), "The request should have been for /_iam_session");
        assertThat("The request body should contain the IAM token",
                recordedRequests[3].getBody().readString(Charset.forName("UTF-8")),
                containsString(IAM_TOKEN_2));
        assertEquals("/",
                recordedRequests[2].getPath(), "The request should have been for /");
    }

    @Test
    public void iamTokenServer429RetryAndFail() throws Exception {
        // Mock request sequence
        mockIamServer.enqueue(new MockResponse().setResponseCode(200).setBody(IAM_TOKEN));
        mockWebServer.enqueue(OK_IAM_COOKIE);
        // First get request succeeds
        mockWebServer.enqueue(new MockResponse().setResponseCode(200)
                .setBody(hello));

        // Second get request has a 401 cookie expired
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).
                setBody("{\"error\":\"credentials_expired\"}"));
        // IAM server 429 on subsequent token requests
        mockIamServer.enqueue(new MockResponse().setStatus("HTTP/1.1 429 Too many requests"));
        mockIamServer.enqueue(new MockResponse().setStatus("HTTP/1.1 429 Too many requests"));
        mockIamServer.enqueue(new MockResponse().setStatus("HTTP/1.1 429 Too many requests"));
        mockIamServer.enqueue(new MockResponse().setStatus("HTTP/1.1 429 Too many requests"));
        // Request will fail

        // Request sequence
        CloudantClient c = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .iamApiKey(IAM_API_KEY)
                .interceptors(Replay429Interceptor.WITH_DEFAULTS)
                .build();

        String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
        assertEquals(hello, response, "The expected response should be received");

        CouchDbException re =
                assertThrows(CouchDbException.class,
                        () -> c.executeRequest(Http.GET(c.getBaseUri())).responseAsString(),
                        "Failure to get a token should throw a CouchDbException.");
        assertTrue(re.getMessage().startsWith("HTTP response error getting session"), "The " +
                "exception should have been for a HTTP response error.");
        assertTrue(re.getMessage().contains("response code 429"), "The exception should report a " +
                "429 response code");

        // iam mock server

        // assert that there were 5 calls
        RecordedRequest[] recordedIamRequests = takeN(mockIamServer, 5);
        // first time, automatically fetch because cookie jar is empty
        assertEquals(iamTokenEndpoint,
                recordedIamRequests[0].getPath(), "The request should have been for " +
                        "/identity/token");
        assertThat("The request body should contain the IAM API key",
                recordedIamRequests[0].getBody().readString(Charset.forName("UTF-8")),
                containsString("apikey=" + IAM_API_KEY));
        // 4 more times all 429 responses
        for (int i = 1; i <= 4; i++) {
            assertEquals(iamTokenEndpoint,
                    recordedIamRequests[i].getPath(), "The request[" + i + "] should have been " +
                            "for " +
                            "/identity/token");
        }

        // cloudant mock server

        // assert that there were 3 calls
        RecordedRequest[] recordedRequests = takeN(mockWebServer, 3);

        assertEquals("/_iam_session",
                recordedRequests[0].getPath(), "The request should have been for /_iam_session");
        assertThat("The request body should contain the IAM token",
                recordedRequests[0].getBody().readString(Charset.forName("UTF-8")),
                containsString(IAM_TOKEN));
        // first request
        assertEquals("/",
                recordedRequests[1].getPath(), "The request should have been for /");
        // The cookie may or may not have the session id quoted, so check both
        assertThat("The Cookie header should contain the expected session value",
                recordedRequests[1].getHeader("Cookie"),
                anyOf(containsString(iamSession(EXPECTED_OK_COOKIE)),
                        containsString(iamSessionUnquoted(EXPECTED_OK_COOKIE))));
        // Second request gives a 401 that starts token renewal
        // Retry of that request never reaches the server
        assertEquals("/", recordedRequests[1].getPath(), "The request should have been for /");
    }
}
