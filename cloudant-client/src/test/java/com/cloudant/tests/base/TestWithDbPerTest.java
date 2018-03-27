/*
 * Copyright © 2018 IBM Corp. All rights reserved.
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

import com.cloudant.tests.extensions.DatabaseExtension;
import com.cloudant.tests.extensions.MultiExtension;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

// Base class for tests which require a new DB for each test method
public class TestWithDbPerTest extends TestWithDb {

    protected static DatabaseExtension.PerTest dbResource = new DatabaseExtension.PerTest
            (clientResource);

    @RegisterExtension
    protected static MultiExtension perTestExtensions = new MultiExtension(clientResource,
            dbResource);

    @BeforeEach
    public void testWithDbBeforeEach() {
        db = dbResource.get();
    }

    @BeforeAll
    public static void testWithDbBeforeAll() {
        account = clientResource.get();
    }

}
