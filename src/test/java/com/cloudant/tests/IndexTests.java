package com.cloudant.tests;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.FindByIndexOptions;
import com.cloudant.client.api.model.Index;
import com.cloudant.client.api.model.IndexField;
import com.cloudant.client.api.model.IndexField.SortOrder;
import com.cloudant.test.main.RequiresCloudant;
import com.google.gson.GsonBuilder;

public class IndexTests {

	private static final Log log = LogFactory.getLog(IndexTests.class);
	private static Database db;
	private CloudantClient account;
	
	@Before
	public  void setUp() {
		account = CloudantClientHelper.getClient();
		
		// create the movies-demo db for our index tests
		com.cloudant.client.api.Replication r = account.replication();
		r.source("https://examples.cloudant.com/movies-demo");
		r.createTarget(true);
		r.target("https://"+ CloudantClientHelper.SERVER_URI.toString() +"/movies-demo");
		r.trigger();
		db = account.database("movies-demo", false);
		
	}

	@After
	public  void tearDown() {
		account.deleteDB("movies-demo");
		account.shutdown();
	}
	
	
	@Test
	@Category(RequiresCloudant.class)
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
				Movie.class,
				new FindByIndexOptions()
					.sort(new IndexField("Movie_year", SortOrder.desc))
					.fields("Movie_name").fields("Movie_year"));
		assertNotNull(movies);
		assert(movies.size() > 0);
		for ( Movie m : movies ) {
			assertNotNull(m.getMovie_name());
			assertNotNull(m.getMovie_year());
		}
		
		movies = db.findByIndex("\"selector\": { \"Movie_year\": {\"$gt\": 1960}, \"Person_name\": \"Alec Guinness\" }",
				Movie.class,
				new FindByIndexOptions()
					.sort(new IndexField("Movie_year", SortOrder.desc))
					.fields("Movie_name").fields("Movie_year")
					.limit(1)
					.skip(1)
					.readQuorum(2));
		assertNotNull(movies);
		assert(movies.size() == 1);
		for ( Movie m : movies ) {
			assertNotNull(m.getMovie_name());
			assertNotNull(m.getMovie_year());
		}
		
		// selectorJson as a proper json object
		Map<String,Object> year = new HashMap<String,Object>();
		year.put("$gt", new Integer(1960));
		Map<String,Object> selector = new HashMap<String,Object>();
		selector.put("Movie_year", year);
		selector.put("Person_name", "Alec Guinness");
		movies = db.findByIndex(new GsonBuilder().create().toJson(selector),
				Movie.class,
				new FindByIndexOptions()
					.sort(new IndexField("Movie_year", SortOrder.desc))
					.fields("Movie_name").fields("Movie_year")
					.limit(1)
					.skip(1)
					.readQuorum(2));
		assertNotNull(movies);
		assert(movies.size() == 1);
		for ( Movie m : movies ) {
			assertNotNull(m.getMovie_name());
			assertNotNull(m.getMovie_year());
		}
	
		db.deleteIndex("Person_name", "Person_name");
		db.deleteIndex("Movie_year", "Movie_year");
	}
	
	
	
	
}
