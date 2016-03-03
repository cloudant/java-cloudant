/*
 *  Copyright (c) 2016 IBM Corp. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *   License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 */

package com.cloudant.http;

/**
 * Interface to provide the Version string for the user agent
 */
public interface Version {


    /**
     *
     * @return a version string for the user-agent e.g. java-cloudant/1.3.0 (Java 1.7 Windows 6.2 IBM)
     */
    String getUserAgentString();

}
