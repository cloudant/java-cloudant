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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cloudant.client.api.query.Field;
import com.cloudant.client.api.query.JsonIndex;
import com.cloudant.client.api.query.Sort;
import com.cloudant.client.api.query.TextIndex;
import com.cloudant.client.api.query.Type;

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
            assertEquals(order, field.getOrder(), "The order should be the same");
        }
    }

    public static class Text extends FieldAssertHelper<Type, TextIndex.Field> {
        Text(Map<String, Type>... expectedTextFields) {
            for (Map<String, Type> m : expectedTextFields) {
                this.expectedFields.putAll(m);
            }
        }

        @Override
        protected void assertField(TextIndex.Field field, Type type) {
            assertEquals(type, field.getType(), "The type should be the same");
        }
    }

    public void assertFields(List<F> actualFields) {
        assertEquals(expectedFields.size(),
                actualFields.size(), "There should be the correct number of fields");
        for (F field : actualFields) {
            assertNotNull("The field should have a name", field.getName());
            T expected = expectedFields.remove(field.getName());
            assertNotNull(expected, "Unexpected field " + field.getName() + " found.");
            assertField(field, expected);
        }
        assertEquals(0, expectedFields.size(), "All fields should be asserted.");
    }

    protected abstract void assertField(F field, T type);
}
