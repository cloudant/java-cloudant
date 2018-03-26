/*
 * Copyright (C) 2011 lightcouch.org
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.cloudant.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cloudant.client.api.model.DbInfo;
import com.cloudant.client.api.model.MetaInformation;
import com.cloudant.test.main.RequiresDB;
import com.cloudant.tests.base.TestWithDbPerClass;

import org.junit.jupiter.api.Test;

import java.util.List;

@RequiresDB
public class DBServerTest extends TestWithDbPerClass {

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
    public void metaInformation() {
        MetaInformation mi = account.metaInformation();
        assertNotNull(mi);
        assertNotNull(mi.getCouchdb());
        assertNotNull(mi.getVendor());
        assertNotNull(mi.getVersion());
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
