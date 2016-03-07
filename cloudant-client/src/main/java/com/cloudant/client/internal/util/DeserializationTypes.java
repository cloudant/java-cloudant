/*
 * Copyright (c) 2016 IBM Corp. All rights reserved.
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

package com.cloudant.client.internal.util;

import com.cloudant.client.api.model.Index;
import com.cloudant.client.api.model.Permissions;
import com.cloudant.client.api.model.Shard;
import com.cloudant.client.api.model.Task;
import com.cloudant.client.org.lightcouch.Response;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class DeserializationTypes {

    public static final Type SHARDS = new TypeToken<List<Shard>>() {
    }.getType();

    public static final Type INDICES = new TypeToken<List<Index>>() {
    }.getType();

    public static final TypeToken<Map<String, EnumSet<Permissions>>> PERMISSIONS_MAP_TOKEN = new
            TypeToken<Map<String, EnumSet<Permissions>>>
                    () {
            };
    public static final Type PERMISSIONS_MAP = PERMISSIONS_MAP_TOKEN.getType();

    public static final Type PERMISSIONS = new TypeToken<EnumSet<Permissions>>() {
    }.getType();

    public static final Type TASKS = new TypeToken<List<Task>>() {
    }.getType();

    public static final Type STRINGS = new TypeToken<List<String>>() {
    }.getType();

    public static final Type LC_RESPONSES = new TypeToken<List<Response>>() {
    }.getType();
}
