/*
 * Copyright (C) 2011 lightcouch.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lightcouch;

/**
 * Contains the response returned from CouchDB.
 * 
 * <p>The response typically contains an <tt>id</tt> and <tt>rev</tt> values,
 * additional data might be returned such as <tt>error</tt> from Bulk request.
 * 
 * @see CouchDbClientBase#save(Object)
 * @since 0.0.2
 * @author Ahmed Yehia
 */
public class Response {
	private String id;
	private String rev;
	
	private String error;
	private String reason;

	/**
	 * @return the <tt>id</tt> of the response
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the <tt>rev</tt> of the response
	 */
	public String getRev() {
		return rev;
	}
	
	public String getError() {
		return error;
	}
	
	public String getReason() {
		return reason;
	}

	/**
	 * @return <tt>id</tt> and <tt>rev</tt> concatenated.
	 */
	@Override
	public String toString() {
		return "Response [id=" + id + ", rev=" + rev + "]";
	}
}
