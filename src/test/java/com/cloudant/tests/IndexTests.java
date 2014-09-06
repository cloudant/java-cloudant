package com.cloudant.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.Replication;

import com.cloudant.CloudantAccount;
import com.cloudant.Database;
import com.cloudant.Index;
import com.cloudant.IndexField;
import com.cloudant.IndexField.SortOrder;

public class IndexTests {

	private static CloudantAccount account;
	private static Database db;
	
	@BeforeClass
	public static void setUpClass() {
		Properties props = CloudantAccountTests.getProperties("cloudant.properties");
		String cloudantaccount = props.getProperty("cloudant.account");
		String userName= props.getProperty("cloudant.username");
		String password = props.getProperty("cloudant.password");
		
		account = new CloudantAccount(cloudantaccount,userName,password);
		
		// create the movies-demo db for our index tests
		Replication r = account.replication();
		r.source("https://examples.cloudant.com/movies-demo");
		r.createTarget(true);
		r.target("https://"+ userName + ":" + password + "@" + cloudantaccount +  ".cloudant.com/movies-demo");
		r.trigger();
		db = account.database("movies-demo", false);
		
	}

	@AfterClass
	public static void tearDownClass() {
		account.deleteDB("movies-demo", "delete database");
		account.shutdown();
	}
	
	
	@Test
	public void indexTestAll() {
		
		db.createIndex("Person_name", "Person_name", null,
				new IndexField[]{
					new IndexField("Person_name",SortOrder.asc),
					new IndexField("Movie_year",SortOrder.asc)});
		db.createIndex("Movie_year", "Movie_year", null,
				new IndexField[]{new IndexField("Movie_year",SortOrder.asc)});
		
		List<Index> indices = db.listIndices();
		assertNotNull(indices);
		assert(indices.size() > 0 );
		for ( Index i : indices ) {
			assertNotNull(i.getName());
			assertNotNull(i.getFields());
			Iterator<IndexField> flds= i.getFields();
			assert(flds.hasNext());
			while ( flds.hasNext() ) {
				IndexField fld = flds.next();
				assertNotNull(fld.getName());
				assertNotNull(fld.getOrder());
			}
			
		}
		
		List<Movie> movies = db.findByIndex("\"selector\": { \"Movie_year\": {\"$gt\": 1960}, \"Person_name\": \"Alec Guinness\" }",
				new IndexField[]{ new IndexField("Movie_year", SortOrder.desc)},
				null, null, 
				new String[]{"Movie_name","Movie_year"},
				null, Movie.class);
		assertNotNull(movies);
		assert(movies.size() > 0);
		for ( Movie m : movies ) {
			assertNotNull(m.getMovie_name());
			assertNotNull(m.getMovie_year());
		}
	
		db.deleteIndex("Person_name", "Person_name");
		db.deleteIndex("Movie_year", "Movie_year");
	}
	
	
	
	
}
