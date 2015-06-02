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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lightcouch.DocumentConflictException;
import org.lightcouch.NoDocumentException;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class DocumentsCRUDTest {

	
	private static Database db;
	private CloudantClient account;
	

	@Before
	public  void setUp() {
		account = CloudantClientHelper.getClient();
		db = account.database("lightcouch-db-test", true);
	}

	@After
	public void tearDown(){
		account.deleteDB("lightcouch-db-test");
		account.shutdown();
	}


	// Find

	@Test
	public void findById() {
		Response response = db.save(new Foo());
		Foo foo = db.find(Foo.class, response.getId());
		assertNotNull(foo);
	}
	
	@Test
	public void findByIdAndRev() {
		Response response = db.save(new Foo());
		Foo foo = db.find(Foo.class, response.getId(), response.getRev());
		assertNotNull(foo);
	}
	
	@Test
	public void findByIdContainSlash() {
		Response response = db.save(new Foo(generateUUID() + "/" + generateUUID()));
		Foo foo = db.find(Foo.class, response.getId());
		assertNotNull(foo);

		Foo foo2 = db.find(Foo.class, response.getId(), response.getRev());
		assertNotNull(foo2);
	}

	@Test
	public void findJsonObject() {
		Response response = db.save(new Foo());
		JsonObject jsonObject = db.find(JsonObject.class, response.getId());
		assertNotNull(jsonObject);
	}

	/* not supported in cloudant
	 * @Test
	public void findAny() {
		String uri = dbClient.getBaseUri() + "_stats";
		JsonObject jsonObject = db.findAny(JsonObject.class, uri);
		assertNotNull(jsonObject);
	}*/

	@Test
	public void findInputstream() throws IOException {
		Response response = db.save(new Foo());
		InputStream inputStream = db.find(response.getId());
		assertTrue(inputStream.read() != -1);
		inputStream.close();
	}

	@Test
	public void findWithParams() {
		Response response = db.save(new Foo());
		Foo foo = db.find(Foo.class, response.getId(), new Params().revsInfo());
		assertNotNull(foo);
	}

	@Test(expected = IllegalArgumentException.class)
	public void findWithInvalidId_throwsIllegalArgumentException() {
		db.find(Foo.class, "");
	}

	@Test(expected = NoDocumentException.class)
	public void findWithUnknownId_throwsNoDocumentException() {
		db.find(Foo.class, generateUUID());
	}

	@Test
	public void contains() {
		Response response = db.save(new Foo());
		boolean found = db.contains(response.getId());
		assertTrue(found);
		
		found = db.contains(generateUUID());
		assertFalse(found);
	}

	// Save

	@Test
	public void savePOJO() {
		db.save(new Foo());
	}

	@Test
	public void saveMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("_id", generateUUID());
		map.put("field1", "value1");
		db.save(map);
	}

	@Test
	public void saveJsonObject() {
		JsonObject json = new JsonObject();
		json.addProperty("_id", generateUUID());
		json.add("json-array", new JsonArray());
		db.save(json);
	}
	
	@Test
	public void saveWithIdContainSlash() {
		String idWithSlash = "a/b/" + generateUUID();
		Response response = db.save(new Foo(idWithSlash));
		assertEquals(idWithSlash, response.getId());
	}
	
	@Test
	public void saveObjectPost() {
		// database generated id will be assigned
		Response response = db.post(new Foo());
		assertNotNull(response.getId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveInvalidObject_throwsIllegalArgumentException() {
		db.save(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveNewDocWithRevision_throwsIllegalArgumentException() {
		Bar bar = new Bar();
		bar.setRevision("unkown");
		db.save(bar);
	}

	@Test(expected = DocumentConflictException.class)
	public void saveDocWithDuplicateId_throwsDocumentConflictException() {
		String id = generateUUID();
		db.save(new Foo(id));
		db.save(new Foo(id));
	}

	@Test
	public void batch() {
		db.batch(new Foo());
	}

	// Update

	@Test
	public void update() {
		Response response = db.save(new Foo());
		Foo foo = db.find(Foo.class, response.getId());
		db.update(foo);
	}
	
	@Test
	public void updateWithIdContainSlash() {
		String idWithSlash = "a/" + generateUUID();
		Response response = db.save(new Bar(idWithSlash));
		
		Bar bar = db.find(Bar.class, response.getId());
		Response responseUpdate = db.update(bar);
		assertEquals(idWithSlash, responseUpdate.getId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void updateWithoutIdAndRev_throwsIllegalArgumentException() {
		db.update(new Foo());
	}

	@Test(expected = DocumentConflictException.class)
	public void updateWithInvalidRev_throwsDocumentConflictException() {
		Response response = db.save(new Foo());
		Foo foo = db.find(Foo.class, response.getId());
		db.update(foo);
		db.update(foo);
	}

	// Delete

	@Test
	public void deleteObject() {
		Response response = db.save(new Foo());
		Foo foo = db.find(Foo.class, response.getId());
		db.remove(foo);
	}

	@Test
	public void deleteByIdAndRevValues() {
		Response response = db.save(new Foo());
		db.remove(response.getId(), response.getRev());
	}
	
	@Test
	public void deleteByIdContainSlash() {
		String idWithSlash = "a/" + generateUUID();
		Response response = db.save(new Bar(idWithSlash));
		
		Response responseRemove = db.remove(response.getId(), response.getRev());
		assertEquals(idWithSlash, responseRemove.getId());
	}

	// Helper
	
	private static String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
