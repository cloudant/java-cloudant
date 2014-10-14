package com.cloudant.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Params;
import com.cloudant.client.api.model.Response;
import com.cloudant.tests.util.Utils;

public class UpdateHandlerTest {
	
	private static final Log log = LogFactory.getLog(UpdateHandlerTest.class);
	private static Properties props ;

	private static CloudantClient dbClient;
	private static Database db;
	

	@BeforeClass
	public static void setUpClass() {
		props = Utils.getProperties("cloudant.properties",log);
		dbClient = new CloudantClient(props.getProperty("cloudant.account"),
									  props.getProperty("cloudant.username"),
									  props.getProperty("cloudant.password"));
		db = dbClient.database("lightcouch-db-test", true);
		db.syncDesignDocsWithDb();
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
	}

	@Test
	public void updateHandler_queryString() {
		final String oldValue = "foo";
		final String newValue = "foo bar";
		
		Response response = db.save(new Foo(null, oldValue));
		
		String query = "field=title&value=" + newValue;
		
		String output = db.invokeUpdateHandler("example/example_update", response.getId(), query);
		
		// retrieve from db to verify
		Foo foo = db.find(Foo.class, response.getId());
		
		assertNotNull(output);
		assertEquals(foo.getTitle(), newValue);
	}
	
	@Test
	public void updateHandler_queryParams() {
		final String oldValue = "foo";
		final String newValue = "foo bar";
		
		Response response = db.save(new Foo(null, oldValue));

		Params params = new Params()
					.addParam("field", "title")
					.addParam("value", newValue);
		String output = db.invokeUpdateHandler("example/example_update", response.getId(), params);
		
		// retrieve from db to verify
		Foo foo = db.find(Foo.class, response.getId());
		
		assertNotNull(output);
		assertEquals(foo.getTitle(), newValue);
	}
}
