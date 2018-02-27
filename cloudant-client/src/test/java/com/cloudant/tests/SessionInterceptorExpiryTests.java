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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import com.cloudant.tests.extensions.MockWebServerExtension;
import com.cloudant.tests.util.MockWebServerResources;

import org.junit.jupiter.api.BeforeAll;
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

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ExtendWith(SessionInterceptorExpiryTests.ParameterProvider.class)
public class SessionInterceptorExpiryTests extends HttpFactoryParameterizedTest {

    static class ParameterProvider implements TestTemplateInvocationContextProvider {
        @Override
        public boolean supportsTestTemplate(ExtensionContext context) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
            return Stream.of(invocationContext(false,"/_iam_session"),
                    invocationContext(false, "/_session"),
                    invocationContext(true, "/_iam_session"),
                    invocationContext(true,"/_session"));
        }

        // because we fill in the args from the left, we can fill in the single argument "okUsable"
        // for the parent class' (HttpFactoryParameterizedTest) @BeforeEach
        public static TestTemplateInvocationContext invocationContext(final boolean okUsable,
                                                                      final String sessionPath) {
            return new TestTemplateInvocationContext() {
                @Override
                public String getDisplayName(int invocationIndex) {
                    return String.format("path:%s,okUsable:%s", sessionPath, okUsable);
                }

                @Override
                public List<Extension> getAdditionalExtensions() {
                    return Collections.<Extension>singletonList(new ParameterResolver() {
                        @Override
                        public boolean supportsParameter(ParameterContext parameterContext,
                                                         ExtensionContext extensionContext) {
                            switch(parameterContext.getIndex()) {
                                case 0:
                                    return parameterContext.getParameter().getType().equals(boolean.class);
                                case 1:
                                    return parameterContext.getParameter().getType().equals(String.class);
                            }
                            return false;
                        }

                        @Override
                        public Object resolveParameter(ParameterContext parameterContext,
                                                       ExtensionContext extensionContext) {
                            switch(parameterContext.getIndex()) {
                                case 0:
                                    return okUsable;
                                case 1:
                                    return sessionPath;
                            }
                            return null;
                        }
                    });
                }
            };
        }
    }

    public static IamSystemPropertyMock iamSystemPropertyMock;

    @RegisterExtension
    public MockWebServerExtension mockWebServerExt = new MockWebServerExtension();
    public MockWebServer mockWebServer;
    @RegisterExtension
    public MockWebServerExtension mockIamServerExt = new MockWebServerExtension();
    public MockWebServer mockIamServer;

    private HttpConnectionRequestInterceptor rqInterceptor;
    private HttpConnectionResponseInterceptor rpInterceptor;

    /**
     * Before running this test class setup the property mock.
     */
    @BeforeAll
    public static void setupIamSystemPropertyMock() {
        iamSystemPropertyMock = new IamSystemPropertyMock();
    }

    @BeforeEach
    public void setupSessionInterceptor(boolean okUsable, String sessionPath) {
        this.mockWebServer = mockWebServerExt.get();
        this.mockIamServer = mockIamServerExt.get();

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

    private void queueResponses(boolean okUsable,
                                String sessionPath,
                                Long expiry,
                                String cookieValue) {
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

    private void executeTest(boolean okUsable,
                             String sessionPath,
                             Long expiryTime,
                             String cookieValue) throws Exception {
        queueResponses(okUsable, sessionPath, expiryTime, cookieValue);
        HttpConnection conn = Http.GET(mockWebServer.url("/").url());
        conn.connectionFactory = (isOkUsable) ? new OkHttpClientHttpUrlConnectionFactory() :
                new DefaultHttpUrlConnectionFactory();
        conn.requestInterceptors.add(rqInterceptor);
        conn.responseInterceptors.add(rpInterceptor);
        conn = conn.execute();

        // Consume response stream and assert ok: true
        String responseStr = conn.responseAsString();
        String okPattern = ".*\"ok\"\\s*:\\s*true.*";
        assertTrue(Pattern.compile(okPattern, Pattern.DOTALL).matcher(responseStr).matches(), "There should be an ok response: " + responseStr);

        // Assert the _session request
        RecordedRequest sessionRequest = mockWebServer.takeRequest(MockWebServerResources
                .TIMEOUT, MockWebServerResources.TIMEOUT_UNIT);

        assertEquals(sessionPath, sessionRequest.getPath(), "The interceptor should make a session request");
        assertNull(sessionRequest.getHeader("Cookie"), "There should be no existing cookie on the session request");

        // Assert the GET request
        RecordedRequest getRequest = mockWebServer.takeRequest(MockWebServerResources.TIMEOUT,
                MockWebServerResources.TIMEOUT_UNIT);
        assertEquals("/", getRequest.getPath(), "The request path should be correct");
        assertNotNull(getRequest.getHeader("Cookie"), "There should be a cookie on the request");
        String expectedCookie = ((sessionPath.equals("/_session")) ? MockWebServerResources.AUTH_COOKIE_NAME :
                MockWebServerResources.IAM_COOKIE_NAME) + "=" + cookieValue;
        assertEquals(expectedCookie, getRequest.getHeader("Cookie"), "The cookie should be the correct session type");
    }

    /**
     * Test the non-expiry case just to validate that things work normally
     * @throws Exception
     */
    @TestTemplate
    public void testMakesCookieRequest(boolean okUsable, String sessionPath) throws Exception {
        executeTest(okUsable, sessionPath, null, MockWebServerResources.EXPECTED_OK_COOKIE);
    }

    /**
     * Test that if a cookie is expired it does not cause a replay cycle. That is we should not
     * retrieve an expired cookie from the store, so a new session request should be made before
     * any subsequent request.
     *
     * @throws Exception
     */
    @TestTemplate
    public void testNewCookieRequestMadeAfterExpiry(boolean okUsable, String sessionPath) throws Exception {
        // Make a GET request and get a cookie valid for 2 seconds
        executeTest(okUsable, sessionPath, System.currentTimeMillis() + 2000, MockWebServerResources.EXPECTED_OK_COOKIE);

        // Sleep 2 seconds and make another request
        // Note 1 second appears to be insufficient probably due to rounding to the nearest second
        // in cookie expiry times.
        Thread.sleep(2000);

        // Since the Cookie is expired it should follow the same sequence of POST /_session GET /
        // If the expired Cookie was retrieved it would only do GET / and the test would fail.
        executeTest(okUsable, sessionPath, null, MockWebServerResources.EXPECTED_OK_COOKIE_2);
    }

}
