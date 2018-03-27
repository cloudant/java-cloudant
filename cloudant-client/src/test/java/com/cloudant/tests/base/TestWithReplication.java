/*
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

package com.cloudant.tests.base;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.tests.extensions.CloudantClientExtension;
import com.cloudant.tests.extensions.DatabaseExtension;
import com.cloudant.tests.extensions.MultiExtension;
import com.cloudant.tests.util.Utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TestWithReplication {

    protected static CloudantClientExtension clientResource = new CloudantClientExtension();
    protected static CloudantClient account;

    protected static DatabaseExtension.PerClass db1Resource = new DatabaseExtension.PerClass
            (clientResource);
    protected static DatabaseExtension.PerClass db2Resource = new DatabaseExtension.PerClass
            (clientResource);

    @RegisterExtension
    public static MultiExtension extensions = new MultiExtension(clientResource, db1Resource,
            db2Resource);

    protected static Database db1;
    protected static Database db2;

    protected static String db1URI;
    protected static String db2URI;

    @BeforeAll
    public static void setUp() throws Exception {
        account = clientResource.get();

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
        int conflictCount = conflicts.getRows().size();
        assertTrue(conflictCount > 0, "There should be at least 1 conflict, there were " +
                conflictCount);
    }
}
