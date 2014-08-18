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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lightcouch.CouchDatabase;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbInfo;

public class DBServerTest {

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
	public void dbInfo() {
		CouchDbInfo dbInfo = db.info();
		assertNotNull(dbInfo);
	}

	@Test
	public void serverVersion() {
		String version = dbClient.serverVersion();
		assertNotNull(version);
	}

	@Test
	public void compactDb() {
		db.compact();
	}

	@Test
	public void allDBs() {
		List<String> allDbs = dbClient.getAllDbs();
		assertThat(allDbs.size(), is(not(0)));
	}

	@Test
	public void ensureFullCommit() {
		db.ensureFullCommit();
	}

	@Test
	public void uuids() {
		List<String> uuids = dbClient.uuids(10);
		assertThat(uuids.size(), is(10));
	}
}
