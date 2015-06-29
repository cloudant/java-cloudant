package com.cloudant.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.Membership;
import com.cloudant.client.api.model.Task;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.tests.util.SingleRequestHttpServer;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbException;

import java.util.List;

public class CloudantClientTests {

    private static final Log log = LogFactory.getLog(CloudantClientTests.class);

    public static CloudantClient cookieBasedClient;
    private CloudantClient account;

    @Before
    public void setUp() {
        account = CloudantClientHelper.getClient();

        String cookie = account.getCookie();
        if (CloudantClientHelper.COUCH_PASSWORD == null) {
            cookieBasedClient = account;
        } else {
            cookieBasedClient = new CloudantClient(
                    CloudantClientHelper.COUCH_USERNAME, cookie);
        }

    }


    @After
    public void tearDown() {
        account.shutdown();
        cookieBasedClient.shutdown();
    }

    @Test
    @Category(RequiresCloudantService.class)
    public void apiKey() {
        ApiKey key = account.generateApiKey();
        assertNotNull(key);
        assertNotNull(key.getKey());
        assertNotNull(key.getPassword());
    }

    @Test
    public void activeTasks() {
        List<Task> tasks = account.getActiveTasks();
        assertNotNull(tasks);
    }

    @Test
    @Category(RequiresCloudant.class)
    public void membership() {
        Membership mship = account.getMembership();
        assertNotNull(mship);
        assertNotNull(mship.getClusterNodes());
        assertNotNull(mship.getClusterNodes().hasNext());
        assertNotNull(mship.getAllNodes());
        assertNotNull(mship.getAllNodes().hasNext());
    }

    @Test
    @Category(RequiresCloudant.class)
    public void cookieTest() {

        Membership membership = cookieBasedClient.getMembership();
        assertNotNull(membership);
    }

    @Test
    public void cookieNegativeTest() {
        String cookie = account.getCookie() + "XXX";
        boolean exceptionRaised = true;
        try {
            new CloudantClient(
                    CloudantClientHelper.SERVER_URI.toString(), cookie).getAllDbs();
            exceptionRaised = false;
        } catch (CouchDbException e) {
            if (e.getMessage().contains("Forbidden")) {
                exceptionRaised = true;
            }
        }
        if (exceptionRaised == false) {
            Assert.fail("could connect to cloudant with random AuthSession cookie");
        }
    }

    private final String userAgentRegex = "java-cloudant/[^\\s]+ " +
            "\\[Java \\([^;]*;[^;]*;[^;]*\\) [^;]*;[^;]*;" +
            "[^;]*\\]";

    /**
     * Assert that the User-Agent header is of the expected form.
     * It would be nice to test that the header is actually added to requests, but we can't
     * assert that without making assumptions about the underlying client or asserting server-side.
     */
    @Test
    public void testUserAgentHeaderString() {
        assertTrue("The value of the User-Agent header should match the format " +
                "\"java-cloudant/version [Java (os.arch; os.name; os.version) jvm.vendor; jvm" +
                ".version; jvm.runtime.version]\"", CouchDbClient.USER_AGENT.matches
                (userAgentRegex));
    }

    @Test
    public void testUserAgentHeaderIsAddedToRequest() {

        int serverPort = 54321;
        SingleRequestHttpServer server = SingleRequestHttpServer.startServer(serverPort);
        //wait for the server to be ready
        server.waitForServer();

        //instantiating the client performs a single post request
        CloudantClient client = new CloudantClient("http://localhost:" + serverPort, "", "");
        //assert that the request had the expected header
        boolean foundUserAgentHeaderOnRequest = false;
        boolean userAgentHeaderMatchedExpectedForm = false;
        for (String line : server.getRequestInput()) {
            if (line.contains("User-Agent")) {
                foundUserAgentHeaderOnRequest = true;
                if (line.matches(".*" + userAgentRegex)) {
                    userAgentHeaderMatchedExpectedForm = true;
                }
            }
        }
        assertTrue("The User-Agent header should be present on the request",
                foundUserAgentHeaderOnRequest);
        assertTrue("The value of the User-Agent header value on the request should match the " +
                        "format " +
                        "\"java-cloudant/version [Java (os.arch; os.name; os.version) jvm" +
                        ".vendor; jvm" +
                        ".version; jvm.runtime.version]\"",
                userAgentHeaderMatchedExpectedForm);

    }
}
