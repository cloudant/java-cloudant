/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.model.Attachment;
import com.cloudant.client.api.model.Document;
import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Response;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.cloudant.tests.util.Utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

@RequiresDB
public class AttachmentsTest extends TestWithDbPerClass {

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
    public void getAttachmentStandaloneWithoutRev() throws IOException, URISyntaxException {
        byte[] bytesToDB = "binary data".getBytes();
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain");

        Document doc = db.find(Document.class, response.getId());
        assertTrue(doc.getAttachments().containsKey("foo.txt"));

        InputStream in = db.getAttachment(response.getId(), "foo.txt");

        try {

            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            IOUtils.copy(in, bytesOut);
            byte[] bytesFromDB = bytesOut.toByteArray();
            assertArrayEquals(bytesToDB, bytesFromDB);
        } finally {
            in.close();
        }
    }

    @Test
    public void getAttachmentStandaloneWithRev() throws IOException, URISyntaxException {
        byte[] bytesToDB = "binary data".getBytes();
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain");

        Document doc = db.find(Document.class, response.getId());
        assertTrue(doc.getAttachments().containsKey("foo.txt"));

        InputStream in = db.getAttachment(
                response.getId(), "foo.txt", response.getRev());

        try {

            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            IOUtils.copy(in, bytesOut);
            byte[] bytesFromDB = bytesOut.toByteArray();
            assertArrayEquals(bytesToDB, bytesFromDB);
        } finally {
            in.close();
        }
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

        InputStream in = db.getAttachment(response.getId(), "foo.txt");

        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            IOUtils.copy(in, bytesOut);
            byte[] bytesFromDB = bytesOut.toByteArray();
            assertArrayEquals(bytesToDB, bytesFromDB);
        } finally {
            in.close();
        }
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

        assertEquals(response.getId(), attResponse.getId(), "The document ID should be the same");
        assertTrue(attResponse.getStatusCode() / 100 == 2, "The response code should be a 20x");
        assertNull(attResponse.getError(), "There should be no error saving the attachment");

        // Assert the attachment is correct
        Document doc = db.find(Document.class, response.getId(), attResponse.getRev());
        assertTrue(doc.getAttachments().containsKey("foo.txt"));

        InputStream in = db.getAttachment(response.getId(), "foo.txt");

        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            IOUtils.copy(in, bytesOut);
            byte[] bytesFromDB = bytesOut.toByteArray();
            assertArrayEquals(bytesToDB, bytesFromDB);
        } finally {
            in.close();
        }
    }

    @Test
    public void attachmentStandaloneNullIdNullRev() throws IOException, URISyntaxException {
        byte[] bytesToDB = "binary data".getBytes();
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        // Save the attachment to a doc with the given ID
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", null, null);

        Document doc = db.find(Document.class, response.getId());
        assertTrue(doc.getAttachments().containsKey("foo.txt"));

        InputStream in = db.getAttachment(response.getId(), "foo.txt");

        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            IOUtils.copy(in, bytesOut);
            byte[] bytesFromDB = bytesOut.toByteArray();
            assertArrayEquals(bytesToDB, bytesFromDB);
        } finally {
            in.close();
        }
    }

    @Test
    public void attachmentStandaloneGivenId() throws IOException, URISyntaxException {
        String docId = Utils.generateUUID();
        byte[] bytesToDB = "binary data".getBytes();
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
        // Save the attachment to a doc with the given ID
        Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", docId, null);

        assertEquals(docId, response.getId(), "The saved document ID should match");

        InputStream in = db.getAttachment(docId, "foo.txt");

        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            IOUtils.copy(in, bytesOut);
            byte[] bytesFromDB = bytesOut.toByteArray();
            assertArrayEquals(bytesToDB, bytesFromDB);
        } finally {
            in.close();
        }
    }

    @Test
    public void attachmentStandaloneNullDocNonNullRev() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                byte[] bytesToDB = "binary data".getBytes();
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
                Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", null,
                        "1-abcdef");
            }
        });
    }

    @Test
    public void attachmentStandaloneEmptyDocId() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                byte[] bytesToDB = "binary data".getBytes();
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
                Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", "",
                        "1-abcdef");
            }
        });
    }

    @Test
    public void attachmentStandaloneDocIdEmptyRev() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                String docId = Utils.generateUUID();
                byte[] bytesToDB = "binary data".getBytes();
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
                Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain", docId, "");
            }
        });
    }

    @Test
    public void removeAttachment() {
        String attachmentName = "txt_1.txt";
        Attachment attachment1 = new Attachment("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=",
                "text/plain");

        Bar bar = new Bar(); // Bar extends Document
        bar.addAttachment(attachmentName, attachment1);

        Response response = db.save(bar);

        Bar bar2 = db.find(Bar.class, response.getId(), new Params().attachments());
        String base64Data = bar2.getAttachments().get("txt_1.txt").getData();
        assertNotNull(base64Data);

        response = db.removeAttachment(bar2, attachmentName);

        Bar bar3 = db.find(Bar.class, response.getId(), new Params().attachments());
        assertNull(bar3.getAttachments());
    }

    @Test
    public void removeAttachmentNullIdNonNullRev() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                String attachmentName = "txt_1.txt";
                Response response = db.removeAttachment(null, "1-abcdef", attachmentName);
            }
        });
    }

    @Test
    public void removeAttachmentNonNullIdNullRev() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                String docId = Utils.generateUUID();
                String attachmentName = "txt_1.txt";
                Response response = db.removeAttachment(docId, null, attachmentName);
            }
        });
    }

    @Test
    public void removeAttachmentNonNullIdNonNullRevNullAttachmentName() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                String docId = Utils.generateUUID();
                String rev = "1-abcdef";
                Response response = db.removeAttachment(docId, rev, null);
            }
        });
    }
}
