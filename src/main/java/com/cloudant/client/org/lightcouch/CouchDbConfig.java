/*
 * Copyright (C) 2011 lightcouch.org
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

package com.cloudant.client.org.lightcouch;

import static com.cloudant.client.org.lightcouch.internal.CouchDbUtil.assertNotEmpty;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Provides configuration to client instance.
 *
 * @author Ahmed Yehia
 */
class CouchDbConfig {
    private static final Logger log = Logger.getLogger(CouchDbConfig.class.getCanonicalName());

    private Properties properties = new Properties();
    private CouchDbProperties dbProperties;

    public CouchDbConfig(CouchDbProperties dbProperties) {
        assertNotEmpty(dbProperties, "Properties");
        assertNotEmpty(dbProperties.getProtocol(), "Protocol");
        assertNotEmpty(dbProperties.getHost(), "Host");
        assertNotEmpty(dbProperties.getPort(), "Port");
        this.dbProperties = dbProperties;
    }

    public CouchDbProperties getProperties() {
        return dbProperties;
    }

    private String getProperty(String key, boolean isRequired) {
        String property = properties.getProperty(key);
        if (property == null && isRequired) {
            String msg = String.format("A required property is missing. Key: %s", key);
            log.severe(msg);
            throw new IllegalStateException(msg);
        } else {
            return (property != null && property.length() != 0) ? property.trim() : null;
        }
    }
}
