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

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.DesignDocument;

public class DesignDocumentsTest {
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
	public void designDocSync() {
		DesignDocument designDoc = db.design().getFromDesk("example");
		db.design().synchronizeWithDb(designDoc);
	}
	
	@Test
	public void designDocCompare() {
		DesignDocument designDoc1 = db.design().getFromDesk("example");
		db.design().synchronizeWithDb(designDoc1);
		
		DesignDocument designDoc11 = db.design().getFromDb("_design/example");
		
		assertEquals(designDoc1, designDoc11);
	}
	
	@Test
	public void designDocs() {
		List<DesignDocument> designDocs = db.design().getAllFromDesk();
		db.syncDesignDocsWithDb();
		
		assertThat(designDocs.size(), not(0));
	}

}
