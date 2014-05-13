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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.CouchDbClient;
import org.lightcouch.DocumentConflictException;
import org.lightcouch.NoDocumentException;
import org.lightcouch.Page;
import org.lightcouch.ViewResult;

import com.google.gson.JsonObject;

public class ViewsTest {

	private static CouchDbClient dbClient;

	@BeforeClass
	public static void setUpClass() {
		dbClient = new CouchDbClient();

		dbClient.syncDesignDocsWithDb();
		
		init(); 
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
	}

	@Test
	public void queryView() {
		List<Foo> foos = dbClient.view("example/foo")
				.includeDocs(true)
				.query(Foo.class);
		assertThat(foos.size(), not(0));
	}

	@Test
	public void byKey() {
		List<Foo> foos = dbClient.view("example/foo")
				.includeDocs(true)
				.key("key-1")
				.query(Foo.class);
		assertThat(foos.size(), is(1));
	}

	@Test
	public void byStartAndEndKey() {
		List<Foo> foos = dbClient.view("example/foo")
				.startKey("key-1")
				.endKey("key-2")
				.includeDocs(true)
				.query(Foo.class);
		assertThat(foos.size(), is(2));
	}

	@Test
	public void byComplexKey() {
		int[] complexKey = new int[] { 2011, 10, 15 };
		List<Foo> foos = dbClient.view("example/by_date")
				.key(complexKey)
//				.key(2011, 10, 15) 
				.includeDocs(true)
				.reduce(false)
				.query(Foo.class);
		assertThat(foos.size(), is(2));
	}

	@Test
	public void viewResultEntries() {
		ViewResult<int[], String, Foo> viewResult = dbClient.view("example/by_date")
				.reduce(false)
				.queryView(int[].class, String.class, Foo.class);
		assertThat(viewResult.getRows().size(), is(3));
	}

	@Test
	public void scalarValues() {
		int allTags = dbClient.view("example/by_tag").queryForInt();
		assertThat(allTags, is(4));

		long couchDbTags = dbClient.view("example/by_tag")
				.key("couchdb")
				.queryForLong();
		assertThat(couchDbTags, is(2L));

		String javaTags = dbClient.view("example/by_tag")
				.key("java")
				.queryForString();
		assertThat(javaTags, is("1"));
	}

	@Test(expected = NoDocumentException.class)
	public void viewWithNoResult_throwsNoDocumentException() {
		dbClient.view("example/by_tag")
		.key("javax")
		.queryForInt();
	}

	@Test
	public void groupLevel() {
		ViewResult<int[], Integer, Foo> viewResult = dbClient
				.view("example/by_date")
				.groupLevel(2)
				.queryView(int[].class, Integer.class, Foo.class);
		assertThat(viewResult.getRows().size(), is(2));
	}

	@Test()
	public void tempViews() {
		dbClient.save(new Foo(generateUUID(), "some-val"));
		List<Foo> list = dbClient.view("_temp_view")
				.tempView("temp_1")
				.includeDocs(true)
				.reduce(false)
				.query(Foo.class);
		assertThat(list.size(), not(0));
	}

	@Test
	public void allDocs() {
		dbClient.save(new Foo());
		List<JsonObject> allDocs = dbClient.view("_all_docs")
				.query(JsonObject.class);
		assertThat(allDocs.size(), not(0));
	}

	@Test
	public void pagination() {
		for (int i = 0; i < 7; i++) {
			Foo foo = new Foo(generateUUID(), "some-val");
			dbClient.save(foo);
		}

		final int rowsPerPage = 3;
		// first page - page #1 (rows 1 - 3)
		Page<Foo> page = dbClient.view("example/foo")
				.queryPage(rowsPerPage,	null, Foo.class);
		assertFalse(page.isHasPrevious());
		assertTrue(page.isHasNext());
		assertThat(page.getResultFrom(), is(1));
		assertThat(page.getResultTo(), is(3));
		assertThat(page.getPageNumber(), is(1));
		assertThat(page.getResultList().size(), is(3));

		String param = page.getNextParam();
		// next page - page #2 (rows 4 - 6)
		page = dbClient.view("example/foo").queryPage(rowsPerPage, param, Foo.class);
		assertTrue(page.isHasPrevious());
		assertTrue(page.isHasNext());
		assertThat(page.getResultFrom(), is(4));
		assertThat(page.getResultTo(), is(6));
		assertThat(page.getPageNumber(), is(2));
		assertThat(page.getResultList().size(), is(3));

		param = page.getPreviousParam();
		// previous page, page #1 (rows 1 - 3)
		page = dbClient.view("example/foo").queryPage(rowsPerPage, param, Foo.class);
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
			dbClient.save(foo);

			foo = new Foo("id-2", "key-2");
			foo.setTags(Arrays.asList(new String[] { "java", "couchdb" }));
			foo.setComplexDate(new int[] { 2011, 10, 15 });
			dbClient.save(foo);

			foo = new Foo("id-3", "key-3");
			foo.setComplexDate(new int[] { 2013, 12, 17 });
			dbClient.save(foo);

		} catch (DocumentConflictException e) {
		}
	}
	
	private static String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
