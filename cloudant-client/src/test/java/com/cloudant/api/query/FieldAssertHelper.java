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
import static org.junit.Assert.assertNotNull;

import com.cloudant.client.api.query.Field;
import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.query.Sort;
import com.cloudant.client.api.query.TextIndex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class FieldAssertHelper<T, F extends Field> {

    protected final Map<String, T> expectedFields = new HashMap<String, T>();

    public static class Json extends FieldAssertHelper<Sort.Order, JsonIndex.Field> {
        Json(Map<String, Sort.Order>... expectedJsonFields) {
            for (Map<String, Sort.Order> m : expectedJsonFields) {
                this.expectedFields.putAll(m);
            }
        }

        @Override
        protected void assertField(JsonIndex.Field field, Sort.Order order) {
            assertEquals("The order should be the same", order, field.getOrder());
        }
    }

    public static class Text extends FieldAssertHelper<TextIndex.Field.Type, TextIndex.Field> {
        Text(Map<String, TextIndex.Field.Type>... expectedTextFields) {
            for (Map<String, TextIndex.Field.Type> m : expectedTextFields) {
                this.expectedFields.putAll(m);
            }
        }

        @Override
        protected void assertField(TextIndex.Field field, TextIndex.Field.Type type) {
            assertEquals("The type should be the same", type, field.getType());
        }
    }

    public void assertFields(List<F> actualFields) {
        assertEquals("There should be the correct number of fields", expectedFields.size(),
                actualFields.size());
        for (F field : actualFields) {
            assertNotNull("The field should have a name", field.getName());
            T expected = expectedFields.remove(field.getName());
            assertNotNull("Unexpected field " + field.getName() + " found.", expected);
            assertField(field, expected);
        }
        assertEquals("All fields should be asserted.", 0, expectedFields.size());
    }

    protected abstract void assertField(F field, T type);
}
