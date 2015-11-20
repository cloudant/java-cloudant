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
 * Contains the response returned from CouchDB.
 * <p>
 * The response typically contains an <tt>id</tt> and <tt>rev</tt> values,
 * additional data might be returned such as <tt>error</tt> from Bulk request.
 *
 * @author Ganesh K Choudhary
 * @see com.cloudant.client.api.Database#save(Object)
 * @since 0.0.1
 */
public class Response {
    private com.cloudant.client.org.lightcouch.Response response;

    public Response() {
        this.response = new com.cloudant.client.org.lightcouch.Response();
    }

    public Response(com.cloudant.client.org.lightcouch.Response response) {
        this.response = response;
    }

    /**
     * @return the <tt>id</tt> of the response
     */
    public String getId() {
        return response.getId();
    }

    /**
     * @return the <tt>rev</tt> of the response
     */
    public String getRev() {
        return response.getRev();
    }

    /**
     * @return error string returned by the server or {@code null}
     */
    public String getError() {
        return response.getError();
    }

    /**
     * @return reason phrase returned by the server or {@code null}
     */
    public String getReason() {
        return response.getReason();
    }

    /**
     * @return the HTTP status code returned by the server or 0 if no code was available
     */
    public int getStatusCode() {
        return response.getStatusCode();
    }

    /**
     * @return <tt>id</tt> and <tt>rev</tt> concatenated.
     */
    public String toString() {
        return response.toString();
    }


}
