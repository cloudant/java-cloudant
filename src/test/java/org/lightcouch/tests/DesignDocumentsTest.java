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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.CouchDbClient;
import org.lightcouch.DesignDocument;

public class DesignDocumentsTest {

	private static CouchDbClient dbClient;

	@BeforeClass
	public static void setUpClass() {
		dbClient = new CouchDbClient();
		dbClient.syncDesignDocsWithDb();
	}

	@AfterClass
	public static void tearDownClass() {
		dbClient.shutdown();
	}

	@Test
	public void designDoc() {
		DesignDocument designDoc = dbClient.design().getFromDesk("example");
		dbClient.design().synchronizeWithDb(designDoc);
		DesignDocument designDocFromDb = dbClient.design().getFromDb("_design/example");
		
		assertTrue(designDoc.equals(designDocFromDb));
	}
	
	@Test
	public void designDocs() {
		List<DesignDocument> designDocs = dbClient.design().getAllFromDesk();
		
		assertThat(designDocs.size(), not(0));
	}

}
