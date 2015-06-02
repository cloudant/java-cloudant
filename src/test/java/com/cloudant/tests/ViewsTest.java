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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lightcouch.DocumentConflictException;
import org.lightcouch.NoDocumentException;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Page;
import com.cloudant.client.api.model.ViewResult;
import com.google.gson.JsonObject;

public class ViewsTest {

	
	private static Database db;
	private CloudantClient account;
	

	@Before
	public  void setUp() {
		account = CloudantClientHelper.getClient();

		db = account.database("lightcouch-db-test", true);

		db.syncDesignDocsWithDb();
		
		init(); 
	}

	@After
	public void tearDown(){
		account.deleteDB("lightcouch-db-test");
		account.shutdown();
	}


	@Test
	public void queryView() {
		List<Foo> foos = db.view("example/foo")
				.includeDocs(true)
				.query(Foo.class);
		assertThat(foos.size(), not(0));
	}

	@Test
	public void byKey() {
		List<Foo> foos = db.view("example/foo")
				.includeDocs(true)
				.key("key-1")
				.query(Foo.class);
		assertThat(foos.size(), is(1));
	}

	@Test
	public void byKeys() {
		List<Foo> foos = db.view("example/foo")
				.includeDocs(true)
				.keys(new Object[] {"key-1", "key-2"})
				.query(Foo.class);
		assertThat(foos.size(), is(2));
	}
	
	@Test
	public void byStartAndEndKey() {
		List<Foo> foos = db.view("example/foo")
				.startKey("key-1")
				.endKey("key-2")
				.includeDocs(true)
				.query(Foo.class);
		assertThat(foos.size(), is(2));
	}

	@Test
	public void byComplexKey() {
		int[] complexKey = new int[] { 2011, 10, 15 };
		List<Foo> foos = db.view("example/by_date")
				.key(complexKey)
//				.key(2011, 10, 15) 
				.includeDocs(true)
				.reduce(false)
				.query(Foo.class);
		assertThat(foos.size(), is(2));
	}

	@Test
	public void byComplexKeys() {
		int[] complexKey1 = new int[] { 2011, 10, 15 };
		int[] complexKey2 = new int[] { 2013, 12, 17 };
		List<Foo> foos = db.view("example/by_date")
				.keys(new Object[] {complexKey1, complexKey2})
				.includeDocs(true)
				.reduce(false)
				.query(Foo.class);
		assertThat(foos.size(), is(3));
	}
	@Test
	public void viewResultEntries() {
		ViewResult<int[], String, Foo> viewResult = db.view("example/by_date")
				.reduce(false)
				.queryView(int[].class, String.class, Foo.class);
		assertThat(viewResult.getRows().size(), is(3));
	}

	@Test
	public void scalarValues() {
		int allTags = db.view("example/by_tag").queryForInt();
		assertThat(allTags, is(4));

		long couchDbTags = db.view("example/by_tag")
				.key("couchdb")
				.queryForLong();
		assertThat(couchDbTags, is(2L));

		String javaTags = db.view("example/by_tag")
				.key("java")
				.queryForString();
		assertThat(javaTags, is("1"));
	}

	@Test(expected = NoDocumentException.class)
	public void viewWithNoResult_throwsNoDocumentException() {
		db.view("example/by_tag")
		.key("javax")
		.queryForInt();
	}

	@Test
	public void groupLevel() {
		ViewResult<int[], Integer, Foo> viewResult = db
				.view("example/by_date")
				.groupLevel(2)
				.queryView(int[].class, Integer.class, Foo.class);
		assertThat(viewResult.getRows().size(), is(2));
	}

	@Test
	public void allDocs() {
		db.save(new Foo());
		List<JsonObject> allDocs = db.view("_all_docs")
				.query(JsonObject.class);
		assertThat(allDocs.size(), not(0));
	}

	@Test
	public void pagination() {
		for (int i = 0; i < 7; i++) {
			Foo foo = new Foo(generateUUID(), "some-val");
			db.save(foo);
		}

		final int rowsPerPage = 3;
		// first page - page #1 (rows 1 - 3)
		Page<Foo> page = db.view("example/foo")
				.queryPage(rowsPerPage,	null, Foo.class);
		assertFalse(page.isHasPrevious());
		assertTrue(page.isHasNext());
		assertThat(page.getResultFrom(), is(1));
		assertThat(page.getResultTo(), is(3));
		assertThat(page.getPageNumber(), is(1));
		assertThat(page.getResultList().size(), is(3));

		String param = page.getNextParam();
		// next page - page #2 (rows 4 - 6)
		page = db.view("example/foo").queryPage(rowsPerPage, param, Foo.class);
		assertTrue(page.isHasPrevious());
		assertTrue(page.isHasNext());
		assertThat(page.getResultFrom(), is(4));
		assertThat(page.getResultTo(), is(6));
		assertThat(page.getPageNumber(), is(2));
		assertThat(page.getResultList().size(), is(3));

		param = page.getPreviousParam();
		// previous page, page #1 (rows 1 - 3)
		page = db.view("example/foo").queryPage(rowsPerPage, param, Foo.class);
		assertFalse(page.isHasPrevious());
		assertTrue(page.isHasNext());
		assertThat(page.getResultFrom(), is(1));
		assertThat(page.getResultTo(), is(3));
		assertThat(page.getPageNumber(), is(1));
		assertThat(page.getResultList().size(), is(3));
	}

	private static void init() {
		try {
			Foo foo = null;

			foo = new Foo("id-1", "key-1");
			foo.setTags(Arrays.asList(new String[] { "couchdb", "views" }));
			foo.setComplexDate(new int[] { 2011, 10, 15 });
			db.save(foo);

			foo = new Foo("id-2", "key-2");
			foo.setTags(Arrays.asList(new String[] { "java", "couchdb" }));
			foo.setComplexDate(new int[] { 2011, 10, 15 });
			db.save(foo);

			foo = new Foo("id-3", "key-3");
			foo.setComplexDate(new int[] { 2013, 12, 17 });
			db.save(foo);

		} catch (DocumentConflictException e) {
		}
	}
	
	private static String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
