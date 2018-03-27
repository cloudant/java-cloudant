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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.http.HttpConnectionInterceptorContext;
import com.cloudant.http.HttpConnectionRequestInterceptor;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.cloudant.tests.util.Utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test cases to verify status code from Response object.
 * Assert status codes in CouchDbException and its subclasses.
 */
public class ResponseTest extends TestWithDbPerClass {


    private Foo foo;

    @BeforeEach
    public void setup() {
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
     * Test that an error stream is correctly assigned to a CouchDbException error field if it comes
     * directly from the lb and not the service.
     * <P>
     * This is a Cloudant service test, where a HTTP proxy may send an error.
     * We can provoke it into doing so by sending an invalid HTTP request - in this case an
     * invalid header field.
     * Originally these errors were wrapped in HTML and so exercised a different path - they are now
     * JSON body responses like most other Cloudant requests so should be on the normal exception
     * handling path, but it is worth checking that we do work with these error types.
     * </P>
     */
    @RequiresCloudant
    @Test
    public void testJsonErrorStreamFromLB() throws Exception {
        final AtomicBoolean badHeaderEnabled = new AtomicBoolean(false);
        CloudantClient c = CloudantClientHelper.getClientBuilder().interceptors(
                new HttpConnectionRequestInterceptor() {
                    @Override
                    public HttpConnectionInterceptorContext interceptRequest
                            (HttpConnectionInterceptorContext context) {
                        if (badHeaderEnabled.get()) {
                            // Note space is also a bad char, but OkHttp prohibits them
                            String badHeaderCharacters = "()<>@,;\\/[]?=";
                            //set a header using characters that are not permitted
                            context.connection.requestProperties.put(badHeaderCharacters,
                                    badHeaderCharacters);
                        }
                        return context;
                    }
                }).build();

        try {
            // Make a good request, which will set up the session etc
            HttpConnection d = c.executeRequest(Http.GET(c.getBaseUri()));
            d.responseAsString();
            assertTrue(d.getConnection().getResponseCode() / 100 == 2, "The first request should " +
                    "succeed");

            // Enable the bad headers and expect the exception on the next request
            badHeaderEnabled.set(true);
            String response = c.executeRequest(Http.GET(c.getBaseUri())).responseAsString();
            fail("A CouchDbException should be thrown, but had response " + response);
        } catch (CouchDbException e) {
            //we expect a CouchDbException

            assertEquals(400, e.getStatusCode(), "The exception should be for a bad request");

            assertNotNull(e.getError(), "The exception should have an error set");
            assertEquals("bad_request", e.getError(), "The exception error should be bad request");
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
        assertNotNull(e.getError(), "The error should not be null");
        if ("".equals(expectedReason)) {
            assertNotNull(e.getReason(), "The reason should not be null");
        } else {
            assertEquals(expectedReason, e.getReason(), "The reason should be " + expectedReason);
        }
    }

    private void assertExceptionStatusCode(CouchDbException e, int expectedCode) {
        assertEquals(expectedCode, e.getStatusCode(), "The HTTP status code should be " +
                expectedCode);
    }
}
