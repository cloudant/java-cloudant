/*
 * Copyright © 2016, 2018 IBM Corp. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.ClientBuilder;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.tests.extensions.MockWebServerExtension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import mockit.Expectations;
import mockit.Mocked;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.security.Security;
import java.security.SecurityPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class LoggingTest {

    private static final String logPrefixPattern = "[0-9,a-f]+-\\d+ ";

    @RegisterExtension
    public static MockWebServerExtension mockWebServerExt = new MockWebServerExtension();
    public static MockWebServer mockWebServer;

    private static CloudantClient client;
    private volatile Logger logger;
    private VerificationLogHandler handler;

    @BeforeEach
    public void setupMockWebServer() throws Exception {
        mockWebServer = mockWebServerExt.get();
        // Set a dispatcher that always returns 200 OK
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse();
            }
        });
        client = CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer)
                .build();
    }

    @BeforeEach
    public void createHandler() {
        handler = new VerificationLogHandler();
    }

    @AfterEach
    public void teardown() throws Exception {
        // Remove the handler from the logger
        logger.removeHandler(handler);

        // Reload whatever log configuration existed before the test
        LogManager.getLogManager().readConfiguration();
    }

    @Test
    public void httpLoggingEnabled() throws Exception {
        logger = setupLogger(HttpConnection.class, Level.ALL);
        client.executeRequest(Http.GET(client.getBaseUri())).responseAsString();
        assertTrue(handler.logEntries.size() > 0, "There should be at least 1 log entry");
    }

    @Test
    public void urlRegexLogging() throws Exception {

        //Set the regex filter property on the LogManager and assert it was set
        String urlFilterPropName = "com.cloudant.http.filter.url";
        String urlFilterPropValue = ".*/testdb.*";
        setAndAssertLogProperty(urlFilterPropName, urlFilterPropValue);

        // Configure HttpConnection logging and get a client
        logger = setupLogger(HttpConnection.class, Level.FINE);

        // Make a request to testdb
        client.executeRequest(Http.GET(new URL(client.getBaseUri().toString() + "/testdb")))
                .responseAsString();

        // Check there were two log messages one for request and one for response
        assertEquals(2, handler.logEntries.size(), "There should be 2 log messages");
        // Check the messages were the ones we expected
        assertHttpMessage("GET .*/testdb request", 0);
        assertHttpMessage("GET .*/testdb response 200 OK", 1);

        // Store the current log size
        int logsize = handler.logEntries.size();

        // Make a second request to a different URL and check that nothing else was logged
        client.executeRequest(Http.GET(client.getBaseUri())).responseAsString();
        assertEquals(logsize, handler.logEntries.size(), "There should have been no more log " +
                "entries");
    }

    private String methodFilterPropName = "com.cloudant.http.filter.method";

    @Test
    public void httpMethodFilterLogging() throws Exception {
        setAndAssertLogProperty(methodFilterPropName, "GET");
        logger = setupLogger(HttpConnection.class, Level.FINE);

        // Make a GET request
        client.executeRequest(Http.GET(client.getBaseUri())).responseAsString();

        // Check there were two log messages one for request and one for response
        assertEquals(2, handler.logEntries.size(), "There should be 2 log messages");
        // Check the messages were the ones we expected
        assertHttpMessage("GET .* request", 0);
        assertHttpMessage("GET .* response 200 OK", 1);

        // Store the current log size
        int logsize = handler.logEntries.size();

        // Make a PUT request to a different URL and check that nothing else was logged
        client.executeRequest(Http.PUT(client.getBaseUri(), "text/plain").setRequestBody(""))
                .responseAsString();
        assertEquals(logsize, handler.logEntries.size(), "There should have been no more log " +
                "entries");
    }

    @Test
    public void httpMethodFilterLoggingList() throws Exception {
        setAndAssertLogProperty(methodFilterPropName, "PUT,GET");
        logger = setupLogger(HttpConnection.class, Level.FINE);

        // Make a GET request
        client.executeRequest(Http.GET(client.getBaseUri())).responseAsString();

        // Check there were two log messages one for request and one for response
        assertEquals(2, handler.logEntries.size(), "There should be 2 log messages");
        // Check the messages were the ones we expected
        assertHttpMessage("GET .* request", 0);
        assertHttpMessage("GET .* response 200 OK", 1);

        // Make a PUT request to a different URL and check that new messages are logged
        client.executeRequest(Http.PUT(client.getBaseUri(), "text/plain").setRequestBody(""))
                .responseAsString();

        assertEquals(4, handler.logEntries.size(), "There should now be 4 log messages");
        // Check the messages were the ones we expected
        assertHttpMessage("PUT .* request", 2);
        assertHttpMessage("PUT .* response 200 OK", 3);
    }

    @Test
    public void clientBuilderLogging() throws Exception {
        logger = setupLogger(ClientBuilder.class, Level.CONFIG);
        CloudantClientHelper.newMockWebServerClientBuilder(mockWebServer).build();
        assertEquals(5, handler.logEntries.size(), "There should be 5 log entries");
        // Validate each of the 5 entries are what we expect
        assertLogMessage("URL: .*", 0);
        assertLogMessage("Building client using URL: .*", 1);
        assertLogMessage("Connect timeout: .*", 2);
        assertLogMessage("Read timeout: .*", 3);
        assertLogMessage("Using default GSON builder", 4);
    }

    /**
     * A basic DNS log test that can be called with different values.
     *
     * @param cacheValue the value to set for the cache lifetime
     * @throws Exception if the test fails or errors
     */
    private void basicDnsLogTest(String cacheValue) throws Exception {
        logger = setupLogger(ClientBuilder.class, Level.WARNING);
        String previous = Security.getProperty("networkaddress.cache.ttl");
        try {
            Security.setProperty("networkaddress.cache.ttl", cacheValue);
            CloudantClientHelper.getClientBuilder().build();
        } finally {
            // No way to unset a property, just reset to previous value
            // or set to 30 if it was null
            Security.setProperty("networkaddress.cache.ttl", (previous == null) ? "30" : previous);
        }
    }

    /**
     * Test that no warning is logged if the DNS lifetime is less than 30 s
     *
     * @throws Exception
     */
    @Test
    public void dnsNoWarningLessThan30() throws Exception {
        basicDnsLogTest("29");
        // Assert no warning was received
        assertEquals(0, handler.logEntries.size(), "There should be no log entry");
    }

    /**
     * Test that no warning is logged if DNS caching is disabled
     *
     * @throws Exception
     */
    @Test
    public void dnsNoWarning0() throws Exception {
        basicDnsLogTest("0");
        // Assert no warning was received
        assertEquals(0, handler.logEntries.size(), "There should be no log entry");
    }

    /**
     * Test that a warning is logged if DNS caching is set to cache forever
     *
     * @throws Exception
     */
    @Test
    public void dnsWarningForever() throws Exception {
        basicDnsLogTest("-1");
        // Assert a warning was received
        assertEquals(1, handler.logEntries.size(), "There should be 1 log entry");
        // Assert that it matches the expected pattern
        assertLogMessage("DNS cache lifetime may be too long\\. .*", 0);
    }

    /**
     * Test that no warning is logged if the DNS lifetime is 30 s
     *
     * @throws Exception
     */
    @Test
    public void dnsNoWarning30() throws Exception {
        basicDnsLogTest("30");
        // Assert no warning was received
        assertEquals(0, handler.logEntries.size(), "There should be no log entry");
    }


    /**
     * Test that a warning is logged if the DNS lifetime is longer than 30 s
     *
     * @throws Exception
     */
    @Test
    public void dnsWarning31() throws Exception {
        basicDnsLogTest("31");
        // Assert a warning was received
        assertEquals(1, handler.logEntries.size(), "There should be 1 log entry");
        // Assert that it matches the expected pattern
        assertLogMessage("DNS cache lifetime may be too long\\. .*", 0);
    }

    /**
     * Test that a warning is logged if the DNS lifetime cannot be checked because of security
     * permissions.
     *
     * @throws Exception
     */
    @Test
    public void dnsWarningPermissionDenied(@Mocked final SecurityManager mockSecurityManager)
            throws Exception {

        // Record the mock expectations
        new Expectations() {
            {
                mockSecurityManager.checkPermission(new SecurityPermission("getProperty" +
                        ".networkaddress.cache.ttl"));
                result = new SecurityException("Test exception to deny property access.");
                times = 1;
            }
        };
        logger = setupLogger(ClientBuilder.class, Level.WARNING);
        try {
            System.setSecurityManager(mockSecurityManager);
            CloudantClientHelper.getClientBuilder().build();
        } finally {
            // Unset the mock security manager
            System.setSecurityManager(null);
        }
        // Assert a warning was received
        assertEquals(1, handler.logEntries.size(), "There should be 1 log entry");
        // Assert that it matches the expected pattern
        assertLogMessage("Permission denied to check Java DNS cache TTL\\. .*", 0);
    }

    /**
     * Test that a warning is logged if a security manager is in use and the DNS cache lifetime
     * property is unset.
     *
     * @throws Exception
     */
    @Test
    public void dnsWarningDefaultWithSecurityManager(@Mocked final SecurityManager
                                                             mockSecurityManager) throws Exception {
        // Record the mock expectations
        new Expectations() {
            {
                mockSecurityManager.checkPermission(new SecurityPermission("getProperty" +
                        ".networkaddress.cache.ttl"));
                minTimes = 2; // Once to set, once to get, and once to reset
                maxTimes = 3; // Possible third call to reset the value, depending on test ordering
            }
        };
        try {
            System.setSecurityManager(mockSecurityManager);
            // We can't set null as a value and there are no APIs for clearing a value. Another test
            // may already have changed the value so we just set it to something invalid "a" to get
            // a default value.
            basicDnsLogTest("a");
        } finally {
            // Unset the mock security manager
            System.setSecurityManager(null);
        }
        // Assert a warning was received
        assertEquals(1, handler.logEntries.size(), "There should be 1 log entry");
        // Assert that it matches the expected pattern
        assertLogMessage("DNS cache lifetime may be too long\\. .*", 0);
    }

    /**
     * Set a LogManager configuration property and assert it was set correctly
     */
    private void setAndAssertLogProperty(String name, String value) throws Exception {
        LogManager.getLogManager().readConfiguration(new ByteArrayInputStream((name
                + "=" + value).getBytes()));
        assertEquals(value, LogManager.getLogManager().getProperty(name), "The log property " +
                "should be the test value");
    }

    /**
     * Assert a HTTP log message matches the prefix pattern plus a suffix
     *
     * @param pattern the regex pattern to check the message matches
     * @param index   the index of the message in the handler list
     */
    private void assertLogMessage(String pattern, int index) {
        Pattern p = Pattern.compile(pattern);
        String msg = handler.logEntries.get(index).getMessage();
        assertTrue(p.matcher(msg).matches(), "The log entry \"" + msg + "\" should match pattern " +
                "" + pattern);
    }

    /**
     * Assert a HTTP log message matches the prefix pattern plus a suffix
     *
     * @param logMsgSuffixPattern the suffix part of the pattern
     * @param index               the index of the message in the handler list
     */
    private void assertHttpMessage(String logMsgSuffixPattern, int index) {
        assertLogMessage(logPrefixPattern + logMsgSuffixPattern, index);
    }


    /**
     * A java.util.logging.Handler that just adds log entries to a list in memory so they can be
     * verified by the tests.
     */
    private static final class VerificationLogHandler extends Handler {

        public List<LogRecord> logEntries = new ArrayList<LogRecord>();

        VerificationLogHandler() {
            // Default to no logging
            this.setLevel(Level.OFF);
        }

        @Override
        public void publish(LogRecord record) {
            logEntries.add(record);
        }

        @Override
        public void flush() {
            // No-op
        }

        @Override
        public void close() throws SecurityException {
            // No-op
        }
    }

    /**
     * A method to get and initialize a logger to test for a given class.
     *
     * @param classToLog the class we want a logger for
     * @param level      the logging level to enable
     * @return the logger
     * @throws Exception if something goes wrong
     */
    private Logger setupLogger(Class<?> classToLog, Level level) throws Exception {
        String loggerName = classToLog.getName();

        // Get the logger and assert non-null
        Logger l = Logger.getLogger(loggerName);

        // Add the verification handler
        l.addHandler(handler);

        // Set the logging level for the test
        l.setLevel(level);
        handler.setLevel(level);

        return l;
    }
}
