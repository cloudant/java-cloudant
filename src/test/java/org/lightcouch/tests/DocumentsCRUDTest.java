/*
 * Copyright (C) 2011 lightcouch.org
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

package org.lightcouch.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.Attachment;
import org.lightcouch.CouchDbClient;
import org.lightcouch.DocumentConflictException;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Params;
import org.lightcouch.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class DocumentsTest {

	private static CouchDbClient dbClient;

	@BeforeClass
	public static void setUpClass() {
		dbClient = new CouchDbClient();
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
	}

	// Find

	@Test
	public void findById() {
		Response response = dbClient.save(new Foo());
		Foo foo = dbClient.find(Foo.class, response.getId());
		assertNotNull(foo);
	}

	@Test
	public void findByIdAndRev() {
		Response response = dbClient.save(new Foo());
		Foo foo = dbClient.find(Foo.class, response.getId(), response.getRev());
		assertNotNull(foo);
	}

	@Test
	public void findJsonObject() {
		Response response = dbClient.save(new Foo());
		JsonObject jsonObject = dbClient.find(JsonObject.class, response.getId());
		assertNotNull(jsonObject);
	}

	@Test
	public void findAny() {
		String uri = dbClient.getDBUri().toString();
		JsonObject jsonObject = dbClient.findAny(JsonObject.class, uri);
		assertNotNull(jsonObject);
	}

	@Test
	public void findInputstream() throws IOException {
		Response response = dbClient.save(new Foo());
		InputStream inputStream = dbClient.find(response.getId());
		assertTrue(inputStream.read() != -1);
		inputStream.close();
	}

	@Test
	public void findWithParams() {
		Response response = dbClient.save(new Foo());
		Foo foo = dbClient.find(Foo.class, response.getId(), new Params().revsInfo().attachments());
		assertNotNull(foo);
	}

	@Test(expected = IllegalArgumentException.class)
	public void findWithInvalidId_throwsIllegalArgumentException() {
		dbClient.find(Foo.class, "");
	}

	@Test(expected = NoDocumentException.class)
	public void findWithUnknownId_throwsNoDocumentException() {
		dbClient.find(Foo.class, generateUUID());
	}

	@Test
	public void contains() {
		boolean found = dbClient.contains(generateUUID());
		assertFalse(found);

		Response response = dbClient.save(new Foo());
		found = dbClient.contains(response.getId());
		assertTrue(found);
	}

	// Save

	@Test
	public void savePOJO() {
		dbClient.save(new Foo());
	}

	@Test
	public void saveMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("_id", generateUUID());
		map.put("field1", "value1");
		dbClient.save(map);
	}

	@Test
	public void saveJsonObject() {
		JsonObject json = new JsonObject();
		json.addProperty("_id", generateUUID());
		json.add("json-array", new JsonArray());
		dbClient.save(json);
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveInvalidObject_throwsIllegalArgumentException() {
		dbClient.save(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveNewDocWithRevision_throwsIllegalArgumentException() {
		Bar bar = new Bar();
		bar.setRevision("some-rev");
		dbClient.save(bar);
	}

	@Test(expected = DocumentConflictException.class)
	public void saveDocWithDuplicateId_throwsDocumentConflictException() {
		String id = generateUUID();
		dbClient.save(new Foo(id));
		dbClient.save(new Foo(id));
	}

	@Test
	public void batch() {
		dbClient.batch(new Foo());
	}

	// Update

	@Test
	public void update() {
		Response response = dbClient.save(new Foo());
		Foo foo = dbClient.find(Foo.class, response.getId());
		dbClient.update(foo);
	}

	@Test(expected = IllegalArgumentException.class)
	public void updateWithoutIdAndRev_throwsIllegalArgumentException() {
		dbClient.update(new Foo());
	}

	@Test(expected = DocumentConflictException.class)
	public void updateWithInvalidRev_throwsDocumentConflictException() {
		Response response = dbClient.save(new Foo());
		Foo foo = dbClient.find(Foo.class, response.getId());
		dbClient.update(foo);
		dbClient.update(foo);
	}

	// Bulk

	@Test
	public void bulkModifyDoc() {
		List<Object> newDocs = new ArrayList<Object>();
		newDocs.add(new Foo());
		newDocs.add(new JsonObject());

		boolean allOrNothing = true;
		List<Response> responses = dbClient.bulk(newDocs, allOrNothing);
		assertThat(responses.size(), is(2));
	}

	@Test
	public void bulkGetDocs() {
		Response r1 = dbClient.save(new Foo());
		Response r2 = dbClient.save(new Foo());
		
		List<String> keys = Arrays.asList(new String[] { r1.getId(), r2.getId() });
		List<Foo> docs = dbClient.view("_all_docs")
				.includeDocs(true)
				.keys(keys)
				.query(Foo.class);
		assertThat(docs.size(), is(2));
	}

	// Attachment

	@Test
	public void attachmentInline() {
		Attachment attachment = new Attachment("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=", "text/plain");

		Attachment attachment2 = new Attachment();
		String data = Base64.encodeBase64String("binary string".getBytes());
		attachment2.setData(data);
		attachment2.setContentType("text/plain");

		Bar bar = new Bar(); // Bar extends Document
		bar.addAttachment("txt1.txt", attachment);
		bar.addAttachment("txt2.txt", attachment2);

		dbClient.save(bar);
	}

	@Test
	public void attachmentInline_getWithDocument() {
		Attachment attachment = new Attachment("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=", "text/plain");
		
		Bar bar = new Bar();
		bar.addAttachment("txt1.txt", attachment);

		Response response = dbClient.save(bar);
		
		bar = dbClient.find(Bar.class, response.getId(), new Params().attachments());
		String base64Data = bar.getAttachments().get("txt1.txt").getData();
		assertNotNull(base64Data);
	}

	@Test
	public void attachmentStandalone() throws IOException {
		byte[] bytesToDB = "binary data".getBytes();
		ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
		Response response = dbClient.saveAttachment(bytesIn, "foo.txt", "text/plain");

		InputStream in = dbClient.find(response.getId() + "/foo.txt");
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

	// Delete

	@Test
	public void deleteObject() {
		Response response = dbClient.save(new Foo());
		Foo foo = dbClient.find(Foo.class, response.getId());
		dbClient.remove(foo);
	}

	@Test
	public void deleteByIdAndRevValues() {
		Response response = dbClient.save(new Foo());
		dbClient.remove(response.getId(), response.getRev());
	}

	private static String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
