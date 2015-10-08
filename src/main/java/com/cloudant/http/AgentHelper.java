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

package com.cloudant.http;

import com.cloudant.client.org.lightcouch.CouchDbClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class AgentHelper {

    /**
     * The {@code User-Agent} identifier for this client.
     */
    public static final String USER_AGENT;

    //init the string, based on a properties file or fallback to some defaults
    static {
        //default to an unknown version java-cloudant-default, but hopefully generate something
        //more specific from a properties file
        String ua = "java-cloudant";
        String version = "unknown";
        final URL url = CouchDbClient.class.getClassLoader().getResource("client.properties");
        final Properties properties = new Properties();
        InputStream propStream = null;
        try {
            properties.load((propStream = url.openStream()));
            ua = properties.getProperty("user.agent.name", ua);
            version = properties.getProperty("user.agent.version", version);
        } catch (Exception ex) {
            //swallow exception and keep using default values
        } finally {
            if (propStream != null) {
                try {
                    propStream.close();
                } catch (IOException e) {
                    //can't do anything else
                }
            }
        }
        USER_AGENT = String.format("%s/%s [Java (%s; %s; %s) %s; %s; %s]",
                ua,
                version,
                System.getProperty("os.arch"),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("java.vendor"),
                System.getProperty("java.version"),
                System.getProperty("java.runtime.version"));
    }

}
