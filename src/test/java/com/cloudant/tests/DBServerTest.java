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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.DbInfo;

public class DBServerTest {

	private static final Log log = LogFactory.getLog(DBServerTest.class);
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
	public void dbInfo() {
		DbInfo dbInfo = db.info();
		assertNotNull(dbInfo);
	}

	@Test
	public void serverVersion() {
		String version = account.serverVersion();
		assertNotNull(version);
	}

	@Test
	public void allDBs() {
		List<String> allDbs = account.getAllDbs();
		assertThat(allDbs.size(), is(not(0)));
	}

	@Test
	public void ensureFullCommit() {
		db.ensureFullCommit();
	}

	@Test
	public void uuids() {
		List<String> uuids = account.uuids(10);
		assertThat(uuids.size(), is(10));
	}
}
