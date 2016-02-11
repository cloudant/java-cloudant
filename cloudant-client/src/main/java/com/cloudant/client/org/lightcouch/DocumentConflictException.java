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

import java.net.HttpURLConnection;

/**
 * Thrown when a conflict is detected during save or update.
 *
 * @author Ahmed Yehia
 * @since 0.0.2
 */
public class DocumentConflictException extends CouchDbException {

    private static final long serialVersionUID = 1L;

    public DocumentConflictException(String message) {
        super(message, HttpURLConnection.HTTP_CONFLICT);
    }
}
