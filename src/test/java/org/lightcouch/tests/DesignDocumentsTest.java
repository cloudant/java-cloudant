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

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.CouchDatabase;
import org.lightcouch.CouchDbClient;
import org.lightcouch.DesignDocument;

public class DesignDocumentsTest {

	private static CouchDbClient dbClient;
	private static CouchDatabase db;
	

	@BeforeClass
	public static void setUpClass() {
		dbClient = new CouchDbClient();
		db = dbClient.database("lightcouch-db-test", true);
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
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
