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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.http.HttpConnectionResponseInterceptor;
import com.cloudant.http.internal.DefaultHttpUrlConnectionFactory;
import com.cloudant.http.internal.interceptors.CookieInterceptor;
import com.cloudant.http.internal.interceptors.IamCookieInterceptor;
import com.cloudant.http.internal.ok.OkHttpClientHttpUrlConnectionFactory;
import com.cloudant.tests.util.HttpFactoryParameterizedTest;
import com.cloudant.tests.util.IamSystemPropertyMock;
import com.cloudant.tests.util.MockWebServerResources;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SessionInterceptorExpiryTests extends HttpFactoryParameterizedTest {

    public static IamSystemPropertyMock iamSystemPropertyMock;

    @Parameterized.Parameters(name = "Using okhttp: {0} for session path {1}")
    public static List<Object[]> testParams() {
        List<Object[]> tests = new ArrayList<Object[]>(4);
        tests.add(new Object[]{false, "/_session"});
        tests.add(new Object[]{true, "/_session"});
        tests.add(new Object[]{false, "/_iam_session"});
        tests.add(new Object[]{true, "/_iam_session"});
        return tests;
    }

    // Note Parameter(0) okUsable is inherited

    @Parameterized.Parameter(1)
    public String sessionPath;

    @Rule
    public MockWebServer mockWebServer = new MockWebServer();
    @Rule
    public MockWebServer mockIamServer = new MockWebServer();

    private HttpConnectionRequestInterceptor rqInterceptor;
    private HttpConnectionResponseInterceptor rpInterceptor;

    /**
     * Before running this test class setup the property mock.
     */
    @BeforeClass
    public static void setupIamSystemPropertyMock() {
        iamSystemPropertyMock = new IamSystemPropertyMock();
    }

    @Before
    public void setupSessionInterceptor() {
        String baseUrl = mockWebServer.url("").toString();

        if (sessionPath.equals("/_session")) {
            CookieInterceptor ci = new CookieInterceptor("user", "pass", baseUrl);
            rqInterceptor = ci;
            rpInterceptor = ci;
        } else if (sessionPath.equals("/_iam_session")) {
            // Set the endpoint value before each test
            iamSystemPropertyMock.setMockIamTokenEndpointUrl(mockIamServer.url("/identity/token")
                    .toString());
            IamCookieInterceptor ici = new IamCookieInterceptor("apikey", baseUrl);
            rqInterceptor = ici;
            rpInterceptor = ici;
        } else {
            fail("Invalid sessionPath " + sessionPath);
        }

    }

    private void queueResponses(Long expiry, String cookieValue) {
        // Queue up the session response
        String cookieString;
        if (sessionPath.equals("/_session")) {
            cookieString = MockWebServerResources.authSessionCookie(cookieValue, expiry);
        } else {
            // Queue up a token response for IAM
            mockIamServer.enqueue(new MockResponse().setResponseCode(200)
                    .setBody(MockWebServerResources.IAM_TOKEN));
            cookieString = MockWebServerResources.iamSessionCookie(cookieValue, expiry);
        }
        MockResponse cookieResponse = new MockResponse()
                .setResponseCode(200)
                .setBody(MockWebServerResources.MOCK_COOKIE_RESPONSE_BODY)
                .addHeader("Set-Cookie", cookieString);
        mockWebServer.enqueue(cookieResponse);

        // Followed by an OK response
        mockWebServer.enqueue(MockWebServerResources.JSON_OK);
    }

    private void executeTest(Long expiryTime, String cookieValue) throws Exception {
        queueResponses(expiryTime, cookieValue);
        HttpConnection conn = Http.GET(mockWebServer.url("/").url());
        conn.connectionFactory = (okUsable) ? new OkHttpClientHttpUrlConnectionFactory() :
                new DefaultHttpUrlConnectionFactory();
        conn.requestInterceptors.add(rqInterceptor);
        conn.responseInterceptors.add(rpInterceptor);
        conn = conn.execute();

        // Consume response stream and assert ok: true
        String responseStr = conn.responseAsString();
        String okPattern = ".*\"ok\"\\s*:\\s*true.*";
        assertTrue("There should be an ok response: " + responseStr, Pattern.compile(okPattern,
                Pattern.DOTALL).matcher(responseStr).matches());

        // Assert the _session request
        RecordedRequest sessionRequest = mockWebServer.takeRequest(MockWebServerResources
                .TIMEOUT, MockWebServerResources.TIMEOUT_UNIT);

        assertEquals("The interceptor should make a session request", sessionPath,
                sessionRequest.getPath());
        assertNull("There should be no existing cookie on the session request", sessionRequest
                .getHeader("Cookie"));

        // Assert the GET request
        RecordedRequest getRequest = mockWebServer.takeRequest(MockWebServerResources.TIMEOUT,
                MockWebServerResources.TIMEOUT_UNIT);
        assertEquals("The request path should be correct", "/", getRequest.getPath());
        assertNotNull("There should be a cookie on the request", getRequest.getHeader("Cookie"));
        String expectedCookie = ((sessionPath.equals("/_session")) ? MockWebServerResources.AUTH_COOKIE_NAME :
                MockWebServerResources.IAM_COOKIE_NAME) + "=" + cookieValue;
        assertEquals("The cookie should be the correct session type", expectedCookie,
                getRequest.getHeader("Cookie"));
    }

    /**
     * Test the non-expiry case just to validate that things work normally
     * @throws Exception
     */
    @Test
    public void testMakesCookieRequest() throws Exception {
        executeTest(null, MockWebServerResources.EXPECTED_OK_COOKIE);
    }

    /**
     * Test that if a cookie is expired it does not cause a replay cycle. That is we should not
     * retrieve an expired cookie from the store, so a new session request should be made before
     * any subsequent request.
     *
     * @throws Exception
     */
    @Test
    public void testNewCookieRequestMadeAfterExpiry() throws Exception {
        // Make a GET request and get a cookie valid for 2 seconds
        executeTest(System.currentTimeMillis() + 2000, MockWebServerResources.EXPECTED_OK_COOKIE);

        // Sleep 2 seconds and make another request
        // Note 1 second appears to be insufficient probably due to rounding to the nearest second
        // in cookie expiry times.
        Thread.sleep(2000);

        // Since the Cookie is expired it should follow the same sequence of POST /_session GET /
        // If the expired Cookie was retrieved it would only do GET / and the test would fail.
        executeTest(null, MockWebServerResources.EXPECTED_OK_COOKIE_2);
    }

}
