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
 * <P>
 * CouchDbException class for HTTP 412 precondition failed status codes
 * </P>
 * <P>
 * An example of when CouchDB will return a 412 status code is for a
 * <a target="_blank" href="http://docs.couchdb.org/en/1.6.1/api/database/common.html#put--db">
 * <code>PUT /{db}</code></a> when the DB already exists.
 * </P>
 */
public class PreconditionFailedException extends CouchDbException {
    public PreconditionFailedException(String message) {
        super(message, HttpURLConnection.HTTP_PRECON_FAILED);
    }
}
