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

package com.cloudant.client.api.model;

/**
 * Encapsulates an ApiKey response from Cloudant
 *
 * @author Mario Briggs
 * @since 0.0.1
 */
public class ApiKey {

    //@SerializedName("db_name")
    private String key;
    //@SerializedName("doc_count")
    private String password;


    /**
     * Return the Apikey
     *
     * @return String
     */
    public String getKey() {
        return key;
    }


    /**
     * Return the password associated with the ApiKey
     *
     * @return String
     */
    public String getPassword() {
        return password;
    }

    /**
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "key:" + key + " password:" + password;
    }


}
