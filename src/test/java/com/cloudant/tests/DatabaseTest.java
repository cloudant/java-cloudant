package com.cloudant.tests;

import static org.junit.Assert.assertNotNull;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.Replication;

import com.cloudant.ApiKey;
import com.cloudant.CloudantAccount;
import com.cloudant.Database;
import com.cloudant.Database.Permissions;
import com.cloudant.Index;
import com.cloudant.IndexField;
import com.cloudant.IndexField.SortOrder;
import com.cloudant.Shard;

public class DatabaseTest {

	private static CloudantAccount account;
	private static Database db;
	
	@BeforeClass
	public static void setUpClass() {
		Properties props = CloudantAccountTests.getProperties("cloudant.properties");
		String cloudantaccount = props.getProperty("cloudant.account");
		String userName= props.getProperty("cloudant.username");
		String password = props.getProperty("cloudant.password");
		account = new CloudantAccount(cloudantaccount,userName,password);
		
		// replciate the animals db for search tests
		Replication r = account.replication();
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
	
		
}
