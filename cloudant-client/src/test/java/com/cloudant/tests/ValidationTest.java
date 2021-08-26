/*
 * Copyright © 2021 IBM Corp. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.cloudant.client.api.model.Params;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.tests.base.TestWithMockedServer;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * A series of tests that checks that doc id validation correctly errors for
 * invalid doc IDs.
 */
public class ValidationTest extends TestWithMockedServer {

    private enum Expect {
        VALIDATION_EXCEPTION_DOCID,
        VALIDATION_EXCEPTION_ATTNAME,
        RESPONSE_404,
        RESPONSE_200,
        RESPONSE_201;
    }

    private enum ExpectPathSegments {
        NONE(0),
        ONE(1),
        TWO(2),
        THREE(3),
        FOUR(4);

        final int segments;
        ExpectPathSegments(int segments) {
            this.segments = segments;
        }
    }

    private static class TestParameters {

        static final TestParameters VALIDATION_EXCEPTION_DOCID = new TestParameters(Expect.VALIDATION_EXCEPTION_DOCID, ExpectPathSegments.NONE);
        static final TestParameters VALIDATION_EXCEPTION_ATTNAME = new TestParameters(Expect.VALIDATION_EXCEPTION_ATTNAME, ExpectPathSegments.NONE);

        final Expect response;
        final ExpectPathSegments pathSegments;
        final Object callResponse;
        int expectedQueryParameters = 0;

        TestParameters(Expect response, ExpectPathSegments pathSegments) {
            this(response, pathSegments, null);
        }

        /**
         * Init a test with expectations for response and pathSegments
         * and a response to the callable.
         * @param response response code and exception validation
         * @param pathSegments number of path segemnts to verify
         * @param callRepsonse the expected response to the client call
         */
        TestParameters(Expect response, ExpectPathSegments pathSegments, Object callResponse) {
            this.response = response;
            this.pathSegments = pathSegments;
            this.callResponse = callResponse;
        }

        /**
         * Return a copy of the query parameters with a modified number of
         * expected query parameters. Used to change the expected parameters
         * when working with variants of the same call.
         * @param queryParameters expected number of query parameters
         * @return the same test parameters with the new expected query parameters
         */
        TestParameters withQueryParams(int queryParameters) {
            TestParameters newWithQueryParams = new TestParameters(response, pathSegments, callResponse);
            newWithQueryParams.expectedQueryParameters = queryParameters;
            return newWithQueryParams;
        }
    }

