package com.cloudant.tests;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudant.ApiKey;
import com.cloudant.CloudantClient;
import com.cloudant.Membership;
import com.cloudant.Task;


public class CloudantClientTests {

	private static final Log log = LogFactory.getLog(CloudantClientTests.class);
	private static CloudantClient account;
	
	

	@BeforeClass
	public static void setUpClass() {
		Properties props = getProperties("cloudant.properties");
		account = new CloudantClient(props.getProperty("cloudant.account"),
									  props.getProperty("cloudant.username"),
									  props.getProperty("cloudant.password"));
		
	}

	@AfterClass
	public static void tearDownClass() {
		account.shutdown();
	}
	
	
	@Test
	public void apiKey() {
		ApiKey key = account.generateApiKey();
		assertNotNull(key);
		assertNotNull(key.getKey());
		assertNotNull(key.getPassword());
	}
	
	@Test
	public void activeTasks() {
		List<Task> tasks = account.getActiveTasks();
		assertNotNull(tasks);
	}
	
	@Test
	public void membership() {
		Membership mship = account.getMembership();
		assertNotNull(mship);
		assertNotNull(mship.getClusterNodes());
		assertNotNull(mship.getClusterNodes().hasNext());
		assertNotNull(mship.getAllNodes());
		assertNotNull(mship.getAllNodes().hasNext());
	}
	
	public static Properties getProperties(String configFile) {
		Properties properties = new Properties();
		try {
			InputStream instream = CloudantClient.class.getClassLoader().getResourceAsStream(configFile);
			properties.load(instream);
		} catch (Exception e) {
			String msg = "Could not read configuration file from the classpath: " + configFile;
			log.error(msg);
			throw new IllegalStateException(msg, e);
		}
		return properties;
	
	}
}
