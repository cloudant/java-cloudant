package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ReplicatorDocument;
import com.cloudant.client.api.model.Response;
import com.cloudant.client.api.model.ViewResult;


@Ignore
public class ReplicatorTest {

	private static Properties props ;
	private static Database db1;
	
	private static Database db2;
	
	private static String db1URI ;
	private static String db2URI ;
	private CloudantClient account;
	
	@Before
	public void setUp() {
		account = CloudantClientHelper.getClient();
		db1 = account.database("lightcouch-db-test", true);
		db1URI = CloudantClientHelper.SERVER_URI.toString()+ "/lightcouch-db-test";
		
	
		db2 = account.database("lightcouch-db-test-2", true);
		db1.syncDesignDocsWithDb();
		db2.syncDesignDocsWithDb();
		
		

		db2URI = CloudantClientHelper.SERVER_URI.toString()+ "/lightcouch-db-test-2";
		
	}

	@After
	public void tearDown(){
		account.deleteDB("lightcouch-db-test");
		account.deleteDB("lightcouch-db-test-2");
		account.shutdown();
	}

	@Test
	public void replication() {
		account.replicator()
				.createTarget(true)
				.source(db1URI)
				.target(db2URI)
				.save();
		
	}

	@Test
	public void replication_filteredWithQueryParams() {
    	Map<String, Object> queryParams = new HashMap<String, Object>();
    	queryParams.put("somekey1", "value 1");
    		
    	account.replicator()
				.createTarget(true)
				.source(db1URI)
				.target(db2URI)
				.filter("example/example_filter")
				.queryParams(queryParams)
				.save();
	}

	@Test
	public void replicatorDB() throws Exception {
		String version = account.serverVersion();
		if (version.startsWith("0") || version.startsWith("1.0")) {
			return; 
		}

		// trigger a replication
		Response response = account.replicator()
				.source(db1URI)
				.target(db2URI).continuous(true)
				.createTarget(true)
				.save();

		Thread.sleep(1000);

		// find all replicator docs
		List<ReplicatorDocument> replicatorDocs = account.replicator()
			.findAll();
		assertThat(replicatorDocs.size(), is(not(0))); 
		
		// find replicator doc
		ReplicatorDocument replicatorDoc = account.replicator()
				.replicatorDocId(response.getId())
				.find();

		// cancel a replication
		account.replicator()
				.replicatorDocId(replicatorDoc.getId())
				.replicatorDocRev(replicatorDoc.getRevision())
				.remove();
	}
	
	@Test
	public void replication_conflict() throws Exception {
		String docId = generateUUID();
		Foo foodb1 = new Foo(docId, "title");
		Foo foodb2 = null;
		
		foodb1 = new Foo(docId, "titleX");
		
		db1.save(foodb1); 
		
		account.replicator().source(db1URI)
		.target(db2URI).replicatorDocId(docId)
		.save();

		Thread.sleep(1000); //we need the replication to finish before continuing

		foodb2 = db2.find(Foo.class, docId); 
		foodb2.setTitle("titleY"); 
		db2.update(foodb2); 

		foodb1 = db1.find(Foo.class, docId); 
		foodb1.setTitle("titleZ"); 
		db1.update(foodb1); 
		
		account.replicator().source(db1URI)
		.target(db2URI).save();

		Thread.sleep(1000);

		ViewResult<String[], String, Foo> conflicts = db2.view("conflicts/conflict")
				.includeDocs(true).queryView(String[].class, String.class, Foo.class);
		
		assertThat(conflicts.getRows().size(), is(not(0))); 
	}
	
	private static String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
