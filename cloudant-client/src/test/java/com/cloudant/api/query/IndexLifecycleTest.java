/*
 * Copyright © 2017, 2018 IBM Corp. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudant.client.api.query.Field;
import com.cloudant.client.api.query.Index;
import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.query.Sort;
import com.cloudant.client.api.query.TextIndex;
import com.cloudant.client.api.query.Type;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.base.TestWithDbPerTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;


// This really requires Couch2.0 + text index support, but we don't have a way of expressing that
@RequiresCloudant

/**
 * Index lifecycle test.
 * Before create indexes
 * Test list indexes
 * After delete indexes
 */
public class IndexLifecycleTest extends TestWithDbPerTest {

    @BeforeEach
    public void createIndexes() throws Exception {

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
            assertEquals(1, jIndexes.size(), "There should be one JSON index");
            JsonIndex jIndex = jIndexes.get(0);
            assertEquals("_design/indexlifecycle", jIndex
                    .getDesignDocumentID(), "The ddoc should be correct");
            assertEquals("testjson", jIndex.getName(), "The name should be correct");
            assertEquals("json", jIndex.getType(), "The type should be correct");
            List<JsonIndex.Field> fields = jIndex.getFields();
            assertEquals(2, fields.size(), "There should be two fields");
            // Field assertions
            new FieldAssertHelper.Json(Collections.singletonMap("testDefaultAsc", Sort.Order.ASC)
                    , Collections.singletonMap("testAsc", Sort.Order.ASC)).assertFields(fields);
        }

        {
            // Text index
            List<TextIndex> tIndexes = db.listIndexes().textIndexes();
            assertEquals(1, tIndexes.size(), "There should be one text index");
            TextIndex tIndex = tIndexes.get(0);
            assertEquals("_design/indexlifecycle", tIndex
                    .getDesignDocumentID(), "The ddoc should be correct");
            assertEquals("testtext", tIndex.getName(), "The name should be correct");
            assertEquals("text", tIndex.getType(), "The type should be correct");
            List<TextIndex.Field> fields = tIndex.getFields();
            assertEquals(3, fields.size(), "There should be three fields");
            // Field assertions
            new FieldAssertHelper.Text(Collections.singletonMap("testString", Type.STRING),
                    Collections.singletonMap("testNumber", Type.NUMBER),
                    Collections.singletonMap("testBoolean", Type.BOOLEAN)).assertFields(fields);
        }

        {
            // All indexes
            List<Index<Field>> allIndexes = db.listIndexes().allIndexes();
            assertEquals(3, allIndexes.size(), "There should be three total indexes");
            for (Index<Field> index : allIndexes) {
                if (index.getType().equals("special")) {
                    assertEquals("_all_docs", index.getName(), "The name should be correct");
                    assertEquals(1, index.getFields().size(), "There should be 1 field");
                    assertEquals("_id", index.getFields().get(0)
                            .getName(), "There field should be called _id");
                }
            }
        }
    }

    @AfterEach
    public void deleteIndexes() throws Exception {
        db.deleteIndex("testjson", "indexlifecycle", "json");
        db.deleteIndex("testtext", "indexlifecycle", "text");
        List<Index<Field>> allIndexes = db.listIndexes().allIndexes();
        assertEquals(1, allIndexes.size(), "There should be one (special) index");
    }
}
