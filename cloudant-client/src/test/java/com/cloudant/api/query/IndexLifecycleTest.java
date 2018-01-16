/*
 * Copyright © 2017 IBM Corp. All rights reserved.
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

package com.cloudant.api.query;

import static org.junit.Assert.assertEquals;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.query.Field;
import com.cloudant.client.api.query.Index;
import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.query.Sort;
import com.cloudant.client.api.query.TextIndex;
import com.cloudant.client.api.query.Type;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.util.CloudantClientResource;
import com.cloudant.tests.util.DatabaseResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import java.util.Collections;
import java.util.List;


// This really requires Couch2.0 + text index support, but we don't have a way of expressing that
@Category(RequiresCloudant.class)

/**
 * Index lifecycle test.
 * Before create indexes
 * Test list indexes
 * After delete indexes
 */
public class IndexLifecycleTest {

    private CloudantClientResource clientResource = new CloudantClientResource();
    private DatabaseResource dbResource = new DatabaseResource(clientResource);
    @Rule
    public RuleChain chain = RuleChain.outerRule(clientResource).around(dbResource);

    private Database db;

    @Before
    public void createIndexes() throws Exception {
        db = dbResource.get();

        // Create a JSON index
        db.createIndex(JsonIndex.builder()
                .designDocument("indexlifecycle")
                .name("testjson")
                .asc("testDefaultAsc", "testAsc")
                .definition()
        );

        // Create a text index
        db.createIndex(TextIndex.builder()
                .designDocument("indexlifecycle")
                .name("testtext")
                .string("testString")
                .number("testNumber")
                .bool("testBoolean")
                .definition()
        );
    }

    @Test
    public void listIndexes() throws Exception {
        {
            // JSON index
            List<JsonIndex> jIndexes = db.listIndexes().jsonIndexes();
            assertEquals("There should be one JSON index", 1, jIndexes.size());
            JsonIndex jIndex = jIndexes.get(0);
            assertEquals("The ddoc should be correct", "_design/indexlifecycle", jIndex
                    .getDesignDocumentID());
            assertEquals("The name should be correct", "testjson", jIndex.getName());
            assertEquals("The type should be correct", "json", jIndex.getType());
            List<JsonIndex.Field> fields = jIndex.getFields();
            assertEquals("There should be two fields", 2, fields.size());
            // Field assertions
            new FieldAssertHelper.Json(Collections.singletonMap("testDefaultAsc", Sort.Order.ASC)
                    , Collections.singletonMap("testAsc", Sort.Order.ASC)).assertFields(fields);
        }

        {
            // Text index
            List<TextIndex> tIndexes = db.listIndexes().textIndexes();
            assertEquals("There should be one text index", 1, tIndexes.size());
            TextIndex tIndex = tIndexes.get(0);
            assertEquals("The ddoc should be correct", "_design/indexlifecycle", tIndex
                    .getDesignDocumentID());
            assertEquals("The name should be correct", "testtext", tIndex.getName());
            assertEquals("The type should be correct", "text", tIndex.getType());
            List<TextIndex.Field> fields = tIndex.getFields();
            assertEquals("There should be three fields", 3, fields.size());
            // Field assertions
            new FieldAssertHelper.Text(Collections.singletonMap("testString", Type.STRING),
                    Collections.singletonMap("testNumber", Type.NUMBER),
                    Collections.singletonMap("testBoolean", Type.BOOLEAN)).assertFields(fields);
        }

        {
            // All indexes
            List<Index<Field>> allIndexes = db.listIndexes().allIndexes();
            assertEquals("There should be three total indexes", 3, allIndexes.size());
            for (Index<Field> index : allIndexes) {
                if (index.getType().equals("special")) {
                    assertEquals("The name should be correct", "_all_docs", index.getName());
                    assertEquals("There should be 1 field", 1, index.getFields().size());
                    assertEquals("There field should be called _id", "_id", index.getFields().get(0)
                            .getName());
                }
            }
        }
    }

    @After
    public void deleteIndexes() throws Exception {
        db.deleteIndex("testjson", "indexlifecycle", "json");
        db.deleteIndex("testtext", "indexlifecycle", "text");
        List<Index<Field>> allIndexes = db.listIndexes().allIndexes();
        assertEquals("There should be one (special) index", 1, allIndexes.size());
    }
}
