package com.cloudant.tests;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.Membership;
import com.cloudant.client.api.model.Task;
import com.cloudant.tests.util.Utils;

public class CloudantClientTests {

	private static final Log log = LogFactory.getLog(CloudantClientTests.class);
	private static CloudantClient account;
	public static CloudantClient cookieBasedClient;
	private static Properties props;

	@BeforeClass
	public static void setUpClass() {
		props = Utils.getProperties("cloudant.properties", log);
		account = new CloudantClient(props.getProperty("cloudant.account"),
				props.getProperty("cloudant.username"),
				props.getProperty("cloudant.password"));
		String cookie = account.getCookie();
		cookieBasedClient = new CloudantClient(
				props.getProperty("cloudant.account"), cookie);

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

	@Test
	public void cookieTest() {
		Membership membership = cookieBasedClient.getMembership();
		assertNotNull(membership);
	}

	@Test
	public void cookieAPITest() {
		boolean exceptionRaised = false;
		try{
			cookieBasedClient.generateApiKey();
			exceptionRaised = false ;
		}catch(Exception e){
			exceptionRaised = true ;
		}
		if(exceptionRaised == false){
			Assert.fail("Need to connect from cloudant using UserName & Password");
		}

	}

	@Test
	public void cookieNegativeTest() {
		String cookie = account.getCookie() + "XXX";
		boolean exceptionRaised = false;
		try {
			cookieBasedClient = new CloudantClient(
					props.getProperty("cloudant.account"), cookie);
			exceptionRaised = false;
		} catch (Exception e) {
			exceptionRaised = true;
		}
		if (exceptionRaised == false) {
			Assert.fail("could not connect to cloudant. Un-recognized cookie");
		}
	}

}
