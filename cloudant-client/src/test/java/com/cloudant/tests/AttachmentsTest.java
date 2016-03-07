/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright (c) 2015 IBM Corp. All rights reserved.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Attachment;
import com.cloudant.client.api.model.Document;
import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.internal.DatabaseURIHelper;
import com.cloudant.http.Http;
import com.cloudant.http.HttpConnection;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.Utils;

import org.apache.commons.codec.binary.Base64;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

@Category(RequiresDB.class)
public class AttachmentsTest {

    public static CloudantClientResource clientResource = new CloudantClientResource();
    public static DatabaseResource dbResource = new DatabaseResource(clientResource);
    @ClassRule
    public static RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);

    private static Database db;

    @BeforeClass
    public static void setUp() {
        db = dbResource.get();
    }


    @Test
    public void attachmentInline() {
        Attachment attachment1 = new Attachment("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=",
                "text/plain");

        Attachment attachment2 = new Attachment();
        attachment2.setData(Base64.encodeBase64String("binary string".getBytes()));
        attachment2.setContentType("text/plain");

        Bar bar = new Bar(); // Bar extends Document
        bar.addAttachment("txt_1.txt", attachment1);
        bar.addAttachment("txt_2.txt", attachment2);

        db.save(bar);
    }

    @Test
    public void attachmentInline_getWithDocument() {
        Attachment attachment = new Attachment("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=",
                "text/plain");
        Bar bar = new Bar();
        bar.addAttachment("txt_1.txt", attachment);

        Response response = db.save(bar);

        Bar bar2 = db.find(Bar.class, response.getId(), new Params().attachments());
        String base64Data = bar2.getAttachments().get("txt_1.txt").getData();
        assertNotNull(base64Data);
    }

    @Test
    public void attachmentStandalone() throws IOException, URISyntaxException {
        byte[] bytesToDB = "binary data".getBytes();
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain");

        Document doc = db.find(Document.class, response.getId());
        assertTrue(doc.getAttachments().containsKey("foo.txt"));

        HttpConnection conn = Http.GET(new DatabaseURIHelper(db.getDBUri())
                .attachmentUri(response.getId(), "foo.txt"));
        InputStream in = clientResource.get().executeRequest(conn).responseAsInputStream();

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        int n;
        while ((n = in.read()) != -1) {
            bytesOut.write(n);
        }
        bytesOut.flush();
        in.close();

        byte[] bytesFromDB = bytesOut.toByteArray();

        assertArrayEquals(bytesToDB, bytesFromDB);
    }

    @Test
    public void attachmentStandaloneUpdate() throws Exception {
        byte[] bytesToDB = "binary data".getBytes("UTF-8");
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain");

        bytesToDB = "updated binary data".getBytes("UTF-8");
        bytesIn = new ByteArrayInputStream(bytesToDB);
        response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", response.getId(), response
                .getRev());

        Document doc = db.find(Document.class, response.getId(), response.getRev());
        assertTrue(doc.getAttachments().containsKey("foo.txt"));

        HttpConnection conn = Http.GET(new DatabaseURIHelper(db.getDBUri())
                .attachmentUri(response.getId(), "foo.txt"));
        InputStream in = clientResource.get().executeRequest(conn).responseAsInputStream();

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        int n;
        while ((n = in.read()) != -1) {
            bytesOut.write(n);
        }
        bytesOut.flush();
        in.close();

        byte[] bytesFromDB = bytesOut.toByteArray();

        assertArrayEquals(bytesToDB, bytesFromDB);
    }

    @Test
    public void addNewAttachmentToExistingDocument() throws Exception {

        // Save a new document
        Bar bar = new Bar();
        Response response = db.save(bar);

        // Create an attachment and save it to the existing document
        byte[] bytesToDB = "binary data".getBytes("UTF-8");
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        Response attResponse = db.saveAttachment(bytesIn, "foo.txt", "text/plain", response.getId
                (), response.getRev());

        assertEquals("The document ID should be the same", response.getId(), attResponse.getId());
        assertTrue("The response code should be a 20x", attResponse.getStatusCode() / 100 == 2);
        assertNull("There should be no error saving the attachment", attResponse.getError());

        // Assert the attachment is correct
        Document doc = db.find(Document.class, response.getId(), attResponse.getRev());
        assertTrue(doc.getAttachments().containsKey("foo.txt"));

        HttpConnection conn = Http.GET(new DatabaseURIHelper(db.getDBUri())
                .attachmentUri(response.getId(), attResponse.getRev(), "foo.txt"));
        InputStream in = clientResource.get().executeRequest(conn).responseAsInputStream();

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        int n;
        while ((n = in.read()) != -1) {
            bytesOut.write(n);
        }
        bytesOut.flush();
        in.close();

        byte[] bytesFromDB = bytesOut.toByteArray();

        assertArrayEquals(bytesToDB, bytesFromDB);

    }

    @Test
    public void attachmentStandaloneNullIdNullRev() throws IOException, URISyntaxException {
        byte[] bytesToDB = "binary data".getBytes();
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        // Save the attachment to a doc with the given ID
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", null, null);

        Document doc = db.find(Document.class, response.getId());
        assertTrue(doc.getAttachments().containsKey("foo.txt"));

        HttpConnection conn = Http.GET(new DatabaseURIHelper(db.getDBUri())
                .attachmentUri(response.getId(), "foo.txt"));
        InputStream in = clientResource.get().executeRequest(conn).responseAsInputStream();

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        int n;
        while ((n = in.read()) != -1) {
            bytesOut.write(n);
        }
        bytesOut.flush();
        in.close();

        byte[] bytesFromDB = bytesOut.toByteArray();

        assertArrayEquals(bytesToDB, bytesFromDB);
    }

    @Test
    public void attachmentStandaloneGivenId() throws IOException, URISyntaxException {
        String docId = Utils.generateUUID();
        byte[] bytesToDB = "binary data".getBytes();
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        // Save the attachment to a doc with the given ID
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", docId, null);

        assertEquals("The saved document ID should match", docId, response.getId());

        Document doc = db.find(Document.class, response.getId());
        assertTrue(doc.getAttachments().containsKey("foo.txt"));

        HttpConnection conn = Http.GET(new DatabaseURIHelper(db.getDBUri())
                .attachmentUri(response.getId(), "foo.txt"));
        InputStream in = clientResource.get().executeRequest(conn).responseAsInputStream();

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        int n;
        while ((n = in.read()) != -1) {
            bytesOut.write(n);
        }
        bytesOut.flush();
        in.close();

        byte[] bytesFromDB = bytesOut.toByteArray();

        assertArrayEquals(bytesToDB, bytesFromDB);
    }

    @Test(expected = IllegalArgumentException.class)
    public void attachmentStandaloneNullDocNonNullRev() {
        byte[] bytesToDB = "binary data".getBytes();
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", null, "1-abcdef");
    }

    @Test(expected = IllegalArgumentException.class)
    public void attachmentStandaloneEmptyDocId() {
        byte[] bytesToDB = "binary data".getBytes();
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", "", "1-abcdef");
    }

    @Test(expected = IllegalArgumentException.class)
    public void attachmentStandaloneDocIdEmptyRev() {
        String docId = Utils.generateUUID();
        byte[] bytesToDB = "binary data".getBytes();
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", docId, "");
    }
}
