/*
 * Copyright (C) 2011 lightcouch.org
 *
 * Modifications for this distribution by IBM Cloudant, Copyright (c) 2015 IBM Corp.
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
package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.cloudant.client.api.Changes;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ChangesResult;
import com.cloudant.client.api.model.ChangesResult.Row;
import com.cloudant.client.api.model.DbInfo;
import com.cloudant.client.api.model.Response;
import com.google.gson.JsonObject;

public class ChangeNotificationsTest {
	
	private static final Log log = LogFactory.getLog(ChangeNotificationsTest.class);
	private static Database db;
	private CloudantClient account;

	

	@Before
	public  void setUp() {
		account = CloudantClientHelper.getClient();
		db = account.database("lightcouch-db-test", true);
	}

	@After
	public void tearDown(){
		account.deleteDB("lightcouch-db-test");
		account.shutdown();
	}

	
	@Test
	public void changes_normalFeed() {
		db.save(new Foo()); 

		ChangesResult changes = db.changes()
				.includeDocs(true)
				.limit(1)
				.getChanges();
		
		List<ChangesResult.Row> rows = changes.getResults();
		
		for (Row row : rows) {
			List<ChangesResult.Row.Rev> revs = row.getChanges();
			String docId = row.getId();
			JsonObject doc = row.getDoc();
			
			assertNotNull(revs);
			assertNotNull(docId);
			assertNotNull(doc);
		}
		
		assertThat(rows.size(), is(1));
	}

	@Test
	public void changes_continuousFeed() {
		db.save(new Foo()); 

		DbInfo dbInfo = db.info();
		String since = dbInfo.getUpdateSeq();

		Changes changes = db.changes()
				.includeDocs(true)
				.since(since)
				.heartBeat(30000)
				.continuousChanges();

		Response response = db.save(new Foo());

		while (changes.hasNext()) {
			ChangesResult.Row feed = changes.next();
			final JsonObject feedObject = feed.getDoc();
			final String docId = feed.getId();
			
			assertEquals(response.getId(), docId);
			assertNotNull(feedObject);
			
			changes.stop();
		}
	}
}
