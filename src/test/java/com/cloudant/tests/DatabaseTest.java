package com.cloudant.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Permissions;
import com.cloudant.client.api.model.ApiKey;
import com.cloudant.client.api.model.Shard;
import com.cloudant.tests.util.Utils;

public class DatabaseTest {
	private static final Log log = LogFactory.getLog(DatabaseTest.class);
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
		r.target("https://"+ userName + ":" + password + "@" + cloudantaccount +  ".cloudant.com/animaldb");
		r.trigger();
		db = account.database("animaldb", false);
	}

	@AfterClass
	public static void tearDownClass() {
		account.deleteDB("animaldb", "delete database");
		account.shutdown();
	}
	
	
	@Test
	public void permissions() {
		Map<String,EnumSet<Permissions>> userPerms = db.getPermissions();
		assertNotNull(userPerms);
		ApiKey key = account.generateApiKey();
		EnumSet<Permissions> p = EnumSet.<Permissions>of( Permissions._reader, Permissions._writer);
		db.setPermissions(key.getKey(), p);
		userPerms = db.getPermissions();
		assertNotNull(userPerms);
		assertEquals(userPerms.size(), 1);
		assertEquals(userPerms.get(key.getKey()), p);
		
		p = EnumSet.noneOf( Permissions.class);
		db.setPermissions(key.getKey(), p);
		userPerms = db.getPermissions();
		assertNotNull(userPerms);
		assertEquals(userPerms.size(), 1);
		assertEquals(userPerms.get(key.getKey()), p);
		
		
	}
	
	@Test
	public void shards() {
		List<Shard> shards = db.getShards();
		assert(shards.size() > 0);
		for (Shard s : shards) { 
			assertNotNull(s.getRange());
			assertNotNull(s.getNodes());
			assertNotNull(s.getNodes().hasNext());
		}
	}
	
	@Test
	public void shard() {
		Shard s = db.getShard("snipe");
		assertNotNull(s);
		assertNotNull(s.getRange());
		assertNotNull(s.getNodes());
		assert(s.getNodes().hasNext());
	}
	
			
	@Test
	public void QuorumTests() {
		
		db.save(new Animal("human"), 2);
		Animal h = db.find(Animal.class, "human", new com.cloudant.client.api.model.Params().readQuorum(2));
		assertNotNull(h);
		assertEquals("human", h.getId());
		
		db.update(h.setClass("inhuman"), 2);
		h = db.find(Animal.class, "human", new com.cloudant.client.api.model.Params().readQuorum(2));
		assertEquals("inhuman", h.getclass());
		
		db.post(new Animal("test"), 2);
		h = db.find(Animal.class, "test", new com.cloudant.client.api.model.Params().readQuorum(3));
		assertEquals("test", h.getId());
		
		
		
	} 
	
}
