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
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
/*import org.lightcouch.CouchDatabase;
import org.lightcouch.CouchDbClient;
import org.lightcouch.DesignDocument;*/
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.tests.util.Utils;

public class DesignDocumentsTest {
	private static final Log log = LogFactory.getLog(DesignDocumentsTest.class);
	private static Properties props ;
	private static CloudantClient dbClient;
	private static Database db;
	

	@BeforeClass
	public static void setUpClass() {
	//	dbClient = new CouchDbClient();
		props = Utils.getProperties("cloudant.properties",log);
		dbClient = new CloudantClient(props.getProperty("cloudant.account"),
									  props.getProperty("cloudant.username"),
									  props.getProperty("cloudant.password"));
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
