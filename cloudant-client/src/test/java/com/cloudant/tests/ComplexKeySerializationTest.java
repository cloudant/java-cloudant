/*
 * Copyright (c) 2015 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain key copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.tests;

import static org.junit.Assert.assertEquals;

import com.cloudant.client.api.views.Key;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

public class ComplexKeySerializationTest {

    private static final Gson gson = new GsonBuilder().registerTypeAdapter(Key.ComplexKey.class,
            new Key.ComplexKeyDeserializer()).create();
    public static final String STR_CMPLX_KEY_JSON = "[\"dog\",\"cat\",\"mouse\"]";
    public static final String BOOL_CMPLX_KEY_JSON = "[true,false,true]";
    public static final String NUM_CMPLX_KEY_JSON = "[1,12,15.0,99999999999999]";
    public static final String MIXED_CMPLX_KEY_JSON = "[\"dog\",true,1,15.0,null,-3]";

    @Test
    public void stringVarArgsSerialization() {
        Key.ComplexKey key = Key.complex("dog", "cat", "mouse");
        assertEquals(STR_CMPLX_KEY_JSON, gson.toJson(key));
    }

    @Test
    public void stringAdditionSerialization() {
        Key.ComplexKey key = Key.complex("dog");
        key.add("cat").add("mouse");
        assertEquals(STR_CMPLX_KEY_JSON, gson.toJson(key));
    }

    @Test
    public void stringDeserialization() {
        Key.ComplexKey expectedKey = Key.complex("dog", "cat", "mouse");
        Key.ComplexKey deserializedKey = gson.fromJson(STR_CMPLX_KEY_JSON, Key.ComplexKey.class);
        assertEquals(STR_CMPLX_KEY_JSON, deserializedKey.toJson());
        assertEquals(expectedKey, deserializedKey);
    }

    @Test
    public void booleanVarArgsSerialization() {
        Key.ComplexKey key = Key.complex(true, false, true);
        assertEquals(BOOL_CMPLX_KEY_JSON, gson.toJson(key));
    }

    @Test
    public void booleanAdditionSerialization() {
        Key.ComplexKey key = Key.complex(true);
        key.add(false).add(true);
        assertEquals(BOOL_CMPLX_KEY_JSON, gson.toJson(key));
    }

    @Test
    public void booleanDeserialization() {
        Key.ComplexKey expectedKey = Key.complex(true, false, true);
        Key.ComplexKey deserializedKey = gson.fromJson(BOOL_CMPLX_KEY_JSON, Key.ComplexKey.class);
        assertEquals(BOOL_CMPLX_KEY_JSON, deserializedKey.toJson());
        assertEquals(expectedKey, deserializedKey);
    }

    @Test
    public void numberVarArgsSerialization() {
        Key.ComplexKey key = Key.complex(1, 12, 15.0, 99999999999999l);
        assertEquals(NUM_CMPLX_KEY_JSON, gson.toJson(key));
    }

    @Test
    public void numberAdditionSerialization() {
        Key.ComplexKey key = Key.complex(1);
        key.add(12).add(15.0).add(99999999999999l);
        assertEquals(NUM_CMPLX_KEY_JSON, gson.toJson(key));
    }

    @Test
    public void numberDeserialization() {
        Key.ComplexKey expectedKey = Key.complex(1, 12, 15.0, 99999999999999l);
        Key.ComplexKey deserializedKey = gson.fromJson(NUM_CMPLX_KEY_JSON, Key.ComplexKey.class);
        assertEquals(NUM_CMPLX_KEY_JSON, deserializedKey.toJson());
        assertEquals(expectedKey, deserializedKey);
    }

    @Test
    public void mixedAdditionSerialization() {
        Key.ComplexKey key = Key.complex("dog");
        key.add(true).add(1).add(15.0).add((String) null).add(-3);
        assertEquals(MIXED_CMPLX_KEY_JSON, gson.toJson(key));
    }

    @Test
    public void mixedDeserialization() {
        Key.ComplexKey expectedKey = Key.complex("dog").add(true).add(1).add(15.0).add((String)
                null).add(-3);
        Key.ComplexKey deserializedKey = gson.fromJson(MIXED_CMPLX_KEY_JSON, Key.ComplexKey.class);
        assertEquals(MIXED_CMPLX_KEY_JSON, deserializedKey.toJson());
        assertEquals(expectedKey, deserializedKey);
    }

}
