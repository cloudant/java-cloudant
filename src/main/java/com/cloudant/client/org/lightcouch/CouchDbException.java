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

/**
 * Base runtime exception class.
 * @see NoDocumentException
 * @see DocumentConflictException
 * @author Ahmed Yehia
 */
public class CouchDbException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CouchDbException(String message) {
        super(message);
    }

    public CouchDbException(Throwable cause) {
        super(cause);
    }

    public CouchDbException(String message, Throwable cause) {
        super(message, cause);
    }

    private int statusCode;
    private String error = "Unknown error";
    private String reason = "Unkown reason";

    public CouchDbException() {};

    public CouchDbException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CouchDbException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
