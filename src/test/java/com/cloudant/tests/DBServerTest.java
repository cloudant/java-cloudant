
package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.DbInfo;
import com.cloudant.tests.util.Utils;

public class DBServerTest {

	private static final Log log = LogFactory.getLog(DBServerTest.class);
	private static CloudantClient dbClient;
	private static Properties props ;
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
	public void dbInfo() {
		DbInfo dbInfo = db.info();
		assertNotNull(dbInfo);
	}

	@Test
	public void serverVersion() {
		String version = dbClient.serverVersion();
		assertNotNull(version);
	}

	@Test
	public void allDBs() {
		List<String> allDbs = dbClient.getAllDbs();
		assertThat(allDbs.size(), is(not(0)));
	}

	@Test
	public void ensureFullCommit() {
		db.ensureFullCommit();
	}

	@Test
	public void uuids() {
		List<String> uuids = dbClient.uuids(10);
		assertThat(uuids.size(), is(10));
	}
}
