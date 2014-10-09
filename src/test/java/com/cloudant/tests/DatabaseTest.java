package com.cloudant.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.Replication;

import com.cloudant.ApiKey;
import com.cloudant.CloudantClient;
import com.cloudant.Database;
import com.cloudant.Database.Permissions;
import com.cloudant.Shard;

public class DatabaseTest {

	private static CloudantClient account;
	private static Database db;
	
	@BeforeClass
	public static void setUpClass() {
		Properties props = CloudantClientTests.getProperties("cloudant.properties");
		String cloudantaccount = props.getProperty("cloudant.account");
		String userName= props.getProperty("cloudant.username");
		String password = props.getProperty("cloudant.password");
		account = new CloudantClient(cloudantaccount,userName,password);
		
		// replciate the animals db for search tests
		com.cloudant.Replication r = account.replication();
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
		ApiKey key = account.generateApiKey();
		EnumSet<Permissions> p = EnumSet.<Permissions>of( Permissions._writer, Permissions._reader);
		db.setPermissions(key.getKey(), p);
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
		Animal h = db.find(Animal.class, "human", new com.cloudant.Params().readQuorum(2));
		assertNotNull(h);
		assertEquals("human", h.getId());
		
		db.update(h.setClass("inhuman"), 2);
		h = db.find(Animal.class, "human", new com.cloudant.Params().readQuorum(2));
		assertEquals("inhuman", h.getclass());
		
		db.post(new Animal("test"), 2);
		h = db.find(Animal.class, "test", new com.cloudant.Params().readQuorum(3));
		assertEquals("test", h.getId());
		
		
		
	}
}
