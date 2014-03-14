/*
 * Copyright (C) 2011 Ahmed Yehia (ahmed.yehia.m@gmail.com)
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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
import org.lightcouch.Changes;
import org.lightcouch.ChangesResult;
import org.lightcouch.ChangesResult.Row;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbInfo;
import org.lightcouch.DesignDocument;
import org.lightcouch.DocumentConflictException;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Page;
import org.lightcouch.Params;
import org.lightcouch.ReplicationResult;
import org.lightcouch.ReplicationResult.ReplicationHistory;
import org.lightcouch.ReplicatorDocument;
import org.lightcouch.Response;
import org.lightcouch.ViewResult;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * API Integration testing with live databases.
 * 
 * @author Ahmed Yehia
 */
public class CouchDbClientTest {

	private static CouchDbClient dbClient;
	private static CouchDbClient dbClient2;

	@BeforeClass
	public static void setUpClass() {
		dbClient = new CouchDbClient();
		dbClient2 = new CouchDbClient("couchdb-2.properties");
		
		// dbClient = new CouchDbClient("db-name", true, "http", "127.0.0.1", 5984, "username", "secret");
		/*CouchDbProperties properties = new CouchDbProperties()
		 .setDbName("db-name")
		 .setCreateDbIfNotExist(true)
		 .setProtocol("https")
		 .setHost("example.com")
		 .setPort(443)
		 .setUsername("username")
		 .setPassword("secret")
		 .setMaxConnections(100);*/
		//dbClient = new CouchDbClient(properties);
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
		dbClient2.shutdown();
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
	public void findPOJO() {
		Response response = dbClient.save(new Foo());
		Foo foo = dbClient.find(Foo.class, response.getId());
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
		JsonObject jsonObject = dbClient.findAny(JsonObject.class, dbClient.getDBUri().toString());
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
		json.add("an-array", new JsonArray());
		dbClient.save(json);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void saveInvalidObject_throwsIllegalArgumentException() {
		dbClient.save(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveWithRevision_throwsIllegalArgumentException() {
		Bar bar = new Bar();
		bar.setRevision("rev-val");
		dbClient.save(bar);
	}
	
	@Test(expected = DocumentConflictException.class)
	public void saveWitDuplicateId_throwsDocumentConflictException() {
		Foo foo = new Foo(generateUUID());
		dbClient.save(foo); 
		dbClient.save(foo); 
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
	public void updateWithOutdatedRev_throwsDocumentConflictException() {
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

		List<Response> responses = dbClient.bulk(newDocs, true);
		assertThat(responses.size(), is(2));
	}
	
	@Test
	public void bulkGetDocs() {
		Response resp1 = dbClient.save(new Foo());
		Response resp2 = dbClient.save(new Foo());
		List<String> keys = Arrays.asList(new String[] { resp1.getId(), resp2.getId() });
		
		List<Foo> docs = dbClient.view("_all_docs").includeDocs(true).keys(keys)
				.query(Foo.class);
		assertThat(docs.size(), is(2));
	}
	
	// Attachment
	
	@Test
	public void attachmentInline() {
		Attachment attachment = new Attachment();
		attachment.setContentType("text/plain");
		attachment.setData("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=");
		
		Attachment attachment2 = new Attachment();
		attachment2.setContentType("text/plain");
		String data = Base64.encodeBase64String("some text contents".getBytes());
		attachment2.setData(data);

		Bar bar = new Bar(); 
		bar.addAttachment("bar.txt", attachment);
		bar.addAttachment("bar2.txt", attachment2);

		dbClient.save(bar);
	}
	
	@Test
	public void attachmentInline_getWithDocument() {
		Attachment attachment = new Attachment();
		attachment.setContentType("text/plain");
		attachment.setData("VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=");

		Bar bar = new Bar(); 
		bar.addAttachment("bar.txt", attachment);
		
		Response response = dbClient.save(bar);
				
		bar = dbClient.find(Bar.class, response.getId(), new Params().attachments());
		String base64Data = bar.getAttachments().get("bar.txt").getData();
		assertNotNull(base64Data);
	}

	@Test
	public void attachmentStandalone() throws IOException {
		byte[] bytesToDB = "some text contents".getBytes();
		ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesToDB);
		Response response = dbClient.saveAttachment(bytesIn, "foo.txt", "text/plain");

		String attachmentId = response.getId() + "/foo.txt";
		InputStream in = dbClient.find(attachmentId);
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
	public void deleteByIdAndRev() {
		Response response = dbClient.save(new Foo());
		dbClient.remove(response.getId(), response.getRev());
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteObjectWithInvalidValues_throwsIllegalArgumentException() {
		Bar bar = new Bar();
		bar.setId("doc-id");
		bar.setRevision(null);
		
		dbClient.remove(bar);
	}

	// Views

	private void initDataForViews() {
		try {
			Foo foo = null;

			foo = new Foo("foo-id-1", "foo-key-1");
			foo.setTags(Arrays.asList(new String[] { "couchdb", "views" }));
			foo.setComplexDate(new int[] { 2011, 10, 15 });
			dbClient.save(foo);

			foo = new Foo("foo-id-2", "foo-key-2");
			foo.setTags(Arrays.asList(new String[] { "java", "couchdb" }));
			foo.setComplexDate(new int[] { 2011, 10, 15 });
			dbClient.save(foo);

			foo = new Foo("foo-id-3", "foo-key-3");
			foo.setComplexDate(new int[] { 2013, 12, 17 });
			dbClient.save(foo);

			DesignDocument exampleDoc = dbClient.design().getFromDesk("example");
			dbClient.design().synchronizeWithDb(exampleDoc);

		} catch (DocumentConflictException e) {
		}
	}

	@Test()
	public void views() {
		initDataForViews();
		
		List<Foo> foos = dbClient.view("example/foo").includeDocs(true).query(Foo.class);
		assertThat(foos.size(), not(0));
	}
	
	@Test()
	public void views_byKey() {
		initDataForViews();
		
		List<Foo> foos = dbClient.view("example/foo").includeDocs(true)
				.key("foo-key-1").query(Foo.class);
		assertThat(foos.size(), is(1));
	}
	
	@Test()
	public void views_byStartAndEndKey() {
		initDataForViews();
		
		List<Foo> foos = dbClient.view("example/foo").startKey("foo-key-1").endKey("foo-key-2").includeDocs(true).query(Foo.class);
		assertThat(foos.size(), is(2));
	}

	@Test()
	public void views_byComplexKey() {
		initDataForViews();

		int[] complexKey = new int[] { 2011, 10, 15 };
		List<Foo> foos = dbClient.view("example/by_date").key(complexKey).includeDocs(true).reduce(false).query(Foo.class);
		assertThat(foos.size(), is(2));
	}
	
	@Test()
	public void views_ViewResult() {
		initDataForViews();

		ViewResult<int[], String, Foo> viewResult = dbClient.view("example/by_date")
				.reduce(false).queryView(int[].class, String.class, Foo.class);
		
		assertThat(viewResult.getRows().size(), is(3));
	}

	@Test()
	public void views_scalarValues() {
		initDataForViews();

		int allTags = dbClient.view("example/by_tag").queryForInt();
		assertThat(allTags, is(4)); 

		long couchDbTags = dbClient.view("example/by_tag").key("couchdb").queryForLong();
		assertThat(couchDbTags, is(2L));

		String javaTags = dbClient.view("example/by_tag").key("java").queryForString();
		assertThat(javaTags, is("1")); 
	}

	@Test(expected = NoDocumentException.class)
	public void viewsWithNoResult_throwsNoDocumentException() {
		initDataForViews();
		dbClient.view("example/by_tag").key("javax").queryForInt();
	}

	@Test
	public void views_ByGroupLevel() {
		ViewResult<int[], Integer, Foo> viewResult = dbClient.view("example/by_date")
				.groupLevel(2).queryView(int[].class, Integer.class, Foo.class);
		
		assertThat(viewResult.getRows().size(), is(2));
	}

	@Test()
	public void tempViews() {
		dbClient.save(new Foo(generateUUID(), "title"));
		List<Foo> list = dbClient.view("_temp_view").tempView("temp_1").includeDocs(true)
				.reduce(false).query(Foo.class);
		assertThat(list.size(), not(0));
	}

	@Test
	public void allDocs() {
		dbClient.save(new Foo());
		List<JsonObject> allDocs = dbClient.view("_all_docs").query(JsonObject.class);
		assertThat(allDocs.size(), not(0));
	}
	
	@Test
	public void testPagination() {
		dbClient.design().synchronizeAllWithDb();

		for (int i = 0; i < 7; i++) {
			dbClient.save(new Foo(generateUUID(), "title"));
		}

		final int rowsPerPage = 3;
		// first page, page #1, rows: 1 - 3
		Page<Foo> page = dbClient.view("example/foo").queryPage(rowsPerPage, null,
				Foo.class);
		assertFalse(page.isHasPrevious());
		assertTrue(page.isHasNext());
		assertThat(page.getResultFrom(), is(1));
		assertThat(page.getResultTo(), is(3));
		assertThat(page.getPageNumber(), is(1));
		assertThat(page.getResultList().size(), is(3));

		String param = page.getNextParam();
		// next page, page #2, rows: 4 - 6
		page = dbClient.view("example/foo").queryPage(rowsPerPage, param, Foo.class);
		assertTrue(page.isHasPrevious());
		assertTrue(page.isHasNext());
		assertThat(page.getResultFrom(), is(4));
		assertThat(page.getResultTo(), is(6));
		assertThat(page.getPageNumber(), is(2));
		assertThat(page.getResultList().size(), is(3));

		param = page.getPreviousParam();
		// previous page, page #1, rows: 1 - 3
		page = dbClient.view("example/foo").queryPage(rowsPerPage, param, Foo.class);
		assertFalse(page.isHasPrevious());
		assertTrue(page.isHasNext());
		assertThat(page.getResultFrom(), is(1));
		assertThat(page.getResultTo(), is(3));
		assertThat(page.getPageNumber(), is(1));
		assertThat(page.getResultList().size(), is(3));
	}

	// Update handler
	
	@Test
	public void updateHandler() {
		dbClient.syncDesignDocsWithDb();

		Foo foo = new Foo();
		foo.setTitle("title");
		Response response = dbClient.save(foo);

		String newTitle = "new title value";
		String query = String.format("field=title&value=%s", newTitle);
		String output = dbClient.invokeUpdateHandler("example/example_update", response.getId(), query);

		foo = dbClient.find(Foo.class, response.getId());
		assertNotNull(output);
		assertEquals(foo.getTitle(), newTitle);
	}
	
	// Replication

	@Test
	public void replication() {
		ReplicationResult result = dbClient.replication()
				.createTarget(true)
				.source(dbClient.getDBUri().toString())
				.target(dbClient2.getDBUri().toString())
				.trigger();

		List<ReplicationHistory> histories = result.getHistories();
		assertThat(histories.size(), not(0));
	}

	@Test
	public void replication_filteredWithQueryParams() {
		Foo foo = new Foo();
		foo.setTitle("somekey1");
		dbClient.save(foo);
		
    	Map<String, Object> queryParams = new HashMap<String, Object>();
    	queryParams.put("somekey1", "value 1");
    	
		dbClient.replication()
				.createTarget(true)
				.source(dbClient.getDBUri().toString())
				.target(dbClient2.getDBUri().toString())
				.filter("example/example_filter")
				.queryParams(queryParams)
				.trigger();
	}

	@Test
	public void replication_conflict() {
		DesignDocument conflictsDoc = dbClient.design().getFromDesk("conflicts");
		dbClient2.design().synchronizeWithDb(conflictsDoc);

		String docId = generateUUID();
		Foo foodb1 = new Foo(docId, "title");
		Foo foodb2 = null;
		
		foodb1 = new Foo(docId, "titleX");
		
		dbClient.save(foodb1); 
		
		dbClient.replication().source(dbClient.getDBUri().toString())
				.target(dbClient2.getDBUri().toString()).trigger();

		foodb2 = dbClient2.find(Foo.class, docId); 
		foodb2.setTitle("titleY"); 
		dbClient2.update(foodb2); 

		foodb1 = dbClient.find(Foo.class, docId); 
		foodb1.setTitle("titleZ"); 
		dbClient.update(foodb1); 

		dbClient.replication().source(dbClient.getDBUri().toString())
				.target(dbClient2.getDBUri().toString()).trigger();

		ViewResult<String[], String, Foo> conflicts = dbClient2.view("conflicts/conflict")
				.includeDocs(true).queryView(String[].class, String.class, Foo.class);
		
		assertThat(conflicts.getRows().size(), is(not(0))); 
	}

	@Test
	public void replicatorDB() throws InterruptedException {
		String version = dbClient.context().serverVersion();
		if (version.startsWith("0") || version.startsWith("1.0")) {
			return; 
		}

		Response response = dbClient.replicator()
				.source(dbClient.getDBUri().toString())
				.target(dbClient2.getDBUri().toString()).continuous(true)
				.createTarget(true)
				.save();

		List<ReplicatorDocument> replicatorDocs = dbClient.replicator()
			.findAll();
		assertThat(replicatorDocs.size(), is(not(0))); 
		
		ReplicatorDocument replicatorDoc = dbClient.replicator()
				.replicatorDocId(response.getId())
				.find();

		dbClient.replicator()
				.replicatorDocId(replicatorDoc.getId())
				.replicatorDocRev(replicatorDoc.getRevision())
				.remove();
	}

	// Database
	
	@Test
	public void dbInfo() {
		CouchDbInfo dbInfo = dbClient.context().info();
		assertNotNull(dbInfo);
	}

	@Test
	public void serverVersion() {
		String version = dbClient.context().serverVersion();
		assertNotNull(version);
	}

	@Test
	public void compactDb() {
		dbClient.context().compact();
	}

	@Test
	public void allDBs() {
		List<String> allDbs = dbClient.context().getAllDbs();
		assertThat(allDbs.size(), is(not(0)));
	}

	@Test
	public void ensureFullCommit() {
		dbClient.context().ensureFullCommit();
	}

	//  Design documents
	
	@Test
	public void designDocs() {
		dbClient.design().synchronizeAllWithDb();

		DesignDocument designDoc = dbClient.design().getFromDesk("example");
		DesignDocument designDoc1 = dbClient.design().getFromDesk("example");
		assertTrue(designDoc.equals(designDoc1)); 

		DesignDocument designDocFromDb = dbClient.design().getFromDb("_design/example");
		assertTrue(designDoc.equals(designDocFromDb)); 

		List<DesignDocument> designDocs = dbClient.design().getAllFromDesk();
		assertThat(designDocs.size(), not(0));
	}

	// Changes
	
	@SuppressWarnings("unused")
	@Test
	public void changes_normalFeed() {
		dbClient.save(new Foo()); 

		ChangesResult changes = dbClient.changes()
				.includeDocs(true)
				.limit(1)
				.getChanges();
		
		List<ChangesResult.Row> rows = changes.getResults();
		
		for (Row row : rows) {
			List<ChangesResult.Row.Rev> revs = row.getChanges();
			String docId = row.getId();
			JsonObject doc = row.getDoc();
			assertNotNull(doc);
		}
		
		assertThat(rows.size(), is(1));
	}

	@Test
	public void changes_continuousFeed() {
		dbClient.save(new Foo()); 

		CouchDbInfo dbInfo = dbClient.context().info();
		String since = dbInfo.getUpdateSeq();

		Changes changes = dbClient.changes()
				.includeDocs(true)
				.since(since)
				.heartBeat(30000)
				.continuousChanges();

		Response response = dbClient.save(new Foo());

		while (changes.hasNext()) {
			ChangesResult.Row feed = changes.next();
			String docId = feed.getId();
			assertEquals(response.getId(), docId);
			changes.stop();
		}
	}
	
	// util

	private static String generateUUID() { 
		return UUID.randomUUID().toString().replace("-", "");
	}
}
