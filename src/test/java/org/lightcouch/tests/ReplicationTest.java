/*
 * Copyright (C) 2011 lightcouch.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lightcouch.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.lightcouch.CouchDatabase;
import org.lightcouch.CouchDbClient;
import org.lightcouch.ReplicationResult;
import org.lightcouch.ReplicatorDocument;
import org.lightcouch.Response;
import org.lightcouch.ViewResult;
import org.lightcouch.ReplicationResult.ReplicationHistory;

@Ignore
public class ReplicationTest {
	
	private static CouchDbClient dbClient;
	private static CouchDatabase db1;
	
	private static CouchDbClient dbClient2;
	private static CouchDatabase db2;
	
	@BeforeClass
	public static void setUpClass() {
		dbClient = new CouchDbClient();
		db1 = dbClient.database("lightcouch-db-test", true);
		
		dbClient2 = new CouchDbClient("couchdb-2.properties");
		db2 = dbClient.database("lightcouch-db-test-2", true);
		
		db1.syncDesignDocsWithDb();
		db2.syncDesignDocsWithDb();
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
		dbClient2.shutdown();
	}

	@Test
	public void replication() {
		ReplicationResult result = dbClient.replication()
				.createTarget(true)
				.source(db1.getDBUri().toString())
				.target(db2.getDBUri().toString())
				.trigger();

		List<ReplicationHistory> histories = result.getHistories();
		assertThat(histories.size(), not(0));
	}

	@Test
	public void replication_filteredWithQueryParams() {
    	Map<String, Object> queryParams = new HashMap<String, Object>();
    	queryParams.put("somekey1", "value 1");
    	
		dbClient.replication()
				.createTarget(true)
				.source(db1.getDBUri().toString())
				.target(db2.getDBUri().toString())
				.filter("example/example_filter")
				.queryParams(queryParams)
				.trigger();
	}

	@Test
	public void replicatorDB() {
		String version = dbClient.serverVersion();
		if (version.startsWith("0") || version.startsWith("1.0")) {
			return; 
		}

		// trigger a replication
		Response response = dbClient.replicator()
				.source(db1.getDBUri().toString())
				.target(db2.getDBUri().toString()).continuous(true)
				.createTarget(true)
				.save();
		
		// find all replicator docs
		List<ReplicatorDocument> replicatorDocs = dbClient.replicator()
			.findAll();
		assertThat(replicatorDocs.size(), is(not(0))); 
		
		// find replicator doc
		ReplicatorDocument replicatorDoc = dbClient.replicator()
				.replicatorDocId(response.getId())
				.find();

		// cancel a replication
		dbClient.replicator()
				.replicatorDocId(replicatorDoc.getId())
				.replicatorDocRev(replicatorDoc.getRevision())
				.remove();
	}
	
	@Test
	public void replication_conflict() {
		String docId = generateUUID();
		Foo foodb1 = new Foo(docId, "title");
		Foo foodb2 = null;
		
		foodb1 = new Foo(docId, "titleX");
		
		db1.save(foodb1); 
		
		dbClient.replication().source(db1.getDBUri().toString())
				.target(db2.getDBUri().toString()).trigger();

		foodb2 = db2.find(Foo.class, docId); 
		foodb2.setTitle("titleY"); 
		db2.update(foodb2); 

		foodb1 = db1.find(Foo.class, docId); 
		foodb1.setTitle("titleZ"); 
		db1.update(foodb1); 

		dbClient.replication().source(db1.getDBUri().toString())
				.target(db2.getDBUri().toString()).trigger();

		ViewResult<String[], String, Foo> conflicts = db2.view("conflicts/conflict")
				.includeDocs(true).queryView(String[].class, String.class, Foo.class);
		
		assertThat(conflicts.getRows().size(), is(not(0))); 
	}
	
	private static String generateUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}
