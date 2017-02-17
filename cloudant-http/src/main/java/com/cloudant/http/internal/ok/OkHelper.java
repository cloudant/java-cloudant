/*
 * Copyright © 2016 IBM Corp. All rights reserved.
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

package com.cloudant.http.internal.ok;

import java.util.logging.Logger;

/**
 * This class should only reflectively try to load the OkUrlFactory and then provide a boolean
 * answer to {@link #isOkUsable()}. Addition of other methods is not advised as it may cause loading
 * of okhttp classes that break the optional dependency relationship.
 */
public class OkHelper {

    private static final Logger log = Logger.getLogger(OkHelper.class.getCanonicalName());
    private final static boolean okUsable;
    
    static {
        Class<?> okFactoryClass;
        try {
            okFactoryClass = Class.forName("okhttp3.OkUrlFactory");
        } catch (Throwable t) {
            log.fine("Failed to load okhttp: " + t.getMessage());
            okFactoryClass = null;
        }
        okUsable = (okFactoryClass != null);
    }

    public static boolean isOkUsable() {
        return okUsable;
    }

}
