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

public class Animal {

    private String _id;
    @SuppressWarnings("unused")
    private String _rev;
    private String Class;

    public void setId(String s) {
        _id = s;
    }

    public String getId() {
        return _id;
    }

    Animal(String _id) {
        super();
        this._id = _id;
        Class = "mammal";
    }


    public String getclass() {
        return Class;
    }


    public Animal setClass(String class1) {
        Class = class1;
        return this;
    }

    Animal() {
        super();

    }


}