    // Test helper that makes a client call and asserts the validation succeeds
    // or fails according to the flag
    /**
     * 
     * @param expected the expected response
     * @param clientCall the call to make
     * @param permittedPathSegments the number of permitted path segements to validate
     * @param response response to assert, assert exceptions if null
     * @return
     * @throws Exception
     */
    private void callAndAssertValidation(TestParameters params, Callable<?> clientCall) throws Exception {
        // Queue mocks based on the necessary server response
        switch(params.response) {
            case RESPONSE_200:
                server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\": true}"));
                break;
            case RESPONSE_201:
                server.enqueue(new MockResponse().setResponseCode(201).setBody("{\"ok\": true}"));
                break;
            case RESPONSE_404:
                server.enqueue(new MockResponse().setResponseCode(404));
                break;
            case VALIDATION_EXCEPTION_DOCID:
            case VALIDATION_EXCEPTION_ATTNAME:
            default:
                server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"Validation failure.\", \"reason\": \"Test request should not reach mock server.\""));
                break;
        }
        Exception caughtException = null;
        Object ret = null;
        try {
            ret = clientCall.call();
        } catch (Exception e) {
            caughtException = e;
        }
        switch(params.response) {
            case RESPONSE_200:
            case RESPONSE_201:
                if (caughtException != null) {
                    fail("There should not be an exception", caughtException);
                }
                break;
            case RESPONSE_404:
                if (params.callResponse == null) {
                    assertNotNull(caughtException, "There should be an exception.");
                    assertTrue(caughtException instanceof NoDocumentException, "There should be a NoDocumentException (404).");
                } else {
                    assertEquals(params.callResponse, ret, "The response should have the correct value.");
                }
                break;
            case VALIDATION_EXCEPTION_DOCID:
                assertNotNull(caughtException, "There should be an exception.");
                assertTrue(caughtException instanceof IllegalArgumentException, "There should be an IllegalArgumentException.");
                assertTrue(caughtException.getMessage().endsWith("is not a valid document ID."),
                        "The validation should have failed for document ID.");
                break;
            case VALIDATION_EXCEPTION_ATTNAME:
                assertNotNull(caughtException, "There should be an exception.");
                assertTrue(caughtException instanceof IllegalArgumentException, "There should be an IllegalArgumentException.");
                assertTrue(caughtException.getMessage().endsWith("is not a valid attachment name."),
                        "The validation should have failed for attachment name.");
                break;
            default:
                fail("Unimplemented case.");
        }
        // We assert the correct path segements in cases where requests are made
        // i.e. everything except VALIDATION_EXCEPTION
        if (!(params.response == Expect.VALIDATION_EXCEPTION_DOCID || params.response == Expect.VALIDATION_EXCEPTION_ATTNAME)) {
            RecordedRequest req = server.takeRequest(1, TimeUnit.SECONDS);
            assertEquals(params.pathSegments.segments, req.getRequestUrl().encodedPathSegments().size(), 
            "There should be the correct number of path segments.");
            assertEquals(params.expectedQueryParameters, req.getRequestUrl().querySize(), 
            "There should be the correct number of query parameters.");
        }
    }

    // Test helper that exercises all variants of the getDocument "find"
    private void testGetDocumentVariants(final String id, final TestParameters params) throws Exception {
        callAndAssertValidation(params, () -> {
            return db.find(id);
        });
        callAndAssertValidation(params.withQueryParams(1), () -> {
            return db.find(id, "1-abc");
        });
        callAndAssertValidation(params, () -> {
            return db.find(JsonObject.class, id);
        });
        callAndAssertValidation(params, () -> {
            return db.find(JsonObject.class, id, new Params());
        });
        callAndAssertValidation(params.withQueryParams(1), () -> {
            return db.find(JsonObject.class, id, "1-abc");
        });
        // We also want to test HEAD for all GET cases
        testContains(id, params);
    }

    // Test helper that exercises HEAD via contains
    private void testContains(final String id, final TestParameters params) throws Exception {
        Boolean expectedReturn;
        switch(params.response) {
            case RESPONSE_200:
                expectedReturn = true;
                break;
            case RESPONSE_404:
                expectedReturn = false;
                break;
            case VALIDATION_EXCEPTION_DOCID:
            default:
                expectedReturn = null;
        }
        callAndAssertValidation(new TestParameters(params.response, params.pathSegments, expectedReturn), () -> {
            return db.contains(id);
        });
    }

    private void testDeleteDocumentVariants(final String id, final TestParameters expected) throws Exception {
        callAndAssertValidation(expected.withQueryParams(1), () -> {
            Map<String, String> doc = new HashMap<>();
            doc.put("_id", id);
            doc.put("_rev", "1-abc");
            return db.remove(doc);
        });
        callAndAssertValidation(expected.withQueryParams(1), () -> {
            return db.remove(id, "1-abc");
        });
    }

    private void testPutDocumentVariants(final String id, final TestParameters expected) throws Exception {
        Map<String, String> doc = new HashMap<>();
        doc.put("_id", id);

        callAndAssertValidation(expected, () -> {
            return db.save(doc);
        });
        callAndAssertValidation(expected.withQueryParams(1), () -> {
            return db.save(doc, 2);
        });
    }

    private InputStream getAttachmentStream() throws Exception {
        return new ByteArrayInputStream("foo".getBytes("UTF-8"));
    }

    // Test helper that exercises all variants of the getDocument "find"
    private void testGetAttachmentVariants(final String id, final String attachmentName, final TestParameters expected) throws Exception {
        callAndAssertValidation(expected, () -> {
            try (InputStream attStream = db.getAttachment(id, attachmentName)){
                return null;
            }
        });
        callAndAssertValidation(expected.withQueryParams(1), () -> {
            try (InputStream attStream = db.getAttachment(id, attachmentName, "1-abc")){
                return null;
            }
        });
    }

    private void testPutAttachmentVariants(final String id, final String attachmentName, final TestParameters expected) throws Exception {
        callAndAssertValidation(expected.withQueryParams(1), () -> {
            return db.saveAttachment(getAttachmentStream(), attachmentName, "text/plain", id, "1-abc");
        });
    }

    private void testDeleteAttachmentVariants(final String id, final String attachmentName, final TestParameters expected) throws Exception {
        callAndAssertValidation(expected.withQueryParams(1), () -> {
            Map<String, String> doc = new HashMap<>();
            doc.put("_id", id);
            doc.put("_rev", "1-abc");
            return db.removeAttachment(doc, attachmentName);
        });
        callAndAssertValidation(expected.withQueryParams(1), () -> {
            return db.removeAttachment(id, "1-abc", attachmentName);
        });
    }

    // GET _all_docs
    // EXPECTED: Validation failure
    @Test
    public void invalidGetAllDocs() throws Exception{
        testGetDocumentVariants("_all_docs", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _design/foo
    // EXPECTED: 200
    @Test
    public void validGetDesignDoc() throws Exception {
        // 3 path parts are expected here because of the design prefix
        testGetDocumentVariants("_design/foo", new TestParameters(Expect.RESPONSE_200, ExpectPathSegments.THREE));
    }

    // GET /_design/foo
    // EXPECTED: 404
    @Test
    public void invalidGetDesignDocLeadingSlash() throws Exception {
        // 2 path parts are expected {db}/{doc_id} where the doc ID is %2F_design%2Ffoo
        testGetDocumentVariants("/_design/foo", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.TWO));
    }

    // GET _design
    // EXPECTED: Validation exception
    @Test
    public void invalidGetDesignDoc() throws Exception {
        // Note no trailing / on the _design prefix
        testGetDocumentVariants("_design", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _design/foo/_view/bar
    // EXPECTED: 404
    @Test
    public void invalidGetView() throws Exception {
        // 3 path parts are expected here because of the design prefix
        testGetDocumentVariants("_design/foo/_view/bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.THREE));
    }

    // GET _design/foo/_info
    // EXPECTED: 404
    @Test
    public void invalidGetViewInfo() throws Exception {
        // 3 path parts are expected here because of the design prefix
        testGetDocumentVariants("_design/foo/_info", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.THREE));
    }

    // GET _design/foo/_search/bar
    // EXPECTED: 404
    @Test
    public void invalidGetSearch() throws Exception {
        // 3 path parts are expected here because of the design prefix
        testGetDocumentVariants("_design/foo/_search/bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.THREE));
        // With a parameter
        testGetDocumentVariants("_design/foo/_search/bar?q=*.*", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.THREE));
    }

    // GET _design/foo/_search_info/bar
    // EXPECTED: 404
    @Test
    public void invalidGetSearchInfo() throws Exception {
        // 3 path parts are expected here because of the design prefix
        testGetDocumentVariants("_design/foo/_search_info/bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.THREE));
    }

    // GET _design/foo/_geo/bar
    // EXPECTED: 404
    @Test
    public void invalidGetGeo() throws Exception {
        // 3 path parts are expected here because of the design prefix
        testGetDocumentVariants("_design/foo/_geo/bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.THREE));
        // With a parameter
        testGetDocumentVariants("_design/foo/_geo/bar?bbox=-50.52,-4.46,54.59,1.45", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.THREE));
    }

    // GET _design/foo/_geo_info/bar
    // EXPECTED: 404
    @Test
    public void invalidGetGeoInfo() throws Exception {
        // 3 path parts are expected here because of the design prefix
        testGetDocumentVariants("_design/foo/_geo_info/bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.THREE));
    }

    // GET _local/foo
    // EXPECTED: 200
    @Test
    public void validGetLocalDoc() throws Exception {
        // 3 path parts are expected here because of the local prefix
        testGetDocumentVariants("_local/foo", new TestParameters(Expect.RESPONSE_200, ExpectPathSegments.THREE));
    }

    // GET _local
    // EXPECTED: Validation exception
    @Test
    public void invalidGetLocalDoc() throws Exception {
        // Note no trailing / on the prefix
        testGetDocumentVariants("_local", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _local_docs
    // EXPECTED: Validation exception
    @Test
    public void invalidGetLocalDocs() throws Exception {
        testGetDocumentVariants("_local_docs", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _design_docs
    // EXPECTED: Validation exception
    @Test
    public void invalidGetDesignDocs() throws Exception {
        testGetDocumentVariants("_design_docs", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _changes
    // EXPECTED: Validation exception
    @Test
    public void invalidGetChanges() throws Exception {
        testGetDocumentVariants("_changes", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _ensure_full_commit
    // EXPECTED: Validation exception
    @Test
    public void invalidGetEnsureFullCommit() throws Exception {
        testGetDocumentVariants("_ensure_full_commit", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _index
    // EXPECTED: Validation exception
    @Test
    public void invalidGetIndex() throws Exception {
        testGetDocumentVariants("_index", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _revs_limit
    // EXPECTED: Validation exception
    @Test
    public void invalidGetRevsLimit() throws Exception {
        testGetDocumentVariants("_revs_limit", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _security
    // EXPECTED: Validation exception
    @Test
    public void invalidGetSecurity() throws Exception {
        testGetDocumentVariants("_security", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _shards
    // EXPECTED: Validation exception
    @Test
    public void invalidGetShards() throws Exception {
        testGetDocumentVariants("_shards", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // DELETE _index/_design/foo/json/bar
    // EXPECTED: Validtion exception
    @Test
    public void invalidDeleteIndex() throws Exception {
        testDeleteDocumentVariants("_index/_design/foo/json/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }
    
    // DELETE _design/foo
    // EXPECTED: 200
    @Test
    public void validDeleteDesignDoc() throws Exception {
        // 3 path parts are expected here because of the design prefix
        testDeleteDocumentVariants("_design/foo", new TestParameters(Expect.RESPONSE_200, ExpectPathSegments.THREE));
    }

    // DELETE _design
    // EXPECTED: Validation exception
    @Test
    public void invalidDeleteDesignDoc() throws Exception {
        // Note no trailing / on the _design prefix
        testDeleteDocumentVariants("_design", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // DELETE _local/foo
    // EXPECTED: 200
    @Test
    public void validDeleteLocalDoc() throws Exception {
        // 3 path parts are expected here because of the local prefix
        testDeleteDocumentVariants("_local/foo", new TestParameters(Expect.RESPONSE_200, ExpectPathSegments.THREE));
    }

    // DELETE _local
    // EXPECTED: Validation exception
    @Test
    public void invalidDeleteLocalDoc() throws Exception {
        // Note no trailing / on the prefix
        testDeleteDocumentVariants("_local", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // PUT _design/foo
    // EXPECTED: 201
    @Test
    public void validPutDesignDoc() throws Exception {
        // 3 path parts are expected here because of the design prefix
        testPutDocumentVariants("_design/foo", new TestParameters(Expect.RESPONSE_201, ExpectPathSegments.THREE));
    }

    // PUT _design
    // EXPECTED: Validation exception
    @Test
    public void invalidPutDesignDoc() throws Exception {
        // Note no trailing / on the _design prefix
        testPutDocumentVariants("_design", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // PUT _local/foo
    // EXPECTED: 201
    @Test
    public void validPutLocalDoc() throws Exception {
        // 3 path parts are expected here because of the local prefix
        testPutDocumentVariants("_local/foo", new TestParameters(Expect.RESPONSE_201, ExpectPathSegments.THREE));
    }

    // PUT _local
    // EXPECTED: Validation exception
    @Test
    public void invalidPutLocalDoc() throws Exception {
        // Note no trailing / on the prefix
        testPutDocumentVariants("_local", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // PUT _revs_limit
    // EXPECTED: Validation exception
    @Test
    public void invalidPutRevsLimit() throws Exception {
        testPutDocumentVariants("_revs_limit", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // PUT _security
    // EXPECTED: Validation exception
    @Test
    public void invalidPutSecurity() throws Exception {
        testPutDocumentVariants("_security", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _design/foo/bar
    // EXPECTED: 200
    @Test
    public void validGetDesignDocAttachment() throws Exception {
        // 4 path parts are expected here because of the design prefix /{db}/_design/{ddoc}/{attachment}
        testGetAttachmentVariants("_design/foo", "bar", new TestParameters(Expect.RESPONSE_200, ExpectPathSegments.FOUR));
    }

    // PUT _design/foo/bar
    // EXPECTED: 201
    @Test
    public void validPutDesignDocAttachment() throws Exception {
        // 4 path parts are expected here because of the design prefix /{db}/_design/{ddoc}/{attachment}
        testPutAttachmentVariants("_design/foo", "bar", new TestParameters(Expect.RESPONSE_201, ExpectPathSegments.FOUR));
    }

    // DELETE _design/foo/bar
    // EXPECTED: 200
    @Test
    public void validDeleteDesignDocAttachment() throws Exception {
        // 4 path parts are expected here because of the design prefix /{db}/_design/{ddoc}/{attachment}
        testDeleteAttachmentVariants("_design/foo", "bar", new TestParameters(Expect.RESPONSE_200, ExpectPathSegments.FOUR));
    }

    // GET _design/foo
    // EXPECTED: Validaton exception
    @Test
    public void invalidGetDesignDocAttachment() throws Exception {
        testGetAttachmentVariants("_design", "foo", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // PUT _design/foo
    // EXPECTED: Validaton exception
    @Test
    public void invalidPutDesignDocAttachment() throws Exception {
        // 4 path parts are expected here because of the design prefix /{db}/_design/{ddoc}/{attachment}
        testPutAttachmentVariants("_design", "foo", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // DELETE _design/foo
    // EXPECTED: Validaton exception
    @Test
    public void invalidDeleteDesignDocAttachment() throws Exception {
        // 4 path parts are expected here because of the design prefix /{db}/_design/{ddoc}/{attachment}
        testDeleteAttachmentVariants("_design", "foo", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // DELETE _index/_design/foo/json/bar
    // EXPECTED: Validtion exception
    @Test
    public void invalidDeleteIndexViaAttachment() throws Exception {
        testDeleteAttachmentVariants("_index", "_design/foo/json/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
        testDeleteAttachmentVariants("_index/_design", "foo/json/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
        testDeleteAttachmentVariants("_index/_design/foo", "json/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
        testDeleteAttachmentVariants("_index/_design/foo/json", "bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _design/foo/_view/bar
    @Test
    public void invalidGetViewViaDesignDocAttachment() throws Exception {
        // Expect 404
        // 4 path parts are expected here because of the design prefix /{db}/_design/{ddoc}/{attachment}
        testGetAttachmentVariants("_design/foo/_view", "bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.FOUR));
        // This has a valid design doc and a leading slash as part of a valid attachment name
        testGetAttachmentVariants("_design/foo", "/_view/bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.FOUR));
        // Expect validation exception for attachment name (_design/foo is a valid ddoc)
        testGetAttachmentVariants("_design/foo", "_view/bar", TestParameters.VALIDATION_EXCEPTION_ATTNAME);
        // Things get interesting with a trailing slash and a valid attachment name
        testGetAttachmentVariants("_design", "foo/_view/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
        testGetAttachmentVariants("_design/", "foo/_view/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // PUT _design/foo/_view/bar
    @Test
    public void invalidPutDesignDocAttachmentAtView() throws Exception {
        // Expect validation exception for attachment name (_design/foo is a valid ddoc)
        testPutAttachmentVariants("_design/foo", "_view/bar", TestParameters.VALIDATION_EXCEPTION_ATTNAME);
        // Things get interesting with a trailing slash and a valid attachment name
        testPutAttachmentVariants("_design", "foo/_view/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
        testPutAttachmentVariants("_design/", "foo/_view/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // DELETE _design/foo/_view/bar
    @Test
    public void invalidDeleteDesignDocAttachmentAtView() throws Exception {
        // Expect validation exception for attachment name (_design/foo is a valid ddoc)
        testDeleteAttachmentVariants("_design/foo", "_view/bar", TestParameters.VALIDATION_EXCEPTION_ATTNAME);
        // Things get interesting with a trailing slash and a valid attachment name
        testDeleteAttachmentVariants("_design", "foo/_view/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
        testDeleteAttachmentVariants("_design/", "foo/_view/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _design/foo/_info
    @Test
    public void invalidGetViewInfoViaDesignDocAttachment() throws Exception {
        // Expect validation exception for attachment name (_design/foo is a valid ddoc)
        testGetAttachmentVariants("_design/foo", "_info", TestParameters.VALIDATION_EXCEPTION_ATTNAME);
        testGetAttachmentVariants("_design", "foo/_info", TestParameters.VALIDATION_EXCEPTION_DOCID);
        testGetAttachmentVariants("_design/", "foo/_info", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _design/foo/_search/bar
    @Test
    public void invalidGetSearchViaDesignDocAttachment() throws Exception {
        // Expect 404
        // 4 path parts are expected here because of the design prefix /{db}/_design/{ddoc}/{attachment}
        testGetAttachmentVariants("_design/foo/_search", "bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.FOUR));
        // With a parameter
        testGetAttachmentVariants("_design/foo/_search", "bar?q=*.*", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.FOUR));
        // Expect validation exception for attachment name (_design/foo is a valid ddoc)
        testGetAttachmentVariants("_design/foo", "_search/bar", TestParameters.VALIDATION_EXCEPTION_ATTNAME);
        testGetAttachmentVariants("_design", "foo/_search/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
        testGetAttachmentVariants("_design/", "foo/_search/bar", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _design/foo/_search_info/bar
    @Test
    public void invalidGetSearchInfoViaDesignDocAttachment() throws Exception {
        // Expect 404
        // 4 path parts are expected here because of the design prefix /{db}/_design/{ddoc}/{attachment}
        testGetAttachmentVariants("_design/foo/_search_info", "bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.FOUR));
        // Expect validation exception for attachment name (_design/foo is a valid ddoc)
        testGetAttachmentVariants("_design/foo", "_search_info/bar", TestParameters.VALIDATION_EXCEPTION_ATTNAME);
    }

    // GET _design/foo/_geo/bar
    @Test
    public void invalidGetGeoViaDesignDocAttachment() throws Exception {
        // Expect 404
        // 4 path parts are expected here because of the design prefix /{db}/_design/{ddoc}/{attachment}
        testGetAttachmentVariants("_design/foo/_geo", "bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.FOUR));
        // With a parameter
        testGetAttachmentVariants("_design/foo/_geo", "bar?bbox=-50.52,-4.46,54.59,1.45", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.FOUR));
        // Expect validation exception for attachment name (_design/foo is a valid ddoc)
        testGetAttachmentVariants("_design/foo", "_geo/bar", TestParameters.VALIDATION_EXCEPTION_ATTNAME);

    }

    // GET _design/foo/_geo_info/bar
    @Test
    public void invalidGetGeoInfoViaDesignDocAttachment() throws Exception {
        // Expect 404
        // 4 path parts are expected here because of the design prefix /{db}/_design/{ddoc}/{attachment}
        testGetAttachmentVariants("_design/foo/_geo_info", "bar", new TestParameters(Expect.RESPONSE_404, ExpectPathSegments.FOUR));
        // Expect validation exception for attachment name (_design/foo is a valid ddoc)
        testGetAttachmentVariants("_design/foo", "_geo_info/bar", TestParameters.VALIDATION_EXCEPTION_ATTNAME);
    }

    // GET _partition/foo
    // EXPECTED: Validation exception
    @Test
    public void invalidGetPartitionInfo() throws Exception{
        testGetDocumentVariants("_partition/foo", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _partition/foo
    // EXPECTED: Validation exception
    @Test
    public void invalidGetPartitionInfoViaAttachment() throws Exception{
        testGetAttachmentVariants("_partition", "foo", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _partition/foo/_all_docs
    // EXPECTED: Validation exception
    @Test
    public void invalidGetPartitionAllDocs() throws Exception{
        testGetDocumentVariants("_partition/foo/_all_docs", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }

    // GET _partition/foo/_all_docs
    // EXPECTED: Validation exception
    @Test
    public void invalidGetPartitionAllDocsViaAttachment() throws Exception{
        testGetAttachmentVariants("_partition", "foo/_all_docs", TestParameters.VALIDATION_EXCEPTION_DOCID);
        testGetAttachmentVariants("_partition/foo", "_all_docs", TestParameters.VALIDATION_EXCEPTION_DOCID);
    }
}
