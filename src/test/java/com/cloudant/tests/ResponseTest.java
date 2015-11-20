package com.cloudant.tests;

import static org.junit.Assert.assertEquals;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.org.lightcouch.CouchDbException;
import com.cloudant.client.org.lightcouch.DocumentConflictException;
import com.cloudant.client.org.lightcouch.NoDocumentException;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.Utils;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.ArrayList;
import java.util.List;

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
        assertEquals(201, response.getStatusCode());
    }

    @Test
    public void verifyDocumentNotFound() {
        try {
            db.find(Response.class, "no_id");
        } catch (NoDocumentException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("missing", e.getReason());
        }
    }

    @Test
    public void verifyDocumentConflict() {
        try {
            Response response = db.save(foo);
            assertEquals(201, response.getStatusCode());

            db.save(foo);
        } catch (DocumentConflictException e) {
            assertEquals(409, e.getStatusCode());
        }
    }

    @Test
    public void verifyBadRequest() {
        try {
            foo.set_rev("bad_rev");
            db.update(foo);
        } catch (CouchDbException e) {
            assertEquals(400, e.getStatusCode());
        }

    }

    @Test
    public void verifyBulkDocumentRequest() {
        ArrayList<Foo> foos = new ArrayList<Foo>();
        foos.add(new Foo(Utils.generateUUID()));
        foos.add(new Foo(Utils.generateUUID()));
        foos.add(new Foo(Utils.generateUUID()));

        List<Response> responses = db.bulk(foos);
        for(Response response : responses) {
            assertEquals(201, response.getStatusCode());
        }
    }
}
