package com.cloudant.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.Utils;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test cases to verify status code from Response object.
 * Assert status codes in CouchDbException and its subclasses.
 */
public class ResponseTest {

    public static CloudantClientResource clientResource = new CloudantClientResource();
    public static DatabaseResource dbResource = new DatabaseResource(clientResource);
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);

    private Foo foo;
    private static Database db;

    @Before
    public void setup() {
        db = dbResource.get();
        foo = new Foo(Utils.generateUUID());
    }

    @Test
    public void verifyDocumentSaved() {
        Response response = db.save(foo);
        assertEquals(2, response.getStatusCode() / 100);
    }

    @Test
    public void verifyDbNotFound() {
        try {
            Database db2 = clientResource.get().database("non_existent_name", false);
            db2.find("no_id");
            db.find(Response.class, "no_id");
            fail("A NoDocumentException should be thrown");
        } catch (NoDocumentException e) {
            exceptionAsserts(e, 404, "");
        }
    }

    @Test
    public void verifyDocumentNotFound() {
        try {
            db.find(Response.class, "no_id");
            fail("A NoDocumentException should be thrown");
        } catch (NoDocumentException e) {
            exceptionAsserts(e, 404, "missing");
        }
    }

    @Test
    public void verifyDocumentConflict() {
        try {
            Response response = db.save(foo);
            assertEquals(2, response.getStatusCode() / 100);

            db.save(foo);
            fail("A DocumentConflictException should be thrown");
        } catch (DocumentConflictException e) {
            exceptionAsserts(e, 409, "");
        }
    }

    @Test
    public void verifyBadRequest() {
        try {
            foo.set_rev("bad_rev");
            db.update(foo);
            fail("A CouchDbException should be thrown");
        } catch (CouchDbException e) {
            exceptionAsserts(e, 400, "");
        }

    }

    @Test
    public void verifyBulkDocumentRequest() {
        ArrayList<Foo> foos = new ArrayList<Foo>();
        foos.add(new Foo(Utils.generateUUID()));
        foos.add(new Foo(Utils.generateUUID()));
        foos.add(new Foo(Utils.generateUUID()));

        List<Response> responses = db.bulk(foos);
        for (Response response : responses) {
            assertEquals(2, response.getStatusCode() / 100);
        }
    }

    /**
     * Test that an HTML error stream is correctly assigned to a CouchDbException error field.
     * <P>
     * This is a Cloudant service test, where a HTTP proxy may send an HTML error instead of JSON.
     * We can provoke it into doing so by sending an invalid HTTP request - in this case an
     * invalid header field.
     * </P>
     */
    @Category(RequiresCloudant.class)
    @Test
    public void testNonJsonErrorStream() {
        final AtomicBoolean badHeaderEnabled = new AtomicBoolean(false);
        CloudantClient c = CloudantClientHelper.getClientBuilder().interceptors(
                new HttpConnectionRequestInterceptor() {
                    @Override
                    public HttpConnectionInterceptorContext interceptRequest
                            (HttpConnectionInterceptorContext context) {
                        if (badHeaderEnabled.get()) {
                            String badHeaderCharacters = "() <> @ , ; \\ / [] ? =";
                            //set a header using characters that are not permitted
                            context.connection.requestProperties.put(badHeaderCharacters,
                                    badHeaderCharacters);
                        }
                        return context;
                    }
                }).build();

        try {
            // Make a good request, which will set up the session etc
            c.executeRequest(Http.GET(c.getBaseUri()));

            // Enable the bad headers and expect the exception on the next request
            badHeaderEnabled.set(true);
            c.executeRequest(Http.GET(c.getBaseUri()));
            fail("A CouchDbException should be thrown");
        } catch (CouchDbException e) {
            //we expect a CouchDbException
            assertNotNull("The exception should have an error set", e.getError());
            assertTrue("The exception error should contain html", e.getError().contains("<html>"));
        } finally {
            // Disable the bad header to allow a clean shutdown
            badHeaderEnabled.set(false);
            c.shutdown();
        }
    }

    /**
     * Utility to assert on the generated exceptions
     *
     * @param e              the exception to check
     * @param expectedCode   the expected status code
     * @param expectedReason the expected reason phrase to check for equality, or "" (the empty
     *                       string) to only assert that the reason was non-null
     */
    private void exceptionAsserts(CouchDbException e, int expectedCode, String expectedReason) {
        assertExceptionStatusCode(e, expectedCode);
        assertNotNull("The error should not be null", e.getError());
        if ("".equals(expectedReason)) {
            assertNotNull("The reason should not be null", e.getReason());
        } else {
            assertEquals("The reason should be " + expectedReason, expectedReason, e.getReason());
        }
    }

    private void assertExceptionStatusCode(CouchDbException e, int expectedCode) {
        assertEquals("The HTTP status code should be " + expectedCode, expectedCode, e
                .getStatusCode());
    }
}
