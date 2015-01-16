package com.cloudant.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
//import org.lightcouch.DesignDocument;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.Search;
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.model.SearchResult;
import com.cloudant.client.api.model.SearchResult.SearchResultRows;
import com.cloudant.tests.util.Utils;

public class SearchTests {

	private static final Log log = LogFactory.getLog(SearchTests.class);
	private static CloudantClient account;
	private static Database db;
	
	@BeforeClass
	public static void setUpClass() {
		Properties props = Utils.getProperties("cloudant.properties",log);
		String cloudantaccount = props.getProperty("cloudant.account");
		String userName= props.getProperty("cloudant.username");
		String password = props.getProperty("cloudant.password");
		
		account = new CloudantClient(cloudantaccount,userName,password);
		
		// replciate the animals db for search tests
		com.cloudant.client.api.Replication r = account.replication();
		r.source("https://examples.cloudant.com/animaldb");
		r.createTarget(true);
		r.target("https://"+ userName + ":" + password + "@" + Utils.getHostName(cloudantaccount) + "/animaldb");
		r.trigger();
		db = account.database("animaldb", false);
		
		// sync the design doc for faceted search
		DesignDocument designDoc = db.design().getFromDesk("views101");
		db.design().synchronizeWithDb(designDoc);
	}

	@AfterClass
	public static void tearDownClass() {
		account.deleteDB("animaldb", "delete database");
		account.shutdown();
	}
	
		
	
	@Test
	public void searchCountsTest() {
				
		// do a faceted search for counts
		Search srch = db.search("views101/animals");
		SearchResult<Animal> rslt= srch.limit(10)
				.includeDocs(true)
				.counts(new String[] {"class","diet"})
				.querySearchResult("l*", Animal.class);
		assertNotNull(rslt);
		assertNotNull(rslt.getCounts());
		assert(rslt.getCounts().keySet().size() == 2);
		assertEquals(rslt.getCounts().get("class").keySet().size(), 1);
		assertEquals(rslt.getCounts().get("class").get("mammal"), new Long(2));
		assertEquals(rslt.getCounts().get("diet").keySet().size() , 2);
		assertEquals(rslt.getCounts().get("diet").get("herbivore") , new Long(1));
		assertEquals(rslt.getCounts().get("diet").get("omnivore") , new Long(1));
		assertNotNull(rslt.getBookmark());
		assertEquals(rslt.getGroups().size() , 0);
		assertEquals(rslt.getRows().size() , 2);
		for ( @SuppressWarnings("rawtypes") SearchResultRows r : rslt.getRows() ) {
			assertNotNull(r.getDoc());
			assertNotNull(r.getFields());
			assertNotNull(r.getId());
			assertNotNull(r.getOrder());
		}
	 
	}
	
	@Test
	public void rowsTest() {
		Search srch = db.search("views101/animals");
		List<Animal> animals= srch.limit(10)
				.includeDocs(true)
				.counts(new String[] {"class","diet"})
				.query("l*",  Animal.class);
		assertNotNull(animals);
		assertEquals(animals.size(), 2);
		 for ( Animal a : animals ) {
			 assertNotNull(a);
		 }
	}
	
	@Test
	public void groupsTest() {
		Search srch = db.search("views101/animals");
		Map<String,List<Animal>> groups = srch.limit(10)
				.includeDocs(true)
				.counts(new String[] {"class","diet"})
				.groupField("class", false)
				.queryGroups("l*", Animal.class);
		assertNotNull(groups);
		assertEquals(groups.size(), 1);
		for ( Entry<String, List<Animal>> g : groups.entrySet()) {
			assertNotNull(g.getKey());
			assertEquals(g.getValue().size(),2);
			for ( Animal a : g.getValue() ) {
				assertNotNull(a);
			 }
		}
	}
	
	@Test
	public void rangesTest() {
		// do a faceted search for ranges
		Search srch = db.search("views101/animals");
		SearchResult<Animal> rslt= srch.includeDocs(true)
				.counts(new String[] {"class","diet"})
				.ranges("{ \"min_length\": {\"small\": \"[0 TO 1.0]\","
				  + "\"medium\": \"[1.1 TO 3.0]\", \"large\": \"[3.1 TO 9999999]\"} }")
				.querySearchResult("class:mammal", Animal.class);
		assertNotNull(rslt);
		assertNotNull(rslt.getRanges());
		assertEquals(rslt.getRanges().entrySet().size(),1);
		assertEquals(rslt.getRanges().get("min_length").entrySet().size(),3);
		assertEquals(rslt.getRanges().get("min_length").get("small"), new Long(3));
		assertEquals(rslt.getRanges().get("min_length").get("medium"), new Long(3));
		assertEquals(rslt.getRanges().get("min_length").get("large"), new Long(2));
		
	}
	
	
	@Test
	public void sortTest() {
		// do a faceted search for counts
		Search srch = db.search("views101/animals");
		SearchResult<Animal> rslt= srch.includeDocs(true)
					.sort("[\"diet<string>\"]")
					.querySearchResult("class:mammal", Animal.class);
		assertNotNull(rslt);
		assertEquals(rslt.getRows().get(0).getOrder()[0].toString(), ("carnivore"));
		assertEquals(rslt.getRows().get(1).getOrder()[0].toString(), ("herbivore"));
		assertEquals(rslt.getRows().get(5).getOrder()[0].toString(), ("omnivore"));
	}
	
	@Test
	public void groupSortTest() {
		Search srch = db.search("views101/animals");
		Map<String,List<Animal>> groups = srch.includeDocs(true)
				.groupField("diet", false)
				.groupSort("[\"-diet<string>\"]")
				.queryGroups("l*", Animal.class);
		assertNotNull(groups);
		assertEquals(groups.size(), 2);
		Iterator<String> it = groups.keySet().iterator();
		assertEquals("omnivore", it.next()); // diet in reverse order
		assertEquals("herbivore",it.next());
	}
	
	
	
	@Test
	public void drillDownTest() {
		// do a faceted search for drilldown
				Search srch = db.search("views101/animals");
				SearchResult<Animal> rslt= srch.includeDocs(true)
						.counts(new String[] {"class","diet"})
						.ranges("{ \"min_length\": {\"small\": \"[0 TO 1.0]\","
						  + "\"medium\": \"[1.1 TO 3.0]\", \"large\": \"[3.1 TO 9999999]\"} }")
						 .drillDown("class", "mammals")
						.querySearchResult("class:mammal", Animal.class);
				assertNotNull(rslt);
				assertNotNull(rslt.getRanges());
				assertEquals(rslt.getRanges().entrySet().size(),1);
				assertEquals(rslt.getRanges().get("min_length").entrySet().size(),3);
				assertEquals(rslt.getRanges().get("min_length").get("small"), new Long(0));
				assertEquals(rslt.getRanges().get("min_length").get("medium"), new Long(0));
				assertEquals(rslt.getRanges().get("min_length").get("large"), new Long(0));
	}
	
	
	@Test
	public void bookmarkTest() {
		Search srch = db.search("views101/animals");
		SearchResult<Animal> rslt= srch.limit(4)
				.querySearchResult("class:mammal", Animal.class);
		
		Search srch1 = db.search("views101/animals");
		srch1.bookmark(rslt.getBookmark())
			.querySearchResult("class:mammal", Animal.class);
		
	}

	
	
}
