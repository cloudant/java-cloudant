package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.tests.util.Utils;

public class DesignDocumentsTest {
	private static final Log log = LogFactory.getLog(DesignDocumentsTest.class);
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
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
	}

	@Test
	public void designDocSync() {
		DesignDocument designDoc = db.design().getFromDesk("example");
		db.design().synchronizeWithDb(designDoc);
	}
	
	@Test
	public void designDocCompare() {
		DesignDocument designDoc1 = db.design().getFromDesk("example");
		db.design().synchronizeWithDb(designDoc1);
		
		DesignDocument designDoc11 = db.design().getFromDb("_design/example");
		
		assertEquals(designDoc1, designDoc11);
	}
	
	@Test
	public void designDocs() {
		List<DesignDocument> designDocs = db.design().getAllFromDesk();
		db.syncDesignDocsWithDb();
		
		assertThat(designDocs.size(), not(0));
	}

}
