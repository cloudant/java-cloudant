package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.DocumentConflictException;
import org.lightcouch.NoDocumentException;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Page;
import com.cloudant.client.api.model.ViewResult;
import com.cloudant.tests.util.Utils;
import com.google.gson.JsonObject;

public class ViewsTest {

	private static final Log log = LogFactory.getLog(ViewsTest.class);
	private static Properties props ;
	
	private static CloudantClient dbClient;
	private static Database db;
	

	@BeforeClass
	public static void setUpClass() {
		props = Utils.getProperties("cloudant.properties",log);
		dbClient = new CloudantClient(props.getProperty("cloudant.account"),
									  props.getProperty("cloudant.username"),
									  props.getProperty("cloudant.password"));
		//dbClient = new CouchDbClient();
		db = dbClient.database("lightcouch-db-test", true);

		db.syncDesignDocsWithDb();
		
		init(); 
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
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
