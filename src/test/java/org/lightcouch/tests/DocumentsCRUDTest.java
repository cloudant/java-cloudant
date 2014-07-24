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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.CouchDbClient;
import org.lightcouch.DocumentConflictException;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Params;
import org.lightcouch.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class DocumentsCRUDTest {

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
	public void findByIdContainSlash() {
		Response response = dbClient.save(new Foo(generateUUID() + "/" + generateUUID()));
		Foo foo = dbClient.find(Foo.class, response.getId());
		assertNotNull(foo);

		Foo foo2 = dbClient.find(Foo.class, response.getId(), response.getRev());
		assertNotNull(foo2);
	}

	@Test
	public void findJsonObject() {
		Response response = dbClient.save(new Foo());
		JsonObject jsonObject = dbClient.find(JsonObject.class, response.getId());
		assertNotNull(jsonObject);
	}

	@Test
	public void findAny() {
		String uri = dbClient.getBaseUri() + "_stats";
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
		Foo foo = dbClient.find(Foo.class, response.getId(), new Params().revsInfo());
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
		Response response = dbClient.save(new Foo());
		boolean found = dbClient.contains(response.getId());
		assertTrue(found);
		
		found = dbClient.contains(generateUUID());
		assertFalse(found);
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
	
	@Test
	public void saveWithIdContainSlash() {
		String idWithSlash = "a/b/" + generateUUID();
		Response response = dbClient.save(new Foo(idWithSlash));
		assertEquals(idWithSlash, response.getId());
	}
	
	@Test
	public void saveObjectPost() {
		// database generated id will be assigned
		Response response = dbClient.post(new Foo());
		assertNotNull(response.getId());
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveInvalidObject_throwsIllegalArgumentException() {
		dbClient.save(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveNewDocWithRevision_throwsIllegalArgumentException() {
		Bar bar = new Bar();
		bar.setRevision("unkown");
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
	
	@Test
	public void updateWithIdContainSlash() {
		String idWithSlash = "a/" + generateUUID();
		Response response = dbClient.save(new Bar(idWithSlash));
		
		Bar bar = dbClient.find(Bar.class, response.getId());
		Response responseUpdate = dbClient.update(bar);
		assertEquals(idWithSlash, responseUpdate.getId());
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
	
	@Test
	public void deleteByIdContainSlash() {
		String idWithSlash = "a/" + generateUUID();
		Response response = dbClient.save(new Bar(idWithSlash));
		
		Response responseRemove = dbClient.remove(response.getId(), response.getRev());
		assertEquals(idWithSlash, responseRemove.getId());
	}

	// Helper
	
	private static String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
