/*
 * Copyright (C) 2011 lightcouch.org
 *
 * Modifications for this distribution by IBM Cloudant, Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudant.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Attachment;
import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Response;

public class AttachmentsTest  {


	private static Database db;
	private CloudantClient account;

	@Before
	public void  setUp() {
		account = CloudantClientHelper.getClient();
		db = account.database("lightcouch-db-test", true);
	}

	@After
	public void tearDown(){
		account.deleteDB("lightcouch-db-test");
		account.shutdown();
	}

	@Test
	public void attachmentInline() {
		Attachment attachment1 = new Attachment("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=", "text/plain");

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
		Attachment attachment = new Attachment("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=", "text/plain");
		Bar bar = new Bar();
		bar.addAttachment("txt_1.txt", attachment);
		
		Response response = db.save(bar);
		
		Bar bar2 = db.find(Bar.class, response.getId(), new Params().attachments());
		String base64Data = bar2.getAttachments().get("txt_1.txt").getData();
		assertNotNull(base64Data);
	}

	@Test
	public void attachmentStandalone() throws IOException {
		byte[] bytesToDB = "binary data".getBytes();
		ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
		Response response = db.saveAttachment(bytesIn, "foo.txt", "text/plain");

		InputStream in = db.find(response.getId() + "/foo.txt");
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
}
