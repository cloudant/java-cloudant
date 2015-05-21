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
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.DbInfo;
import com.cloudant.tests.util.Utils;

public class DBServerTest {

	private static final Log log = LogFactory.getLog(DBServerTest.class);
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
	public void dbInfo() {
		DbInfo dbInfo = db.info();
		assertNotNull(dbInfo);
	}

	@Test
	public void serverVersion() {
		String version = dbClient.serverVersion();
		assertNotNull(version);
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
