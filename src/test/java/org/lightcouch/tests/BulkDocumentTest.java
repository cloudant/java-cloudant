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
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.Response;
import com.cloudant.tests.util.Utils;
import com.google.gson.JsonObject;

public class BulkDocumentTest {

	private static final Log log = LogFactory.getLog(BulkDocumentTest.class);
	private static CloudantClient dbClient;
	private static Properties props ;
	private static Database db;

	@BeforeClass
	public static void setUpClass() {
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
	public void bulkModifyDocs() {
		List<Object> newDocs = new ArrayList<Object>();
		newDocs.add(new Foo());
		newDocs.add(new JsonObject());

	//	boolean allOrNothing = true;
		
		// allorNothing is not supported in cloudant
	//	List<Response> responses = db.bulk(newDocs, allOrNothing);
		List<Response> responses = db.bulk(newDocs);
		
		assertThat(responses.size(), is(2));
	}

	@Test
	public void bulkDocsRetrieve() {
		Response r1 = db.save(new Foo());
		Response r2 = db.save(new Foo());
		
		List<String> keys = Arrays.asList(new String[] { r1.getId(), r2.getId() });
		
		List<Foo> docs = db.view("_all_docs")
				.includeDocs(true)
				.keys(keys)
				.query(Foo.class);
		
		assertThat(docs.size(), is(2));
	}

}
