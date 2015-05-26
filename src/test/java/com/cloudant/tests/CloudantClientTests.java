package com.cloudant.tests;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.lightcouch.CouchDbException;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.Membership;
import com.cloudant.client.api.model.Task;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.test.main.RequiresCloudantService;
import com.cloudant.tests.util.Utils;

public class CloudantClientTests {

	private static final Log log = LogFactory.getLog(CloudantClientTests.class);

	public static CloudantClient cookieBasedClient;
	private CloudantClient account;

	@Before
	public  void setUp() {
		account = CloudantClientHelper.getClient();

		String cookie = account.getCookie();
		if(CloudantClientHelper.COUCH_PASSWORD == null){
			cookieBasedClient = account;
		} else {
			cookieBasedClient = new CloudantClient(
					CloudantClientHelper.COUCH_USERNAME, cookie);
		}

	}


	@After
	public void tearDown(){
		account.shutdown();
		cookieBasedClient.shutdown();
	}

	@Test
	@Category(RequiresCloudantService.class)
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
	@Category(RequiresCloudant.class)
	public void membership() {
		Membership mship = account.getMembership();
		assertNotNull(mship);
		assertNotNull(mship.getClusterNodes());
		assertNotNull(mship.getClusterNodes().hasNext());
		assertNotNull(mship.getAllNodes());
		assertNotNull(mship.getAllNodes().hasNext());
	}

	@Test
	@Category(RequiresCloudant.class)
	public void cookieTest() {

		Membership membership = cookieBasedClient.getMembership();
		assertNotNull(membership);
	}

	@Test
	public void cookieNegativeTest() {
		String cookie = account.getCookie() + "XXX";
		boolean exceptionRaised = true;
		try {
			new CloudantClient(
					CloudantClientHelper.SERVER_URI.toString(), cookie).getAllDbs();
			exceptionRaised = false;
		} catch (CouchDbException e) {
			if ( e.getMessage().contains("Forbidden") ) {
			exceptionRaised = true;
			}
		}
		if (exceptionRaised == false) {
			Assert.fail("could connect to cloudant with random AuthSession cookie");
		}
	}

}
