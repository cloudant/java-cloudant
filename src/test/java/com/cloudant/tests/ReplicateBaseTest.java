/*
 * Copyright (c) 2015 IBM Corp. All rights reserved.
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
import static org.junit.Assert.assertThat;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;
import com.cloudant.tests.util.Utils;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

public class ReplicateBaseTest {

    @ClassRule
    public static CloudantClientResource clientResource = new CloudantClientResource();
    protected CloudantClient account = clientResource.get();

    @Rule
    public DatabaseResource db1Resource = new DatabaseResource(clientResource);
    @Rule
    public DatabaseResource db2Resource = new DatabaseResource(clientResource);

    protected Database db1;

    protected Database db2;

    protected static String db1URI;
    protected static String db2URI;

    @Before
    public void setUp() throws Exception {

        db1 = db1Resource.get();
        db1URI = db1Resource.getDbURIWithUserInfo();
        Utils.putDesignDocs(db1);

        db2 = db2Resource.get();
        db2URI = db2Resource.getDbURIWithUserInfo();
        Utils.putDesignDocs(db2);
    }

    protected void assertConflictsNotZero(Database db) throws Exception {
        ViewResponse<Key.ComplexKey, String> conflicts = db.getViewRequestBuilder
                ("conflicts", "conflict").newRequest(Key.Type.COMPLEX, String.class).build()
                .getResponse();
        assertThat(conflicts.getRows().size(), is(not(0)));
    }
}
