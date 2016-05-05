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

package com.cloudant.client.org.lightcouch;

/**
 * <P>
 * CouchDbException class for HTTP 429 too many requests status code
 * </P>
 * <P>
 * This exception is thrown when a 429 response is received and the client is not going to make
 * any additional attempts.
 * </P>
 */
public class TooManyRequestsException extends CouchDbException {
    public TooManyRequestsException(String message) {
        super(message, 429);
    }
}
